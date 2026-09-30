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
import com.example.mediconnect_android.util.ReminderPreference;
import com.example.mediconnect_android.util.SessionManager;
import com.example.mediconnect_android.util.ThemePreference;
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

    /** The queues this patient is in, which the offers switch describes. */
    private final List<WaitlistEntry> waitlists = new ArrayList<>();

    /** The offers switch as the clinic has it; null until it has answered. */
    private Boolean offersOn;

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
        boolean remindersOn = ReminderPreference.defaultOn(requireContext());
        binding.switchReminders.switchToggle.setChecked(remindersOn);
        describeSwitch(binding.switchReminders, R.string.profile_reminders, remindersOn);

        binding.switchReminders.switchRow.setOnClickListener(v -> {
            boolean on = !binding.switchReminders.switchToggle.isChecked();
            binding.switchReminders.switchToggle.setChecked(on);
            describeSwitch(binding.switchReminders, R.string.profile_reminders, on);
            ReminderPreference.setDefaultOn(requireContext(), on);
        });
    }

    /**
     * Whether the clinic may offer earlier slots (Profile's switch).
     *
     * A real preference, on by default. Off pauses the offers: the patient
     * stays on every waitlist and keeps their place, but the clinic offers
     * them nothing until it is back on. It used to mean "leave every
     * waitlist", so it could only ever be turned off, and only by someone
     * already on one.
     */
    private void loadWaitlists() {
        bindOffersSwitch();

        Background.run(
                () -> {
                    Boolean on = waitlistClient.offersOn(email());
                    List<WaitlistEntry> entries = waitlistClient.list(email());
                    return new Object[]{on, entries};
                },
                result -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    @SuppressWarnings("unchecked")
                    List<WaitlistEntry> entries = (List<WaitlistEntry>) result[1];
                    offersOn = (Boolean) result[0];
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
        boolean known = offersOn != null;
        boolean on = known && offersOn;
        int count = waitlists.size();

        binding.switchOffers.switchIcon.setImageResource(R.drawable.ic_hourglass);
        binding.switchOffers.switchTitle.setText(R.string.profile_offers);
        binding.switchOffers.switchToggle.setChecked(on);
        binding.switchOffers.switchToggle.setEnabled(known);
        describeSwitch(binding.switchOffers, R.string.profile_offers, on);

        if (!known) {
            binding.switchOffers.switchSub.setText(R.string.profile_offers_body_unknown);
        } else if (on && count > 0) {
            binding.switchOffers.switchSub.setText(getResources().getQuantityString(
                    R.plurals.profile_offers_body_on, count, count));
        } else if (on) {
            binding.switchOffers.switchSub.setText(R.string.profile_offers_body_on_none);
        } else if (count > 0) {
            binding.switchOffers.switchSub.setText(getResources().getQuantityString(
                    R.plurals.profile_offers_body_paused, count, count));
        } else {
            binding.switchOffers.switchSub.setText(R.string.profile_offers_body_off);
        }

        // Until the clinic has said where the switch stands, a tap could
        // only guess which way to flip it.
        binding.switchOffers.switchRow.setOnClickListener(known ? v -> setOffers(!offersOn) : null);
        binding.switchOffers.switchRow.setClickable(known);
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

    /**
     * Flips the switch at once and tells the clinic. Nothing is lost
     * either way, so there is no "are you sure"; if the clinic does not
     * take it, the switch goes back and says so.
     */
    private void setOffers(boolean on) {
        Boolean before = offersOn;
        offersOn = on;
        bindOffersSwitch();

        Background.run(
                () -> waitlistClient.setOffersOn(email(), on),
                saved -> {
                    if (binding == null) {
                        return;
                    }
                    if (saved == null) {
                        offersOn = before;
                        bindOffersSwitch();
                        DialogUtils.showMessageDialog(getContext(),
                                getString(R.string.profile_offers_failed));
                        return;
                    }
                    offersOn = saved;
                    bindOffersSwitch();
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    offersOn = before;
                    bindOffersSwitch();
                    DialogUtils.showMessageDialog(getContext(),
                            getString(R.string.error_no_server));
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
