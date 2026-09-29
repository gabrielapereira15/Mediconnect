package com.example.mediconnect_android.fragment;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.FragmentBookingReviewBinding;
import com.example.mediconnect_android.model.BookingResult;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.WhenLabel;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Review and confirm (board P06).
 *
 * One screen where the flow used to have two. The first of them asked for a
 * name, a date of birth and a phone number before it showed what was being
 * booked — three things the clinic already holds for the patient doing the
 * booking, which is nearly every booking. Those fields appear here only
 * when the visit is for somebody else.
 *
 * The booking is also the point where a patient learns their record will be
 * read, which is why the banner is here rather than buried in the terms.
 */
public class BookingReviewFragment extends Fragment {

    private static final String TAG = "BookingReview";

    private static final String ARG_DOCTOR_NAME = "doctorName";
    private static final String ARG_DOCTOR_SPECIALTY = "doctorSpecialty";
    private static final String ARG_SLOT_ID = "slotId";
    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";

    private FragmentBookingReviewBinding binding;
    private final AppointmentClient appointmentClient = new AppointmentClientImpl();
    private final HealthClient healthClient = new HealthClientImpl();

    private String doctorName;
    private String doctorSpecialty;
    private String slotId;
    private LocalDate date;
    private LocalTime time;

    private boolean forSomeoneElse;

