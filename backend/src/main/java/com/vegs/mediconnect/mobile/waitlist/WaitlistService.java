package com.vegs.mediconnect.mobile.waitlist;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.notification.Notification;
import com.vegs.mediconnect.datasource.notification.NotificationPatient;
import com.vegs.mediconnect.datasource.notification.NotificationPatientRepository;
import com.vegs.mediconnect.datasource.notification.NotificationKind;
import com.vegs.mediconnect.datasource.notification.NotificationRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import com.vegs.mediconnect.mobile.schedule.BookingRules;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistRequest;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The waitlist: who wants an earlier appointment, and who gets first refusal
 * when one frees up.
 *
 * A freed slot is held for one patient at a time — the one who has waited
 * longest and can use it — for a limited time (boards P08 and P13: "We are
 * holding it for you for 1 h 42 min"). They take it or keep what they have;
 * if they do neither, the hold lapses and the next person gets it. Offering
 * it to everybody at once, as this used to, meant the fastest thumb won and
 * everyone else opened the app to find it gone.
 *
 * How they hear about it: an in-app message. There is no push service wired
 * into this project, so the offer is waiting the next time they open the
 * app, which is one reason the hold is hours rather than minutes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    /** How long a freed slot is held for the patient it is offered to. */
    public static final Duration HOLD = Duration.ofHours(2);

    /** A hold shorter than this is not worth making; the slot stays free. */
    static final Duration SHORTEST_HOLD = Duration.ofMinutes(15);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final WaitlistEntryRepository waitlistRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPatientRepository notificationPatientRepository;

    // ---- the patient's side --------------------------------------------------

    @Transactional
    public WaitlistResponse join(String email, WaitlistRequest request) {
        Patient patient = patient(email);
        var doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "That doctor was not found."));

        if (waitlistRepository.existsByPatientAndDoctorAndStatus(
                patient, doctor, WaitlistStatus.WAITING)
                || waitlistRepository.existsByPatientAndDoctorAndStatus(
                patient, doctor, WaitlistStatus.OFFERED)) {
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
        WaitlistEntry entry = owned(email, entryId);
        if (entry.getStatus() == WaitlistStatus.OFFERED) {
            release(entry, WaitlistStatus.WITHDRAWN);
            return;
        }
        entry.setStatus(WaitlistStatus.WITHDRAWN);
        waitlistRepository.save(entry);
    }

    /**
     * "Keep mine": they would rather keep the appointment they have. They
     * stay on the list for the next slot, and this one moves on now rather
     * than when the hold would have run out.
     */
    @Transactional
    public void decline(String email, UUID entryId) {
        WaitlistEntry entry = owned(email, entryId);
        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new OfferEndedException("There is no offer to turn down.");
        }
        release(entry, WaitlistStatus.WAITING);
    }

    /** The entry, if it belongs to this patient; "not found" otherwise. */
    @Transactional
    public WaitlistEntry owned(String email, UUID entryId) {
        Patient patient = patient(email);
        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "That waitlist entry was not found."));
        if (!entry.getPatient().getId().equals(patient.getId())) {
            // Not "forbidden": that would confirm the id exists.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "That waitlist entry was not found.");
        }
        return entry;
    }

    /**
     * Takes a patient off this doctor's waitlist once they book something
     * earlier than the appointment they were waiting to improve on.
     *
     * Without this they would keep being offered slots they no longer want,
     * which is the fastest way to teach someone to ignore notifications. A
     * slot being held for them goes to the next person.
     */
    @Transactional
    public void onAppointmentBooked(Patient patient, Doctor doctor, LocalDate bookedDate) {
        try {
            var entries = waitlistRepository.findAllByPatientOrderByDateCreatedDesc(patient);
            for (WaitlistEntry entry : entries) {
                boolean sameDoctor = entry.getDoctor().getId().equals(doctor.getId());
                boolean earlier = bookedDate.isBefore(entry.getCurrentAppointmentDate());
                if (!sameDoctor || !earlier) {
                    continue;
                }
                if (entry.getStatus() == WaitlistStatus.OFFERED) {
                    release(entry, WaitlistStatus.BOOKED);
                } else if (entry.getStatus() == WaitlistStatus.WAITING) {
                    entry.setStatus(WaitlistStatus.BOOKED);
                    waitlistRepository.save(entry);
                }
            }
        } catch (RuntimeException e) {
            // Tidying the waitlist must never cost the patient their booking.
            log.error("Could not close the waitlist entry after a booking", e);
        }
    }

    // ---- freed slots -----------------------------------------------------------

    /**
     * Called when an appointment is cancelled or moved and its slot goes
     * back on sale.
     *
     * Catches everything it can, because a failure to notify should not be
     * what stops a patient cancelling. That is not an absolute guarantee:
     * this runs inside the cancellation's transaction, and a persistence
     * error marks that transaction rollback-only whether or not the
     * exception is caught here.
     */
    @Transactional
    public int offerFreedSlot(Appointment cancelled) {
        try {
            return offerSlot(cancelled.getScheduleTime(), cancelled.getPatient()) ? 1 : 0;
        } catch (RuntimeException e) {
            log.error("Could not offer the freed slot to the waitlist", e);
            return 0;
        }
    }

    /**
     * Holds a free slot for the longest-waiting patient who can use it.
     *
     * Never the patient who just gave it up, nor one who already passed on
     * this same slot. Returns whether anyone was offered it; when nobody
     * was, the slot is left as it was.
     */
    @Transactional
    public boolean offerSlot(ScheduleTime slot, Patient except) {
        if (Boolean.TRUE.equals(slot.getBlocked())) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = holdEnds(slot, now);
        if (expires == null) {
            return false;
        }

        var schedule = slot.getSchedule();
        for (WaitlistEntry entry : waitlistRepository.findAllByDoctorAndStatusOrderByDateCreatedAsc(
                schedule.getDoctor(), WaitlistStatus.WAITING)) {
            if (except != null && entry.getPatient().getId().equals(except.getId())) {
                continue;
            }
            if (slot.getId().equals(entry.getPassedSlotId())) {
                continue;
            }
            if (!entry.wants(schedule.getDate())) {
                continue;
            }
            hold(entry, slot, now, expires);
            return true;
        }
        return false;
    }

    /**
     * The desk offers one free slot to one person on the list (board B05's
     * "Offer a slot"), skipping the queue on purpose — they may have rung
     * up, or be the only one who can come at short notice.
     *
     * @return why it cannot be offered, or null once the hold is made
     */
    @Transactional
    public String offerTo(WaitlistEntry entry, ScheduleTime slot) {
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            return "Only someone still waiting can be offered a slot.";
        }
        if (!Boolean.TRUE.equals(slot.getAvailable()) || Boolean.TRUE.equals(slot.getBlocked())) {
            return "That time is no longer free.";
        }
        if (!slot.getSchedule().getDoctor().getId().equals(entry.getDoctor().getId())) {
            return "That time is with a different doctor.";
        }
        if (!entry.wants(slot.getSchedule().getDate())) {
            return "That time is not earlier than their visit, or they cannot come that day.";
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = holdEnds(slot, now);
        if (expires == null) {
            return "That time is too soon to hold for anyone.";
        }
        hold(entry, slot, now, expires);
        return null;
    }

    /**
     * When a hold made now would end, or null when it is not worth making.
     *
     * Capped at the last moment the slot can still be booked, so an offer
     * taken in its final minute is one the booking rules still allow.
     */
    public static OffsetDateTime holdEnds(ScheduleTime slot, OffsetDateTime now) {
        OffsetDateTime lastBookable = slot.getDateTime()
                .minusHours(BookingRules.LEAD_HOURS)
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();
        OffsetDateTime ends = now.plus(HOLD);
        if (ends.isAfter(lastBookable)) {
            ends = lastBookable;
        }
        return Duration.between(now, ends).compareTo(SHORTEST_HOLD) < 0 ? null : ends;
    }

    private void hold(WaitlistEntry entry, ScheduleTime slot, OffsetDateTime now,
                      OffsetDateTime expires) {
        // Off sale for everyone else while it is being held.
        slot.setAvailable(false);
        scheduleTimeRepository.save(slot);

        entry.setStatus(WaitlistStatus.OFFERED);
        entry.setOfferedSlot(slot);
        entry.setOfferExpiresAt(expires);
        entry.setLastOfferedAt(now);
        waitlistRepository.save(entry);

        notifyOffer(entry, slot, expires);
        log.info("Holding the {} slot for a waiting patient until {}", slot.getDateTime(), expires);
    }

    /**
     * Ends a hold without a booking — turned down, lapsed, or they left the
     * list — and passes the slot to the next person, or back on sale.
     */
    @Transactional
    public void release(WaitlistEntry entry, WaitlistStatus next) {
        ScheduleTime slot = entry.getOfferedSlot();
        entry.setStatus(next);
        entry.setOfferedSlot(null);
        entry.setOfferExpiresAt(null);
        if (slot != null) {
            entry.setPassedSlotId(slot.getId());
        }
        waitlistRepository.save(entry);

        if (slot == null) {
            return;
        }
        if (!offerSlot(slot, entry.getPatient())) {
            slot.setAvailable(!Boolean.TRUE.equals(slot.getBlocked()));
            scheduleTimeRepository.save(slot);
        }
    }

    /**
     * Passes on every hold that has run out.
     *
     * Once a minute, which is as precise as "1 h 42 min" on the patient's
     * screen claims to be.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOffers() {
        var lapsed = waitlistRepository.findAllByStatusAndOfferExpiresAtBefore(
                WaitlistStatus.OFFERED, OffsetDateTime.now());
        for (WaitlistEntry entry : lapsed) {
            release(entry, WaitlistStatus.WAITING);
        }
        if (!lapsed.isEmpty()) {
            log.info("Passed on {} lapsed waitlist offer(s)", lapsed.size());
        }
    }

    private void notifyOffer(WaitlistEntry entry, ScheduleTime slot, OffsetDateTime expires) {
        var doctor = slot.getSchedule().getDoctor();
        var notification = new Notification();
        notification.setKind(NotificationKind.WAITLIST_OFFER);
        notification.setTitle("An earlier slot opened up");
        notification.setMessage(String.format(
                "Dr. %s can see you %s at %s. We are holding it for you until %s.",
                doctor.getLastName(),
                slot.getSchedule().getDate().format(DATE_FORMAT),
                slot.getTime().format(TIME_FORMAT),
                expires.atZoneSameInstant(ZoneId.systemDefault()).format(TIME_FORMAT)));
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

    public WaitlistResponse toResponse(WaitlistEntry entry) {
        ScheduleTime slot = entry.getOfferedSlot();
        return WaitlistResponse.builder()
                .id(entry.getId())
                .doctorId(entry.getDoctor().getId())
                .doctorName(entry.getDoctor().getFirstName() + " " + entry.getDoctor().getLastName())
                .status(entry.getStatus().name())
                .currentAppointmentDate(entry.getCurrentAppointmentDate() == null ? null
                        : entry.getCurrentAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .availableFrom(entry.getAvailableFrom() == null ? null
                        : entry.getAvailableFrom().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .offeredSlotId(slot == null ? null : slot.getId())
                .offeredStartsAt(slot == null ? null
                        : slot.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .offerExpiresAt(entry.getOfferExpiresAt() == null ? null
                        : entry.getOfferExpiresAt()
                                .atZoneSameInstant(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
