package com.vegs.mediconnect.backoffice.schedule_time;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/scheduleTimes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ScheduleTimeResource {

    private final ScheduleTimeService scheduleTimeService;

    public ScheduleTimeResource(final ScheduleTimeService scheduleTimeService) {
        this.scheduleTimeService = scheduleTimeService;
    }

    @GetMapping
    public ResponseEntity<List<ScheduleTimeDTO>> getAllScheduleTimes() {
        return ResponseEntity.ok(scheduleTimeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleTimeDTO> getScheduleTime(
            @PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(scheduleTimeService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createScheduleTime(
            @RequestBody @Valid final ScheduleTimeDTO scheduleTimeDTO) {
        final UUID createdId = scheduleTimeService.create(scheduleTimeDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateScheduleTime(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final ScheduleTimeDTO scheduleTimeDTO) {
        scheduleTimeService.update(id, scheduleTimeDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteScheduleTime(@PathVariable(name = "id") final UUID id) {
        scheduleTimeService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
