package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.DoctorSeeAllAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentMock;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.databinding.FragmentDoctorsBinding;
import com.example.mediconnect_android.databinding.FragmentSpecialtiesBinding;
import com.example.mediconnect_android.model.Doctor;

import java.util.ArrayList;
import java.util.List;

public class DoctorsFragment extends Fragment {

    FragmentDoctorsBinding binding;
    List<Doctor> doctorList = new ArrayList<>();
    DoctorSeeAllAdapter mAdapter;

    public DoctorsFragment() {
        // Default constructor
    }

    /**
     * Factory method to create a new instance of the fragment with a doctor list.
     */
    public static DoctorsFragment newInstance(List<Doctor> doctors) {
        DoctorsFragment fragment = new DoctorsFragment();
        Bundle args = new Bundle();
        args.putSerializable("doctorList", new ArrayList<>(doctors));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the doctor list from the arguments
        if (getArguments() != null) {
            doctorList = (List<Doctor>) getArguments().getSerializable("doctorList");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentDoctorsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();
        return view;
    }

    private void init() {
        bindAdapter();
    }

    private void bindAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        binding.recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DoctorSeeAllAdapter(doctorList, getContext());
        binding.recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
