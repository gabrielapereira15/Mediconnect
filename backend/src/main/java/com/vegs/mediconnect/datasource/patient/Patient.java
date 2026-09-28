package com.vegs.mediconnect.datasource.patient;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String clinicCode;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String gender;

    @Column
    private String birthdate;

    @Column
    private String phoneNumber;

    @Column
    private String address;

    @OneToMany(mappedBy = "patient")
    private List<Appointment> appointments;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    public String getFullName() {
        if (isNull(lastName)) {
            return firstName;
        }
        if (isNull(firstName)) {
            return lastName;
        }
        return lastName
                .concat(", ")
                .concat(firstName);
    }

}
