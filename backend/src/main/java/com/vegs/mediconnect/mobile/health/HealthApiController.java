package com.vegs.mediconnect.mobile.health;

import com.vegs.mediconnect.mobile.health.model.HealthEntryRequest;
import com.vegs.mediconnect.mobile.health.model.HealthEntryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The patient's own health record: allergies, medications and conditions.
 *
 * This is the data the clinic needs before an appointment and the data a
 * patient summary is made of, so it is collected once and used for both
 * rather than asked for again on a form that goes nowhere.
 */
@RestController
@RequestMapping(value = "/api/mobile/health", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class HealthApiController {

    private final HealthApiService healthApiService;

    @GetMapping("/{email}")
    public ResponseEntity<List<HealthEntryResponse>> list(@PathVariable String email) {
        return ResponseEntity.ok(healthApiService.getEntries(email));
    }

    @PostMapping("/{email}")
    public ResponseEntity<HealthEntryResponse> add(@PathVariable String email,
                                                   @RequestBody @Valid HealthEntryRequest request) {
        return ResponseEntity.ok(healthApiService.addEntry(email, request));
    }

    /**
     * Marks an entry inactive rather than deleting it. A summary that
     * silently drops a past reaction is worse than one that records it as
     * no longer current.
     */
    @PostMapping("/{email}/{entryId}/stop")
    public ResponseEntity<Void> stop(@PathVariable String email, @PathVariable UUID entryId) {
        healthApiService.deactivate(email, entryId);
        return ResponseEntity.noContent().build();
    }
}
