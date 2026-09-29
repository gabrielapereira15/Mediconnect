package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.DoctorItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.WhenLabel;

import java.util.List;
import java.util.Locale;

/**
 * The "Available today" strip on Home.
 *
 * Each card leads with when the doctor is next free, because that is what a
 * patient is choosing on. The old card showed a stock photograph and a
 * "Book Appointment" button identical on every one of them, with no
 * indication of whether that doctor had anything open at all.
 */
public class DoctorHomeScreenAdapter
        extends RecyclerView.Adapter<DoctorHomeScreenAdapter.ViewHolder> {

    private final Context context;
    private final List<Doctor> doctorList;

    public DoctorHomeScreenAdapter(List<Doctor> doctorList, Context context) {
        this.doctorList = doctorList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(DoctorItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindView(doctorList.get(position));
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final DoctorItemBinding binding;

        ViewHolder(DoctorItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    openBooking(doctorList.get(position));
                }
            });
        }

        void bindView(Doctor doctor) {
            String name = WhenLabel.doctorName(doctor.getName());
            binding.doctorName.setText(name);
            binding.doctorSpecialty.setText(doctor.getSpecialty());
            binding.doctorInitials.setText(WhenLabel.initials(doctor.getName()));

            bindRating(doctor);
            bindNextSlot(doctor);

            binding.getRoot().setContentDescription(name + ", " + doctor.getSpecialty());
        }

        /**
         * A rating, or nothing at all. A star with no number beside it reads
         * as a zero rating rather than as "not rated yet"; the full list has
         * room to say so in words, a 220dp card does not.
         */
        private void bindRating(Doctor doctor) {
            boolean rated = doctor.getScore() != null;
            binding.doctorRatingGroup.setVisibility(rated ? View.VISIBLE : View.GONE);
            if (rated) {
                binding.doctorRating.setText(
                        String.format(Locale.ENGLISH, "%.1f", doctor.getScore()));
            }
        }

        private void bindNextSlot(Doctor doctor) {
            WhenLabel.parse(doctor.getNextAvailableAt()).ifPresentOrElse(slot -> {
                binding.doctorNextSlot.setText(WhenLabel.nextSlotWords(context, slot));
                // Today's opening is the one worth colouring; a slot next
                // week is information, not good news.
                boolean today = WhenLabel.isToday(slot);
                binding.doctorNextSlot.setBackgroundResource(
                        today ? R.drawable.badge_success : R.drawable.badge_neutral);
                binding.doctorNextSlot.setTextColor(ContextCompat.getColor(context,
                        today ? R.color.md_success : R.color.md_on_surface_variant));
            }, () -> {
                binding.doctorNextSlot.setText(R.string.slot_none);
                binding.doctorNextSlot.setBackgroundResource(R.drawable.badge_neutral);
                binding.doctorNextSlot.setTextColor(
                        ContextCompat.getColor(context, R.color.md_on_surface_variant));
            });
        }

        private void openBooking(Doctor doctor) {
            BookAppointmentFragment.open(
                    ((AppCompatActivity) context).getSupportFragmentManager(),
                    doctor.getId(), doctor.getName(), doctor.getSpecialty());
        }
    }
}
