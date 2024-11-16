package com.example.mediconnect_android.fragment;

import static android.app.Activity.RESULT_OK;
import static com.example.mediconnect_android.util.ImageUtils.getImageFromInternalStorage;
import static com.example.mediconnect_android.util.ImageUtils.saveImageToInternalStorage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FragmentEditProfileBinding;
import com.example.mediconnect_android.util.FragmentUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class EditProfileFragment extends Fragment {

    FragmentEditProfileBinding binding;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    public EditProfileFragment() {
        // Required empty public constructor
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
        binding.btnSubmit.setOnClickListener(v -> {
            if (isFormValidated()) {
                submitForm();
            }
        });
    }

    private void submitForm() {
        // Navigate to the next fragment
        HomeFragment homeFragment = new HomeFragment();
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentUtils.loadFragment(fragmentManager, R.id.flFragment, homeFragment);
        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_LONG).show();
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

        // Validate email
        String email = binding.etEmail.getText().toString().trim();
        if (isEmpty(binding.etEmail)) {
            binding.etEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email");
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