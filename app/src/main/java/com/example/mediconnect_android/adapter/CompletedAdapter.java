package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.CompletedItemBinding;
import com.example.mediconnect_android.fragment.AddReviewFragment;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;

public class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.ViewHolder> {

    private final Context context;
    CompletedItemBinding completedItemBinding;
    private final List<Appointment> appointmentList;

    public CompletedAdapter(List<Appointment> appointmentList, Context context) {
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        completedItemBinding = CompletedItemBinding.inflate(layoutInflater, parent, false);
        listeners();
        return new ViewHolder(completedItemBinding);
    }

    private void listeners() {
        completedItemBinding.addReviewButton.setOnClickListener(v -> {
            AddReviewFragment addReviewFragment = new AddReviewFragment();
            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, addReviewFragment);
        });

            completedItemBinding.rebookButton.setOnClickListener(v -> {
                BookAppointmentFragment bookAppointmentFragment = new BookAppointmentFragment();
                FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, bookAppointmentFragment);
            });
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
        }

        public void bindView(Appointment appointment) {
            Schedule schedule = appointment.getSchedule();
/*            if (schedule != null) {
                Doctor doctor = schedule.getDoctor();
                if (doctor == null) {
                    recyclerItemBinding.appointmentDate.setText(schedule.toString());
                    recyclerItemBinding.doctorName.setText(doctor.getName());
                    recyclerItemBinding.doctorImage.setImageResource(Integer.parseInt(doctor.getPhoto()));
                    recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());
                }
            }*/

            // To test
            if (schedule != null) {
                recyclerItemBinding.appointmentDate.setText(schedule.toString());

                Doctor doctor = schedule.getDoctor();
                if (doctor != null) {
                    recyclerItemBinding.doctorName.setText(doctor.getName());
                    recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());

                    if (doctor.getPhoto() != null) {
                        try {
                            int photoResId = Integer.parseInt(doctor.getPhoto());
                            recyclerItemBinding.doctorImage.setImageResource(photoResId);
                        } catch (NumberFormatException e) {
                            recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);
                        }
                    } else {
                        recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);
                    }
                } else {
                    recyclerItemBinding.doctorName.setText(R.string.speacialty_text);
                    recyclerItemBinding.doctorSpeacialty.setText(R.string.speacialty_text);
                    recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);
                }
            } else {
                recyclerItemBinding.appointmentDate.setText(R.string.mock_date);

                recyclerItemBinding.doctorName.setText(R.string.speacialty_text);
                recyclerItemBinding.doctorSpeacialty.setText(R.string.speacialty_text);
                recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);
            }
        }
    }
}
