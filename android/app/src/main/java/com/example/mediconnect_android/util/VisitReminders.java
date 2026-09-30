package com.example.mediconnect_android.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.model.Appointment;
import com.example.mediconnect_android.model.Doctor;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A visit's two reminders: the day before, and 30 minutes before.
 *
 * Each alarm used to be set with the time of setting it as its request
 * code, which is a key the app could never produce again, so nothing could
 * take a reminder back. Cancelling or rescheduling a visit, switching its
 * reminder off and signing out all left both alarms set, and a patient on a
 * shared phone could be told about the previous patient's visit. Every alarm
 * is now keyed by its appointment and by which of the two it is, so it can
 * always be found again.
 *
 * What is set is also written down here, with the words the notification
 * will show. Android forgets every alarm when the phone restarts, and this
 * is what puts them back; the receiver also checks it before showing
 * anything, so an alarm that somehow outlived its visit stays quiet.
 */
public final class VisitReminders {

    private static final String TAG = "VisitReminders";

    /** The store. SessionManager empties it on sign-out with the rest. */
    public static final String PREFS = "VisitReminders";

    /** The first alarm: the same time, the day before. */
    static final int DAY_BEFORE = 0;
    /** The second: half an hour before the visit starts. */
    static final int SOON = 1;

    private static final int[] SLOTS = {DAY_BEFORE, SOON};

    /** The only status whose reminders are still worth sending. */
    private static final String UPCOMING = "UPCOMING";

    private static final String SCHEME = "mediconnect-reminder";

    private static final Gson GSON = new Gson();

    private VisitReminders() {
    }

    /** What the store keeps for one visit. Plain fields, so Gson reads it back as written. */
    static final class Entry {
        /** The start the alarms were worked out from, as an ISO local date-time. */
        String startsAt;
        List<Alarm> alarms = new ArrayList<>();
    }

    /** One alarm: when it goes off, and what it says when it does. */
    static final class Alarm {
        int slot;
        /** Epoch milliseconds. */
        long at;
        String title;
        String body;

        Alarm() {
        }

        Alarm(int slot, long at, String title, String body) {
            this.slot = slot;
            this.at = at;
            this.title = title;
            this.body = body;
        }
    }

    // ---- what Android allows ----------------------------------------------

