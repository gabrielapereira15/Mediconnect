package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.CompletedItemBinding;
import com.example.mediconnect_android.fragment.AddReviewFragment;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;

public class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.ViewHolder> {

    private final Context context;
    private final List<Appointment> appointmentList;
    CompletedItemBinding completedItemBinding;

    public CompletedAdapter(List<Appointment> appointmentList, Context context) {
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        completedItemBinding = CompletedItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(completedItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindView(appointmentList.get(position));
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CompletedItemBinding recyclerItemBinding;

        public ViewHolder(CompletedItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;

            // Rebook button click listener
            recyclerItemBinding.rebookButton.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Appointment appointment = appointmentList.get(position);
                    Doctor doctor = appointment.getDoctor();

                    BookAppointmentFragment bookAppointmentFragment = new BookAppointmentFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("doctorId", doctor.getId());
                    bundle.putString("doctorName", doctor.getName());
                    bundle.putString("doctorPhoto", doctor.getPhoto());
                    bundle.putString("doctorSpecialty", doctor.getSpecialty());
                    bookAppointmentFragment.setArguments(bundle);
                    FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, bookAppointmentFragment);
                }
            });

            // Add Review button click listener
            recyclerItemBinding.addReviewButton.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Appointment appointment = appointmentList.get(position);
                    Doctor doctor = appointment.getDoctor();

                    AddReviewFragment addReviewFragment = new AddReviewFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("doctorId", doctor.getId());
                    bundle.putString("doctorName", doctor.getName());
                    bundle.putString("doctorPhoto", doctor.getPhoto());
                    bundle.putString("doctorSpecialty", doctor.getSpecialty());
                    bundle.putString("appointmentId", appointment.getId());
                    addReviewFragment.setArguments(bundle);
                    FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, addReviewFragment);
                }
            });
        }

        public void bindView(Appointment appointment) {
            Doctor doctor = appointment.getDoctor();

            recyclerItemBinding.appointmentDate.setText(appointment.toString());
            recyclerItemBinding.doctorName.setText(doctor.getName());
            recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());

            if (appointment.getReviewed()) {
                recyclerItemBinding.addReviewButton.setVisibility(View.GONE);
            } else {
                recyclerItemBinding.addReviewButton.setVisibility(View.VISIBLE);
            }

            Glide.with(context)
                    .load(doctor.getPhoto())
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(recyclerItemBinding.doctorImage);
        }
    }
}
