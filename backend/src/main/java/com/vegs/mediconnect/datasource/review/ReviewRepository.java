package com.vegs.mediconnect.datasource.review;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findAllByDoctor(Doctor doctor);
    Optional<Review> findByAppointment(Appointment appointment);

}
