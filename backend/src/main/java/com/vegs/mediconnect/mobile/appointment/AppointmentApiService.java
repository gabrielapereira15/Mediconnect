package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.review.Review;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import com.vegs.mediconnect.mobile.doctor.DoctorApiService;
import com.vegs.mediconnect.mobile.notification.PatientMessages;
import com.vegs.mediconnect.mobile.waitlist.WaitlistService;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import com.vegs.mediconnect.mobile.schedule.BookingRules;
import com.vegs.mediconnect.mobile.schedule.ScheduleTimeNotFoundException;
import com.vegs.mediconnect.mobile.schedule.SlotNoLongerAvailableException;
import jakarta.transaction.Transactional;
import com.vegs.mediconnect.datasource.previsit.PreVisitFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentApiService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final DoctorApiService doctorApiService;
    private final ReviewRepository reviewRepository;
    private final WaitlistService waitlistService;
    private final PreVisitFormRepository preVisitFormRepository;
    private final PatientMessages patientMessages;

    @Transactional
    public AppointmentResponse create(AppointmentRequest appointmentRequest) {
        var appointment = createAppointment(appointmentRequest.getScheduleTimeId(), appointmentRequest.getPatientEmail());
        applyBookedFor(appointment, appointmentRequest);
        var scheduleTime = appointment.getScheduleTime();
        // Make Schedule Time unavailable
        scheduleTime.setAvailable(false);
        scheduleTimeRepository.save(scheduleTime);
        // Create new appointment
        var createdAppointment = appointmentRepository.save(appointment);

        // If they were waiting for something earlier with this doctor and
        // this is it, they come off the list.
        waitlistService.onAppointmentBooked(
                createdAppointment.getPatient(),
                createdAppointment.getDoctor(),
                scheduleTime.getSchedule().getDate());

        return mapToAppointmentResponse(createdAppointment);
    }

    @Transactional
    public List<AppointmentResponse> getAppointments(String email) {
        var optPatient = patientRepository.findByEmail(email);
        if (optPatient.isEmpty()) {
            return List.of();
        }
        var patient = optPatient.get();

        return appointmentRepository.findAllByPatient(patient)
                .stream()
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .filter(this::notRemoved)
                .map(this::mapToAppointmentResponse)
                .toList();
    }

    /**
     * Records who the visit is for when it is not the account holder.
     *
     * A blank name means the patient booked it for themselves, in which case
     * only the note is theirs to keep. The note used to be dropped with the
     * rest, so a patient who wrote what they wanted to talk about and was
     * booking for themselves — which is nearly everyone — sent it nowhere.
     */
    private void applyBookedFor(Appointment appointment, AppointmentRequest request) {
        appointment.setBookedForNotes(trimToNull(request.getBookedForNotes()));

        var name = request.getBookedForName();
        if (name == null || name.isBlank()) {
            return;
        }
        appointment.setBookedForName(name.trim());
        appointment.setBookedForPhone(trimToNull(request.getBookedForPhone()));
        appointment.setBookedForDateOfBirth(parseDateOfBirth(request.getBookedForDateOfBirth()));
    }

    /**
     * A date of birth the app could not format is not worth rejecting the
     * booking over — the appointment still has a name and a phone number.
     */
    private LocalDate parseDateOfBirth(String value) {
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
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean notRemoved(Appointment appointment) {
        return !AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus());
    }

    /**
     * Records that the patient has arrived for today's visit.
     *
     * Ownership is checked the same way cancelling is, and for the same
     * reason: an appointment belonging to somebody else is reported as
     * missing rather than forbidden, because "forbidden" confirms the id
     * exists.
     *
     * Checking in twice is not an error. A patient who taps it again after
     * the screen has reloaded should see the same answer, not a failure,
     * and the desk should keep the time they actually arrived.
     */
    @Transactional
    public AppointmentResponse checkIn(UUID appointmentId, String requestingEmail) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        if (requestingEmail == null
                || !requestingEmail.equalsIgnoreCase(appointment.getPatient().getEmail())) {
            throw new AppointmentNotFoundException();
        }
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            throw new CheckInNotOpenException("That visit was cancelled.");
        }
        if (!appointment.getScheduleTime().getSchedule().getDate().isEqual(LocalDate.now())) {
            // Checking in the day before tells the front desk nothing and
            // puts the patient in a queue they are not standing in.
            throw new CheckInNotOpenException("You can check in on the day of your visit.");
        }

        if (appointment.getCheckedInAt() == null) {
            appointment.setCheckedInAt(OffsetDateTime.now());
            appointmentRepository.save(appointment);
        }
        return mapToAppointmentResponse(appointment);
    }

    /**
     * The patient says they will be there (board P09's checklist).
     *
     * Their own appointment only, and only while it is still ahead of
     * them; saying it twice is harmless.
     */
    @Transactional
    public AppointmentResponse confirmAttendance(UUID appointmentId, String requestingEmail) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        if (requestingEmail == null
                || !requestingEmail.equalsIgnoreCase(appointment.getPatient().getEmail())) {
            throw new AppointmentNotFoundException();
        }
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            throw new CheckInNotOpenException("That visit was cancelled.");
        }
        if (appointment.getDateTime().isBefore(LocalDateTime.now())) {
            throw new CheckInNotOpenException("That visit has already happened.");
        }

        if (appointment.getAttendanceConfirmedAt() == null) {
            appointment.setAttendanceConfirmedAt(OffsetDateTime.now());
            appointmentRepository.save(appointment);
        }
        return mapToAppointmentResponse(appointment);
    }

    /**
     * Books a slot the waitlist was holding for this patient.
     *
     * Skips the "is it free" check on purpose: it is off sale precisely
     * because it was being kept for them. What they told the clinic about
     * the visit it replaces — who it is for, their note, the form — moves
     * with them rather than having to be given again.
     */
    @Transactional
    public AppointmentResponse bookHeldSlot(ScheduleTime slot, Patient patient, Appointment replacing) {
        var appointment = createAppointment(slot, patient, slot.getSchedule().getDoctor());
        if (replacing != null) {
            appointment.setBookedForName(replacing.getBookedForName());
            appointment.setBookedForDateOfBirth(replacing.getBookedForDateOfBirth());
            appointment.setBookedForPhone(replacing.getBookedForPhone());
            appointment.setBookedForNotes(replacing.getBookedForNotes());
            appointment.setFormSubmittedAt(replacing.getFormSubmittedAt());
        }
        slot.setAvailable(false);
        scheduleTimeRepository.save(slot);
        var saved = appointmentRepository.save(appointment);

        if (replacing != null) {
            preVisitFormRepository.findByAppointment(replacing).ifPresent(form -> {
                form.setAppointment(saved);
                preVisitFormRepository.save(form);
            });
        }
        return mapToAppointmentResponse(saved);
    }

    /**
     * Cancels an appointment on behalf of the patient who booked it.
     *
     * The email comes from the caller's token. An appointment belonging to
     * somebody else is reported as missing rather than forbidden, because
     * saying "forbidden" would confirm that the id exists.
     */
    @Transactional
    public void cancelAppointment(UUID appointmentId, String requestingEmail) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        if (requestingEmail == null
                || !requestingEmail.equalsIgnoreCase(appointment.getPatient().getEmail())) {
            throw new AppointmentNotFoundException();
        }

        cancelAppointment(appointment);
    }

    /**
     * Cancels on behalf of the clinic rather than the patient.
     *
     * Staff in the back office can cancel anyone's appointment — that is
     * their job — so there is no email to check. Kept as a separate method
     * so that exemption is explicit rather than an overload someone calls
     * by accident from the patient-facing API.
     */
    @Transactional
    public void cancelAppointmentAsClinic(UUID appointmentId) {
        cancelAppointmentAsClinic(appointmentId, null);
    }

    /**
     * Cancels for the clinic and says why (board B03's "Cancel…").
     *
     * The reason is kept on the appointment, not in anybody's head: the
     * next person to open it can see what happened. Nothing is deleted —
     * a cancelled visit is still part of the patient's history.
     *
     * The patient is sent a message: a visit that silently disappears
     * from the app is one they turn up for anyway.
     */
    @Transactional
    public void cancelAppointmentAsClinic(UUID appointmentId, String reason) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            return;
        }
        appointment.setCancelReason(trimToNull(reason));
        cancelAppointment(appointment);
        patientMessages.visitCancelled(appointment);
    }

    /**
     * The front desk marks the patient as arrived. Once: checking in twice
     * keeps the first time, so nobody is sent to the back of a queue they
     * have been standing in.
     */
    @Transactional
    public void checkInAsClinic(UUID appointmentId) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            throw new CheckInNotOpenException("That visit was cancelled.");
        }
        if (!appointment.getScheduleTime().getSchedule().getDate().isEqual(LocalDate.now())) {
            throw new CheckInNotOpenException("Check-in is only open on the day of the visit.");
        }
        if (appointment.getCheckedInAt() == null) {
            appointment.setCheckedInAt(OffsetDateTime.now());
            appointmentRepository.save(appointment);
        }
    }

    /**
     * Books a patient in from the desk.
     *
     * The slot must be free, but not six hours away: the lead time is there
     * so the app does not sell a slot nobody can prepare for, and a patient
     * standing at the desk is already prepared.
     */
    @Transactional
    public Appointment bookAsClinic(UUID patientId, UUID slotId, String note) {
        var slot = scheduleTimeRepository.findById(slotId)
                .orElseThrow(ScheduleTimeNotFoundException::new);
        requireFreeAndAhead(slot);
        var patient = patientRepository.findById(patientId)
                .orElseThrow(PatientNotFoundException::new);

        var appointment = createAppointment(slot, patient, slot.getSchedule().getDoctor());
        appointment.setBookedForNotes(trimToNull(note));
        slot.setAvailable(false);
        scheduleTimeRepository.save(slot);
        var saved = appointmentRepository.save(appointment);

        waitlistService.onAppointmentBooked(patient, saved.getDoctor(),
                slot.getSchedule().getDate());
        patientMessages.visitBooked(saved);
        return saved;
    }

    /**
     * Moves an appointment to another free slot with the same doctor.
     *
     * The patient is sent a message with the old time and the new, since
     * the time they have in mind is the old one.
     *
     * What the patient told the clinic moves with it — who it is for, the
     * note, the form — and the slot it leaves goes to the waitlist, as any
     * freed slot does.
     */
    @Transactional
    public Appointment rescheduleAsClinic(UUID appointmentId, UUID slotId) {
        var current = appointmentRepository.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (Boolean.TRUE.equals(current.getCanceled())) {
            throw new SlotNoLongerAvailableException("A cancelled visit cannot be moved.");
        }
        var slot = scheduleTimeRepository.findById(slotId)
                .orElseThrow(ScheduleTimeNotFoundException::new);
        if (!slot.getSchedule().getDoctor().getId().equals(current.getDoctor().getId())) {
            throw new SlotNoLongerAvailableException("That time is with a different doctor.");
        }
        requireFreeAndAhead(slot);

        var moved = createAppointment(slot, current.getPatient(), current.getDoctor());
        moved.setBookedForName(current.getBookedForName());
        moved.setBookedForDateOfBirth(current.getBookedForDateOfBirth());
        moved.setBookedForPhone(current.getBookedForPhone());
        moved.setBookedForNotes(current.getBookedForNotes());
        moved.setFormSubmittedAt(current.getFormSubmittedAt());
        slot.setAvailable(false);
        scheduleTimeRepository.save(slot);
        var saved = appointmentRepository.save(moved);

        preVisitFormRepository.findByAppointment(current).ifPresent(form -> {
            form.setAppointment(saved);
            preVisitFormRepository.save(form);
        });
        removeAppointment(current);
        patientMessages.visitMoved(current.getDateTime(), saved);
        return saved;
    }

    private void requireFreeAndAhead(ScheduleTime slot) {
        if (!Boolean.TRUE.equals(slot.getAvailable()) || Boolean.TRUE.equals(slot.getBlocked())) {
            throw new SlotNoLongerAvailableException("That time has just been taken.");
        }
        if (!slot.getDateTime().isAfter(LocalDateTime.now())) {
            throw new SlotNoLongerAvailableException("That time has already passed.");
        }
    }

    @Transactional
    public void removeAppointment(UUID appointmentId) {
        appointmentRepository.findById(appointmentId)
                .ifPresentOrElse(this::removeAppointment, AppointmentNotFoundException::new);
    }

    private void cancelAppointment(Appointment appointment) {
        appointment.setCanceled(Boolean.TRUE);
        appointment.setStatus(AppointmentStatus.CANCELED.getStatus());
        appointmentRepository.save(appointment);
        var schedule = appointment.getScheduleTime();
        schedule.setAvailable(true);
        scheduleTimeRepository.save(schedule);

        // The slot is back on sale, and it is worth nothing once the day
        // passes. Anyone waiting for something earlier hears about it now.
        waitlistService.offerFreedSlot(appointment);
    }

    private void removeAppointment(Appointment appointment) {
        appointment.setCanceled(Boolean.TRUE);
        appointment.setStatus(AppointmentStatus.REMOVED.getStatus());
        appointmentRepository.save(appointment);
        var schedule = appointment.getScheduleTime();
        schedule.setAvailable(true);
        scheduleTimeRepository.save(schedule);

        // Rescheduling frees the old slot just as cancelling does.
        waitlistService.offerFreedSlot(appointment);
    }

    // Mapping to AppointmentResponse

    private AppointmentResponse mapToAppointmentResponse(Appointment appointment) {
        // Build AppointmentResponse
        var scheduleTime = appointment.getScheduleTime();
        var doctor = scheduleTime.getSchedule().getDoctor();
        var schedule = scheduleTime.getSchedule();
        var dataFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
        var timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

        var status = getStatus(appointment);
        boolean isReviewed = false;
        Float reviewScore = null;
        if (AppointmentStatus.COMPLETED.getStatus().equals(status)) {
            var review = reviewRepository.findByAppointment(appointment);
            isReviewed = review.isPresent();
            reviewScore = review.map(Review::getScore).orElse(null);
        }

        return AppointmentResponse
                .builder()
                .id(appointment.getId())
                .date(schedule.getDate().format(dataFormat))
                .time(scheduleTime.getTime().format(timeFormat))
                .startsAt(LocalDateTime.of(schedule.getDate(), scheduleTime.getTime())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isReviewed(isReviewed)
                .bookedForName(appointment.getBookedForName())
                .checkedInAt(isoOrNull(appointment.getCheckedInAt()))
                .formSubmittedAt(isoOrNull(appointment.getFormSubmittedAt()))
                .attendanceConfirmedAt(isoOrNull(appointment.getAttendanceConfirmedAt()))
                .reviewScore(reviewScore)
                .status(status)
                .doctor(doctorApiService.mapToDoctorSimpleResponse(doctor))
                .build();
    }

    private String isoOrNull(OffsetDateTime at) {
        return at == null
                ? null
                : at.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String getStatus(Appointment appointment) {
        if (appointment.getCanceled()) {
            return AppointmentStatus.CANCELED.getStatus();
        }
        if (appointment.getScheduleTime().getSchedule().getDate().isBefore(LocalDate.now())) {
            return AppointmentStatus.COMPLETED.getStatus();
        }
        return AppointmentStatus.UPCOMING.getStatus();
    }

    // Create new Appointment

    private Appointment createAppointment(UUID scheduleTimeId, String patientEmail) {
        // Get ScheduleTime Entity
        var scheduleTime = scheduleTimeRepository.findById(scheduleTimeId)
                .orElseThrow(ScheduleTimeNotFoundException::new);

        // The booking screen shows taken slots, and two patients can reach
        // the same free one seconds apart, so the slot is checked here as
        // well as there. Without this the second booking quietly won and
        // both patients were told they had the appointment.
        if (!Boolean.TRUE.equals(scheduleTime.getAvailable())) {
            throw new SlotNoLongerAvailableException("That time has just been taken.");
        }
        if (!BookingRules.hasEnoughNotice(scheduleTime)) {
            throw new SlotNoLongerAvailableException("That time is too close to now to book.");
        }

        // Get Patient Entity
        var patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(PatientNotFoundException::new);

        // Get Doctor Entity
        var doctor = scheduleTime.getSchedule().getDoctor();

        // Map to Appointment Entity
        return createAppointment(scheduleTime, patient, doctor);
    }

    private Appointment createAppointment(ScheduleTime scheduleTime, Patient patient, Doctor doctor) {
        var appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.UPCOMING.getStatus());
        appointment.setCanceled(false);
        appointment.setScheduleTime(scheduleTime);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        return appointment;
    }

}
