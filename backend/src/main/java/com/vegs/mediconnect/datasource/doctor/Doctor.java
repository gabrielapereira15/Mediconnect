package com.vegs.mediconnect.datasource.doctor;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Doctor {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String experienceInYears;

    @Column(columnDefinition = "text")
    private String about;

    @Column
    private String specialty;

    @OneToMany(mappedBy = "doctor")
    private Set<Schedule> schedules;

    @OneToMany(mappedBy = "doctor")
    private Set<Appointment> appointments;

    @Column
    private byte[] profilePhoto;

    @Column
    private String profilePhotoExtension;

    /**
     * How long one appointment is, in minutes (board B06). Null on
     * doctors created before availability could be set; read through
     * {@link #slotLength()}.
     */
    @Column
    private Integer slotMinutes;

    /** How many weeks ahead patients may book (board B06). */
    @Column
    private Integer bookingWeeks;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    /** The appointment length, with the clinic's usual half hour as the default. */
    public int slotLength() {
        return slotMinutes == null ? 30 : slotMinutes;
    }

    /** The booking horizon, three weeks unless the doctor's is set. */
    public int horizonWeeks() {
        return bookingWeeks == null ? 3 : bookingWeeks;
    }

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
