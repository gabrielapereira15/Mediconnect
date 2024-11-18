package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.DoctorListItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;

public class DoctorSeeAllAdapter extends RecyclerView.Adapter<DoctorSeeAllAdapter.ViewHolder> {
    private final List<Doctor> doctorList;
    private final Context context;
    DoctorListItemBinding doctorListItemBinding;

    public DoctorSeeAllAdapter(List<Doctor> doctorList, Context context) {
        super();
        this.doctorList = doctorList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        doctorListItemBinding = DoctorListItemBinding.inflate(layoutInflater, parent, false);
        listeners();
        return new ViewHolder(doctorListItemBinding);
    }

    private void listeners() {
        doctorListItemBinding.bookAppointmentButton.setOnClickListener(v -> {
            BookAppointmentFragment bookAppointmentFragment = new BookAppointmentFragment();
            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, bookAppointmentFragment);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorSeeAllAdapter.ViewHolder holder, int position) {
        ((ViewHolder) holder).bindView(doctorList.get(position));
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        DoctorListItemBinding recyclerItemBinding;
        public ViewHolder(DoctorListItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }

        public void bindView(Doctor doctor) {
            recyclerItemBinding.doctorName.setText(doctor.getName());
            recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());
            recyclerItemBinding.doctorImage.setImageResource(R.drawable.doctorimage);
            recyclerItemBinding.doctorScore.setText(String.valueOf(doctor.getScore()));
        }

    }

}