    /**
     * Whether a notification from this app would reach the patient at all.
     *
     * From Android 13 that is a runtime permission a new install starts
     * without; before it, the patient can still turn notifications off in
     * Settings. Either way a reminder would be set and then never seen.
     */
    public static boolean notificationsAllowed(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Whether the system will let this app set an alarm to the minute. */
    public static boolean exactAlarmsAllowed(Context context) {
        AlarmManager alarmManager = alarmManager(context);
        return alarmManager != null && exactAlarmsAllowed(alarmManager);
    }

    private static boolean exactAlarmsAllowed(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Granted at install time before Android 12.
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }

    /** Both of the above: what a switch showing "on" has to be able to promise. */
    public static boolean canRemind(Context context) {
        return notificationsAllowed(context) && exactAlarmsAllowed(context);
    }

    // ---- setting and taking back ------------------------------------------

    /**
     * Sets both of a visit's reminders, replacing any it already had.
     *
     * Returns whether reminders are in place afterwards, which is what a
     * switch should show. Calling it again for the same visit moves the
     * alarms rather than adding two more, because they are keyed by the
     * visit; that is how a visit the clinic moved gets reminded at its new
     * time. An alarm whose moment has already passed is left out.
     */
    public static boolean schedule(Context context, Appointment appointment) {
        Context app = context.getApplicationContext();
        String id = appointment.getId();
        Optional<LocalDateTime> start = startOf(appointment);
        if (id == null || start.isEmpty()) {
            Log.w(TAG, "No start time to remind about for appointment " + id);
            return false;
        }
        AlarmManager alarmManager = alarmManager(app);
        if (alarmManager == null || !exactAlarmsAllowed(alarmManager)) {
            return false;
        }

        // Whatever was set before goes first. A visit moved closer may no
        // longer have a "day before", and the old one must not stay behind.
        cancelAlarms(app, alarmManager, id);

        Doctor doctor = appointment.getDoctor();
        String doctorName = WhenLabel.doctorName(doctor == null ? null : doctor.getName());
        long now = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();

        Entry entry = new Entry();
        entry.startsAt = start.get().toString();
        for (int slot : SLOTS) {
            long at = triggerAt(start.get(), slot, zone);
            if (at <= now) {
                continue;
            }
            Alarm alarm = slot == DAY_BEFORE
                    ? new Alarm(slot, at,
                            app.getString(R.string.reminder_tomorrow_title),
                            app.getString(R.string.reminder_tomorrow_body,
                                    doctorName, WhenLabel.timeWords(start.get())))
                    : new Alarm(slot, at,
                            app.getString(R.string.reminder_soon_title),
                            app.getString(R.string.reminder_soon_body, doctorName));
            if (!arm(app, alarmManager, id, alarm)) {
                // Half a set of reminders would be worse than none, because
                // the switch would say they are on.
                cancelAlarms(app, alarmManager, id);
                store(app).edit().remove(id).apply();
                return false;
            }
            entry.alarms.add(alarm);
        }
        store(app).edit().putString(id, encode(entry)).apply();
        return true;
    }

    /**
     * Takes back both of a visit's reminders, and any it has already shown.
     *
     * Works whether or not the store knows about the visit: the alarms are
     * found by the same key they were set with, not by looking them up.
     */
    public static void cancel(Context context, String appointmentId) {
        if (appointmentId == null) {
            return;
        }
        Context app = context.getApplicationContext();
        AlarmManager alarmManager = alarmManager(app);
        if (alarmManager != null) {
            cancelAlarms(app, alarmManager, appointmentId);
        }
        // A "tomorrow" reminder still sitting in the shade is about a visit
        // that no longer exists, so it goes too.
        NotificationManagerCompat shade = NotificationManagerCompat.from(app);
        for (int slot : SLOTS) {
            shade.cancel(requestCode(appointmentId, slot));
        }
        store(app).edit().remove(appointmentId).apply();
    }

    /**
     * Every reminder that was set, for signing out.
     *
     * They belong to the patient who is leaving. Needs only a context, since
     * a session that expires signs out with no screen involved.
     */
    public static void cancelAll(Context context) {
        Context app = context.getApplicationContext();
        for (String appointmentId : new ArrayList<>(store(app).getAll().keySet())) {
            cancel(app, appointmentId);
        }
        store(app).edit().clear().apply();
    }

    /**
     * Sets the stored reminders again, after a restart or an app update.
     *
     * Only the ones still ahead: a reminder for a moment that passed while
     * the phone was off would arrive late and say something untrue.
     */
    public static void restoreAll(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager alarmManager = alarmManager(app);
        if (alarmManager == null || !exactAlarmsAllowed(alarmManager)) {
            // Nothing can be set now. Opening Visits sets them again once
            // the patient allows exact alarms.
            Log.w(TAG, "Exact alarms unavailable; reminders not restored");
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ?> stored : store(app).getAll().entrySet()) {
            Entry entry = decode(stored.getValue() instanceof String
                    ? (String) stored.getValue() : null);
            if (entry == null) {
                continue;
            }
            for (Alarm alarm : entry.alarms) {
                if (alarm.at > now) {
                    arm(app, alarmManager, stored.getKey(), alarm);
                }
            }
        }
    }

    /**
     * Brings the reminders in line with the visits as the server has them.
     *
     * The Visits list is how the app hears that the clinic cancelled a
     * visit or moved it, and a reminder for either would be wrong. A visit
     * that is no longer upcoming loses its reminders; one that still is
     * gets them set again, so they follow it if its time changed.
     */
    public static void sync(Context context, List<Appointment> appointments) {
        Context app = context.getApplicationContext();
        Map<String, Appointment> upcoming = upcomingById(appointments);
        boolean canRemind = canRemind(app);
        for (String appointmentId : new ArrayList<>(store(app).getAll().keySet())) {
            Appointment visit = upcoming.get(appointmentId);
            if (visit == null) {
                cancel(app, appointmentId);
            } else if (canRemind) {
                schedule(app, visit);
            }
        }
    }

    /**
     * The alarm that has just gone off, if it should still be shown.
     *
     * Null when the store no longer holds it: its visit was cancelled, its
     * reminder switched off, or the patient who set it has signed out.
     */
    static Alarm stillWanted(Context context, String appointmentId, int slot) {
        if (appointmentId == null) {
            return null;
        }
        Entry entry = decode(store(context.getApplicationContext())
                .getString(appointmentId, null));
        if (entry == null) {
            return null;
        }
        for (Alarm alarm : entry.alarms) {
            if (alarm.slot == slot) {
                return alarm;
            }
        }
        return null;
    }

    // ---- the pieces that need no phone, kept apart so they can be tested ----

    /**
     * The request code for one of a visit's alarms.
     *
     * The same visit always gives the same code, which is what lets an
     * alarm be cancelled later. Two alarms a visit, so doubling the hash
     * keeps a visit's pair from ever landing on each other. It doubles as
     * the notification's id, so cancelling a visit can clear what it showed.
     */
    static int requestCode(String appointmentId, int slot) {
        return appointmentId.hashCode() * 2 + slot;
    }

    /**
     * When one of a visit's alarms goes off, in epoch milliseconds.
     *
     * "The day before" is the same time on the previous day rather than 24
     * hours earlier, so it still says the right time across a clock change.
     */
    static long triggerAt(LocalDateTime start, int slot, ZoneId zone) {
        LocalDateTime at = slot == DAY_BEFORE ? start.minusDays(1) : start.minusMinutes(30);
        return at.atZone(zone).toInstant().toEpochMilli();
    }

    /** The moment an appointment starts, empty if it cannot be worked out. */
    public static Optional<LocalDateTime> startOf(Appointment appointment) {
        return startOf(appointment, LocalDateTime.now());
    }

    /**
     * Prefers the API's ISO timestamp. The fallback parses the display
     * string, which is fragile — it broke the moment the server started
     * including minutes in the time — so it is only a last resort, and it
     * returns empty rather than null so a failure cannot crash the caller.
     */
    static Optional<LocalDateTime> startOf(Appointment appointment, LocalDateTime now) {
        Optional<LocalDateTime> iso = WhenLabel.parse(appointment.getStartsAt());
        if (iso.isPresent()) {
            return iso;
        }
        return parseDisplayString(appointment.getDate() + " | " + appointment.getTime(), now);
    }

    /** Last resort: read back a string that was formatted for humans. */
    private static Optional<LocalDateTime> parseDisplayString(String text, LocalDateTime now) {
        // Both are tried because the server's time format has changed once
        // already, and a reminder is not worth a crash.
        for (String pattern : new String[]{"EEE, d MMM | h:mm a", "EEE, d MMM | h a"}) {
            try {
                Date date = new SimpleDateFormat(pattern, Locale.ENGLISH).parse(text);
                if (date == null) {
                    continue;
                }
                Calendar parsed = Calendar.getInstance();
                parsed.setTime(date);
                // The pattern carries no year, so it came back as 1970.
                LocalDateTime event = LocalDateTime.of(now.getYear(),
                        parsed.get(Calendar.MONTH) + 1, parsed.get(Calendar.DAY_OF_MONTH),
                        parsed.get(Calendar.HOUR_OF_DAY), parsed.get(Calendar.MINUTE));
                if (event.isBefore(now)) {
                    event = event.plusYears(1);
                }
                return Optional.of(event);
            } catch (ParseException ignored) {
                // try the next pattern
            }
        }
        return Optional.empty();
    }

    /** The visits whose reminders should stand, by id. */
    static Map<String, Appointment> upcomingById(List<Appointment> appointments) {
        Map<String, Appointment> upcoming = new HashMap<>();
        if (appointments == null) {
            return upcoming;
        }
        for (Appointment appointment : appointments) {
            if (appointment != null && appointment.getId() != null
                    && UPCOMING.equals(appointment.getStatus())) {
                upcoming.put(appointment.getId(), appointment);
            }
        }
        return upcoming;
    }

    static String encode(Entry entry) {
        return GSON.toJson(entry);
    }

    /** Null for anything unreadable, which counts as nothing set. */
    static Entry decode(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Entry entry = GSON.fromJson(json, Entry.class);
            if (entry != null && entry.alarms == null) {
                entry.alarms = new ArrayList<>();
            }
            return entry;
        } catch (JsonParseException e) {
            return null;
        }
    }

