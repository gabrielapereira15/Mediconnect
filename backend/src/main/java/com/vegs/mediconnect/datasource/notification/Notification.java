package com.vegs.mediconnect.datasource.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Notification {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean sendAllPatients;

    @Column
    private Boolean isDeleted;

    /**
     * What sort of message this is, so the app can give it the right icon
     * and the right thing to do next.
     *
     * Null means an announcement from the clinic, which is what every
     * notification was before the waitlist started sending offers.
     */
    @Column
    private String kind;

    /**
     * The visit a reminder is about, when it is about one.
     *
     * Lets the message carry the action that goes with it — "Fill in
     * form" opens that visit's form — rather than telling the patient to
     * go and find it. Kept as the id rather than a relation: the message
     * outlives the appointment, and a cancelled visit must not take its
     * reminder with it.
     */
    @Column(columnDefinition = "UUID")
    private java.util.UUID appointmentId;

    /**
     * Who it went to, in words (board B07): "All patients", "Patients of
     * Dr. Chase", "Patients booked on Thu 1 Oct". Kept as sent, because the
     * people it reached are fixed at that moment even if the group changes.
     */
    @Column
    private String audience;

    /**
     * Written but not sent. A draft reaches nobody: it has no delivery
     * rows, so no patient's inbox can show it.
     */
    @Column
    private Boolean draft;

    /** When it went out; null while it is a draft. */
    @Column
    private OffsetDateTime sentAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    public boolean notDeleted() {
        return !Boolean.TRUE.equals(isDeleted);
    }

}
