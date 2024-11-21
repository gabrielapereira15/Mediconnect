package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.UpcomingItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.fragment.CancelledFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;

public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.ViewHolder> {

    private final Context context;
    UpcomingItemBinding upcomingItemBindingbinding;
    private final List<Appointment> appointmentList;

    public UpcomingAdapter(List<Appointment> appointmentList, Context context) {
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        upcomingItemBindingbinding = UpcomingItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(upcomingItemBindingbinding);
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel the appointment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showCancellationMessage();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void showCancellationMessage() {
        new AlertDialog.Builder(context)
                .setTitle("Appointment Cancelled")
                .setMessage("Your appointment has been successfully cancelled.")
                .setIcon(R.drawable.baseline_check_circle_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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

        UpcomingItemBinding recyclerItemBinding;

        public ViewHolder(UpcomingItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }

        public void bindView(Appointment appointment) {
            Doctor doctor = appointment.getDoctor();
            recyclerItemBinding.appointmentDate.setText(appointment.getDate());
            recyclerItemBinding.doctorName.setText(doctor.getName());
            recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());

            Glide.with(context)
                    .load(doctor.getPhoto())
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(recyclerItemBinding.doctorImage);

            recyclerItemBinding.rescheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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

            recyclerItemBinding.cancelButton.setOnClickListener(v -> showCancelConfirmationDialog());
        }
    }
}
