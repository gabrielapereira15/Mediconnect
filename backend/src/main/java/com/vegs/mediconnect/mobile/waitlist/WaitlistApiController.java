package com.vegs.mediconnect.mobile.waitlist;

import com.vegs.mediconnect.mobile.waitlist.model.WaitlistRequest;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistResponse;
import jakarta.validation.Valid;
import com.vegs.mediconnect.mobile.appointment.model.AppointmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/mobile/waitlist", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WaitlistApiController {

    private final WaitlistService waitlistService;
    private final WaitlistOfferService offerService;

    @GetMapping("/{email}")
    public ResponseEntity<List<WaitlistResponse>> list(@PathVariable String email) {
        return ResponseEntity.ok(waitlistService.forPatient(email));
    }

    @PostMapping("/{email}")
    public ResponseEntity<WaitlistResponse> join(@PathVariable String email,
                                                 @RequestBody @Valid WaitlistRequest request) {
        return ResponseEntity.ok(waitlistService.join(email, request));
    }

    /** "Take it": books the held slot in place of the appointment it replaces. */
    @PostMapping("/{email}/{entryId}/accept")
    public ResponseEntity<AppointmentResponse> accept(@PathVariable String email,
                                                      @PathVariable UUID entryId) {
        return ResponseEntity.ok(offerService.accept(email, entryId));
    }

    /** "Keep mine": passes the held slot to the next person. */
    @PostMapping("/{email}/{entryId}/decline")
    public ResponseEntity<Void> decline(@PathVariable String email, @PathVariable UUID entryId) {
        waitlistService.decline(email, entryId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(OfferEndedException.class)
    public ResponseEntity<String> offerEnded(OfferEndedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @PostMapping("/{email}/{entryId}/leave")
    public ResponseEntity<Void> leave(@PathVariable String email, @PathVariable UUID entryId) {
        waitlistService.leave(email, entryId);
        return ResponseEntity.noContent().build();
    }
}
