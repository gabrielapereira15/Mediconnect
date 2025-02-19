package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.model.TimSlotRecord;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeslotViewHolder> {

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
        holder.glTimeSlots.removeAllViews(); // Remove existing views to avoid duplicates

        // Dynamically add time slots to the GridLayout
        for (TimSlotRecord timeSlot : timeSlots) {
            TextView timeSlotView = new TextView(context);
            timeSlotView.setTag(timeSlot.id());
            timeSlotView.setText(timeSlot.time());
            timeSlotView.setPadding(16, 16, 16, 16);
            timeSlotView.setBackgroundResource(R.drawable.time_slot_background);
            timeSlotView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            // Set click listener if needed
            timeSlotView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lastSelectedView != null && lastSelectedView != v) {
                        lastSelectedView.setSelected(false);
                        lastSelectedView.setBackgroundResource(R.drawable.time_slot_background);
                    }

                    v.setSelected(true);
                    v.setBackgroundResource(R.drawable.time_slot_background);

                    lastSelectedView = v;
                    var textViewTimeSlot = ((TextView) lastSelectedView);
                    selectedTimeSlotId = String.valueOf(textViewTimeSlot.getTag());
                    selectedTimeSlotTime = String.valueOf(textViewTimeSlot.getText());
                    selectedDate = holder.tvDate.getText().toString();
                }
            });

            // Add the time slot to the GridLayout
            holder.glTimeSlots.addView(timeSlotView);
        }
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class TimeslotViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        GridLayout glTimeSlots;

        public TimeslotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            glTimeSlots = itemView.findViewById(R.id.gl_timeslots);
        }
    }
}
