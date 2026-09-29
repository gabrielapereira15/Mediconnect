package com.example.mediconnect_android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.HealthEntryItemBinding;
import com.example.mediconnect_android.model.HealthEntry;

import java.util.List;
import java.util.function.Consumer;

/**
 * The patient's allergies, medications and conditions, in one list.
 *
 * Kept in one list rather than three because the record is read as a whole
 * — what a clinician wants is everything relevant, not a tab per category.
 */
public class HealthEntryAdapter extends RecyclerView.Adapter<HealthEntryAdapter.ViewHolder> {

    private final List<HealthEntry> entries;
    private final Consumer<HealthEntry> onStop;

    public HealthEntryAdapter(List<HealthEntry> entries, Consumer<HealthEntry> onStop) {
        this.entries = entries;
        this.onStop = onStop;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(HealthEntryItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final HealthEntryItemBinding binding;

        ViewHolder(HealthEntryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.btnStop.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onStop.accept(entries.get(position));
                }
            });
        }

        void bind(HealthEntry entry) {
            binding.entryType.setText(label(entry.getType()));
            binding.entryDescription.setText(entry.getDescription());

            String note = entry.getNote();
            binding.entryNote.setVisibility(note == null || note.isEmpty() ? View.GONE : View.VISIBLE);
            binding.entryNote.setText(note);

            // An entry that is no longer current stays on the list and says
            // so; a past reaction still matters to whoever treats them next.
            boolean active = entry.isActive();
            binding.entryInactive.setVisibility(active ? View.GONE : View.VISIBLE);
            binding.btnStop.setVisibility(active ? View.VISIBLE : View.GONE);
            binding.entryDescription.setAlpha(active ? 1f : 0.6f);
        }

        private int label(String type) {
            if (HealthEntry.TYPE_MEDICATION.equals(type)) {
                return R.string.health_type_medication;
            }
            if (HealthEntry.TYPE_CONDITION.equals(type)) {
                return R.string.health_type_condition;
            }
            return R.string.health_type_allergy;
        }
    }
}
