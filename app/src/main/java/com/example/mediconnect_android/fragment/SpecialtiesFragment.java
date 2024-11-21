package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.GridViewAdapter;
import com.example.mediconnect_android.databinding.FragmentSettingsBinding;
import com.example.mediconnect_android.databinding.FragmentSpecialtiesBinding;
import com.example.mediconnect_android.model.Specialty;

import java.util.ArrayList;
import java.util.List;

public class SpecialtiesFragment extends Fragment {

    FragmentSpecialtiesBinding binding;
    GridViewAdapter adapter;
    List<Specialty> specialties = new ArrayList<>();

    public SpecialtiesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSpecialtiesBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        specialties.add(new Specialty("Optometrist", R.drawable.ophthalmologists));
        specialties.add(new Specialty("Cardiologist", R.drawable.cardiologist));
        specialties.add(new Specialty("Gynecologist", R.drawable.gynecologist));
        specialties.add(new Specialty("Gastroenterologists", R.drawable.gastroenterologist));
        specialties.add(new Specialty("Pediatrician", R.drawable.baseline_child_care_24));
        specialties.add(new Specialty("Neurologist", R.drawable.neurologist));

        bindAdapter();

        listener();
    }

    private void listener() {
        binding.gridView.setOnItemClickListener((parent, view, position, id) -> {
            // Handle item click here
        });
    }

    private void bindAdapter() {
        adapter = new GridViewAdapter(getContext(), specialties);
        binding.gridView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}