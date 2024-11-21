package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentTransaction;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.SpecialtyItemBinding;
import com.example.mediconnect_android.fragment.DoctorsFragment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Specialty;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.ArrayList;
import java.util.List;

public class SpecialtiesAdapter extends RecyclerView.Adapter<SpecialtiesAdapter.SpecialtyViewHolder> {

    private final List<Specialty> specialties;
    private final Context context;
    private final List<Doctor> doctorList;

    public SpecialtiesAdapter(List<Specialty> specialties, Context context, List<Doctor> doctorList) {
        this.specialties = specialties;
        this.context = context;
        this.doctorList = doctorList;
    }

    @NonNull
    @Override
    public SpecialtyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SpecialtyItemBinding binding = SpecialtyItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new SpecialtyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SpecialtyViewHolder holder, int position) {
        Specialty specialty = specialties.get(position);
        holder.binding.specialtyIcon.setImageResource(specialty.getImageResId());
        holder.binding.specialty.setText(specialty.getName());

        holder.itemView.setOnClickListener(v -> loadDoctorsBySpecialty(specialty.getName()));
    }

    @Override
    public int getItemCount() {
        return specialties.size();
    }

    private void loadDoctorsBySpecialty(String specialty) {
        // Filter doctors by the selected specialty
        List<Doctor> filteredDoctors = new ArrayList<>();
        for (Doctor doctor : doctorList) {
            if (doctor.getSpecialty().equalsIgnoreCase(specialty)) {
                filteredDoctors.add(doctor);
            }
        }

        // Navigate to DoctorsFragment with the filtered doctor list
        DoctorsFragment doctorsFragment = DoctorsFragment.newInstance(filteredDoctors);
        FragmentTransaction transaction = ((AppCompatActivity) context)
                .getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null);
        transaction.replace(R.id.flFragment, doctorsFragment).commit();
    }

    static class SpecialtyViewHolder extends RecyclerView.ViewHolder {
        SpecialtyItemBinding binding;

        public SpecialtyViewHolder(SpecialtyItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
