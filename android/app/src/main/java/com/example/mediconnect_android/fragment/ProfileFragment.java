package com.example.mediconnect_android.fragment;

import static com.example.mediconnect_android.util.FragmentUtils.loadFragment;
import static com.example.mediconnect_android.util.ImageUtils.getImageFromInternalStorage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.activity.WelcomeActivity;
import com.example.mediconnect_android.client.WaitlistClient;
import com.example.mediconnect_android.client.WaitlistClientImpl;
import com.example.mediconnect_android.databinding.FragmentProfileBinding;
import com.example.mediconnect_android.databinding.ViewDetailRowBinding;
import com.example.mediconnect_android.model.WaitlistEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.ReminderPermissions;
import com.example.mediconnect_android.util.ReminderPreference;
import com.example.mediconnect_android.util.SessionManager;
import com.example.mediconnect_android.util.ThemePreference;
import com.example.mediconnect_android.util.VisitReminders;
import com.example.mediconnect_android.util.WhenLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Profile and settings (board P14).
 *
 * Who the clinic thinks you are, how the app behaves, and what you can do
 * with your own data — in that order, because that is the order a patient
 * asks them in. Settings and Sign out used to live behind a hamburger.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private final WaitlistClient waitlistClient = new WaitlistClientImpl();

    /** The queues this patient is in, which the offers switch reflects. */
    private final List<WaitlistEntry> waitlists = new ArrayList<>();

    /** Registered here, as a field, because the Activity Result API needs it before onCreate. */
    private final ReminderPermissions reminderPermissions = new ReminderPermissions(this);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.nav_profile);
    }

    private void init() {
        bindHeader();
        bindDetails();
        bindThemeToggle();
        bindReminderSwitch();
        bindDataRows();
        bindActions();
        loadWaitlists();

        binding.swipeRefresh.setOnRefreshListener(() -> {
            bindHeader();
            bindDetails();
            loadWaitlists();
        });
    }

    // ---- who you are -------------------------------------------------------

    private void bindHeader() {
        SharedPreferences prefs = prefs();
        String fullName = (prefs.getString("first_name", "") + " "
                + prefs.getString("last_name", "")).trim();

        binding.tvFirstName.setText(fullName);
        binding.tvEmail.setText(prefs.getString("email", ""));
        binding.profileInitials.setText(WhenLabel.initials(fullName));

        Bitmap photo = getImageFromInternalStorage(getContext(), "profile_image.jpg");
        binding.profileImage.setVisibility(photo == null ? View.GONE : View.VISIBLE);
        if (photo != null) {
            binding.profileImage.setImageBitmap(photo);
        }
    }

    private void bindDetails() {
        SharedPreferences prefs = prefs();
        binding.detailRows.removeAllViews();

        addDetail(R.string.profile_phone, prefs.getString("phone_number", ""));
        addDetail(R.string.profile_address, prefs.getString("address", ""));
        addDetail(R.string.profile_dob, prefs.getString("dob", ""));
        addDetail(R.string.profile_health_card, healthCard(prefs));
        addDetail(R.string.profile_clinic, clinic(prefs));
    }

    private void addDetail(int labelRes, String value) {
        ViewDetailRowBinding row =
                ViewDetailRowBinding.inflate(getLayoutInflater(), binding.detailRows, false);
        row.detailLabel.setText(labelRes);
        // A blank row would read as though the clinic has it and is not
        // showing it, which is the opposite of true.
        row.detailValue.setText(value == null || value.trim().isEmpty()
                ? getString(R.string.profile_not_given)
                : value);

        if (binding.detailRows.getChildCount() > 0) {
            binding.detailRows.addView(divider());
        }
        binding.detailRows.addView(row.getRoot());
    }

    /**
     * The card with only its last digits.
     *
     * A health card number identifies someone to an entire provincial
     * system, so the whole of it does not belong on a screen being held on
     * a bus. The last four are enough to tell which card it is.
     */
    private String healthCard(SharedPreferences prefs) {
        String number = prefs.getString("health_card_number", "");
        String province = prefs.getString("health_card_province", "");
        if (number == null || number.length() < 4) {
            return "";
        }
        return getString(R.string.profile_health_card_value,
                number.substring(number.length() - 4),
                province == null ? "" : province);
    }

    private String clinic(SharedPreferences prefs) {
        String code = prefs.getString("clinic_code", "");
        if (code == null || code.trim().isEmpty()) {
            return "";
        }
        return getString(R.string.profile_clinic_value, getString(R.string.clinic_name), code);
    }

    private View divider() {
        View line = new View(requireContext());
        line.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.stroke_hairline)));
        line.setBackgroundColor(androidx.core.content.ContextCompat.getColor(
                requireContext(), R.color.md_outline_variant));
        return line;
    }

    // ---- preferences --------------------------------------------------------

    private void bindThemeToggle() {
        int saved = ThemePreference.get(requireContext());
        binding.themeToggle.check(switch (saved) {
            case ThemePreference.MODE_LIGHT -> R.id.themeLight;
            case ThemePreference.MODE_DARK -> R.id.themeDark;
            default -> R.id.themeSystem;
        });

        binding.themeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int chosen = ThemePreference.MODE_SYSTEM;
            if (checkedId == R.id.themeLight) {
                chosen = ThemePreference.MODE_LIGHT;
            } else if (checkedId == R.id.themeDark) {
                chosen = ThemePreference.MODE_DARK;
            }
            // Only act on a real change. Applying a night mode recreates
            // every activity, and doing that while binding the initial state
            // would put the screen in a loop.
            if (chosen != ThemePreference.get(requireContext())) {
                ThemePreference.set(requireContext(), chosen);
            }
        });
    }

    /**
     * Whether a newly booked visit starts with its reminder on.
     *
     * Each visit still has its own switch; this is what a new one inherits,
     * so a patient who wants reminders does not have to say so eight times.
     */
    private void bindReminderSwitch() {
        binding.switchReminders.switchIcon.setImageResource(R.drawable.ic_bell);
        binding.switchReminders.switchTitle.setText(R.string.profile_reminders);
        binding.switchReminders.switchSub.setText(R.string.profile_reminders_body);
        // On only if a visit inheriting it would really be reminded. With
        // notifications blocked since, it would promise what it cannot do.
        boolean remindersOn = ReminderPreference.defaultOn(requireContext())
                && VisitReminders.canRemind(requireContext());
        showReminderDefault(remindersOn);

        binding.switchReminders.switchRow.setOnClickListener(v -> {
            Context app = requireContext().getApplicationContext();
            if (binding.switchReminders.switchToggle.isChecked()) {
                ReminderPreference.setDefaultOn(app, false);
                showReminderDefault(false);
                return;
            }
            // This turns reminders on for every visit that inherits it, so
            // it asks for what they need now, while the patient is thinking
            // about reminders, and stays off if the answer is no.
            reminderPermissions.ensure(
                    () -> {
                        ReminderPreference.setDefaultOn(app, true);
                        showReminderDefault(true);
                    },
                    () -> {
                        ReminderPreference.setDefaultOn(app, false);
                        showReminderDefault(false);
                    });
        });
    }

    /** The answer can come back after the screen has gone, hence the check. */
    private void showReminderDefault(boolean on) {
        if (binding == null) {
            return;
        }
        binding.switchReminders.switchToggle.setChecked(on);
        describeSwitch(binding.switchReminders, R.string.profile_reminders, on);
    }

    /**
     * Whether the clinic may offer earlier slots.
     *
     * It reflects something real: the queues the patient is actually in.
     * Turning it off leaves all of them, which is the only thing "no
     * thank you" could honestly mean.
     */
    private void loadWaitlists() {
        bindOffersSwitch();

        Background.run(
                () -> waitlistClient.list(email()),
                entries -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    waitlists.clear();
                    if (entries != null) {
                        waitlists.addAll(entries.stream()
                                .filter(WaitlistEntry::isOpen)
                                .collect(Collectors.toList()));
                    }
                    bindOffersSwitch();
                },
                error -> {
                    if (binding != null) {
                        binding.swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void bindOffersSwitch() {
        int count = waitlists.size();
        boolean on = count > 0;

        binding.switchOffers.switchIcon.setImageResource(R.drawable.ic_hourglass);
        binding.switchOffers.switchTitle.setText(R.string.profile_offers);
        binding.switchOffers.switchToggle.setChecked(on);
        binding.switchOffers.switchToggle.setEnabled(on);
        describeSwitch(binding.switchOffers, R.string.profile_offers, on);

        if (count == 1) {
            binding.switchOffers.switchSub.setText(
                    getString(R.string.profile_offers_body_on, count));
        } else if (count > 1) {
            binding.switchOffers.switchSub.setText(
                    getString(R.string.profile_offers_body_many, count));
        } else {
            binding.switchOffers.switchSub.setText(R.string.profile_offers_body_off);
        }

        // With no waitlists there is nothing to turn off, and turning it on
        // is done from a visit — which is where the doctor is named.
        binding.switchOffers.switchRow.setOnClickListener(on ? v -> confirmLeaveAll() : null);
        binding.switchOffers.switchRow.setClickable(on);
    }

    /**
     * Says the state aloud.
     *
     * The switch itself is not focusable — the whole row is the target —
     * so without this a screen reader read the title and never said
     * whether it was on.
     */
    private void describeSwitch(
            com.example.mediconnect_android.databinding.ViewSwitchRowBinding row,
            int titleRes, boolean on) {
        row.switchRow.setContentDescription(getString(R.string.cd_switch,
                getString(titleRes), getString(on ? R.string.cd_on : R.string.cd_off)));
    }

    private void confirmLeaveAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_offers)
                .setMessage(getString(waitlists.size() == 1
                        ? R.string.profile_offers_body_on
                        : R.string.profile_offers_body_many, waitlists.size()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_offers_leave, (dialog, which) -> leaveAll())
                .show();
    }

    private void leaveAll() {
        List<WaitlistEntry> leaving = new ArrayList<>(waitlists);
        Background.run(
                () -> {
                    for (WaitlistEntry entry : leaving) {
                        waitlistClient.leave(email(), entry.getId());
                    }
                    return true;
                },
                left -> {
                    if (binding == null) {
                        return;
                    }
                    waitlists.clear();
                    bindOffersSwitch();
                    DialogUtils.showMessageDialog(getContext(),
                            getString(R.string.profile_offers_left));
                },
                error -> {
                    if (binding != null) {
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.error_no_server));
                        loadWaitlists();
                    }
                });
    }

    // ---- your data ----------------------------------------------------------

    private void bindDataRows() {
        bindRow(binding.rowHealthRecord.rowIcon, binding.rowHealthRecord.rowTitle,
                binding.rowHealthRecord.rowSub, R.drawable.ic_record,
                R.string.profile_row_record, R.string.profile_row_record_sub);
        binding.rowHealthRecord.visitRow.setOnClickListener(
                v -> show(new HealthRecordFragment()));

        bindRow(binding.rowSummary.rowIcon, binding.rowSummary.rowTitle,
                binding.rowSummary.rowSub, R.drawable.ic_download,
                R.string.profile_row_summary, R.string.profile_row_summary_sub);
        binding.rowSummary.visitRow.setOnClickListener(
                v -> show(HealthSummaryFragment.of(HealthSummaryFragment.ACTION_DOWNLOAD)));

        bindRow(binding.rowPrivacy.rowIcon, binding.rowPrivacy.rowTitle,
                binding.rowPrivacy.rowSub, R.drawable.ic_shield,
                R.string.profile_privacy, R.string.profile_privacy_sub);
        binding.rowPrivacy.visitRow.setOnClickListener(v -> showPrivacy());
    }

    private void bindRow(android.widget.ImageView icon, android.widget.TextView title,
                         android.widget.TextView sub, int iconRes, int titleRes, int subRes) {
        icon.setImageResource(iconRes);
        title.setText(titleRes);
        sub.setText(subRes);
    }

    private void showPrivacy() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_privacy)
                .setMessage(R.string.profile_privacy_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // ---- leaving -------------------------------------------------------------

    private void bindActions() {
        binding.btnEditProfile.setOnClickListener(v -> show(new EditProfileFragment()));

        binding.btnSignOut.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_sign_out, (dialog, which) -> signOut())
                .show());
    }

    private void signOut() {
        // Every store that belongs to the patient, not only the session:
        // the profile, the reminder switches and any drafted form answers
        // are theirs too.
        new SessionManager(requireContext()).logoutUser();

        Intent intent = new Intent(requireContext(), WelcomeActivity.class);
        // Nothing from the signed-in session should be reachable with Back.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void show(Fragment fragment) {
        loadFragment(requireActivity().getSupportFragmentManager(), R.id.flFragment, fragment);
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
    }

    private String email() {
        return prefs().getString("email", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
