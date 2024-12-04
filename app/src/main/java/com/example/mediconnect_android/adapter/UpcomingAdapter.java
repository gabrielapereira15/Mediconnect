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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.List;

public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.ViewHolder> {

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

                if (isChecked) {
                    Calendar calendar = parseAndSetCalendar(String.valueOf(appointment));
                    Calendar now = Calendar.getInstance();

                    System.out.println("Now: " + now.getTime());
                    System.out.println("Appointment: " + calendar.getTime());

                    Calendar thirtyMinutesBefore = (Calendar) calendar.clone();
                    thirtyMinutesBefore.add(Calendar.MINUTE, -30);

                    System.out.println("30 min before: " + thirtyMinutesBefore.getTime());

                    if (thirtyMinutesBefore.after(now)) {
                        scheduleExactAlarm(
                                context.getApplicationContext(),
                                "Reminder: Appointment Soon",
                                "Your appointment with " + doctor.getName() + " is in 30 minutes.",
                                thirtyMinutesBefore
                        );
                    } else {
                        System.out.println("Skipping 30 minutes before, it’s in the past.");
                    }

                    Calendar twentyFourHoursBefore = (Calendar) calendar.clone();
                    twentyFourHoursBefore.add(Calendar.HOUR_OF_DAY, -24);

                    System.out.println("24 hours before: " + twentyFourHoursBefore.getTime());

                    if (twentyFourHoursBefore.after(now)) {
                        scheduleExactAlarm(
                                context.getApplicationContext(),
                                "Reminder: Appointment Tomorrow",
                                "You have an appointment with " + doctor.getName() + " tomorrow at " + appointment.toString() + ".",
                                twentyFourHoursBefore
                        );
                    } else {
                        System.out.println("Skipping 24 hours before, it’s in the past.");
                    }

                    Calendar oneMinuteFromNow = Calendar.getInstance();
                    oneMinuteFromNow.add(Calendar.SECOND, 10);

                    System.out.println("10 secs from now: " + oneMinuteFromNow.getTime());

                    scheduleExactAlarm(
                            context.getApplicationContext(),
                            "Reminder: Upcoming appointment",
                            "This is a test reminder set for 1 minute from now.",
                            oneMinuteFromNow
                    );
                }
            });
        }

        public Calendar parseAndSetCalendar(String appointmentDateTime) {
            String dateTimeFormat = "EEE, d MMM | h a";
            SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat, Locale.ENGLISH);

            try {
                Date date = sdf.parse(appointmentDateTime);

                Calendar currentCalendar = Calendar.getInstance();
                Calendar eventCalendar = Calendar.getInstance();

                if (date != null) {
                    eventCalendar.setTime(date);

                    eventCalendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));

                    if (eventCalendar.before(currentCalendar)) {
                        eventCalendar.add(Calendar.YEAR, 1);
                    }
                }

                System.out.println("Scheduled Event Calendar: " + eventCalendar.getTime());
                return eventCalendar;

            } catch (ParseException e) {
                e.printStackTrace();
                System.out.println("Failed to parse the appointment date and time.");
                return null;
            }
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

            System.out.println("Scheduled time: " + calendar.getTime());
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
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (isAppointmentCancelled(appointment)) {
                            BookAppointmentFragment bookAppointmentFragment = getBookAppointmentFragment(doctor);
                            FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, bookAppointmentFragment);
                        } else {
                            DialogUtils.showMessageDialog(context, "Appointment not cancelled, please try again later");
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }

        private void showCancelConfirmationDialog(Appointment appointment) {
            new AlertDialog.Builder(context)
                    .setTitle("Cancel Appointment")
                    .setMessage("Are you sure you want to cancel the appointment?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (isAppointmentCancelled(appointment)) {
                            showCancellationMessage();
                        } else {
                            DialogUtils.showMessageDialog(context, "Appointment not cancelled, please try again later");
                        }
                    })
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
