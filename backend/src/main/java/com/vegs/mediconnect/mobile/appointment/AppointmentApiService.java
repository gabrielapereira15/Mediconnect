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
import com.vegs.mediconnect.mobile.waitlist.WaitlistService;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import com.vegs.mediconnect.mobile.schedule.ScheduleTimeNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * A blank name means the patient booked it for themselves, so nothing is
     * stored and the row stays as it was.
     */
    private void applyBookedFor(Appointment appointment, AppointmentRequest request) {
        var name = request.getBookedForName();
        if (name == null || name.isBlank()) {
            return;
        }
        appointment.setBookedForName(name.trim());
        appointment.setBookedForPhone(trimToNull(request.getBookedForPhone()));
        appointment.setBookedForNotes(trimToNull(request.getBookedForNotes()));
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
        appointmentRepository.findById(appointmentId)
                .ifPresentOrElse(this::cancelAppointment, AppointmentNotFoundException::new);
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
                .reviewScore(reviewScore)
                .status(status)
                .doctor(doctorApiService.mapToDoctorSimpleResponse(doctor))
                .build();
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
