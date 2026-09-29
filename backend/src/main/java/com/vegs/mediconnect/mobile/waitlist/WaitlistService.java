package com.vegs.mediconnect.mobile.waitlist;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistRequest;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The waitlist: who wants an earlier appointment, and who to tell when one
 * frees up.
 *
 * A cancelled slot is worth nothing the moment the day passes, so the point
 * of this is speed — the patient at the front of the queue hears about it
 * the instant somebody cancels, rather than when a receptionist gets round
 * to the list.
 *
 * How they hear about it: an in-app notification. There is no push service
 * wired into this project, so the offer is waiting for them the next time
 * they open the app, and the wording says so rather than implying a text
 * message that will never arrive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final WaitlistEntryRepository waitlistRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository notificationPatientRepository;

    @Transactional
    public WaitlistResponse join(String email, WaitlistRequest request) {
        Patient patient = patient(email);
        var doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "That doctor was not found."));

        if (waitlistRepository.existsByPatientAndDoctorAndStatus(
                patient, doctor, WaitlistStatus.WAITING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already on the waitlist for this doctor.");
        }

        var entry = new WaitlistEntry();
        entry.setPatient(patient);
        entry.setDoctor(doctor);
        entry.setCurrentAppointmentDate(request.getCurrentAppointmentDate());
        entry.setAvailableFrom(request.getAvailableFrom());
        entry.setStatus(WaitlistStatus.WAITING);

        return toResponse(waitlistRepository.save(entry));
    }

    @Transactional
    public List<WaitlistResponse> forPatient(String email) {
        return waitlistRepository.findAllByPatientOrderByDateCreatedDesc(patient(email))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void leave(String email, UUID entryId) {
        Patient patient = patient(email);
        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "That waitlist entry was not found."));

        if (!entry.getPatient().getId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "That waitlist entry was not found.");
        }

        entry.setStatus(WaitlistStatus.WITHDRAWN);
        waitlistRepository.save(entry);
    }

    /**
     * Called when an appointment is cancelled and its slot goes back on
     * sale.
     *
     * Offers it to everyone waiting on that doctor who could actually use
     * it, rather than only the first — whoever books it takes it, and the
     * others find it gone. That is blunt, but a clinic that offers a slot to
     * one person at a time and waits for an answer usually loses the slot.
     *
     * Deliberately never throws: this runs as a consequence of a
     * cancellation, and a failure to notify must not roll back the
     * cancellation the patient asked for.
     */
    @Transactional
    public int offerFreedSlot(Appointment cancelled) {
        try {
            var scheduleTime = cancelled.getScheduleTime();
            LocalDate slotDate = scheduleTime.getSchedule().getDate();
            var doctor = scheduleTime.getSchedule().getDoctor();

            List<WaitlistEntry> waiting = waitlistRepository
                    .findAllByDoctorAndStatusOrderByDateCreatedAsc(doctor, WaitlistStatus.WAITING);

            int offered = 0;
            for (WaitlistEntry entry : waiting) {
                // Never offer somebody the slot they just gave up.
                if (entry.getPatient().getId().equals(cancelled.getPatient().getId())) {
                    continue;
                }
                if (!entry.wants(slotDate)) {
                    continue;
                }

                notifyOffer(entry, doctor.getFirstName() + " " + doctor.getLastName(),
                        slotDate, scheduleTime.getTime());
                entry.setStatus(WaitlistStatus.OFFERED);
                entry.setLastOfferedAt(OffsetDateTime.now());
                waitlistRepository.save(entry);
                offered++;
            }

            if (offered > 0) {
                log.info("Offered the freed slot on {} to {} waiting patient(s)", slotDate, offered);
            }
            return offered;
        } catch (RuntimeException e) {
            log.error("Could not offer the freed slot to the waitlist", e);
            return 0;
        }
    }

    private void notifyOffer(WaitlistEntry entry, String doctorName,
                             LocalDate date, java.time.LocalTime time) {
        var notification = new Notification();
        notification.setTitle("An earlier appointment is available");
        notification.setMessage(String.format(
                "%s has a slot on %s at %s. Open Mediconnect to book it before someone else does.",
                doctorName,
                date.format(DATE_FORMAT),
                time.format(TIME_FORMAT)));
        notification.setSendAllPatients(false);
        notification.setIsDeleted(false);
        Notification saved = notificationRepository.save(notification);

        // The notification itself carries no recipient; who sees it is
        // decided by this join row, which is also what tracks whether they
        // have read it.
        var delivery = new NotificationPatient();
        delivery.setNotificationId(saved);
        delivery.setPatientId(entry.getPatient());
        delivery.setAcknowledged(false);
        notificationPatientRepository.save(delivery);
    }

    private Patient patient(String email) {
        return patientRepository.findByEmail(email).orElseThrow(PatientNotFoundException::new);
    }

    private WaitlistResponse toResponse(WaitlistEntry entry) {
        return WaitlistResponse.builder()
                .id(entry.getId())
                .doctorId(entry.getDoctor().getId())
                .doctorName(entry.getDoctor().getFirstName() + " " + entry.getDoctor().getLastName())
                .status(entry.getStatus().name())
                .currentAppointmentDate(entry.getCurrentAppointmentDate() == null ? null
                        : entry.getCurrentAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .availableFrom(entry.getAvailableFrom() == null ? null
                        : entry.getAvailableFrom().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .build();
    }
}
