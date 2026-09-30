package com.vegs.mediconnect.mobile.schedule.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ScheduleResponse {

    /**
     * ISO yyyy-MM-dd, not a formatted date.
     *
     * It used to be "Wed, 30 Sep", which meant the phone had to parse the
     * server's wording back into a date to decide whether that was today —
     * and had no way at all to show it in the patient's own locale.
     */
    private String date;

    /** Every slot the clinic still holds open on that day, taken or free. */
    private List<ScheduleTimeResponse> times;

}
