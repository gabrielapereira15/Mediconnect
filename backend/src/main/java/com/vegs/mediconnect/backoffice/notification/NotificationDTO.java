package com.vegs.mediconnect.backoffice.notification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class NotificationDTO {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String title;
    @NotNull
    private String message;
    @NotNull
    private Boolean sendAllPatients;

    private List<String> patients;
    private String emails;

}
