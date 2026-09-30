package com.vegs.mediconnect.mobile.waitlist;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.appointment.AppointmentApiService;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * "Take it": moves a patient into the slot being held for them.
 *
 * Its own service because it needs both sides — the waitlist that holds the
 * slot and the appointments that book it — and the appointment service
 * already calls the waitlist when a slot frees up. Putting this in either
 * would make the two depend on each other.
 */
@Service
@RequiredArgsConstructor
public class WaitlistOfferService {

    private final WaitlistService waitlistService;
    private final WaitlistEntryRepository waitlistRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentApiService appointmentApiService;

    /**
     * Books the held slot and gives up the appointment it replaces.
     *
     * The old appointment is released after the new one exists, so a
     * failure in between leaves the patient with two appointments rather
     * than none; and releasing it offers that slot to the next person on
     * the list, which is how one cancellation can move several people up.
     */
    @Transactional
    public AppointmentResponse accept(String email, UUID entryId) {
        WaitlistEntry entry = waitlistService.owned(email, entryId);
        if (entry.getStatus() == WaitlistStatus.BOOKED) {
            // A second tap, or a retry after a slow response.
            throw new OfferEndedException("You already took that offer.");
        }
        if (entry.getStatus() != WaitlistStatus.OFFERED
                || entry.getOfferedSlot() == null
                || entry.getOfferExpiresAt() == null
                || entry.getOfferExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new OfferEndedException("That offer has ended. The time went to the next person.");
        }

        var slot = entry.getOfferedSlot();
        Appointment current = currentAppointment(entry);

        // Closed before booking, so the booking's own waitlist tidy-up does
        // not see a hold on this slot and pass it on.
        entry.setStatus(WaitlistStatus.BOOKED);
        entry.setOfferedSlot(null);
        entry.setOfferExpiresAt(null);
        waitlistRepository.save(entry);

        AppointmentResponse booked = appointmentApiService.bookHeldSlot(slot, entry.getPatient(), current);
        if (current != null) {
            appointmentApiService.removeAppointment(current.getId());
        }
        return booked;
    }

    /** The appointment they joined the list to improve on, if it is still there. */
    private Appointment currentAppointment(WaitlistEntry entry) {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getPatient().getId().equals(entry.getPatient().getId()))
                .filter(appointment -> appointment.getDoctor().getId().equals(entry.getDoctor().getId()))
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> entry.getCurrentAppointmentDate()
                        .equals(appointment.getScheduleTime().getSchedule().getDate()))
                .findFirst()
                .orElse(null);
    }
}
