package com.example.mediconnect_android.adapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import com.example.mediconnect_android.client.ApiException;
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
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
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();
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

            String bookedFor = appointment.getBookedForName();
            if (bookedFor == null || bookedFor.trim().isEmpty()) {
                recyclerItemBinding.bookedFor.setVisibility(View.GONE);
            } else {
                recyclerItemBinding.bookedFor.setVisibility(View.VISIBLE);
                recyclerItemBinding.bookedFor.setText(
                        context.getString(R.string.booked_for, bookedFor));
            }

            Glide.with(context)
                    .load(doctor.getPhoto())
                    .placeholder(R.drawable.doctorimage)
                    .error(R.drawable.doctorimage)
                    .into(recyclerItemBinding.doctorImage);

            boolean isReminderEnabled = getReminderState(appointment.getId());
            recyclerItemBinding.switchRemindMe.setChecked(isReminderEnabled);

            recyclerItemBinding.waitlistButton.setOnClickListener(v -> joinWaitlist(appointment, doctor));

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

                if (!canScheduleExactAlarms()) {
                    // Android 12 onwards withholds this by default, so the
                    // first reminder on a modern phone always stops here.
                    buttonView.setChecked(false);
                    saveReminderState(appointment.getId(), false);
                    requestExactAlarmPermission(context);
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

        /**
         * Asks to be told if this doctor gets an earlier opening.
         *
         * The date sent is the one they already hold, parsed from the ISO
         * timestamp rather than the display string — the server only offers
         * slots earlier than it, so getting it wrong would either flood them
         * with useless offers or silently exclude them.
         */
        private void joinWaitlist(Appointment appointment, Doctor doctor) {
            Optional<Calendar> start = appointmentStart(appointment);
            if (start.isEmpty()) {
                DialogUtils.showMessageDialog(context,
                        context.getString(R.string.reminder_unavailable));
                return;
            }

            String isoDate = String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                    start.get().get(Calendar.YEAR),
                    start.get().get(Calendar.MONTH) + 1,
                    start.get().get(Calendar.DAY_OF_MONTH));

            new AlertDialog.Builder(context)
                    .setTitle(R.string.waitlist_join)
                    .setMessage(context.getString(R.string.waitlist_join_body, doctor.getName()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.waitlist_join_confirm, (dialog, which) -> Background.run(
                            () -> waitlistClient.join(email(), doctor.getId(), isoDate),
                            joined -> DialogUtils.showMessageDialog(context,
                                    context.getString(R.string.waitlist_joined)),
                            error -> DialogUtils.showMessageDialog(context,
                                    error instanceof ApiException
                                            ? error.getMessage()
                                            : context.getString(R.string.error_no_server))))
                    .show();
        }

        private String email() {
            return context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    .getString("email", "");
        }

        /** Whether the system will let this app set an alarm to the minute. */
        private boolean canScheduleExactAlarms() {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && exactAlarmsAllowed(alarmManager);
        }

        private boolean exactAlarmsAllowed(AlarmManager alarmManager) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Granted at install time before Android 12.
                return true;
            }
            return alarmManager.canScheduleExactAlarms();
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

        /**
         * Sets one alarm. Callable on every supported version: the permission
         * check it used to make unconditionally only exists from Android 12,
         * so on anything older it threw NoSuchMethodError instead.
         */
        private void scheduleExactAlarm(Context context, String title, String message, Calendar calendar) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                if (exactAlarmsAllowed(alarmManager)) {
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
                        // Revoked between the check above and here.
                        Log.w(TAG, "Exact alarm refused while scheduling", e);
                    }
                } else {
                    Log.w(TAG, "Exact alarms unavailable at scheduling time");
                }
            }
        }

        /**
         * Sends the patient to the system screen that grants exact alarms.
         *
         * From Android 12 this permission is off by default, so the first
         * "Remind me" on a modern device always lands here. It used to launch
         * straight from the adapter's context and crash — startActivity needs
         * NEW_TASK when it is not called from an Activity — and it did so
         * without ever saying why the screen had appeared.
         */
        private void requestExactAlarmPermission(Context context) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.reminder_permission_title)
                    .setMessage(R.string.reminder_permission_body)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.reminder_permission_open,
                            (dialog, which) -> openExactAlarmSettings(context))
                    .show();
        }

        private void openExactAlarmSettings(Context context) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            // Straight to this app's entry rather than the whole list.
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Some builds do not ship the screen at all.
                Log.w(TAG, "No exact-alarm settings screen on this device", e);
                DialogUtils.showMessageDialog(context,
                        context.getString(R.string.reminder_permission_unavailable));
            }
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
                    .setIcon(R.drawable.ic_check_circle)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();

            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, new MedicalHistoryFragment());
        }

        private boolean isAppointmentCancelled(Appointment appointment) {
            return appointmentClient.cancelAppointment(appointment.getId());
        }
    }
}
