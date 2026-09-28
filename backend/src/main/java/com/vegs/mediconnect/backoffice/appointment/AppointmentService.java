package com.vegs.mediconnect.backoffice.appointment;

import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleTimeRepository scheduleTimeRepository;


    @Transactional
    public List<AppointmentDTO> findAll() {
        final List<Appointment> appointments = appointmentRepository.findAll(Sort.by("id"));
        return appointments.stream()
                .map(appointment -> mapToDTO(appointment, new AppointmentDTO()))
                .toList();
    }

    @Transactional
    public List<AppointmentDTO> findAllBook() {
        final List<Appointment> appointments = appointmentRepository.findAll(Sort.by(
                "doctor.lastName", "doctor.firstName", "scheduleTime.schedule.date", "scheduleTime.time"));
        return appointments.stream()
                .map(appointment -> mapToBook(appointment, new AppointmentDTO()))
                .toList();
    }

    @Transactional
    public AppointmentDTO get(final UUID id) {
        return appointmentRepository.findById(id)
                .map(appointment -> mapToDTO(appointment, new AppointmentDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final AppointmentDTO appointmentDTO) {
        final Appointment appointment = new Appointment();
        mapToEntity(appointmentDTO, appointment);
        return appointmentRepository.save(appointment).getId();
    }

    public void update(final UUID id, final AppointmentDTO appointmentDTO) {
        final Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(appointmentDTO, appointment);
        appointmentRepository.save(appointment);
    }

    public void delete(final UUID id) {
        appointmentRepository.deleteById(id);
    }

    private AppointmentDTO mapToBook(final Appointment appointment,
                                    final AppointmentDTO appointmentDTO) {
        appointmentDTO.setId(appointment.getId());
        appointmentDTO.setPatientId(appointment.getPatient().getId());
        appointmentDTO.setDoctorId(appointment.getDoctor().getId());
        appointmentDTO.setScheduleTimeId(appointment.getScheduleTime().getId());
        appointmentDTO.setCancelled(appointment.getCanceled());
        appointmentDTO.setRemoved(AppointmentStatus.REMOVED.getStatus().equals(appointment.getStatus()));

        appointmentDTO.setPatientName(appointment.getPatient().getFullName());
        appointmentDTO.setDoctorName(appointment.getDoctor().getFullName());

        var dataFormat = DateTimeFormatter.ofPattern("EEE, d MMM");
        var timeFormat = DateTimeFormatter.ofPattern("h a");
        var localDateTime = appointment.getScheduleTime().getDateTime();

        var bookTime = localDateTime.toLocalDate().format(dataFormat)
                + " at "
                + localDateTime.toLocalTime().format(timeFormat);

        appointmentDTO.setBookTime(bookTime);

        return appointmentDTO;
    }

    private AppointmentDTO mapToDTO(final Appointment appointment,
            final AppointmentDTO appointmentDTO) {
        appointmentDTO.setId(appointment.getId());
        appointmentDTO.setStatus(appointment.getStatus());
        appointmentDTO.setCancelled(appointment.getCanceled());
        appointmentDTO.setPatientId(appointment.getPatient().getId());
        appointmentDTO.setDoctorId(appointment.getDoctor().getId());
        appointmentDTO.setScheduleTimeId(appointment.getScheduleTime().getId());
        return appointmentDTO;
    }

    private Appointment mapToEntity(final AppointmentDTO appointmentDTO,
            final Appointment appointment) {
        appointment.setStatus(appointmentDTO.getStatus());
        appointment.setCanceled(appointmentDTO.getCancelled());
        appointment.setPatient(patientRepository.getReferenceById(appointmentDTO.getPatientId()));
        appointment.setDoctor(doctorRepository.getReferenceById(appointmentDTO.getDoctorId()));
        appointment.setScheduleTime(scheduleTimeRepository.getReferenceById(appointmentDTO.getScheduleTimeId()));
        return appointment;
    }

    public ReferencedWarning getReferencedWarning(final UUID id) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        return null;
    }

    public void book(AppointmentDTO appointmentDTO) {
        var date = appointmentDTO.getScheduleTime().toLocalDate();
        var time = appointmentDTO.getScheduleTime().toLocalTime();
        var doctor = doctorRepository.getReferenceById(appointmentDTO.getDoctorId());
        var scheduleTime = createSchedule(doctor, date, time);
        if (!scheduleTime.getAvailable()) {
            throw new ScheduleTimeUnavailable();
        }

        scheduleTime.setAvailable(false);
        scheduleTimeRepository.saveAndFlush(scheduleTime);
        var patient = patientRepository.getReferenceById(appointmentDTO.getPatientId());
        var appointment = createAppointment(scheduleTime, patient, doctor);
        appointmentRepository.save(appointment);
    }

    private ScheduleTime createSchedule(Doctor doctor, LocalDate date, LocalTime time) {
        var scheduleTime = new ScheduleTime();
        scheduleTime.setTime(time);
        var hasSchedule = scheduleRepository.findByDoctorIdAndDate(doctor.getId(), date);
        if (hasSchedule.isPresent()) {
            var existSchedule = hasSchedule.get();
            var hasTime = scheduleTimeRepository.findByTimeAndSchedule(time, existSchedule);
            if (hasTime.isPresent()) {
                return hasTime.get();
            }
            scheduleTime.setSchedule(existSchedule);
            scheduleTime.setAvailable(true);
            return scheduleTimeRepository.saveAndFlush(scheduleTime);
        } else {
            var schedule = new Schedule();
            schedule.setDoctor(doctor);
            schedule.setDate(date);
            schedule.setAvailable(true);
            scheduleTime.setAvailable(true);
            var createdSchedule = scheduleRepository.saveAndFlush(schedule);
            scheduleTime.setSchedule(createdSchedule);
            return scheduleTimeRepository.saveAndFlush(scheduleTime);
        }
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
