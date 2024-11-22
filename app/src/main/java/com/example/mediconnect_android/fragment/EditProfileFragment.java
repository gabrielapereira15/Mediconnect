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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.activity.MainActivity;
import com.example.mediconnect_android.client.PatientClient;
import com.example.mediconnect_android.client.PatientClientImpl;
import com.example.mediconnect_android.databinding.FragmentEditProfileBinding;
import com.example.mediconnect_android.util.DialogUtils;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Objects;

public class EditProfileFragment extends Fragment {

    FragmentEditProfileBinding binding;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    PatientClient patientClient;
    String email;

    public EditProfileFragment() {
        patientClient = new PatientClientImpl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Registrar os Launchers de atividade
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

        listeners();
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
            gender = selectedRadioButton.getText().toString();;
        }

        String jsonString = String.format(
                "{" +
                        "    \"email\": \"%s\",\n" +
                        "    \"clinicCode\": \"%s\",\n" +
                        "    \"firstName\": \"%s\",\n" +
                        "    \"lastName\": \"%s\",\n" +
                        "    \"gender\": \"%s\",\n" +
                        "    \"birthdate\": \"%s\",\n" +
                        "    \"phoneNumber\": \"%s\",\n" +
                        "    \"address\": \"%s\"\n" +
                        "}",
                email,
                clinicCode,
                firstName,
                lastName,
                gender,
                dob,
                phoneNumber,
                address
        );

        if (!isPatientcreated(jsonString)){
            DialogUtils.showMessageDialog(getContext(), "Error! Patient not created. Please, try again later.");
            return;
        }

        saveToSharedPreferences(firstName, lastName, email, phoneNumber, clinicCode, address, dob, gender);

        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.createLoginSession(email);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            String fullName = firstName + " " + lastName;
            mainActivity.updateUserName(fullName);
        }


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
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserProfile", getContext().MODE_PRIVATE);
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