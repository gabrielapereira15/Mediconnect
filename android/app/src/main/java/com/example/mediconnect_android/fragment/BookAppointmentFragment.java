package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
import com.example.mediconnect_android.databinding.FragmentBookAppointmentBinding;
import com.example.mediconnect_android.databinding.ViewDateChipBinding;
import com.example.mediconnect_android.databinding.ViewSlotBinding;
import com.example.mediconnect_android.model.DoctorDetails;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.WhenLabel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Book a visit: pick a day, then a time (board P05).
 *
 * The screen this replaces listed every working day the doctor had, each
 * with its own wrapped row of chips, so a patient scrolled past a fortnight
 * to reach the afternoon they wanted and had no idea which times were
 * already gone. Here the day comes first and only that day's times are
 * shown, which is also what leaves room to show the times somebody else has
 * taken: a patient can then see how fast a day is filling rather than
 * guessing why only four remain.
 */
public class BookAppointmentFragment extends Fragment {

    public static final String ARG_DOCTOR_ID = "doctorId";
    public static final String ARG_DOCTOR_NAME = "doctorName";
    public static final String ARG_DOCTOR_SPECIALTY = "doctorSpecialty";

    /** Names the booking flow's first step, so the flow can pop itself. */
    public static final String BACK_STACK = "booking";

    private static final String STATE_DAY = "selectedDay";
    private static final String STATE_SLOT = "selectedSlotId";

    private static final DateTimeFormatter DAY_TITLE =
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_SHORT =
            DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    /** Slots before this belong under "Morning". */
    private static final LocalTime NOON = LocalTime.NOON;

    /** How many times fit across the grid. */
    private static final int SLOT_COLUMNS = 3;

    private FragmentBookAppointmentBinding binding;
    private final DoctorClient doctorClient = new DoctorClientImpl();
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();

    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;

    private final List<Day> days = new ArrayList<>();
    private LocalDate selectedDay;
    private String selectedSlotId;
    private LocalTime selectedTime;

    /** A day and the slots the clinic still holds on it, taken or free. */
    private static final class Day {
        final LocalDate date;
        final List<DoctorDetails.Schedule.TimeSlot> slots;

        Day(LocalDate date, List<DoctorDetails.Schedule.TimeSlot> slots) {
            this.date = date;
            this.slots = slots;
        }

        boolean hasFreeSlot() {
            return slots.stream().anyMatch(DoctorDetails.Schedule.TimeSlot::isAvailable);
        }
    }

    /**
     * Opens booking for one doctor.
     *
     * Every way in goes through here so the back-stack entry is named the
     * same each time; the confirmation screen relies on that name to remove
     * the steps behind it.
     */
    public static void open(FragmentManager fragmentManager, String doctorId,
                            String doctorName, String doctorSpecialty) {
        BookAppointmentFragment fragment = new BookAppointmentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCTOR_ID, doctorId);
        args.putString(ARG_DOCTOR_NAME, doctorName);
        args.putString(ARG_DOCTOR_SPECIALTY, doctorSpecialty);
        fragment.setArguments(args);

        FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, fragment, BACK_STACK);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            doctorId = args.getString(ARG_DOCTOR_ID);
            doctorName = WhenLabel.doctorName(args.getString(ARG_DOCTOR_NAME));
            doctorSpecialty = args.getString(ARG_DOCTOR_SPECIALTY);
        }
        if (savedInstanceState != null) {
            selectedDay = parseDate(savedInstanceState.getString(STATE_DAY));
            selectedSlotId = savedInstanceState.getString(STATE_SLOT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookAppointmentBinding.inflate(inflater, container, false);

        binding.stateView.setContentView(binding.content);
        binding.doctorName.setText(doctorName);
        binding.doctorSpecialty.setText(doctorSpecialty);
        binding.doctorInitials.setText(WhenLabel.initials(doctorName));
        binding.readMoreLink.setOnClickListener(v -> toggleDescription());
        binding.waitlistCard.setOnClickListener(v -> askToJoinWaitlist());
        binding.btnNext.setOnClickListener(v -> openReview());
        bindFooter();

        load();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.booking_title);
    }

    // ---- loading --------------------------------------------------------

    private void load() {
        binding.stateView.showLoading();
        Background.run(
                () -> doctorClient.getDoctor(doctorId),
                details -> {
                    if (binding == null) {
                        return;
                    }
                    if (details == null) {
                        binding.stateView.showError(this::load);
                        return;
                    }
                    bindDoctor(details);
                    bindDays(details);
                },
                error -> {
                    if (binding != null) {
                        binding.stateView.showError(this::load);
                    }
                });
    }

    private void bindDoctor(DoctorDetails details) {
        // The list screen passed a name and a specialty so the header can be
        // filled before the request lands; the detail response wins wherever
        // it has something better.
        if (details.getName() != null && !details.getName().isEmpty()) {
            doctorName = WhenLabel.doctorName(details.getName());
            binding.doctorName.setText(doctorName);
            binding.doctorInitials.setText(WhenLabel.initials(doctorName));
        }

        // The specialty can arrive empty — a waitlist offer knows the
        // doctor but not what they do — so the line is built from whatever
        // is actually there rather than rendering " · 18 yrs".
        String years = details.getExperienceYears();
        boolean hasSpecialty = doctorSpecialty != null && !doctorSpecialty.trim().isEmpty();
        boolean hasYears = years != null && !years.trim().isEmpty();

        if (hasSpecialty && hasYears) {
            binding.doctorSpecialty.setText(
                    getString(R.string.doctors_years, doctorSpecialty, years));
        } else if (hasSpecialty) {
            binding.doctorSpecialty.setText(doctorSpecialty);
        } else if (hasYears) {
            binding.doctorSpecialty.setText(getString(R.string.doctors_years_only, years));
        } else {
            binding.doctorSpecialty.setVisibility(View.GONE);
        }

        String description = details.getDescription();
        boolean hasDescription = description != null && !description.trim().isEmpty();
        show(binding.doctorDescription, hasDescription);
        show(binding.readMoreLink, hasDescription);
        if (hasDescription) {
            binding.doctorDescription.setText(description);
        }

        bindRating(details);
    }

    /** A rating with its review count, or the words rather than a zero. */
    private void bindRating(DoctorDetails details) {
        if (details.getScore() == null) {
            binding.doctorStar.setImageResource(R.drawable.ic_star_outline);
            binding.doctorStar.setImageTintList(ContextCompat.getColorStateList(
                    requireContext(), R.color.md_on_surface_variant));
            binding.doctorRating.setText(R.string.doctors_no_reviews);
            return;
        }

        int reviews = details.getReviewCount() == null ? 0 : details.getReviewCount();
        String score = String.format(Locale.ENGLISH, "%.1f", details.getScore());
        binding.doctorRating.setText(getResources().getQuantityString(
                R.plurals.doctors_rating, reviews, score, reviews));
        binding.doctorRatingGroup.setContentDescription(
                getString(R.string.cd_rating, score, reviews));
    }

    private void toggleDescription() {
        boolean collapsed = binding.doctorDescription.getMaxLines() == 3;
        binding.doctorDescription.setMaxLines(collapsed ? Integer.MAX_VALUE : 3);
        binding.readMoreLink.setText(collapsed
                ? R.string.read_less_link
                : R.string.read_more_link);
    }

    // ---- days -----------------------------------------------------------

    private void bindDays(DoctorDetails details) {
        days.clear();
        if (details.getSchedule() != null) {
            for (DoctorDetails.Schedule schedule : details.getSchedule()) {
                LocalDate date = parseDate(schedule.getDate());
                if (date != null && schedule.getTimes() != null && !schedule.getTimes().isEmpty()) {
                    days.add(new Day(date, schedule.getTimes()));
                }
            }
        }

        if (days.isEmpty()) {
            binding.stateView.showEmpty(R.drawable.ic_calendar,
                    R.string.booking_day_none_title, R.string.booking_day_none);
            return;
        }

        bindDateRange();
        // Land on the first day that can actually be booked rather than
        // simply the first the clinic has opened: a patient should not have
        // to discover that today is full before the screen is any use.
        if (findDay(selectedDay) == null) {
            selectedDay = days.stream()
                    .filter(Day::hasFreeSlot)
                    .findFirst()
                    .orElse(days.get(0))
                    .date;
        }
        buildDateStrip();
        bindSlots();
        bindFooter();
        binding.stateView.showContent();
    }

    private void bindDateRange() {
        String from = days.get(0).date.format(MONTH);
        String to = days.get(days.size() - 1).date.format(MONTH);
        binding.dateRange.setText(from.equals(to)
                ? from
                : getString(R.string.booking_date_range, from, to));
    }

    private void buildDateStrip() {
        binding.dateStrip.removeAllViews();
        int gap = getResources().getDimensionPixelSize(R.dimen.space_sm);

        for (Day day : days) {
            ViewDateChipBinding chip = ViewDateChipBinding.inflate(
                    getLayoutInflater(), binding.dateStrip, false);
            chip.dateWeekday.setText(day.date.format(WEEKDAY));
            chip.dateDay.setText(String.valueOf(day.date.getDayOfMonth()));
            show(chip.dateNote, !day.hasFreeSlot());

            // A full day stays in the strip and stays tappable. Dropping it
            // makes the week skip a day for no visible reason, and a patient
            // scanning for the soonest opening needs to see that Friday was
            // considered and is gone.
            chip.getRoot().setSelected(day.date.equals(selectedDay));
            chip.getRoot().setContentDescription(day.date.format(DAY_TITLE));
            chip.getRoot().setOnClickListener(v -> selectDay(day.date));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(gap);
            binding.dateStrip.addView(chip.getRoot(), params);
        }
    }

    private void selectDay(LocalDate date) {
        if (date.equals(selectedDay)) {
            return;
        }
        selectedDay = date;
        // The time belonged to the day before it.
        selectedSlotId = null;
        selectedTime = null;
        buildDateStrip();
        bindSlots();
        bindFooter();
    }

    // ---- times ----------------------------------------------------------

    private void bindSlots() {
        Day day = findDay(selectedDay);
        if (day == null) {
            return;
        }

        // Two things at once. A slot taken while the patient was on the
        // review screen must not stay selected — selected draws over
        // disabled, so the footer would still offer Continue on a time that
        // is gone. And only the id survives a rotation or a switch to dark
        // mode, so the time behind it is found again here; without it the
        // slot drew as chosen while the footer still said "Pick a time".
        DoctorDetails.Schedule.TimeSlot chosen = selectedSlotId == null
                ? null
                : day.slots.stream()
                        .filter(s -> selectedSlotId.equals(s.getId()) && s.isAvailable())
                        .findFirst()
                        .orElse(null);
        if (chosen == null) {
            selectedSlotId = null;
            selectedTime = null;
        } else if (selectedTime == null) {
            selectedTime = parseTime(chosen.getTime());
        }

        binding.dayTitle.setText(day.date.format(DAY_TITLE));
        binding.morningSlots.removeAllViews();
        binding.afternoonSlots.removeAllViews();

        int morning = 0;
        int afternoon = 0;
        for (DoctorDetails.Schedule.TimeSlot slot : day.slots) {
            LocalTime at = parseTime(slot.getTime());
            if (at == null) {
                continue;
            }
            if (at.isBefore(NOON)) {
                addSlot(binding.morningSlots, slot, at, morning++);
            } else {
                addSlot(binding.afternoonSlots, slot, at, afternoon++);
            }
        }

        show(binding.morningLabel, morning > 0);
        show(binding.morningSlots, morning > 0);
        show(binding.afternoonLabel, afternoon > 0);
        show(binding.afternoonSlots, afternoon > 0);
        show(binding.dayFullNote, !day.hasFreeSlot());
    }

    private void addSlot(GridLayout grid, DoctorDetails.Schedule.TimeSlot slot,
                         LocalTime at, int indexInGrid) {
        ViewSlotBinding view = ViewSlotBinding.inflate(getLayoutInflater(), grid, false);
        String label = at.format(TIME);
        view.slot.setText(label);
        view.slot.setEnabled(slot.isAvailable());
        view.slot.setSelected(slot.getId() != null && slot.getId().equals(selectedSlotId));
        // Announced with its state: a screen reader hearing eight times in
        // a row has no other way to tell which of them can be booked.
        view.slot.setContentDescription(getString(slot.isAvailable()
                ? R.string.cd_slot_available
                : R.string.cd_slot_taken, label));
        view.slot.setOnClickListener(v -> selectSlot(slot, at));

        int column = indexInGrid % SLOT_COLUMNS;
        int gap = getResources().getDimensionPixelSize(R.dimen.space_sm);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        // Wrap rather than fix: at a large font size a fixed cell cuts the
        // time through the middle.
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(column, 1f);
        params.rowSpec = GridLayout.spec(indexInGrid / SLOT_COLUMNS);
        params.setMargins(0, 0, column == SLOT_COLUMNS - 1 ? 0 : gap, gap);
        grid.addView(view.getRoot(), params);
    }

    private void selectSlot(DoctorDetails.Schedule.TimeSlot slot, LocalTime at) {
        selectedSlotId = slot.getId();
        selectedTime = at;
        bindSlots();
        bindFooter();
    }

    private void bindFooter() {
        boolean chosen = selectedSlotId != null && selectedTime != null && selectedDay != null;
        binding.btnNext.setEnabled(chosen);

        if (selectedDay == null) {
            binding.footerDay.setText(R.string.booking_pick_day);
            binding.footerTime.setText("");
            return;
        }
        binding.footerDay.setText(selectedDay.format(DAY_SHORT));
        binding.footerTime.setText(chosen
                ? selectedTime.format(TIME)
                : getString(R.string.booking_pick_time));
    }

    // ---- moving on ------------------------------------------------------

    private void openReview() {
        if (selectedSlotId == null || selectedDay == null || selectedTime == null) {
            return;
        }
        FragmentUtils.loadFragment(getParentFragmentManager(), R.id.flFragment,
                BookingReviewFragment.of(doctorName, doctorSpecialty,
                        selectedSlotId, selectedDay, selectedTime));
    }

    /**
     * Joining the waitlist here means "anything before the day I am looking
     * at", which is the same promise the clinic already keeps for a patient
     * who holds an appointment they would rather bring forward.
     */
    private void askToJoinWaitlist() {
        if (selectedDay == null) {
            return;
        }
        LocalDate before = selectedDay;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.waitlist_sooner_title)
                .setMessage(getString(R.string.waitlist_sooner_confirm,
                        doctorName, before.format(DAY_SHORT)))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.waitlist_join_confirm, (dialog, which) -> Background.run(
                        () -> waitlistClient.join(email(), doctorId, before.toString()),
                        joined -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.waitlist_joined)),
                        error -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server))))
                .show();
    }

    private String email() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        return prefs.getString("email", "");
    }

    // ---- odds and ends --------------------------------------------------

    private static void show(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private Day findDay(@Nullable LocalDate date) {
        if (date == null) {
            return null;
        }
        return days.stream().filter(d -> d.date.equals(date)).findFirst().orElse(null);
    }

    @Nullable
    private static LocalDate parseDate(@Nullable String iso) {
        try {
            return iso == null ? null : LocalDate.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static LocalTime parseTime(@Nullable String iso) {
        try {
            return iso == null ? null : LocalTime.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_DAY, selectedDay == null ? null : selectedDay.toString());
        outState.putString(STATE_SLOT, selectedSlotId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
