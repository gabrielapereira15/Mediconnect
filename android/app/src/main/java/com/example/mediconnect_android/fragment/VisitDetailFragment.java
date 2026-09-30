package com.example.mediconnect_android.fragment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
import com.example.mediconnect_android.databinding.FragmentVisitDetailBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.WhenLabel;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Your visit (board P09).
 *
 * On the day itself a patient has one question — how long until it starts,
 * and is there anything left to do — so the hero answers the first and the
 * checklist the second. The screen it replaces was a mock check-in form
 * with no appointment attached to it: it could not say when the visit was,
 * which doctor it was with, or whether the form had been sent.
 */
public class VisitDetailFragment extends Fragment {

    private static final String ARG_ID = "appointmentId";
    private static final String ARG_STARTS_AT = "startsAt";
    private static final String ARG_DOCTOR_ID = "doctorId";
    private static final String ARG_DOCTOR_NAME = "doctorName";
    private static final String ARG_DOCTOR_SPECIALTY = "doctorSpecialty";
    private static final String ARG_FORM_SUBMITTED_AT = "formSubmittedAt";
    private static final String ARG_CHECKED_IN_AT = "checkedInAt";
    private static final String ARG_ATTENDANCE = "attendanceConfirmedAt";

    private FragmentVisitDetailBinding binding;
    private final AppointmentClient appointmentClient = new AppointmentClientImpl();
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();

    private String appointmentId;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private LocalDateTime startsAt;
    private String formSubmittedAt;
    private String checkedInAt;
    private String attendanceConfirmedAt;

    /**
     * Opens the screen for one appointment.
     *
     * The appointment travels as its fields rather than as an object,
     * because a fragment's arguments have to survive the process being
     * killed and come back the same.
     */
    public static VisitDetailFragment of(Appointment appointment) {
        VisitDetailFragment fragment = new VisitDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, appointment.getId());
        args.putString(ARG_STARTS_AT, appointment.getStartsAt());
        args.putString(ARG_FORM_SUBMITTED_AT, appointment.getFormSubmittedAt());
        args.putString(ARG_CHECKED_IN_AT, appointment.getCheckedInAt());
        args.putString(ARG_ATTENDANCE, appointment.getAttendanceConfirmedAt());
        if (appointment.getDoctor() != null) {
            args.putString(ARG_DOCTOR_ID, appointment.getDoctor().getId());
            args.putString(ARG_DOCTOR_NAME, appointment.getDoctor().getName());
            args.putString(ARG_DOCTOR_SPECIALTY, appointment.getDoctor().getSpecialty());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        appointmentId = args.getString(ARG_ID);
        doctorId = args.getString(ARG_DOCTOR_ID);
        doctorName = WhenLabel.doctorName(args.getString(ARG_DOCTOR_NAME));
        doctorSpecialty = args.getString(ARG_DOCTOR_SPECIALTY, "");
        startsAt = WhenLabel.parse(args.getString(ARG_STARTS_AT)).orElse(null);
        formSubmittedAt = args.getString(ARG_FORM_SUBMITTED_AT);
        checkedInAt = args.getString(ARG_CHECKED_IN_AT);
        attendanceConfirmedAt = args.getString(ARG_ATTENDANCE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVisitDetailBinding.inflate(inflater, container, false);

        bindWhen();
        bindChecklist();
        bindDoctor();

        // Sending the form pops straight back here, to an instance holding
        // the arguments it was opened with — so it hears about it rather
        // than going on saying the form is pending.
        getParentFragmentManager().setFragmentResultListener(
                PreAppointmentFormFragment.RESULT_SENT, getViewLifecycleOwner(),
                (key, result) -> {
                    formSubmittedAt = result.getString(
                            PreAppointmentFormFragment.RESULT_SUBMITTED_AT);
                    bindChecklist();
                });

        binding.directions.setOnClickListener(v -> openDirections());
        binding.btnCheckIn.setOnClickListener(v -> checkIn());
        binding.btnWaitlist.setOnClickListener(v -> askToJoinWaitlist());
        binding.btnReschedule.setOnClickListener(v -> reschedule());
        binding.btnCancel.setOnClickListener(v -> confirmCancel());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.visit_title);
    }

