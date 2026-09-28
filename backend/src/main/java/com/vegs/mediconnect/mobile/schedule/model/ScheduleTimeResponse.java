package com.vegs.mediconnect.mobile.schedule.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ScheduleTimeResponse {

    UUID id;
    String time;

}
