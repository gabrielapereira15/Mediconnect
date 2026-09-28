package com.vegs.mediconnect.mobile.notification.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private LocalDateTime creationDate;

}