    // ---- when ------------------------------------------------------------

    private void bindWhen() {
        if (startsAt == null) {
            return;
        }
        binding.visitDay.setText(WhenLabel.relativeDayWords(requireContext(), startsAt));
        binding.visitTime.setText(WhenLabel.timeWords(startsAt));
        binding.visitCountdown.setText(countdownWords());
        bindCheckInState();
    }

    /**
     * "Starts in 1 h 42 min", or how long ago it started.
     *
     * A countdown is the reason to open this screen at all on the day, and
     * it is the one thing the server cannot say for the patient: it depends
     * on the minute they are looking.
     */
    private String countdownWords() {
        // Counting in hours only helps on the day. "Starts in 41 h 44 min"
        // is not how anyone thinks about a visit that is two days away, so
        // further out it counts in days instead.
        long days = ChronoUnit.DAYS.between(LocalDate.now(), startsAt.toLocalDate());
        if (days > 1) {
            return getResources().getQuantityString(R.plurals.visit_in_days_plural,
                    (int) days, (int) days, doctorName, doctorSpecialty);
        }

        Duration until = Duration.between(LocalDateTime.now(), startsAt);
        boolean ahead = !until.isNegative();
        Duration size = ahead ? until : until.negated();

        long hours = size.toHours();
        long minutes = size.toMinutes() % 60;
        String length = hours > 0
                ? getString(R.string.visit_hours_minutes, hours, minutes)
                : getString(R.string.visit_minutes, Math.max(minutes, 1));

        return getString(ahead ? R.string.visit_starts_in : R.string.visit_started,
                length, doctorName, doctorSpecialty);
    }

    private void bindCheckInState() {
        boolean isToday = startsAt.toLocalDate().isEqual(LocalDate.now());
        boolean checkedIn = checkedInAt != null && !checkedInAt.isEmpty();

        // The prompt only belongs on the day. Before then it invites a tap
        // that the clinic would have to ignore.
        binding.checkInNote.setVisibility(isToday ? View.VISIBLE : View.GONE);
        binding.btnCheckIn.setVisibility(isToday ? View.VISIBLE : View.GONE);
        binding.footer.setVisibility(isToday ? View.VISIBLE : View.GONE);

        if (!isToday) {
            return;
        }
        if (checkedIn) {
            binding.checkInNoteIcon.setImageResource(R.drawable.ic_check);
            binding.checkInNoteText.setText(R.string.visit_checked_in);
            binding.btnCheckIn.setEnabled(false);
            WhenLabel.parse(checkedInAt).ifPresent(at -> binding.btnCheckIn.setText(
                    getString(R.string.visit_checked_in_at, WhenLabel.timeWords(at))));
            return;
        }
        binding.checkInNoteIcon.setImageResource(R.drawable.ic_pin);
        binding.checkInNoteText.setText(R.string.visit_check_in_prompt);
        binding.btnCheckIn.setEnabled(true);
        binding.btnCheckIn.setText(R.string.visit_check_in);
    }

