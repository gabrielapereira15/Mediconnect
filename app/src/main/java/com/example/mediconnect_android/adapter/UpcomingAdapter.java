package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.UpcomingItemBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.model.Schedule;

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
            // Verifique se o schedule é não-nulo, caso contrário, defina valores padrão
            if (schedule != null) {
                recyclerItemBinding.appointmentDate.setText(schedule.toString());

                Doctor doctor = schedule.getDoctor();
                if (doctor != null) {
                    recyclerItemBinding.doctorName.setText(doctor.getName());
                    recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());

                    // Se o doctor tiver uma foto, use-a, senão use uma imagem padrão
                    if (doctor.getPhoto() != null) {
                        try {
                            int photoResId = Integer.parseInt(doctor.getPhoto());
                            recyclerItemBinding.doctorImage.setImageResource(photoResId);
                        } catch (NumberFormatException e) {
                            // Se a foto não for um recurso válido, use uma imagem padrão
                            recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage); // Imagem padrão
                        }
                    } else {
                        // Se a foto do doctor for nula, use uma imagem padrão
                        recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);
                    }
                } else {
                    // Se o doctor for nulo, use um nome padrão e especialidade
                    recyclerItemBinding.doctorName.setText(R.string.speacialty_text);
                    recyclerItemBinding.doctorSpeacialty.setText(R.string.speacialty_text);
                    recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage); // Imagem padrão
                }
            } else {
                // Se o schedule for nulo, defina a data e hora padrão
                recyclerItemBinding.appointmentDate.setText(R.string.mock_date);

                // Defina os valores padrão para o médico
                recyclerItemBinding.doctorName.setText(R.string.speacialty_text);
                recyclerItemBinding.doctorSpeacialty.setText(R.string.speacialty_text);
                recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage); // Imagem padrão
            }
        }
    }
}
