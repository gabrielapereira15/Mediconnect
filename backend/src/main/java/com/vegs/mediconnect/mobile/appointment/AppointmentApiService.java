package com.vegs.mediconnect.mobile.appointment;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentRequest;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import com.vegs.mediconnect.mobile.doctor.DoctorApiService;
import com.vegs.mediconnect.mobile.patient.PatientNotFoundException;
import com.vegs.mediconnect.mobile.schedule.ScheduleTimeNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @Transactional
    public AppointmentResponse create(AppointmentRequest appointmentRequest) {
        var appointment = createAppointment(appointmentRequest.getScheduleTimeId(), appointmentRequest.getPatientEmail());
        var scheduleTime = appointment.getScheduleTime();
        // Make Schedule Time unavailable
        scheduleTime.setAvailable(false);
        scheduleTimeRepository.save(scheduleTime);
        // Create new appointment
        var createdAppointment = appointmentRepository.save(appointment);
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

    private boolean notRemoved(Appointment appointment) {
        return !AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus());
    }

    @Transactional
    public void cancelAppointment(UUID appointmentId) {
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
    }

    private void removeAppointment(Appointment appointment) {
        appointment.setCanceled(Boolean.TRUE);
        appointment.setStatus(AppointmentStatus.REMOVED.getStatus());
        appointmentRepository.save(appointment);
        var schedule = appointment.getScheduleTime();
        schedule.setAvailable(true);
        scheduleTimeRepository.save(schedule);
    }

    // Mapping to AppointmentResponse

    private AppointmentResponse mapToAppointmentResponse(Appointment appointment) {
        // Build AppointmentResponse
        var scheduleTime = appointment.getScheduleTime();
        var doctor = scheduleTime.getSchedule().getDoctor();
        var schedule = scheduleTime.getSchedule();
        var dataFormat = DateTimeFormatter.ofPattern("EEE, d MMM");
        var timeFormat = DateTimeFormatter.ofPattern("h a");

        var status = getStatus(appointment);
        boolean isReviewed = false;
        if (AppointmentStatus.COMPLETED.getStatus().equals(status)) {
            isReviewed = reviewRepository
                    .findByAppointment(appointment)
                    .isPresent();
        }

        return AppointmentResponse
                .builder()
                .id(appointment.getId())
                .date(schedule.getDate().format(dataFormat))
                .time(scheduleTime.getTime().format(timeFormat))
                .isReviewed(isReviewed)
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
