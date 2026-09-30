package com.vegs.mediconnect.datasource.doctor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DoctorHoursRepository extends JpaRepository<DoctorHours, UUID> {

    List<DoctorHours> findAllByDoctorOrderByDayOfWeekAscStartTimeAsc(Doctor doctor);

    void deleteAllByDoctor(Doctor doctor);
}
