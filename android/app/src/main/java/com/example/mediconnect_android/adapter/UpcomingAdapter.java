package com.example.mediconnect_android.adapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mediconnect_android.R;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentClientImpl;
import com.example.mediconnect_android.databinding.UpcomingItemBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.fragment.MedicalHistoryFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.Notification;
import com.example.mediconnect_android.util.Background;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import android.util.Log;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.List;

public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.ViewHolder> {

    private static final String TAG = "UpcomingAdapter";

    private final Context context;
    private final List<Appointment> appointmentList;
    UpcomingItemBinding upcomingItemBindingbinding;
    AppointmentClient appointmentClient;

    public UpcomingAdapter(List<Appointment> appointmentList, Context context) {
        this.appointmentList = appointmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        upcomingItemBindingbinding = UpcomingItemBinding.inflate(layoutInflater, parent, false);
        appointmentClient = new AppointmentClientImpl();
        return new ViewHolder(upcomingItemBindingbinding);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindView(appointmentList.get(position));
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        UpcomingItemBinding recyclerItemBinding;

        public ViewHolder(UpcomingItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }

        private static @NonNull BookAppointmentFragment getBookAppointmentFragment(Doctor doctor) {
            BookAppointmentFragment bookAppointmentFragment = new BookAppointmentFragment();
            Bundle bundle = new Bundle();
            bundle.putString("doctorId", doctor.getId());
            bundle.putString("doctorName", doctor.getName());
            bundle.putString("doctorPhoto", doctor.getPhoto());
            bundle.putString("doctorSpecialty", doctor.getSpecialty());
            bookAppointmentFragment.setArguments(bundle);
            return bookAppointmentFragment;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void bindView(Appointment appointment) {
            Doctor doctor = appointment.getDoctor();
            recyclerItemBinding.appointmentDate.setText(appointment.toString());
            recyclerItemBinding.doctorName.setText(doctor.getName());
            recyclerItemBinding.doctorSpeacialty.setText(doctor.getSpecialty());

            Glide.with(context)
                    .load(doctor.getPhoto())
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(recyclerItemBinding.doctorImage);

            boolean isReminderEnabled = getReminderState(appointment.getId());
            recyclerItemBinding.switchRemindMe.setChecked(isReminderEnabled);

            recyclerItemBinding.rescheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rescheduleAppointmentDialog(appointment, doctor);
                }
            });

            recyclerItemBinding.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showCancelConfirmationDialog(appointment);
                }
            });

            recyclerItemBinding.switchRemindMe.setOnCheckedChangeListener((buttonView, isChecked) -> {
                saveReminderState(appointment.getId(), isChecked);

                if (!isChecked) {
                    return;
                }

                Optional<Calendar> start = appointmentStart(appointment);
                if (start.isEmpty()) {
                    // Cannot work out when it is, so there is nothing to
                    // schedule. Previously this dereferenced null and crashed
                    // the app, which looked like being logged out.
                    buttonView.setChecked(false);
                    saveReminderState(appointment.getId(), false);
                    DialogUtils.showMessageDialog(context,
                            context.getString(R.string.reminder_unavailable));
                    return;
                }

                Calendar now = Calendar.getInstance();
                scheduleIfFuture(start.get(), -30, Calendar.MINUTE, now,
                        context.getString(R.string.reminder_soon_title),
                        context.getString(R.string.reminder_soon_body, doctor.getName()));
                scheduleIfFuture(start.get(), -24, Calendar.HOUR_OF_DAY, now,
                        context.getString(R.string.reminder_tomorrow_title),
                        context.getString(R.string.reminder_tomorrow_body,
                                doctor.getName(), appointment.getTime()));
            });
        }

        /** Schedules a reminder at an offset from the appointment, if not already past. */
        private void scheduleIfFuture(Calendar start, int amount, int unit,
                                      Calendar now, String title, String body) {
            Calendar when = (Calendar) start.clone();
            when.add(unit, amount);
            if (when.after(now)) {
                scheduleExactAlarm(context.getApplicationContext(), title, body, when);
            }
        }

        /**
         * The moment an appointment starts.
         *
         * Prefers the API's ISO timestamp. The fallback parses the display
         * string, which is fragile — it broke the moment the server started
         * including minutes in the time — so it is only a last resort, and it
         * returns empty rather than null so a failure cannot crash the caller.
         */
        private Optional<Calendar> appointmentStart(Appointment appointment) {
            String iso = appointment.getStartsAt();
            if (iso != null && !iso.isEmpty()) {
                try {
                    LocalDateTime parsed = LocalDateTime.parse(iso);
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(parsed.getYear(), parsed.getMonthValue() - 1, parsed.getDayOfMonth(),
                            parsed.getHour(), parsed.getMinute(), 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    return Optional.of(calendar);
                } catch (DateTimeParseException e) {
                    Log.w(TAG, "Unreadable startsAt: " + iso, e);
                }
            }
            return parseDisplayString(appointment.getDate() + " | " + appointment.getTime());
        }

        /** Last resort: read back a string that was formatted for humans. */
        private Optional<Calendar> parseDisplayString(String appointmentDateTime) {
            // Both are tried because the server's time format has changed once
            // already, and a reminder is not worth a crash.
            for (String pattern : new String[]{"EEE, d MMM | h:mm a", "EEE, d MMM | h a"}) {
                try {
                    Date date = new SimpleDateFormat(pattern, Locale.ENGLISH).parse(appointmentDateTime);
                    if (date == null) {
                        continue;
                    }
                    Calendar now = Calendar.getInstance();
                    Calendar event = Calendar.getInstance();
                    event.setTime(date);
                    // The pattern carries no year, so it defaults to 1970.
                    event.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    if (event.before(now)) {
                        event.add(Calendar.YEAR, 1);
                    }
                    return Optional.of(event);
                } catch (ParseException ignored) {
                    // try the next pattern
                }
            }
            Log.w(TAG, "Could not read an appointment time from: " + appointmentDateTime);
            return Optional.empty();
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void scheduleExactAlarm(Context context, String title, String message, Calendar calendar) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                // Check if exact alarms are allowed
                if (alarmManager.canScheduleExactAlarms()) {
                    Intent intent = new Intent(context, Notification.class);
                    intent.putExtra(Notification.titleExtra, title);
                    intent.putExtra(Notification.messageExtra, message);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            (int) System.currentTimeMillis(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    try {
                        // Schedule the exact alarm
                        alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    } catch (SecurityException e) {
                        // Handle the exception and prompt the user
                        e.printStackTrace();
                        requestExactAlarmPermission(context);
                    }
                } else {
                    // Prompt the user to enable exact alarm permission
                    requestExactAlarmPermission(context);
                }
            }
        }

        public void requestExactAlarmPermission(Context context) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            context.startActivity(intent);
        }

        private void saveReminderState(String appointmentId, boolean isReminderEnabled) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(appointmentId, isReminderEnabled);
            editor.apply();
        }

        private boolean getReminderState(String appointmentId) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE);
            return sharedPreferences.getBoolean(appointmentId, false);
        }

        private void rescheduleAppointmentDialog(Appointment appointment, Doctor doctor) {
            new AlertDialog.Builder(context)
                    .setTitle("Reschedule Appointment")
                    .setMessage("Are you sure you want to reschedule the appointment? \n\n* This action will cancel the current appointment.")
                    .setPositiveButton("Yes", (dialog, which) -> Background.run(
                            () -> isAppointmentCancelled(appointment),
                            cancelled -> {
                                if (cancelled) {
                                    FragmentUtils.loadFragment(
                                            ((AppCompatActivity) context).getSupportFragmentManager(),
                                            R.id.flFragment, getBookAppointmentFragment(doctor));
                                } else {
                                    DialogUtils.showMessageDialog(context,
                                            "Appointment not cancelled, please try again later");
                                }
                            },
                            error -> DialogUtils.showMessageDialog(context,
                                    context.getString(R.string.error_no_server))))
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }

        private void showCancelConfirmationDialog(Appointment appointment) {
            new AlertDialog.Builder(context)
                    .setTitle("Cancel Appointment")
                    .setMessage("Are you sure you want to cancel the appointment?")
                    .setPositiveButton("Yes", (dialog, which) -> Background.run(
                            () -> isAppointmentCancelled(appointment),
                            cancelled -> {
                                if (cancelled) {
                                    showCancellationMessage();
                                } else {
                                    DialogUtils.showMessageDialog(context,
                                            "Appointment not cancelled, please try again later");
                                }
                            },
                            error -> DialogUtils.showMessageDialog(context,
                                    context.getString(R.string.error_no_server))))
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }

        private void showCancellationMessage() {
            new AlertDialog.Builder(context)
                    .setTitle("Appointment Cancelled")
                    .setMessage("Your appointment has been successfully cancelled.")
                    .setIcon(R.drawable.baseline_check_circle_24)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();

            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, new MedicalHistoryFragment());
        }

        private boolean isAppointmentCancelled(Appointment appointment) {
            return appointmentClient.cancelAppointment(appointment.getId());
        }
    }
}