    // ---- plumbing -----------------------------------------------------------

    /** Sets one alarm; false if Android refused it. */
    private static boolean arm(Context app, AlarmManager alarmManager, String appointmentId,
                               Alarm alarm) {
        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm.at,
                    pendingIntent(app, appointmentId, alarm.slot));
            return true;
        } catch (SecurityException e) {
            // Exact alarms were revoked between the check and here.
            Log.w(TAG, "Exact alarm refused while scheduling", e);
            return false;
        }
    }

    private static void cancelAlarms(Context app, AlarmManager alarmManager, String appointmentId) {
        for (int slot : SLOTS) {
            PendingIntent pending = pendingIntent(app, appointmentId, slot);
            alarmManager.cancel(pending);
            pending.cancel();
        }
    }

    /**
     * The broadcast behind one alarm, built the same way every time.
     *
     * The visit and the slot go in the data as well as the request code, so
     * two visits can never share an alarm even if their codes collide, and
     * cancelling finds it without having set it in this process.
     */
    private static PendingIntent pendingIntent(Context app, String appointmentId, int slot) {
        Intent intent = new Intent(app, Notification.class)
                .setData(Uri.fromParts(SCHEME, appointmentId + "/" + slot, null))
                .putExtra(Notification.appointmentIdExtra, appointmentId)
                .putExtra(Notification.slotExtra, slot);
        return PendingIntent.getBroadcast(app, requestCode(appointmentId, slot), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static AlarmManager alarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static SharedPreferences store(Context app) {
        return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
