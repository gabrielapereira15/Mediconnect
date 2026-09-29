package com.vegs.mediconnect.mobile.previsit;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.previsit.PreVisitForm;
import com.vegs.mediconnect.datasource.previsit.PreVisitFormRepository;
import com.vegs.mediconnect.mobile.appointment.AppointmentNotFoundException;
import com.vegs.mediconnect.mobile.previsit.model.PreVisitFormRequest;
import com.vegs.mediconnect.mobile.previsit.model.PreVisitFormResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The pre-appointment form.
 *
 * The app had asked these questions for as long as it had existed and then
 * dropped the answers on the floor: the screen validated three radio
 * buttons, said "Form Submitted Successfully" and went home. Nothing was
 * sent, so no doctor ever read one. This stores them against the
 * appointment and marks the appointment as having its form in, which is
 * what every screen that says "Form pending" is asking about.
 */
@Service
@RequiredArgsConstructor
public class PreVisitFormService {

    private static final String SYMPTOM_SEPARATOR = ",";

    private final PreVisitFormRepository formRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Saves the answers and records that the form is in.
     *
     * Sending it again replaces what was there: a patient who remembers
     * something on the way to the clinic should be able to add it, and the
     * doctor wants the latest answer rather than two of them.
     */
    @Transactional
    public PreVisitFormResponse submit(UUID appointmentId, String requestingEmail,
                                       PreVisitFormRequest request) {
        var appointment = owned(appointmentId, requestingEmail);

        var form = formRepository.findByAppointment(appointment).orElseGet(() -> {
            var created = new PreVisitForm();
            created.setAppointment(appointment);
            return created;
        });

        form.setReason(trimToNull(request.getReason()));
        form.setSymptoms(joinSymptoms(request.getSymptoms()));
        form.setHadSurgery(trimToNull(request.getHadSurgery()));
        form.setSmokes(trimToNull(request.getSmokes()));
        form.setDrinksAlcohol(trimToNull(request.getDrinksAlcohol()));
        form.setNotes(trimToNull(request.getNotes()));
        formRepository.save(form);

        // The timestamp lives on the appointment because that is what the
        // lists ask about, and asking them to join to a form row to answer
        // "is it in?" is how a list of eight visits becomes nine queries.
        appointment.setFormSubmittedAt(OffsetDateTime.now());
        appointmentRepository.save(appointment);

        return toResponse(form, appointment);
    }

    /** The answers as they stand, or empty if the form has not been sent. */
    @Transactional(readOnly = true)
    public Optional<PreVisitFormResponse> get(UUID appointmentId, String requestingEmail) {
        var appointment = owned(appointmentId, requestingEmail);
        return formRepository.findByAppointment(appointment)
                .map(form -> toResponse(form, appointment));
    }

    /**
     * The appointment, if it belongs to the caller.
     *
     * Somebody else's is reported as missing rather than forbidden, the
     * same way cancelling and checking in do: "forbidden" confirms the id
     * exists, which is the whole of what a guess needs.
     */
    private Appointment owned(UUID appointmentId, String requestingEmail) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (requestingEmail == null
                || !requestingEmail.equalsIgnoreCase(appointment.getPatient().getEmail())) {
            throw new AppointmentNotFoundException();
        }
        return appointment;
    }

    private PreVisitFormResponse toResponse(PreVisitForm form, Appointment appointment) {
        return PreVisitFormResponse.builder()
                .reason(form.getReason())
                .symptoms(splitSymptoms(form.getSymptoms()))
                .hadSurgery(form.getHadSurgery())
                .smokes(form.getSmokes())
                .drinksAlcohol(form.getDrinksAlcohol())
                .notes(form.getNotes())
                .submittedAt(appointment.getFormSubmittedAt() == null
                        ? null
                        : appointment.getFormSubmittedAt().toLocalDateTime()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private String joinSymptoms(List<String> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return null;
        }
        return String.join(SYMPTOM_SEPARATOR, symptoms.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList());
    }

    private List<String> splitSymptoms(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split(SYMPTOM_SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
