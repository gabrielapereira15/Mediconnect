package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.mobile.patient.model.PatientDetailRequest;
import com.vegs.mediconnect.mobile.patient.model.PatientDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PatientApiService {

    private final PatientRepository patientRepository;

    public PatientDetailResponse getPatientByEmail(String email) {
        var patient = patientRepository.findByEmail(email)
                .orElseThrow(PatientNotFoundException::new);
        return mapToPatientDetailResponse(patient);
    }

    /**
     * Creates the patient, or updates the one already holding this email.
     *
     * The app saves Edit profile through here too, so an existing row is
     * updated in place rather than rebuilt from the request. Rebuilding it
     * meant anything the form did not send was saved as null, and the form
     * did not send the health card: every profile save erased it, and with
     * it the patient's identifier on the chart and in the PS-CA export.
     */
    @Transactional
    public UUID create(@Valid PatientDetailRequest patientDTO) {
        var patient = patientRepository.findByEmail(patientDTO.getEmail())
                .orElseGet(Patient::new);
        // First, so a card the clinic cannot use stops the save before any
        // other field has been touched.
        applyHealthCard(patientDTO.getHealthCardNumber(), patientDTO.getHealthCardProvince(), patient);
        setIfPresent(patientDTO.getEmail(), patient::setEmail);
        setIfPresent(patientDTO.getClinicCode(), patient::setClinicCode);
        setIfPresent(patientDTO.getFirstName(), patient::setFirstName);
        setIfPresent(patientDTO.getLastName(), patient::setLastName);
        setIfPresent(patientDTO.getGender(), patient::setGender);
        setIfPresent(patientDTO.getBirthdate(), patient::setBirthdate);
        setIfPresent(patientDTO.getPhoneNumber(), patient::setPhoneNumber);
        setIfPresent(patientDTO.getAddress(), patient::setAddress);
        return patientRepository.save(patient).getId();
    }

    /** The same rule as {@link #create}: what the request leaves out is kept. */
    @Transactional
    public void update(UUID id, PatientDetailResponse patientDTO) {
        var patient = patientRepository.findById(id)
                .orElseThrow(PatientNotFoundException::new);
        applyHealthCard(patientDTO.getHealthCardNumber(), patientDTO.getHealthCardProvince(), patient);
        setIfPresent(patientDTO.getEmail(), patient::setEmail);
        setIfPresent(patientDTO.getClinicCode(), patient::setClinicCode);
        setIfPresent(patientDTO.getFirstName(), patient::setFirstName);
        setIfPresent(patientDTO.getLastName(), patient::setLastName);
        setIfPresent(patientDTO.getGender(), patient::setGender);
        setIfPresent(patientDTO.getBirthdate(), patient::setBirthdate);
        setIfPresent(patientDTO.getPhoneNumber(), patient::setPhoneNumber);
        setIfPresent(patientDTO.getAddress(), patient::setAddress);
        patientRepository.save(patient);
    }

    /**
     * Applies the card the request carries, if it carries one.
     *
     * A field left out keeps what is stored, since an older app or another
     * form has no card to send and is not asking for it to be removed. A
     * blank number is the explicit way to remove it. The province goes with
     * the number: a number alone does not say which system to look it up in,
     * and a province alone identifies nobody.
     */
    private void applyHealthCard(String enteredNumber, String enteredProvince, Patient patient) {
        if (enteredNumber == null && enteredProvince == null) {
            return;
        }
        String number = enteredNumber == null
                ? patient.getHealthCardNumber()
                : HealthCard.number(enteredNumber);
        String province = enteredProvince == null
                ? patient.getHealthCardProvince()
                : HealthCard.province(enteredProvince);

        if (number == null) {
            province = null;
        } else if (province == null) {
            throw new InvalidHealthCardException(
                    "Choose the province or territory that issued the health card.");
        }
        patient.setHealthCardNumber(number);
        patient.setHealthCardProvince(province);
    }

    private static void setIfPresent(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private PatientDetailResponse mapToPatientDetailResponse(Patient patient) {
        return PatientDetailResponse
                .builder()
                .id(patient.getId())
                .email(patient.getEmail())
                .clinicCode(patient.getClinicCode())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(patient.getFullName())
                .gender(patient.getGender())
                .birthdate(patient.getBirthdate())
                .phoneNumber(patient.getPhoneNumber())
                .address(patient.getAddress())
                .healthCardNumber(patient.getHealthCardNumber())
                .healthCardProvince(patient.getHealthCardProvince())
                .build();
    }
}
