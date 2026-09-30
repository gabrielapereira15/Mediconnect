package com.vegs.mediconnect.mobile.notification.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationRequest {

    @NotNull
    private String patientEmail;

}
