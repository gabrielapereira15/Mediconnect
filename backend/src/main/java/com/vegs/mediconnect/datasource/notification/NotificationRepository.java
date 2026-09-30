package com.vegs.mediconnect.datasource.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Scheduled messages whose time has come and that have not gone out yet. */
    List<Notification> findAllBySentAtIsNullAndScheduledForLessThanEqual(OffsetDateTime now);
}
