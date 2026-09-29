package com.vegs.mediconnect.mobile.health;

import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.mobile.health.model.HealthEntryRequest;
import com.vegs.mediconnect.mobile.health.model.HealthEntryResponse;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthApiService {

    private final HealthEntryRepository healthEntryRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public List<HealthEntryResponse> getEntries(String email) {
        return healthEntryRepository
                .findAllByPatientOrderByDateCreatedAsc(patient(email))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HealthEntryResponse addEntry(String email, HealthEntryRequest request) {
        var entry = new HealthEntry();
        entry.setPatient(patient(email));
        entry.setType(request.getType());
        entry.setDescription(request.getDescription().trim());
        entry.setNote(trimToNull(request.getNote()));
        entry.setOnsetDate(parseDate(request.getOnsetDate()));
        entry.setActive(true);

        return toResponse(healthEntryRepository.save(entry));
    }

    @Transactional
    public void deactivate(String email, UUID entryId) {
        Patient patient = patient(email);
        HealthEntry entry = healthEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "That entry was not found."));

        // An entry belongs to exactly one patient; a token for someone else
        // must not be able to edit it.
        if (!entry.getPatient().getId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That entry was not found.");
        }

        entry.setActive(false);
        healthEntryRepository.save(entry);
    }

    private Patient patient(String email) {
        return patientRepository.findByEmail(email)
                .orElseThrow(PatientNotFoundException::new);
    }

    private HealthEntryResponse toResponse(HealthEntry entry) {
        return HealthEntryResponse.builder()
                .id(entry.getId())
                .type(entry.getType())
                .description(entry.getDescription())
                .note(entry.getNote())
                .onsetDate(entry.getOnsetDate() == null ? null
                        : entry.getOnsetDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .active(entry.isActive())
                .coded(entry.getCode() != null && !entry.getCode().isBlank())
                .build();
    }

    /**
     * An unreadable date costs the entry its onset, not the entry itself:
     * "penicillin" with no date is still worth recording.
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
