package com.example.mediconnect_android.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentBookingConfirmedBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.WhenLabel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Booked (board P07).
 *
 * The flow used to end in an alert dialog saying "Booking Confirmed" over
 * the screen the patient had just filled in, and then drop them into their
 * medical history for no stated reason. A booking has things that follow
 * it, and this is the one moment a patient is certain to be looking: the
 * form the doctor reads beforehand comes first, because it is the only one
 * of the three that needs them to do anything.
 */
public class BookingConfirmedFragment extends Fragment {

    private static final String ARG_DOCTOR_NAME = "doctorName";
    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";

    /** How long to block out in a calendar, for want of a duration on the slot. */
    private static final int ASSUMED_MINUTES = 30;

    private FragmentBookingConfirmedBinding binding;

    private String doctorName;
    private LocalDate date;
    private LocalTime time;

    public static BookingConfirmedFragment of(String doctorName, LocalDate date, LocalTime time) {
        BookingConfirmedFragment fragment = new BookingConfirmedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCTOR_NAME, doctorName);
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
        date = LocalDate.parse(args.getString(ARG_DATE));
        time = LocalTime.parse(args.getString(ARG_TIME));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingConfirmedBinding.inflate(inflater, container, false);

        binding.confirmedDoctor.setText(doctorName);
        binding.confirmedWhen.setText(
                WhenLabel.whenWords(requireContext(), LocalDateTime.of(date, time)));
        binding.stepFormSub.setText(getString(R.string.confirmed_step_form_sub, doctorName));

        binding.btnStartForm.setOnClickListener(v -> show(new PreAppointmentFormFragment()));
        binding.btnAddCalendar.setOnClickListener(v -> addToCalendar());
        binding.btnDone.setOnClickListener(v -> show(new MedicalHistoryFragment()));
        binding.btnClose.setOnClickListener(v -> show(new HomeFragment()));

        return binding.getRoot();
    }

    /**
     * The app bar has nothing to add to a screen whose whole job is one
     * piece of good news, so it steps out of the way for it.
     */
    @Override
    public void onResume() {
        super.onResume();
        setAppBarVisible(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        setAppBarVisible(true);
    }

    private void setAppBarVisible(boolean visible) {
        View toolbar = requireActivity().findViewById(R.id.materialToolbar);
        if (toolbar != null) {
            toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Hands the appointment to whatever calendar the patient already uses,
     * rather than asking for calendar permissions to write it ourselves.
     */
    private void addToCalendar() {
        LocalDateTime start = LocalDateTime.of(date, time);
        long startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        startMillis + ASSUMED_MINUTES * 60_000L)
                .putExtra(CalendarContract.Events.TITLE, getString(
                        R.string.confirmed_calendar_title, doctorName,
                        getString(R.string.clinic_name)))
                .putExtra(CalendarContract.Events.EVENT_LOCATION,
                        getString(R.string.clinic_address));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            DialogUtils.showMessageDialog(getContext(), getString(R.string.confirmed_no_calendar));
        }
    }

    private void show(Fragment fragment) {
        FragmentUtils.loadFragment(getParentFragmentManager(), R.id.flFragment, fragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
