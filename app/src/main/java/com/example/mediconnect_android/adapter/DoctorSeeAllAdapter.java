package com.example.mediconnect_android.adapter;

import android.annotation.SuppressLint;
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
        return new ViewHolder(doctorListItemBinding);
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

        @SuppressLint("SetTextI18n")
        public void bindView(Doctor doctor) {
            recyclerItemBinding.doctorName.setText(doctor.getName());
            recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());

            // Check if the score is available
            if (doctor.getScore() != null) {
                recyclerItemBinding.doctorScore.setText(String.valueOf(doctor.getScore()));
                recyclerItemBinding.star.setColorFilter(context.getResources().getColor(R.color.yellow), android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                recyclerItemBinding.doctorScore.setText(""); // Set no text for score
                recyclerItemBinding.star.setColorFilter(context.getResources().getColor(R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            // Check if the review count is available
            if (doctor.getReviewCount() != null) {
                recyclerItemBinding.reviewCount.setText("(" + String.valueOf(doctor.getReviewCount() + ")"));
            } else {
                recyclerItemBinding.reviewCount.setText("");
            }

            String doctorImageURL = doctor.getPhoto();
            Glide.with(context)
                    .load(doctorImageURL)
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(recyclerItemBinding.doctorImage);

            recyclerItemBinding.bookAppointmentButton.setOnClickListener(new View.OnClickListener() {
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
        }
    }

}
