package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.databinding.DoctorItemBinding;
import com.example.mediconnect_android.model.Doctor;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Doctor> doctorList;
    DoctorItemBinding doctorItemBinding;

    public DoctorAdapter(List<Doctor> doctorList, Context context) {
        super();
        this.doctorList = doctorList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        doctorItemBinding = DoctorItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(doctorItemBinding);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        DoctorItemBinding recyclerItemBinding;
        public ViewHolder(DoctorItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }
        public void bindView(Doctor doctor) {
            recyclerItemBinding.doctorName.setText(doctor.getName());
            recyclerItemBinding.doctorSpecialty.setText(doctor.getSpecialty());
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bindView(doctorList.get(position));
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }
}
