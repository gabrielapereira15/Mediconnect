package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.databinding.SpecialtyItemBinding;
import com.example.mediconnect_android.model.Specialty;

import java.util.List;

public class SpecialtiesAdapter extends RecyclerView.Adapter<SpecialtiesAdapter.ViewHolder> {

    private final List<Specialty> specialties;
    private final Context context;

    public SpecialtiesAdapter(List<Specialty> specialties, Context context) {
        this.specialties = specialties;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SpecialtyItemBinding binding = SpecialtyItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Specialty specialty = specialties.get(position);
        holder.bind(specialty);
    }

    @Override
    public int getItemCount() {
        return specialties.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final SpecialtyItemBinding binding;

        public ViewHolder(@NonNull SpecialtyItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Specialty specialty) {
            binding.specialtyIcon.setImageResource(specialty.getImageResId());
            binding.specialty.setText(specialty.getName());
        }
    }
}
