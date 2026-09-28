package com.vegs.mediconnect.datasource.schedule;

import com.vegs.mediconnect.datasource.doctor.Doctor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = { @UniqueConstraint(name = "UC_DOCTOR_DATE", columnNames = { "date", "doctor_id" }) })
@Getter
@Setter
public class Schedule {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @OneToMany(mappedBy = "schedule", fetch = FetchType.EAGER)
    private List<ScheduleTime> scheduleTimes;

    @Column
    private Boolean available;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    public String getDoctorName() {
        return doctor.getFullName();
    }

    public String getScheduleDoctor() {
        return getDoctorName().concat(" ").concat(getDate().format(DateTimeFormatter.ofPattern("LLLL dd yyyy")));
    }

}