    /** Opens the screen for one chosen slot. */
    public static BookingReviewFragment of(String doctorName, String doctorSpecialty,
                                           String slotId, LocalDate date, LocalTime time) {
        BookingReviewFragment fragment = new BookingReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCTOR_NAME, doctorName);
        args.putString(ARG_DOCTOR_SPECIALTY, doctorSpecialty);
        args.putString(ARG_SLOT_ID, slotId);
        args.putString(ARG_DATE, date.toString());
        args.putString(ARG_TIME, time.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        doctorName = args.getString(ARG_DOCTOR_NAME, "");
        doctorSpecialty = args.getString(ARG_DOCTOR_SPECIALTY, "");
        slotId = args.getString(ARG_SLOT_ID);
        date = LocalDate.parse(args.getString(ARG_DATE));
        time = LocalTime.parse(args.getString(ARG_TIME));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingReviewBinding.inflate(inflater, container, false);

        bindAppointment();
        bindWhoFor();
        bindHealthBanner();

        binding.changeDoctor.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.recordBanner.setOnClickListener(v -> FragmentUtils.loadFragment(
                getParentFragmentManager(), R.id.flFragment, new HealthRecordFragment()));
        binding.btnConfirm.setOnClickListener(v -> confirm());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.review_title);
    }

    private void bindAppointment() {
        binding.doctorName.setText(doctorName);
        binding.doctorSpecialty.setText(doctorSpecialty);
        binding.doctorInitials.setText(WhenLabel.initials(doctorName));
        binding.appointmentWhen.setText(
                WhenLabel.whenWords(requireContext(), LocalDateTime.of(date, time)));
    }

    // ---- who the visit is for -------------------------------------------

    private void bindWhoFor() {
        SharedPreferences prefs = prefs();
        String name = (prefs.getString("first_name", "") + " "
                + prefs.getString("last_name", "")).trim();
        String dob = prefs.getString("dob", "");

        binding.choiceMe.choiceTitle.setText(R.string.review_for_me);
        binding.choiceMe.choiceSub.setText(dob.isEmpty() ? name : name + " · " + dob);
        binding.choiceMe.choiceSub.setVisibility(name.isEmpty() ? View.GONE : View.VISIBLE);
        binding.choiceOther.choiceTitle.setText(R.string.review_for_someone);
        binding.choiceOther.choiceSub.setText(R.string.review_for_someone_sub);

        binding.choiceMe.getRoot().setOnClickListener(v -> setForSomeoneElse(false));
        binding.choiceOther.getRoot().setOnClickListener(v -> setForSomeoneElse(true));
        binding.otherDob.setOnClickListener(v -> pickDateOfBirth());

        setForSomeoneElse(forSomeoneElse);
    }

    private void setForSomeoneElse(boolean other) {
        forSomeoneElse = other;
        binding.choiceMe.choiceRadio.setChecked(!other);
        binding.choiceOther.choiceRadio.setChecked(other);
        binding.otherDetails.setVisibility(other ? View.VISIBLE : View.GONE);
    }

    private void pickDateOfBirth() {
        Calendar today = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> binding.otherDob.setText(
                        String.format(Locale.ENGLISH, "%d-%02d-%02d", year, month + 1, day)),
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    // ---- what the doctor will see ---------------------------------------

    private void bindHealthBanner() {
        binding.recordShared.setText(getString(R.string.review_record_shared,
                doctorName, getString(R.string.review_record_loading)));

        Background.run(
                () -> healthClient.getEntries(email()),
                entries -> {
                    if (binding != null && entries != null) {
                        bindHealthCounts(entries);
                    }
                },
                error -> {
                    // The banner still has to say the record is shared, even
                    // when we cannot say how much of it there is.
                    if (binding != null) {
                        binding.recordShared.setText(getString(
                                R.string.review_record_shared_unknown, doctorName));
                    }
                });
    }

    private void bindHealthCounts(List<HealthEntry> entries) {
        long allergies = count(entries, HealthEntry.TYPE_ALLERGY);
        long medications = count(entries, HealthEntry.TYPE_MEDICATION);
        long conditions = count(entries, HealthEntry.TYPE_CONDITION);

        if (allergies + medications + conditions == 0) {
            binding.recordShared.setText(getString(R.string.review_record_empty, doctorName));
            return;
        }
        String counts = getString(R.string.home_record_counts,
                (int) allergies, (int) medications, (int) conditions);
        binding.recordShared.setText(
                getString(R.string.review_record_shared, doctorName, counts));
    }

    private long count(List<HealthEntry> entries, String type) {
        return entries.stream().filter(e -> type.equals(e.getType()) && e.isActive()).count();
    }

    // ---- confirming ------------------------------------------------------

    private void confirm() {
        if (!binding.bookingTerms.isChecked()) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.review_terms_required));
            return;
        }
        if (forSomeoneElse && text(binding.otherName).isEmpty()) {
            binding.otherName.setError(getString(R.string.review_name_required));
            binding.otherName.requestFocus();
            return;
        }

        String payload = payload();
        if (payload == null) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.review_failed));
            return;
        }

        binding.btnConfirm.setEnabled(false);
        Background.run(
                () -> appointmentClient.createAppointment(payload),
                result -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnConfirm.setEnabled(true);
                    onBookingResult(result);
                },
                error -> {
                    if (binding != null) {
                        binding.btnConfirm.setEnabled(true);
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server));
                    }
                });
    }

    private void onBookingResult(BookingResult result) {
        if (result.isBooked()) {
            // The steps behind this are spent. Left on the stack, the back
            // gesture walks the patient into a review screen for an
            // appointment they already hold, and Confirm there books it a
            // second time.
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager.popBackStack(BookAppointmentFragment.BACK_STACK,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            String id = result.getAppointment() == null
                    ? null
                    : result.getAppointment().getId();
            FragmentUtils.loadFragment(fragmentManager, R.id.flFragment,
                    BookingConfirmedFragment.of(id, doctorName, date, time));
            return;
        }
        if (result.isSlotTaken()) {
            // The slot went while this screen was open. Saying so and
            // sending them back to the times is the only useful answer.
            DialogUtils.showMessageDialog(getContext(), getString(R.string.review_slot_taken));
            getParentFragmentManager().popBackStack();
            return;
        }
        DialogUtils.showMessageDialog(getContext(), getString(R.string.review_failed));
    }

    @Nullable
    private String payload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("patientEmail", email());
            payload.put("scheduleTimeId", slotId);

            String note = text(binding.note);
            if (forSomeoneElse) {
                payload.put("bookedForName", text(binding.otherName));
                payload.put("bookedForDateOfBirth", text(binding.otherDob));
                payload.put("bookedForPhone", text(binding.otherPhone));
                payload.put("bookedForNotes", note);
            } else if (!note.isEmpty()) {
                payload.put("bookedForNotes", note);
            }
        } catch (JSONException e) {
            // Built rather than formatted: a name with a quote or a
            // backslash in it used to produce a body the server could not
            // read.
            Log.e(TAG, "Could not build the booking payload", e);
            return null;
        }
        return payload.toString();
    }

    // ---- plumbing --------------------------------------------------------

    private static String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
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
