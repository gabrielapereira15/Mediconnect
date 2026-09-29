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
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.activity.WelcomeActivity;
import com.example.mediconnect_android.client.ApiConfig;
import com.example.mediconnect_android.client.HealthClient;
import com.example.mediconnect_android.client.HealthClientImpl;
import com.example.mediconnect_android.databinding.FragmentProfileBinding;
import com.example.mediconnect_android.model.HealthEntry;
import com.example.mediconnect_android.util.Background;
import com.example.mediconnect_android.util.ThemePreference;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Everything about the person using the app.
 *
 * Four things live here, in the order someone is likely to want them: who
 * they are, what the clinic knows about their health, how the app should
 * behave, and the way out. It used to be a photo, four lines of text and an
 * edit button — enough to prove the data existed, not enough to open twice.
 */
public class ProfileFragment extends Fragment {

    /** Beyond this many, the line becomes a count instead of a list. */
    private static final int MAX_NAMED_ENTRIES = 3;

    private FragmentProfileBinding binding;
    private final HealthClient healthClient = new HealthClientImpl();

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        bindDetails();
        bindThemeToggle();
        bindActions();
        loadHealthRecord();

        binding.swipeRefresh.setOnRefreshListener(() -> {
            bindDetails();
            loadHealthRecord();
        });
    }

    private void bindDetails() {
        SharedPreferences prefs = prefs();

        String fullName = (prefs.getString("first_name", "") + " "
                + prefs.getString("last_name", "")).trim();
        binding.tvFirstName.setText(fullName);
        binding.tvEmail.setText(prefs.getString("email", ""));
        binding.tvPhoneNumber.setText(prefs.getString("phone_number", ""));
        binding.tvAddress.setText(prefs.getString("address", ""));

        bindHealthCard(prefs.getString("health_card_number", ""),
                prefs.getString("health_card_province", ""));

        Bitmap photo = getImageFromInternalStorage(getContext(), "profile_image.jpg");
        if (photo != null) {
            // The placeholder is an inset, tinted icon; a real photo has to
            // fill the circle instead of inheriting either.
            binding.profileImage.setPadding(0, 0, 0, 0);
            binding.profileImage.setImageTintList(null);
            binding.profileImage.setImageBitmap(photo);
        }
    }

    /**
     * Shows the card with only its last digits.
     *
     * A health card number identifies someone to an entire provincial
     * system, so the whole of it does not belong on a screen being held on a
     * bus. The last three are enough to tell which card it is.
     */
    private void bindHealthCard(String number, String province) {
        if (number == null || number.length() < 3) {
            binding.tvHealthCard.setVisibility(View.GONE);
            return;
        }
        binding.tvHealthCard.setVisibility(View.VISIBLE);
        binding.tvHealthCard.setText(getString(R.string.health_card_masked,
                number.substring(number.length() - 3),
                province == null ? "" : province));
    }

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

    private void bindActions() {
        binding.btnEditProfile.setOnClickListener(v -> loadFragment(
                requireActivity().getSupportFragmentManager(),
                R.id.flFragment, new EditProfileFragment()));

        binding.btnManageHealth.setOnClickListener(v -> loadFragment(
                requireActivity().getSupportFragmentManager(),
                R.id.flFragment, new HealthRecordFragment()));

        binding.btnDownloadSummary.setOnClickListener(v -> loadFragment(
                requireActivity().getSupportFragmentManager(),
                R.id.flFragment, new HealthSummaryFragment()));

        binding.btnSignOut.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_sign_out, (dialog, which) -> signOut())
                .show());
    }

    /**
     * Loads the health record and reduces it to one line per kind.
     *
     * The full list has its own screen; this is the glance that says whether
     * there is anything there at all.
     */
    private void loadHealthRecord() {
        String email = prefs().getString("email", "");

        Background.run(
                () -> healthClient.getEntries(email),
                entries -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    bindHealthLine(binding.tvAllergies, R.string.health_allergies,
                            entries, HealthEntry.TYPE_ALLERGY);
                    bindHealthLine(binding.tvMedications, R.string.health_medications,
                            entries, HealthEntry.TYPE_MEDICATION);
                    bindHealthLine(binding.tvConditions, R.string.health_conditions,
                            entries, HealthEntry.TYPE_CONDITION);
                },
                error -> {
                    if (binding == null) {
                        return;
                    }
                    binding.swipeRefresh.setRefreshing(false);
                    // The rest of this screen is local and still useful, so a
                    // failed fetch says so on the one line it affects rather
                    // than replacing the whole page with an error.
                    binding.tvAllergies.setText(R.string.error_no_server);
                    binding.tvMedications.setText("");
                    binding.tvConditions.setText("");
                });
    }

    private void bindHealthLine(TextView view, @StringRes int labelRes,
                                List<HealthEntry> entries, String type) {
        List<HealthEntry> matching = entries.stream()
                .filter(entry -> type.equals(entry.getType()) && entry.isActive())
                .collect(Collectors.toList());

        String summary;
        if (matching.isEmpty()) {
            summary = getString(R.string.health_none_recorded);
        } else if (matching.size() <= MAX_NAMED_ENTRIES) {
            summary = matching.stream()
                    .map(HealthEntry::getDescription)
                    .collect(Collectors.joining(", "));
        } else {
            // Naming a dozen medications on one line helps nobody.
            summary = getString(R.string.health_count, matching.size());
        }

        view.setText(getString(R.string.health_line, getString(labelRes), summary));
    }

    private void signOut() {
        prefs().edit().clear().apply();
        // The token is what actually grants access, so it goes too —
        // clearing the profile alone would leave a signed-in session behind.
        ApiConfig.clearToken();

        Intent intent = new Intent(requireContext(), WelcomeActivity.class);
        // Nothing from the signed-in session should be reachable with Back.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
