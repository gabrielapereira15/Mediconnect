package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.CompletedItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.WhenLabel;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.ViewHolder> {

    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    private final Context context;
    private final List<Appointment> appointmentList;

    /** Called when a card asks to leave a review, so the list can show it. */
    private final Consumer<Appointment> onLeaveReview;
    CompletedItemBinding completedItemBinding;

    public CompletedAdapter(List<Appointment> appointmentList, Context context) {
        this(appointmentList, context, null);
    }

    public CompletedAdapter(List<Appointment> appointmentList, Context context,
                            Consumer<Appointment> onLeaveReview) {
        this.onLeaveReview = onLeaveReview;
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        completedItemBinding = CompletedItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(completedItemBinding);
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

            recyclerItemBinding.rebookButton.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Doctor doctor = appointmentList.get(position).getDoctor();
                    BookAppointmentFragment.open(
                            ((AppCompatActivity) context).getSupportFragmentManager(),
                            doctor.getId(), doctor.getName(), doctor.getSpecialty());
                }
            });

            // A sheet over the list rather than a screen: the review belongs
            // to the visit the patient can still see behind it.
            recyclerItemBinding.addReviewButton.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onLeaveReview != null) {
                    onLeaveReview.accept(appointmentList.get(position));
                }
            });
        }

        public void bindView(Appointment appointment) {
            Doctor doctor = appointment.getDoctor();

            WhenLabel.parse(appointment.getStartsAt()).ifPresent(at -> {
                recyclerItemBinding.badgeWeekday.setText(at.format(WEEKDAY));
                recyclerItemBinding.badgeDay.setText(String.valueOf(at.getDayOfMonth()));
                // The month goes beside the time rather than in the date
                // block, which has room for a number and nothing else.
                recyclerItemBinding.appointmentTime.setText(context.getString(
                        R.string.doctor_and_specialty,
                        WhenLabel.timeWords(at), at.format(MONTH)));
            });

            recyclerItemBinding.doctorName.setText(context.getString(
                    R.string.doctor_and_specialty,
                    WhenLabel.doctorName(doctor.getName()), doctor.getSpecialty()));

            bindReviewState(appointment);
        }

        /**
         * Either offers the review or confirms one was left.
         *
         * Previously the button simply disappeared once reviewed, which looks
         * the same as the option never being there. Swapping in a badge keeps
         * the row's own record of what the patient did.
         */
        private void bindReviewState(Appointment appointment) {
            boolean reviewed = Boolean.TRUE.equals(appointment.getReviewed());

            recyclerItemBinding.addReviewButton.setVisibility(reviewed ? View.GONE : View.VISIBLE);
            recyclerItemBinding.reviewedBadge.setVisibility(reviewed ? View.VISIBLE : View.GONE);

            if (!reviewed) {
                return;
            }

            Float score = appointment.getReviewScore();
            if (score == null) {
                // Reviewed, but the score did not come back — say so without a number.
                recyclerItemBinding.reviewedLabel.setText(R.string.review_left);
            } else {
                recyclerItemBinding.reviewedLabel.setText(
                        context.getString(R.string.review_left_score, score));
            }
            recyclerItemBinding.reviewedBadge.setContentDescription(
                    recyclerItemBinding.reviewedLabel.getText());
        }
    }
}
