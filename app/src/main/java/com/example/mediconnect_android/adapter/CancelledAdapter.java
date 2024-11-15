package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.CancelledItemBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;

import java.util.List;

public class CancelledAdapter extends RecyclerView.Adapter<CancelledAdapter.ViewHolder> {

    private final Context context;
    CancelledItemBinding cancelledItemBinding;
    private final List<Appointment> appointmentList;

    public CancelledAdapter(List<Appointment> appointmentList, Context context) {
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        cancelledItemBinding = CancelledItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(cancelledItemBinding);
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

        CancelledItemBinding recyclerItemBinding;

        public ViewHolder(CancelledItemBinding recyclerItemBinding) {
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
