package com.example.mediconnect_android.adapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.databinding.UpcomingItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.fragment.CancelledFragment;
import com.example.mediconnect_android.fragment.MedicalHistoryFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.ViewHolder> {

    private final Context context;
    UpcomingItemBinding upcomingItemBindingbinding;
    private final List<Appointment> appointmentList;
    AppointmentClient appointmentClient;

    public UpcomingAdapter(List<Appointment> appointmentList, Context context) {
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        upcomingItemBindingbinding = UpcomingItemBinding.inflate(layoutInflater, parent, false);
        appointmentClient = new AppointmentClientImpl();
        return new ViewHolder(upcomingItemBindingbinding);
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
            recyclerItemBinding.appointmentDate.setText(appointment.toString());
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
                    rescheduleAppointmentDialog(appointment, doctor);
                }
            });

            recyclerItemBinding.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showCancelConfirmationDialog(appointment);
                }
            });

        }

        private void rescheduleAppointmentDialog(Appointment appointment, Doctor doctor) {
            new AlertDialog.Builder(context)
                    .setTitle("Reschedule Appointment")
                    .setMessage("Are you sure you want to reschedule the appointment? \n\n* This action will cancel the current appointment.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (isAppointmentCancelled(appointment)) {
                            BookAppointmentFragment bookAppointmentFragment = getBookAppointmentFragment(doctor);
                            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, bookAppointmentFragment);
                        } else {
                            DialogUtils.showMessageDialog(context, "Appointment not cancelled, please try again later");
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }

        private static @NonNull BookAppointmentFragment getBookAppointmentFragment(Doctor doctor) {
            BookAppointmentFragment bookAppointmentFragment = new BookAppointmentFragment();
            Bundle bundle = new Bundle();
            bundle.putString("doctorId", doctor.getId());
            bundle.putString("doctorName", doctor.getName());
            bundle.putString("doctorPhoto", doctor.getPhoto());
            bundle.putString("doctorSpecialty", doctor.getSpecialty());
            bookAppointmentFragment.setArguments(bundle);
            return bookAppointmentFragment;
        }

        private void showCancelConfirmationDialog(Appointment appointment) {
            new AlertDialog.Builder(context)
                    .setTitle("Cancel Appointment")
                    .setMessage("Are you sure you want to cancel the appointment?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (isAppointmentCancelled(appointment)) {
                            showCancellationMessage();
                        } else {
                            DialogUtils.showMessageDialog(context, "Appointment not cancelled, please try again later");
                        }
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

            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, new MedicalHistoryFragment());
        }

        private boolean isAppointmentCancelled(Appointment appointment) {
            return appointmentClient.cancelAppointment(appointment.getId());
        }
    }
}
