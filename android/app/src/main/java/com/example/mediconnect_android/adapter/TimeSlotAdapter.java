package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.model.TimSlotRecord;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeslotViewHolder> {

    private ChipGroup lastSelectedGroup;

    private final List<String> dateList;
    private final List<List<TimSlotRecord>> timeSlotsList;
    private final Context context;
    public String selectedDate = null;
    public String selectedTimeSlotTime = null;
    public String selectedTimeSlotId = null;
    private View lastSelectedView = null;

    public TimeSlotAdapter(Context context, List<String> dateList, List<List<TimSlotRecord>> timeSlotsList) {
        this.context = context;
        this.dateList = dateList;
        this.timeSlotsList = timeSlotsList;
    }

    @NonNull
    @Override
    public TimeslotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeslot, parent, false);
        return new TimeslotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeslotViewHolder holder, int position) {
        String date = dateList.get(position);
        List<TimSlotRecord> timeSlots = timeSlotsList.get(position);

        holder.tvDate.setText(date);
        holder.cgTimeSlots.removeAllViews();

        for (TimSlotRecord timeSlot : timeSlots) {
            Chip chip = new Chip(context);
            chip.setText(timeSlot.time());
            chip.setTag(timeSlot.id());
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            // Give each chip a stable id so ChipGroup's single-selection works.
            chip.setId(View.generateViewId());

            chip.setOnClickListener(v -> {
                selectedTimeSlotId = String.valueOf(v.getTag());
                selectedTimeSlotTime = ((Chip) v).getText().toString();
                selectedDate = holder.tvDate.getText().toString();

                // ChipGroup only deselects within its own group, so clear the
                // selection on the other days too — a booking is one slot.
                if (lastSelectedGroup != null && lastSelectedGroup != holder.cgTimeSlots) {
                    lastSelectedGroup.clearCheck();
                }
                lastSelectedGroup = holder.cgTimeSlots;
            });

            holder.cgTimeSlots.addView(chip);
        }
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class TimeslotViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        ChipGroup cgTimeSlots;

        public TimeslotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            cgTimeSlots = itemView.findViewById(R.id.cg_timeslots);
        }
    }
}
