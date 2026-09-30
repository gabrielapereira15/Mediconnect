package com.vegs.mediconnect.mobile.patient;

import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.mobile.patient.model.PatientDetailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers saving a profile over a patient who already has a health card.
 *
 * Edit profile sent no card fields, and the service used to rebuild the
 * patient from the request: every save stored the card as null, which took
 * the identifier off the chart and out of the PS-CA export. These check
 * the card survives a save that does not mention it, can still be changed
 * or removed on purpose, and that a card the clinic could not use is
 * refused before anything is written.
 */
class PatientApiServiceTest {

    private static final String EMAIL = "jane.doe@example.com";

    private PatientRepository patientRepository;
    private PatientApiService service;
    private Patient stored;

    @BeforeEach
    void setUp() {
        stored = new Patient();
        stored.setId(UUID.randomUUID());
        stored.setEmail(EMAIL);
        stored.setClinicCode("KIT001");
        stored.setFirstName("Jane");
        stored.setLastName("Doe");
        stored.setGender("Female");
        stored.setBirthdate("1990-04-12");
        stored.setPhoneNumber("519-555-0100");
        stored.setAddress("15 Wellington Street, Kitchener, ON");
        stored.setHealthCardNumber("1234567890");
        stored.setHealthCardProvince("ON");

        patientRepository = mock(PatientRepository.class);
        when(patientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(stored));
        // Saving hands back what it was given, so the test can read the row
        // the service wrote.
        when(patientRepository.save(any(Patient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new PatientApiService(patientRepository);
    }

    /** What Edit profile sent before it had a card section. */
    private PatientDetailRequest.PatientDetailRequestBuilder profileSave() {
        return PatientDetailRequest.builder()
                .email(EMAIL)
                .clinicCode("KIT001")
                .firstName("Jane")
                .lastName("Doe-Smith")
                .gender("Female")
                .birthdate("1990-04-12")
                .phoneNumber("519-555-0199")
                .address("20 Queen Street, Kitchener, ON");
    }

    private Patient saved() {
        var captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("saving a profile without card fields keeps the stored card")
    void profileSaveKeepsTheCard() {
        UUID id = service.create(profileSave().build());

        Patient patient = saved();
        assertEquals(stored.getId(), id);
        assertEquals("1234567890", patient.getHealthCardNumber());
        assertEquals("ON", patient.getHealthCardProvince());
        // The rest of the form is still applied.
        assertEquals("Doe-Smith", patient.getLastName());
        assertEquals("519-555-0199", patient.getPhoneNumber());
        assertEquals("20 Queen Street, Kitchener, ON", patient.getAddress());
    }

    @Test
    @DisplayName("a card sent on purpose replaces the stored one, without its spaces and dashes")
    void explicitCardReplacesTheStoredOne() {
        service.create(profileSave()
                .healthCardNumber(" 9876-543 210 ")
                .healthCardProvince("bc")
                .build());

        Patient patient = saved();
        assertEquals("9876543210", patient.getHealthCardNumber());
        assertEquals("BC", patient.getHealthCardProvince());
    }

    @Test
    @DisplayName("letters are kept, in capitals: Quebec numbers start with four")
    void lettersAreKept() {
        service.create(profileSave()
                .healthCardNumber("doej 9004 1234")
                .healthCardProvince("QC")
                .build());

        assertEquals("DOEJ90041234", saved().getHealthCardNumber());
    }

    @Test
    @DisplayName("a new number alone keeps the province on file")
    void numberAloneKeepsTheProvince() {
        service.create(profileSave().healthCardNumber("1111222233").build());

        Patient patient = saved();
        assertEquals("1111222233", patient.getHealthCardNumber());
        assertEquals("ON", patient.getHealthCardProvince());
    }

    @Test
    @DisplayName("a blank number removes the card, province and all")
    void blankNumberRemovesTheCard() {
        service.create(profileSave()
                .healthCardNumber("")
                .healthCardProvince("ON")
                .build());

        Patient patient = saved();
        assertNull(patient.getHealthCardNumber());
        assertNull(patient.getHealthCardProvince());
    }

    @Test
    @DisplayName("a first profile with no card is saved without one")
    void newPatientWithoutCard() {
        String email = "new.patient@example.com";
        when(patientRepository.findByEmail(email)).thenReturn(Optional.empty());

        service.create(profileSave().email(email).build());

        Patient patient = saved();
        assertEquals(email, patient.getEmail());
        assertNull(patient.getHealthCardNumber());
        assertNull(patient.getHealthCardProvince());
    }

    @Test
    @DisplayName("a number with other characters is refused and nothing is saved")
    void refusesOtherCharacters() {
        var request = profileSave()
                .healthCardNumber("1234/567/890")
                .healthCardProvince("ON")
                .build();

        var error = assertThrows(InvalidHealthCardException.class, () -> service.create(request));

        assertTrue(error.getMessage().contains("letters, numbers, spaces and dashes"));
        verify(patientRepository, never()).save(any(Patient.class));
        // Refused before the rest of the form touched the patient either.
        assertEquals("1234567890", stored.getHealthCardNumber());
        assertEquals("Doe", stored.getLastName());
    }

    @Test
    @DisplayName("a number too short or too long to be a card is refused")
    void refusesImplausibleLengths() {
        var tooShort = profileSave().healthCardNumber("12-34").healthCardProvince("ON").build();
        var tooLong = profileSave().healthCardNumber("1234 5678 9012 3").healthCardProvince("ON").build();

        assertThrows(InvalidHealthCardException.class, () -> service.create(tooShort));
        assertThrows(InvalidHealthCardException.class, () -> service.create(tooLong));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("a province that is not Canadian is refused")
    void refusesUnknownProvince() {
        var request = profileSave()
                .healthCardNumber("1234567890")
                .healthCardProvince("NY")
                .build();

        var error = assertThrows(InvalidHealthCardException.class, () -> service.create(request));

        assertTrue(error.getMessage().contains("YT"));
        verify(patientRepository, never()).save(any(Patient.class));
        assertEquals("ON", stored.getHealthCardProvince());
    }

    @Test
    @DisplayName("a number with its province taken away is refused")
    void refusesNumberWithoutProvince() {
        var request = profileSave()
                .healthCardNumber("1234567890")
                .healthCardProvince(" ")
                .build();

        var error = assertThrows(InvalidHealthCardException.class, () -> service.create(request));

        assertTrue(error.getMessage().contains("province or territory"));
        verify(patientRepository, never()).save(any(Patient.class));
    }
}
