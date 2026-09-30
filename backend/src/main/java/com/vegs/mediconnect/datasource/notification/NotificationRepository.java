package com.vegs.mediconnect.datasource.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Scheduled messages whose time has come and that have not gone out yet. */
    List<Notification> findAllBySentAtIsNullAndScheduledForLessThanEqual(OffsetDateTime now);

    /**
     * Marks a scheduled message as sent, but only if it still is one: not
     * yet sent, not moved back to drafts, not cancelled.
     *
     * One guarded statement rather than load-and-save, so a Cancel or an
     * Edit that lands while the dispatcher is working is never overwritten
     * by the dispatcher's older copy of the row. Returns 1 when this caller
     * won the message and must deliver it, 0 when it must not.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n set n.sentAt = :now
            where n.id = :id
              and n.sentAt is null
              and n.scheduledFor is not null
              and (n.draft is null or n.draft = false)
              and (n.isDeleted is null or n.isDeleted = false)
            """)
    int claimForSending(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    /** Scheduled back to draft, only while it has not gone out. Returns rows changed. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n set n.draft = true, n.scheduledFor = null
            where n.id = :id
              and n.sentAt is null
              and n.scheduledFor is not null
              and (n.draft is null or n.draft = false)
              and (n.isDeleted is null or n.isDeleted = false)
            """)
    int moveBackToDraft(@Param("id") UUID id);

    /**
     * Takes a draft for this caller to send, schedule or save, only if it is
     * still a draft. Two clicks on Send cannot both have it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n set n.draft = false
            where n.id = :id
              and n.draft = true
              and (n.isDeleted is null or n.isDeleted = false)
            """)
    int claimDraft(@Param("id") UUID id);

    /** Withdraws a message, touching nothing else on the row. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.isDeleted = true where n.id = :id")
    int markWithdrawn(@Param("id") UUID id);
}
