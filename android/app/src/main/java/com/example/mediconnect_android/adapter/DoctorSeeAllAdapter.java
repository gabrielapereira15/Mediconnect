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
import com.example.mediconnect_android.databinding.DoctorListItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.WhenLabel;

import java.util.List;
import java.util.Locale;

/**
 * The doctor list on "Find a doctor" (board P04).
 *
 * Every card used to carry the same "Book Appointment" button and nothing
 * else, so eight identical rows gave a patient no basis for choosing between
 * them. These lead with when the doctor is next free.
 */
public class DoctorSeeAllAdapter extends RecyclerView.Adapter<DoctorSeeAllAdapter.ViewHolder> {

    private final Context context;
    private final List<Doctor> doctorList;

    public DoctorSeeAllAdapter(List<Doctor> doctorList, Context context) {
        this.doctorList = doctorList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(DoctorListItemBinding.inflate(
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

        private final DoctorListItemBinding binding;

        ViewHolder(DoctorListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            View.OnClickListener open = v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    openBooking(doctorList.get(position));
                }
            };
            binding.getRoot().setOnClickListener(open);
            binding.doctorBook.setOnClickListener(open);
        }

        void bindView(Doctor doctor) {
            binding.doctorName.setText(WhenLabel.doctorName(doctor.getName()));
            binding.doctorInitials.setText(WhenLabel.initials(doctor.getName()));

            String years = doctor.getExperienceYears();
            binding.doctorSpecialty.setText(years == null || years.isEmpty()
                    ? doctor.getSpecialty()
                    : context.getString(R.string.doctors_years, doctor.getSpecialty(), years));

            bindRating(doctor);
            bindNextSlot(doctor);
        }

        /**
         * A rating with its review count, or "No reviews yet" in words. A
         * filled star with no number reads as a score of zero, so an unrated
         * doctor gets the outline star and a sentence instead.
         */
        private void bindRating(Doctor doctor) {
            if (doctor.getScore() == null) {
                binding.doctorStar.setImageResource(R.drawable.ic_star_outline);
                binding.doctorStar.setImageTintList(ContextCompat.getColorStateList(
                        context, R.color.md_on_surface_variant));
                binding.doctorRating.setText(R.string.doctors_no_reviews);
                binding.doctorRatingGroup.setContentDescription(
                        context.getString(R.string.doctors_no_reviews));
                return;
            }

            int reviews = doctor.getReviewCount() == null ? 0 : doctor.getReviewCount();
            String score = String.format(Locale.ENGLISH, "%.1f", doctor.getScore());

            binding.doctorStar.setImageResource(R.drawable.ic_star);
            binding.doctorStar.setImageTintList(
                    ContextCompat.getColorStateList(context, R.color.md_rating));
            binding.doctorRating.setText(context.getResources().getQuantityString(
                    R.plurals.doctors_rating, reviews, score, reviews));
            binding.doctorRatingGroup.setContentDescription(
                    context.getString(R.string.cd_rating, score, reviews));
        }

        private void bindNextSlot(Doctor doctor) {
            WhenLabel.parse(doctor.getNextAvailableAt()).ifPresentOrElse(slot -> {
                binding.doctorNextSlot.setText(WhenLabel.nextSlotWords(context, slot));
                boolean today = WhenLabel.isToday(slot);
                binding.doctorNextSlot.setBackgroundResource(
                        today ? R.drawable.badge_success : R.drawable.badge_neutral);
                binding.doctorNextSlot.setTextColor(ContextCompat.getColor(context,
                        today ? R.color.md_success : R.color.md_on_surface_variant));
                binding.doctorBook.setEnabled(true);
            }, () -> {
                binding.doctorNextSlot.setText(R.string.slot_none);
                binding.doctorNextSlot.setBackgroundResource(R.drawable.badge_neutral);
                binding.doctorNextSlot.setTextColor(
                        ContextCompat.getColor(context, R.color.md_on_surface_variant));
                // Nothing to book, so the button does not invite a tap that
                // lands on an empty slot picker.
                binding.doctorBook.setEnabled(false);
            });
        }

        private void openBooking(Doctor doctor) {
            BookAppointmentFragment.open(
                    ((AppCompatActivity) context).getSupportFragmentManager(),
                    doctor.getId(), doctor.getName(), doctor.getSpecialty());
        }
    }
}
