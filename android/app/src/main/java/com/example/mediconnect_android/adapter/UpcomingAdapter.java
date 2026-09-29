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
import androidx.core.content.ContextCompat;
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
import com.example.mediconnect_android.databinding.ViewSectionHeaderBinding;
import com.example.mediconnect_android.fragment.BookAppointmentFragment;
import com.example.mediconnect_android.fragment.MedicalHistoryFragment;
import com.example.mediconnect_android.fragment.PreAppointmentFormFragment;
import com.example.mediconnect_android.fragment.VisitDetailFragment;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.WhenLabel;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.Notification;
import com.example.mediconnect_android.util.Background;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import android.util.Log;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Locale;
import java.util.Date;
import java.util.List;

public class UpcomingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "UpcomingAdapter";

    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_VISIT = 1;

    /** A visit within this many days belongs under "This week". */
    private static final int THIS_WEEK_DAYS = 7;

    private final Context context;
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();
    private final AppointmentClient appointmentClient = new AppointmentClientImpl();

    /** Either a heading or a visit; the list is built once, in order. */
    private final List<Object> rows = new ArrayList<>();

    public UpcomingAdapter(List<Appointment> appointmentList, Context context) {
        this.context = context;
        buildRows(appointmentList);
    }

    /**
     * Groups the visits under "This week" and "Later".
     *
     * A flat list of eight cards makes a patient read every date to find
     * the one that is nearly here. The headings do that reading for them,
     * and a heading only appears when it has something under it.
     */
    private void buildRows(List<Appointment> appointments) {
        if (appointments == null) {
            return;
        }
        LocalDate soon = LocalDate.now().plusDays(THIS_WEEK_DAYS);
        List<Appointment> thisWeek = new ArrayList<>();
        List<Appointment> later = new ArrayList<>();

        for (Appointment appointment : appointments) {
            LocalDate day = WhenLabel.parse(appointment.getStartsAt())
                    .map(LocalDateTime::toLocalDate)
                    .orElse(null);
            if (day != null && day.isAfter(soon)) {
                later.add(appointment);
            } else {
                thisWeek.add(appointment);
            }
        }

        if (!thisWeek.isEmpty()) {
            rows.add(context.getString(R.string.visit_this_week));
            rows.addAll(thisWeek);
        }
        if (!later.isEmpty()) {
            rows.add(context.getString(R.string.visit_later));
            rows.addAll(later);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof Appointment ? TYPE_VISIT : TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(ViewSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new ViewHolder(UpcomingItemBinding.inflate(inflater, parent, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).bindView((Appointment) row);
        } else {
            ((HeaderViewHolder) holder).binding.sectionTitle.setText((String) row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        final ViewSectionHeaderBinding binding;

        HeaderViewHolder(ViewSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        UpcomingItemBinding recyclerItemBinding;

        public ViewHolder(UpcomingItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void bindView(Appointment appointment) {
            Doctor doctor = appointment.getDoctor();

            WhenLabel.parse(appointment.getStartsAt()).ifPresent(at -> {
                recyclerItemBinding.badgeWeekday.setText(at.format(WEEKDAY));
                recyclerItemBinding.badgeDay.setText(String.valueOf(at.getDayOfMonth()));
                recyclerItemBinding.appointmentTime.setText(WhenLabel.timeWords(at));
                bindDateBadge(at);
            });

            recyclerItemBinding.doctorName.setText(context.getString(R.string.doctor_and_specialty,
                    WhenLabel.doctorName(doctor.getName()), doctor.getSpecialty()));

            String bookedFor = appointment.getBookedForName();
            boolean forSomeoneElse = bookedFor != null && !bookedFor.trim().isEmpty();
            recyclerItemBinding.bookedFor.setVisibility(forSomeoneElse ? View.VISIBLE : View.GONE);
            if (forSomeoneElse) {
                recyclerItemBinding.bookedFor.setText(
                        context.getString(R.string.booked_for, bookedFor));
            }

            bindStatus(appointment);
            bindActions(appointment, doctor);
            bindReminder(appointment, doctor);

            recyclerItemBinding.getRoot().setOnClickListener(v -> openVisit(appointment));
        }

        /**
         * The next visit's date block is filled; the ones behind it are not.
         * Eight identical brand-coloured blocks would say nothing about
         * which one is nearly here.
         */
        private void bindDateBadge(LocalDateTime at) {
            boolean soon = !at.toLocalDate().isAfter(LocalDate.now().plusDays(1));
            recyclerItemBinding.dateBadge.setBackgroundResource(
                    soon ? R.drawable.date_badge_brand : R.drawable.date_badge_quiet);
            int colour = soon ? R.color.md_on_primary : R.color.md_on_surface;
            recyclerItemBinding.badgeWeekday.setTextColor(ContextCompat.getColor(context, colour));
            recyclerItemBinding.badgeDay.setTextColor(ContextCompat.getColor(context, colour));
        }

        /**
         * What the visit is waiting on, in a word.
         *
         * Nothing recorded whether the form had been sent until the
         * appointment started carrying the answer, so this used to be the
         * same sentence on every card whether it was true or not.
         */
        private void bindStatus(Appointment appointment) {
            boolean formIn = appointment.isFormSubmitted();
            recyclerItemBinding.statusBadge.setText(formIn
                    ? R.string.visit_confirmed
                    : R.string.visit_form_pending);
            recyclerItemBinding.statusBadge.setBackgroundResource(formIn
                    ? R.drawable.badge_success
                    : R.drawable.badge_sun);
            recyclerItemBinding.statusBadge.setTextColor(ContextCompat.getColor(context,
                    formIn ? R.color.md_success : R.color.md_on_rating_container));
        }

        /**
         * One primary action, and it is whatever the visit still needs. The
         * card used to carry five buttons of equal weight, which left a
         * patient working out which of them was the point.
         */
        private void bindActions(Appointment appointment, Doctor doctor) {
            boolean formIn = appointment.isFormSubmitted();
            recyclerItemBinding.primaryButton.setText(formIn
                    ? R.string.visit_details
                    : R.string.visit_fill_in_form);
            recyclerItemBinding.primaryButton.setOnClickListener(v -> {
                if (formIn) {
                    openVisit(appointment);
                } else {
                    FragmentUtils.loadFragment(
                            ((AppCompatActivity) context).getSupportFragmentManager(),
                            R.id.flFragment, PreAppointmentFormFragment.of(appointment));
                }
            });

            recyclerItemBinding.rescheduleButton.setOnClickListener(
                    v -> rescheduleAppointmentDialog(appointment, doctor));
        }

        private void bindReminder(Appointment appointment, Doctor doctor) {
            // Set without the listener attached: recycling a card would
            // otherwise fire it with the previous visit's state and
            // schedule an alarm nobody asked for.
            recyclerItemBinding.switchRemindMe.setOnCheckedChangeListener(null);
            recyclerItemBinding.switchRemindMe.setChecked(getReminderState(appointment.getId()));

            recyclerItemBinding.switchRemindMe.setOnCheckedChangeListener((buttonView, isChecked) -> {
                saveReminderState(appointment.getId(), isChecked);

                if (!isChecked) {
                    return;
                }

                Optional<Calendar> start = appointmentStart(appointment);
                if (start.isEmpty()) {
                    // Cannot work out when it is, so there is nothing to
                    // schedule. Previously this dereferenced null and
                    // crashed the app, which looked like being logged out.
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

        private void openVisit(Appointment appointment) {
            FragmentUtils.loadFragment(
                    ((AppCompatActivity) context).getSupportFragmentManager(),
                    R.id.flFragment, VisitDetailFragment.of(appointment));
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
                                    BookAppointmentFragment.open(
                                            ((AppCompatActivity) context).getSupportFragmentManager(),
                                            doctor.getId(), doctor.getName(),
                                            doctor.getSpecialty());
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
