package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.SpecialtiesAdapter;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.databinding.FragmentSpecialtiesBinding;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Specialty;

import java.util.ArrayList;
import java.util.List;

public class SpecialtiesFragment extends Fragment {

    private FragmentSpecialtiesBinding binding;
    private SpecialtiesAdapter adapter;
    private DoctorClient doctorClient; // Class-level field
    private List<Doctor> doctorList = new ArrayList<>();
    private List<Specialty> specialties = new ArrayList<>();

    public SpecialtiesFragment() {
        // Properly initialize the class-level field
        this.doctorClient = new DoctorClientImpl();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSpecialtiesBinding.inflate(inflater, container, false);
        doctorList = doctorClient.getDoctors(); // Ensure doctorClient is properly initialized
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSpecialties();
        bindAdapter();
    }

    private void initSpecialties() {
        // Clear the list to avoid duplication
        specialties.clear();
        specialties.add(new Specialty("Optometrist", R.drawable.ophthalmologists));
        specialties.add(new Specialty("Cardiologist", R.drawable.cardiologist));
        specialties.add(new Specialty("Gynecologist", R.drawable.gynecologist));
        specialties.add(new Specialty("Gastroenterologist", R.drawable.gastroenterologist));
        specialties.add(new Specialty("Pediatrician", R.drawable.baseline_child_care_24));
        specialties.add(new Specialty("Neurologist", R.drawable.neurologist));
    }

    private void bindAdapter() {
        adapter = new SpecialtiesAdapter(specialties, requireContext(), doctorList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // Show the empty message if the list is empty
        binding.tvEmptyMessage.setVisibility(specialties.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
