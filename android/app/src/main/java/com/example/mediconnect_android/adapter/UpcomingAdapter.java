package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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
import com.example.mediconnect_android.util.ReminderPermissions;
import com.example.mediconnect_android.util.ReminderPreference;
import com.example.mediconnect_android.util.VisitReminders;
import com.example.mediconnect_android.util.WhenLabel;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.Background;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class UpcomingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_VISIT = 1;

    /** A visit within this many days belongs under "This week". */
    private static final int THIS_WEEK_DAYS = 7;

    private final Context context;
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();
    private final AppointmentClient appointmentClient = new AppointmentClientImpl();

    /** Asks for what a reminder needs; registered by the fragment showing the list. */
    private final ReminderPermissions reminderPermissions;

    /** Either a heading or a visit; the list is built once, in order. */
    private final List<Object> rows = new ArrayList<>();

    /** Whether this list has already asked for notifications for the Profile default. */
    private boolean askedForDefault;

    public UpcomingAdapter(List<Appointment> appointmentList, Context context,
                           ReminderPermissions reminderPermissions) {
        this.context = context;
        this.reminderPermissions = reminderPermissions;
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

    /**
     * The Profile default says reminders on, but Android is not letting
     * notifications through, so no visit that inherits it can have them.
     *
     * Nobody asked, because the patient never touched a switch here: they
     * said so once, in Profile, possibly before Android 13 needed asking at
     * all. So the list asks, once, rather than showing every switch off
     * without a word. A yes binds the list again and the switches come on.
     * A no turns the default off, as it would in Profile, so the question
     * does not come back every time Visits opens.
     */
    private void askForDefault() {
        if (askedForDefault
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || !ReminderPreference.defaultOn(context)
                || VisitReminders.notificationsAllowed(context)) {
            return;
        }
        askedForDefault = true;
        // Posted, so the prompt does not start in the middle of laying out
        // the list.
        Background.onMain(() -> reminderPermissions.ensure(
                this::notifyDataSetChanged,
                () -> {
                    // Only a no to notifications. Refusing exact alarms
                    // leaves the default alone: once they are allowed, the
                    // switches come on by themselves.
                    if (!VisitReminders.notificationsAllowed(context)) {
                        ReminderPreference.setDefaultOn(context, false);
                    }
                }));
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

        /** The visit this card shows now; it changes when the card is recycled. */
        private Appointment bound;

        public ViewHolder(UpcomingItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void bindView(Appointment appointment) {
            bound = appointment;
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
            bindReminder(appointment);

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

        /**
         * The switch, and the alarms behind it.
         *
         * It shows on only when the alarms are set and Android will let
         * them through. A visit with no stored state inherits the patient's
         * default from Profile, and if that default is on, the alarms are
         * set here and now. Showing the switch on without scheduling
         * anything would be the worst of both: the patient trusts it and
         * misses the visit.
         */
        private void bindReminder(Appointment appointment) {
            // Set without the listener attached: recycling a card would
            // otherwise fire it with the previous visit's state and
            // schedule an alarm nobody asked for.
            recyclerItemBinding.switchRemindMe.setOnCheckedChangeListener(null);

            String id = appointment.getId();
            boolean on = false;
            // No prompts per card: binding happens card by card as the list
            // scrolls, and a dialog each would stack up. A visit that
            // cannot be reminded shows off until the patient turns it on,
            // which is where the asking happens; the one exception is the
            // Profile default, asked about once for the whole list.
            if (getReminderState(id) && VisitReminders.canRemind(context)) {
                // Set again rather than trusted, which also puts back
                // alarms that were lost and follows a visit that moved.
                on = VisitReminders.schedule(context, appointment);
                if (on && !hasStoredReminderState(id)) {
                    // Inherited rather than chosen, and now real, so kept.
                    saveReminderState(id, true);
                }
            } else if (!hasStoredReminderState(id)) {
                askForDefault();
            }
            showReminder(appointment, on);
        }

        /**
         * The patient flipped the switch.
         *
         * Off takes the alarms back. It used to leave them set, so a
         * reminder switched off still arrived. On has to be earned: the
         * switch drops back to off unless notifications and exact alarms
         * are both allowed and the alarms are actually set.
         */
        private void onReminderSwitched(Appointment appointment, boolean wanted) {
            String id = appointment.getId();
            if (!wanted) {
                VisitReminders.cancel(context, id);
                saveReminderState(id, false);
                return;
            }

            if (VisitReminders.startOf(appointment).isEmpty()) {
                // Cannot work out when it is, so there is nothing to
                // schedule. Previously this dereferenced null and crashed
                // the app, which looked like being logged out.
                DialogUtils.showMessageDialog(context,
                        context.getString(R.string.reminder_unavailable));
                saveReminderState(id, false);
                showReminder(appointment, false);
                return;
            }

            reminderPermissions.ensure(
                    () -> {
                        boolean applied = VisitReminders.schedule(context, appointment);
                        saveReminderState(id, applied);
                        showReminder(appointment, applied);
                    },
                    () -> {
                        saveReminderState(id, false);
                        showReminder(appointment, false);
                    });
        }

        /**
         * Puts the switch where the visit's reminders really are, if this
         * card still shows that visit. The permission prompt can outlast a
         * scroll, and a card that has moved on to another visit reads the
         * saved state when it is bound anyway.
         */
        private void showReminder(Appointment appointment, boolean on) {
            if (bound != appointment) {
                return;
            }
            recyclerItemBinding.switchRemindMe.setOnCheckedChangeListener(null);
            recyclerItemBinding.switchRemindMe.setChecked(on);
            recyclerItemBinding.switchRemindMe.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> onReminderSwitched(appointment, isChecked));
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
            Optional<LocalDateTime> start = VisitReminders.startOf(appointment);
            if (start.isEmpty()) {
                DialogUtils.showMessageDialog(context,
                        context.getString(R.string.reminder_unavailable));
                return;
            }

            String isoDate = start.get().toLocalDate().toString();

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

        private void saveReminderState(String appointmentId, boolean isReminderEnabled) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(appointmentId, isReminderEnabled);
            editor.apply();
        }

        /**
         * This visit's switch, or the patient's own default if they have
         * not touched it — so someone who asked for reminders in Profile
         * does not have to say so again on every card.
         */
        private boolean hasStoredReminderState(String appointmentId) {
            return context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
                    .contains(appointmentId);
        }

        private boolean getReminderState(String appointmentId) {
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE);
            return sharedPreferences.getBoolean(appointmentId,
                    ReminderPreference.defaultOn(context));
        }

        private void rescheduleAppointmentDialog(Appointment appointment, Doctor doctor) {
            new AlertDialog.Builder(context)
                    .setTitle("Reschedule Appointment")
                    .setMessage("Are you sure you want to reschedule the appointment? \n\n* This action will cancel the current appointment.")
                    .setPositiveButton("Yes", (dialog, which) -> Background.run(
                            () -> isAppointmentCancelled(appointment),
                            cancelled -> {
                                if (cancelled) {
                                    // Rescheduling books a new visit; the
                                    // old one's reminders go with it.
                                    VisitReminders.cancel(context, appointment.getId());
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
                                    VisitReminders.cancel(context, appointment.getId());
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
