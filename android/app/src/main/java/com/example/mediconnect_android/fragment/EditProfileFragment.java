package com.example.mediconnect_android.fragment;

import static android.app.Activity.RESULT_OK;
import static com.example.mediconnect_android.util.ImageUtils.getImageFromInternalStorage;
import static com.example.mediconnect_android.util.ImageUtils.saveImageToInternalStorage;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.activity.MainActivity;
import com.example.mediconnect_android.client.ApiException;
import com.example.mediconnect_android.client.PatientClient;
import com.example.mediconnect_android.client.PatientClientImpl;
import com.example.mediconnect_android.databinding.FragmentEditProfileBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.HealthCardInput;
import com.example.mediconnect_android.util.SessionManager;
import com.example.mediconnect_android.util.Background;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;

public class EditProfileFragment extends Fragment {

    private static final String ARG_FOCUS_HEALTH_CARD = "focusHealthCard";

    FragmentEditProfileBinding binding;
    PatientClient patientClient;
    String email;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    /**
     * Whether the form opened with a card filled in. Only then does an
     * empty number field mean the patient removed it; otherwise the phone
     * may just not know about a card the clinic holds.
     */
    private boolean hadHealthCard;

    /** Set once the card has been focused, so coming back does not do it again. */
    private boolean healthCardFocused;

    public EditProfileFragment() {
        patientClient = new PatientClientImpl();
    }