    private void checkIn() {
        binding.btnCheckIn.setEnabled(false);
        Background.run(
                () -> appointmentClient.checkIn(appointmentId),
                appointment -> {
                    if (binding == null) {
                        return;
                    }
                    if (appointment == null) {
                        binding.btnCheckIn.setEnabled(true);
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.visit_check_in_day_only));
                        return;
                    }
                    checkedInAt = appointment.getCheckedInAt();
                    bindCheckInState();
                },
                error -> {
                    if (binding != null) {
                        binding.btnCheckIn.setEnabled(true);
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server));
                    }
                });
    }

    // ---- before you go in -------------------------------------------------

    private void bindChecklist() {
        boolean formIn = formSubmittedAt != null && !formSubmittedAt.isEmpty();

        binding.rowForm.rowTitle.setText(R.string.visit_form_row);
        if (formIn) {
            done(binding.rowForm.rowIcon);
            binding.rowForm.rowSub.setText(getString(R.string.visit_form_row_sent,
                    WhenLabel.parse(formSubmittedAt)
                            .map(at -> WhenLabel.whenWords(requireContext(), at))
                            .orElse("")));
            binding.rowForm.rowAction.setText(R.string.visit_form_view);
        } else {
            todo(binding.rowForm.rowIcon, R.drawable.ic_form);
            binding.rowForm.rowSub.setText(R.string.visit_form_row_todo);
            binding.rowForm.rowAction.setText(R.string.visit_form_fill);
        }
        binding.rowForm.rowAction.setVisibility(View.VISIBLE);
        // A sent form opens as what was sent; editing it is a step further.
        View.OnClickListener openForm = v -> show(formIn
                ? FormAnswersFragment.of(appointmentId)
                : PreAppointmentFormFragment.of(appointmentId));
        binding.rowForm.rowAction.setOnClickListener(openForm);
        binding.rowForm.visitRow.setOnClickListener(openForm);

        bindAttendance();
        bindHealthCard();
    }

    /**
     * "I will be there" (board P09's second checklist item).
     *
     * Asked of the patient days ahead, so the front desk can tell somebody
     * who is running late from somebody who is not coming. Once given it
     * is shown as done rather than offered again.
     */
    private void bindAttendance() {
        boolean confirmed = attendanceConfirmedAt != null && !attendanceConfirmedAt.isEmpty();
        if (confirmed) {
            done(binding.rowAttendance.rowIcon);
            binding.rowAttendance.rowTitle.setText(R.string.visit_attendance_done);
            binding.rowAttendance.rowSub.setText(R.string.visit_attendance_done_sub);
            binding.rowAttendance.rowAction.setVisibility(View.GONE);
            binding.rowAttendance.visitRow.setOnClickListener(null);
            return;
        }
        todo(binding.rowAttendance.rowIcon, R.drawable.ic_calendar_check);
        binding.rowAttendance.rowTitle.setText(R.string.visit_attendance);
        binding.rowAttendance.rowSub.setText(R.string.visit_attendance_sub);
        binding.rowAttendance.rowAction.setVisibility(View.VISIBLE);
        binding.rowAttendance.rowAction.setText(R.string.visit_attendance_confirm);
        binding.rowAttendance.rowAction.setOnClickListener(v -> confirmAttendance());
        binding.rowAttendance.visitRow.setOnClickListener(v -> confirmAttendance());
    }

    private void confirmAttendance() {
        binding.rowAttendance.rowAction.setEnabled(false);
        Background.run(
                () -> appointmentClient.confirmAttendance(appointmentId),
                appointment -> {
                    if (binding == null) {
                        return;
                    }
                    binding.rowAttendance.rowAction.setEnabled(true);
                    if (appointment == null) {
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.visit_attendance_failed));
                        return;
                    }
                    attendanceConfirmedAt = appointment.getAttendanceConfirmedAt();
                    bindAttendance();
                },
                error -> {
                    if (binding != null) {
                        binding.rowAttendance.rowAction.setEnabled(true);
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server));
                    }
                });
    }

    private void bindHealthCard() {
        SharedPreferences prefs = prefs();
        String number = prefs.getString("health_card_number", "");
        String province = prefs.getString("health_card_province", "");

        todo(binding.rowHealthCard.rowIcon, R.drawable.ic_card);
        binding.rowHealthCard.rowTitle.setText(R.string.visit_health_card);

        if (number.isEmpty()) {
            binding.rowHealthCard.rowSub.setText(R.string.visit_health_card_missing);
            binding.rowHealthCard.rowAction.setVisibility(View.GONE);
            // Straight to the card, not to the top of a long form.
            binding.rowHealthCard.visitRow.setOnClickListener(
                    v -> show(EditProfileFragment.forHealthCard()));
            return;
        }
        // Only the last four, the way the card is read back at a desk.
        String last4 = number.length() <= 4 ? number : number.substring(number.length() - 4);
        binding.rowHealthCard.rowSub.setText(
                getString(R.string.visit_health_card_sub, province, last4));
        binding.rowHealthCard.rowAction.setVisibility(View.GONE);
        binding.rowHealthCard.visitRow.setOnClickListener(null);
    }

    private void done(android.widget.ImageView icon) {
        icon.setImageResource(R.drawable.ic_check);
        icon.setBackgroundResource(R.drawable.tile_success_soft);
        icon.setImageTintList(ContextCompat.getColorStateList(
                requireContext(), R.color.md_success));
    }

    private void todo(android.widget.ImageView icon, int drawable) {
        icon.setImageResource(drawable);
        icon.setBackgroundResource(R.drawable.tile_brand_soft);
        icon.setImageTintList(ContextCompat.getColorStateList(
                requireContext(), R.color.md_on_primary_container));
    }

    // ---- details ----------------------------------------------------------

    private void bindDoctor() {
        binding.doctorName.setText(doctorName);
        binding.doctorSpecialty.setText(doctorSpecialty);
        binding.doctorInitials.setText(WhenLabel.initials(doctorName));
    }

    /**
     * Hands the clinic's address to whatever maps app the patient uses.
     *
     * A geo: query rather than a link to one provider, so the phone's own
     * default answers it.
     */
    private void openDirections() {
        String address = getString(R.string.clinic_name) + ", " + getString(R.string.clinic_address);
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=" + Uri.encode(address)));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.visit_no_maps));
        }
    }

    // ---- changing or leaving ----------------------------------------------

    private void askToJoinWaitlist() {
        if (startsAt == null || doctorId == null) {
            return;
        }
        LocalDate before = startsAt.toLocalDate();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.waitlist_join)
                .setMessage(getString(R.string.waitlist_join_body, doctorName))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.waitlist_join_confirm, (dialog, which) -> Background.run(
                        () -> waitlistClient.join(email(), doctorId, before.toString()),
                        joined -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.waitlist_joined)),
                        error -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server))))
                .show();
    }

    /**
     * Rescheduling is a cancel and a rebook, so it says so before it does
     * the first half.
     */
    private void reschedule() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.visit_reschedule)
                .setMessage(R.string.visit_reschedule_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.visit_reschedule_confirm, (dialog, which) -> Background.run(
                        () -> appointmentClient.cancelAppointment(appointmentId),
                        cancelled -> {
                            if (!Boolean.TRUE.equals(cancelled)) {
                                DialogUtils.showMessageDialog(getContext(),
                                        getString(R.string.visit_cancel_failed));
                                return;
                            }
                            BookAppointmentFragment.open(getParentFragmentManager(),
                                    doctorId, doctorName, doctorSpecialty);
                        },
                        error -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server))))
                .show();
    }

    private void confirmCancel() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.visit_cancel)
                .setMessage(R.string.visit_cancel_body)
                .setNegativeButton(R.string.visit_cancel_keep, null)
                .setPositiveButton(R.string.visit_cancel_confirm, (dialog, which) -> Background.run(
                        () -> appointmentClient.cancelAppointment(appointmentId),
                        cancelled -> {
                            if (!Boolean.TRUE.equals(cancelled)) {
                                DialogUtils.showMessageDialog(getContext(),
                                        getString(R.string.visit_cancel_failed));
                                return;
                            }
                            getParentFragmentManager().popBackStack();
                        },
                        error -> DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server))))
                .show();
    }

    // ---- plumbing ----------------------------------------------------------

    private void show(Fragment fragment) {
        com.example.mediconnect_android.util.FragmentUtils.loadFragment(
                getParentFragmentManager(), R.id.flFragment, fragment);
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
    }

    private String email() {
        return prefs().getString("email", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
