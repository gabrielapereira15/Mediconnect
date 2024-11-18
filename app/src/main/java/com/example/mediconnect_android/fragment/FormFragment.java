package com.example.mediconnect_android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediconnect_android.adapter.FormAdapter;
import com.example.mediconnect_android.databinding.FragmentFormBinding;
import com.example.mediconnect_android.model.FormItem;

import java.util.ArrayList;
import java.util.List;


public class FormFragment extends Fragment {

    FragmentFormBinding binding;
    FormAdapter adapter;
    List<FormItem> forms = new ArrayList<>();

    public FormFragment() {
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
        binding = FragmentFormBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    private void init() {
        forms.add(new FormItem("Pre-Appointment Form", "Pending", "If you have an upcoming appointment, streamline the process taking 3-5 minutes to fill out this form"));
        forms.add(new FormItem("Check-In Form", "Pending", "If you want to check in for your appointment and streamline the process, fill out this form at least 2 hours before your appointment"));
        bindAdapter();
    }

    private void bindAdapter() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FormAdapter(getContext(), forms);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}