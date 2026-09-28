package com.vegs.mediconnect.backoffice.schedule;

import com.vegs.mediconnect.backoffice.util.ReferencedException;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
public class ScheduleResource {

    private final ScheduleService scheduleService;

    public ScheduleResource(final ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<List<ScheduleDTO>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDTO> getSchedule(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(scheduleService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createSchedule(@RequestBody @Valid final ScheduleDTO scheduleDTO) {
        final UUID createdId = scheduleService.create(scheduleDTO).getId();
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateSchedule(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final ScheduleDTO scheduleDTO) {
        scheduleService.update(id, scheduleDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteSchedule(@PathVariable(name = "id") final UUID id) {
        final ReferencedWarning referencedWarning = scheduleService.getReferencedWarning(id);
        if (referencedWarning != null) {
            throw new ReferencedException(referencedWarning);
        }
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
