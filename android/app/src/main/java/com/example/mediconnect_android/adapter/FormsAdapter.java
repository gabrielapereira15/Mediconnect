package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FormVisitItemBinding;
import com.example.mediconnect_android.databinding.ViewSectionHeaderBinding;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.FormSections;
import com.example.mediconnect_android.util.WhenLabel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Forms screen's rows: "To fill in", then "Sent", each under a heading
 * with its count.
 *
 * Built once per load, headings included, the way the visit list groups
 * "This week" and "Later" — and as there, a heading only appears when it
 * has something under it.
 */
public class FormsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Called with the visit whose form was tapped. */
    public interface OnOpen {
        void open(Appointment appointment);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FORM = 1;

    private final Context context;
    private final OnOpen onOpen;

    /** Either a heading or a visit, in the order they are shown. */
    private final List<Object> rows = new ArrayList<>();

    public FormsAdapter(Context context, FormSections sections, OnOpen onOpen) {
        this.context = context;
        this.onOpen = onOpen;
        addSection(R.string.forms_section_to_fill, sections.toFillIn());
        addSection(R.string.forms_section_sent, sections.sent());
    }

    private void addSection(@StringRes int heading, List<Appointment> visits) {
        if (visits.isEmpty()) {
            return;
        }
        rows.add(context.getString(heading, visits.size()));
        rows.addAll(visits);
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof Appointment ? TYPE_FORM : TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(ViewSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new FormViewHolder(FormVisitItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof FormViewHolder) {
            ((FormViewHolder) holder).bind((Appointment) row);
        } else {
            ((HeaderViewHolder) holder).bind((String) row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final ViewSectionHeaderBinding binding;

        HeaderViewHolder(ViewSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Lets a TalkBack user jump between the two sections by heading.
            ViewCompat.setAccessibilityHeading(binding.getRoot(), true);
        }

        void bind(String heading) {
            binding.sectionTitle.setText(heading);
        }
    }

    class FormViewHolder extends RecyclerView.ViewHolder {

        private final FormVisitItemBinding binding;

        FormViewHolder(FormVisitItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Appointment appointment) {
            boolean sent = appointment.isFormSubmitted();
            Doctor doctor = appointment.getDoctor();
            String name = doctor == null ? "" : WhenLabel.doctorName(doctor.getName());
            String specialty = doctor == null || doctor.getSpecialty() == null
                    ? ""
                    : doctor.getSpecialty();

            binding.doctor.setText(specialty.isEmpty()
                    ? name
                    : context.getString(R.string.doctor_and_specialty, name, specialty));

            // FormSections only lets through visits with a readable time,
            // so this is always there.
            Optional<LocalDateTime> at = WhenLabel.parse(appointment.getStartsAt());
            binding.visitWhen.setText(at.map(time -> context.getString(R.string.forms_day_time,
                            WhenLabel.relativeDayWords(context, time), WhenLabel.timeWords(time)))
                    .orElse(""));
            String spokenWhen = at.map(time -> WhenLabel.whenWords(context, time)).orElse("");

            String bookedFor = appointment.getBookedForName();
            boolean forSomeoneElse = bookedFor != null && !bookedFor.trim().isEmpty();
            String forWords = forSomeoneElse
                    ? context.getString(R.string.booked_for, bookedFor.trim())
                    : "";
            binding.bookedFor.setVisibility(forSomeoneElse ? View.VISIBLE : View.GONE);
            binding.bookedFor.setText(forWords);

            String sentWhen = WhenLabel.parse(appointment.getFormSubmittedAt())
                    .map(time -> WhenLabel.whenWords(context, time))
                    .orElse("");
            binding.sentAt.setVisibility(sent ? View.VISIBLE : View.GONE);
            binding.sentAt.setText(sentWhen.isEmpty()
                    ? context.getString(R.string.visit_form_sent)
                    : context.getString(R.string.visit_form_row_sent, sentWhen));

            bindState(sent);

            // One target that says everything the row shows, in the order
            // it shows it, ending on whether the form is still to do.
            List<String> said = new ArrayList<>();
            addIfPresent(said, name);
            addIfPresent(said, specialty);
            addIfPresent(said, spokenWhen);
            addIfPresent(said, forWords);
            if (!sent) {
                said.add(context.getString(R.string.forms_cd_to_fill));
            } else if (sentWhen.isEmpty()) {
                said.add(context.getString(R.string.visit_form_sent));
            } else {
                said.add(context.getString(R.string.forms_cd_sent, sentWhen));
            }
            itemView.setContentDescription(String.join(". ", said));

            // "Double tap to fill in the form" rather than "to activate".
            ViewCompat.replaceAccessibilityAction(itemView, AccessibilityActionCompat.ACTION_CLICK,
                    context.getString(sent
                            ? R.string.forms_cd_open_sent
                            : R.string.forms_cd_open_to_fill),
                    null);

            itemView.setOnClickListener(v -> onOpen.open(appointment));
        }

        /** The tick once it is sent; the form's own icon while it is owed. */
        private void bindState(boolean sent) {
            binding.formStateIcon.setImageResource(sent ? R.drawable.ic_check : R.drawable.ic_form);
            binding.formStateIcon.setBackgroundResource(sent
                    ? R.drawable.tile_success_soft
                    : R.drawable.tile_brand_soft);
            binding.formStateIcon.setImageTintList(ContextCompat.getColorStateList(context,
                    sent ? R.color.md_success : R.color.md_on_primary_container));
            binding.action.setText(sent ? R.string.visit_form_view : R.string.visit_form_fill);
        }

        private void addIfPresent(List<String> parts, String part) {
            if (part != null && !part.isEmpty()) {
                parts.add(part);
            }
        }
    }
}
