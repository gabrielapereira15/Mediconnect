package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentMock;
import com.example.mediconnect_android.databinding.FragmentBookAppointmentBinding;
import com.example.mediconnect_android.model.Doctor;

public class BookAppointmentFragment extends Fragment {

    FragmentBookAppointmentBinding  binding;
    AppointmentClient appointmentClient;

    public BookAppointmentFragment() {
        appointmentClient = new AppointmentMock();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBookAppointmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String doctorId = getArguments().getString("doctorId");
            String doctorPhoto = getArguments().getString("doctorPhoto");
            String doctorSpecialty = getArguments().getString("doctorSpecialty");

            Doctor doctorDetail = appointmentClient.getDoctor(doctorId);
            // Update the UI with doctor's details
            binding.doctorName.setText(doctorDetail.getName());
            binding.doctorSpecialty.setText(doctorSpecialty);
            binding.doctorDescription.setText(doctorDetail.getDescription());
            binding.doctorScore.setText(String.valueOf(doctorDetail.getScore()));
            binding.doctorImage.setImageResource(R.drawable.doctorimage);
        }
    }
}