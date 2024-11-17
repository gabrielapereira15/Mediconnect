package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.DoctorItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Doctor> doctorList;
    DoctorItemBinding doctorItemBinding;

    public DoctorAdapter(List<Doctor> doctorList, Context context) {
        super();
        this.doctorList = doctorList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        doctorItemBinding = DoctorItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(doctorItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bindView(doctorList.get(position));
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
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
            recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);

            recyclerItemBinding.btnBookAppointment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BookAppointmentFragment bookAppointmentFragment = new BookAppointmentFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("doctorId", doctor.getId());
                    bundle.putString("doctorPhoto", doctor.getPhoto());
                    bundle.putString("doctorSpecialty", doctor.getSpecialty());
                    bookAppointmentFragment.setArguments(bundle);

                    FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, bookAppointmentFragment);
                }
            });
        }
    }
}
