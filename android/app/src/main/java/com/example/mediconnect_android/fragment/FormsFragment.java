package com.example.mediconnect_android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.FormsAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.databinding.FragmentFormsBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.FormSections;
import com.example.mediconnect_android.util.FragmentUtils;

import java.time.LocalDateTime;

/**
 * Forms: every upcoming visit's pre-appointment form, and which visit each
 * one is for.
 *
 * Home's Forms tile counted the forms still owed and then, on a tap,
 * dropped the patient straight into one of them without saying which
 * visit it belonged to. With three visits booked there was no telling
 * whether the questions were about Thursday's appointment or a child's
 * check-up next week. The tile opens this list instead, so the patient
 * picks the visit, and the forms already sent sit underneath where their
 * answers can be read back.
 */
public class FormsFragment extends Fragment {

    private FragmentFormsBinding binding;
    private final AppointmentClient appointmentClient = new AppointmentClientImpl();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFormsBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.stateView.setContentView(binding.recyclerView);
        binding.swipeRefresh.setOnRefreshListener(() -> load(true));

        // Sending a form pops back here, and the list below is fetched
        // afresh anyway, so there is nothing to do with the result. It is
        // still taken here so it does not sit in the fragment manager
        // waiting for the next screen that listens.
        getParentFragmentManager().setFragmentResultListener(
                PreAppointmentFormFragment.RESULT_SENT, getViewLifecycleOwner(),
                (key, result) -> { /* the reload below already shows it */ });

        // Loaded every time the view is made, so coming back from a form
        // that was just sent shows it under Sent rather than still owed.
        load(false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.forms_title);
    }

    private void load(boolean isRefresh) {
        // On a refresh the existing list stays put behind the spinner.
        if (!isRefresh) {
            binding.stateView.showLoading();
        }
        String email = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("email", "");

        Background.run(
                () -> appointmentClient.getAppointments(email),
                appointments -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    bind(FormSections.split(appointments, LocalDateTime.now()));
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    binding.stateView.showError(() -> load(false));
                });
    }

    private void bind(FormSections sections) {
        binding.recyclerView.setAdapter(new FormsAdapter(requireContext(), sections, this::open));

        if (!sections.isEmpty()) {
            binding.stateView.showContent();
            return;
        }
        // A form comes with a booking, so the way forward from here is to
        // book one.
        binding.stateView.showEmpty(
                R.drawable.ic_form,
                R.string.forms_empty_title,
                R.string.forms_empty_body,
                R.string.state_book_appointment,
                () -> show(new DoctorsFragment()));
    }

    /** The form while it is owed; what was sent once it is in. */
    private void open(Appointment appointment) {
        show(FormAnswersFragment.forVisit(appointment));
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