    /**
     * The form, opened at the health card with the keyboard up.
     *
     * For Visit day's "Bring your health card" row: the patient tapped it
     * to add the card, and landing at the top of the form with the card
     * below the fold made them hunt for it.
     */
    public static EditProfileFragment forHealthCard() {
        EditProfileFragment fragment = new EditProfileFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FOCUS_HEALTH_CARD, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                            saveImageToInternalStorage(getContext(), selectedImage, "profile_image.jpg");
                            binding.ivProfileImage.setImageBitmap(selectedImage);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "You haven't picked Image", Toast.LENGTH_LONG).show();
                    }
                });

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            String fileName = fileUri.getLastPathSegment();
                            binding.tvFileName.setText(fileName);
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                // Salva a imagem no armazenamento interno
                saveImageToInternalStorage(getContext(), selectedImage, "profile_image.jpg");
                // Exibe a imagem no ImageView
                binding.ivProfileImage.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        boolean askedFor = args != null && args.getBoolean(ARG_FOCUS_HEALTH_CARD, false);
        // Not after a rotation: the patient has moved on from wherever it put them.
        if (askedFor && savedInstanceState == null && !healthCardFocused) {
            // Posted, so the section has been laid out and has a position.
            view.post(this::focusHealthCard);
        }
    }

    private void focusHealthCard() {
        if (binding == null) {
            return;
        }
        healthCardFocused = true;
        binding.etHealthCardNumber.requestFocus();
        // From the section's heading rather than the field, so the patient
        // also sees what the card is for.
        binding.scroll.scrollTo(0, binding.healthCardSection.getTop());
        InputMethodManager keyboard = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.showSoftInput(binding.etHealthCardNumber, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void init() {
        Bitmap profileImage = getImageFromInternalStorage(getContext(), "profile_image.jpg");
        if (profileImage != null) {
            binding.ivProfileImage.setImageBitmap(profileImage);
        }

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        email = sharedPreferences.getString("email", "");
        binding.etEmail.setText(email);

        try {
            String firstName = sharedPreferences.getString("first_name", "");
            String lastName = sharedPreferences.getString("last_name", "");
            String phoneNumber = sharedPreferences.getString("phone_number", "");
            String clinicCode = sharedPreferences.getString("clinic_code", "");
            String address = sharedPreferences.getString("address", "");
            String dob = sharedPreferences.getString("dob", "");
            String gender = sharedPreferences.getString("gender", "");

            binding.etFirstName.setText(firstName);
            binding.etLastName.setText(lastName);
            binding.etPhoneNumber.setText(phoneNumber);
            binding.etClinicCode.setText(clinicCode);
            binding.etAddress.setText(address);
            binding.etDob.setText(dob);
            if (gender.equals("Male")) {
                binding.rbMale.setChecked(true);
            } else if (gender.equals("Female")) {
                binding.rbFemale.setChecked(true);
            } else if (gender.equals("Other")) {
                binding.rbOther.setChecked(true);
            }
        } catch (Exception e) {
            Log.e("EditProfileFragment", "Patient does not exist");
        }

        // Written at sign-in from the clinic's copy, and after every save here.
        String cardNumber = sharedPreferences.getString(HealthCardInput.PREF_NUMBER, "");
        String cardProvince = sharedPreferences.getString(HealthCardInput.PREF_PROVINCE, "");
        hadHealthCard = cardNumber != null && !cardNumber.trim().isEmpty();
        binding.etHealthCardNumber.setText(cardNumber);
        showProvince(cardProvince);

        listeners();
    }

    /** Shows the province by name; anything unrecognised falls back to Ontario. */
    private void showProvince(String code) {
        String[] codes = getResources().getStringArray(R.array.health_card_province_codes);
        String[] names = getResources().getStringArray(R.array.health_card_province_names);
        int index = indexOf(codes, code);
        if (index < 0) {
            index = indexOf(codes, HealthCardInput.DEFAULT_PROVINCE);
        }
        binding.etHealthCardProvince.setText(names[index]);
    }

    /**
     * The code for the province the field shows.
     *
     * Read back from the field rather than kept alongside it, so the two
     * cannot disagree after the field restores its own text on rotation.
     */
    private String provinceCode() {
        String[] codes = getResources().getStringArray(R.array.health_card_province_codes);
        String[] names = getResources().getStringArray(R.array.health_card_province_names);
        int index = indexOf(names, text(binding.etHealthCardProvince));
        return index < 0 ? HealthCardInput.DEFAULT_PROVINCE : codes[index];
    }

    private void pickProvince() {
        String[] names = getResources().getStringArray(R.array.health_card_province_names);
        int current = indexOf(names, text(binding.etHealthCardProvince));
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_profile_card_province_pick)
                .setSingleChoiceItems(names, current, (dialog, which) -> {
                    if (binding != null) {
                        binding.etHealthCardProvince.setText(names[which]);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void listeners() {
        binding.btnUploadId.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });
        binding.ivProfileImage.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            imagePickerLauncher.launch(photoPickerIntent);
        });

        binding.etDob.setOnClickListener(v -> showDatePickerDialog());

        binding.etHealthCardProvince.setOnClickListener(v -> pickProvince());
        binding.tilHealthCardProvince.setEndIconOnClickListener(v -> pickProvince());

        binding.btnSubmit.setOnClickListener(v -> {
            if (isFormValidated()) {
                submitForm();
            }
        });
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    @SuppressLint("DefaultLocale") String formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    binding.etDob.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void submitForm() {
        String email = binding.etEmail.getText().toString().trim();
        String clinicCode = binding.etClinicCode.getText().toString().trim();
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String dob = binding.etDob.getText().toString().trim();
        String phoneNumber = binding.etPhoneNumber.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        int selectedId = binding.rgGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedId != -1) {
            RadioButton selectedRadioButton = binding.getRoot().findViewById(selectedId);
            gender = selectedRadioButton.getText().toString();
        }

        String cardNumber = HealthCardInput.clean(text(binding.etHealthCardNumber));
        String cardProvince = provinceCode();

        // Built with Gson rather than by formatting a string, which broke
        // the body the moment an address held a quotation mark.
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("clinicCode", clinicCode);
        body.addProperty("firstName", firstName);
        body.addProperty("lastName", lastName);
        body.addProperty("gender", gender);
        body.addProperty("birthdate", dob);
        body.addProperty("phoneNumber", phoneNumber);
        body.addProperty("address", address);
        // The card used to be missing from this body, and the server saved
        // its absence: every profile save erased the patient's card.
        HealthCardInput.addTo(body, cardNumber, cardProvince, hadHealthCard);
        String jsonString = body.toString();

        // gender is reassigned above, so take a final copy for the lambda.
        final String selectedGender = gender;
        final boolean cardRemoved = cardNumber.isEmpty() && hadHealthCard;

        Background.run(
                () -> isPatientcreated(jsonString),
                created -> {
                    // Nothing below needs the view, only a context to save into.
                    if (getContext() == null) {
                        return;
                    }
                    if (!created) {
                        DialogUtils.showMessageDialog(getContext(),
                                "Error! Patient not created. Please, try again later.");
                        return;
                    }
                    saveHealthCard(cardNumber, cardProvince, cardRemoved);
                    onProfileSaved(firstName, lastName, email, phoneNumber,
                            clinicCode, address, dob, selectedGender);
                },
                error -> {
                    if (getContext() == null) {
                        return;
                    }
                    // A 400 carries the clinic's reason, such as a card
                    // number no province issues, which says more than "no server".
                    String reason = error instanceof ApiException
                            && ((ApiException) error).getStatus() == 400
                            ? error.getMessage() : null;
                    DialogUtils.showMessageDialog(getContext(),
                            reason == null || reason.trim().isEmpty()
                                    ? getString(R.string.error_no_server)
                                    : reason);
                });
    }

    /**
     * Keeps this phone's copy of the card in step with the clinic's, so
     * Profile and Visit day show what was saved rather than what was there
     * at sign-in. Nothing sent means nothing to change.
     */
    private void saveHealthCard(String number, String province, boolean removed) {
        SharedPreferences.Editor editor = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .edit();
        if (!number.isEmpty()) {
            editor.putString(HealthCardInput.PREF_NUMBER, number);
            editor.putString(HealthCardInput.PREF_PROVINCE, province);
        } else if (removed) {
            editor.remove(HealthCardInput.PREF_NUMBER);
            editor.remove(HealthCardInput.PREF_PROVINCE);
        }
        editor.apply();
    }

    /** Runs once the server has accepted the profile. */
    private void onProfileSaved(String firstName, String lastName, String email,
                                String phoneNumber, String clinicCode, String address,
                                String dob, String gender) {
        saveToSharedPreferences(firstName, lastName, email, phoneNumber, clinicCode, address, dob, gender);

        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.createLoginSession(email);

        // The drawer header this used to refresh is gone; Profile reads the
        // name from preferences every time it binds.

        // Navigate to the next fragment
        HomeFragment homeFragment = new HomeFragment();
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, homeFragment);
        DialogUtils.showMessageDialog(getContext(), "Profile updated successfully");
    }

    private boolean isPatientcreated(String jsonString) {
        return patientClient.createPatient(jsonString);
    }

    private void saveToSharedPreferences(String name, String lastName, String email, String phoneNumber, String clinicCode, String address, String dob, String gender) {
        getContext();
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("first_name", name);
        editor.putString("last_name", lastName);
        editor.putString("email", email);
        editor.putString("phone_number", phoneNumber);
        editor.putString("clinic_code", clinicCode);
        editor.putString("address", address);
        editor.putString("dob", dob);
        editor.putString("gender", gender);

        editor.apply();
    }

    private boolean isFormValidated() {
        boolean isValid = true;

        // Validate first name
        if (isEmpty(binding.etFirstName)) {
            binding.etFirstName.setError("First name is required");
            isValid = false;
        }

        // Validate last name
        if (isEmpty(binding.etLastName)) {
            binding.etLastName.setError("Last name is required");
            isValid = false;
        }

        // Validate phone number
        String phone = binding.etPhoneNumber.getText().toString().trim();
        if (isEmpty(binding.etPhoneNumber)) {
            binding.etPhoneNumber.setError("Phone number is required");
            isValid = false;
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.etPhoneNumber.setError("Enter a valid phone number");
            isValid = false;
        }

        // Validate clinic code
        if (isEmpty(binding.etClinicCode)) {
            binding.etClinicCode.setError("Clinic code is required");
            isValid = false;
        }

        // Validate address
        if (isEmpty(binding.etAddress)) {
            binding.etAddress.setError("Address is required");
            isValid = false;
        }

        // Validate birth date
        if (isEmpty(binding.etDob)) {
            binding.etDob.setError("Birth date is required");
            isValid = false;
        }

        // The card is optional, but one that is typed has to be usable.
        HealthCardInput.Problem cardProblem = HealthCardInput.check(text(binding.etHealthCardNumber));
        if (cardProblem == HealthCardInput.Problem.CHARACTERS) {
            binding.tilHealthCardNumber.setError(getString(R.string.edit_profile_card_characters));
            isValid = false;
        } else if (cardProblem == HealthCardInput.Problem.LENGTH) {
            binding.tilHealthCardNumber.setError(getString(R.string.edit_profile_card_length,
                    HealthCardInput.MIN_LENGTH, HealthCardInput.MAX_LENGTH));
            isValid = false;
        } else {
            binding.tilHealthCardNumber.setError(null);
        }

        return isValid;
    }

    private boolean isEmpty(EditText editText) {
        return editText.getText() == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}