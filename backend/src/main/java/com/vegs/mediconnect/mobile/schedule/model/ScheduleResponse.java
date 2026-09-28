package com.vegs.mediconnect.mobile.schedule.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ScheduleResponse {

    private String date;
    private List<ScheduleTimeResponse> times;

}
