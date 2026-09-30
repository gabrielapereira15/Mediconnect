package com.vegs.mediconnect.datasource.schedule;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = { @UniqueConstraint(name = "UC_SCHEDULE_DATE", columnNames = { "time", "schedule_id" }) })
@Getter
@Setter
public class ScheduleTime {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "UUID")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private LocalTime time;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column
    private Boolean available = true;

    /**
     * Taken out of the diary by the clinic (board B02's "Block") rather
     * than booked. Unavailable either way, but the schedule has to say
     * which: a blocked slot is not somebody's appointment, and unblocking
     * it must not free a slot a patient holds.
     */
    @Column
    private Boolean blocked = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    public LocalDateTime getDateTime() {
        return getSchedule().getDate().atTime(getTime());
    }
}
