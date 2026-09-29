package com.vegs.mediconnect.mobile.waitlist;

import com.vegs.mediconnect.mobile.waitlist.model.WaitlistRequest;
import com.vegs.mediconnect.mobile.waitlist.model.WaitlistResponse;
import jakarta.validation.Valid;
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

    @GetMapping("/{email}")
    public ResponseEntity<List<WaitlistResponse>> list(@PathVariable String email) {
        return ResponseEntity.ok(waitlistService.forPatient(email));
    }

    @PostMapping("/{email}")
    public ResponseEntity<WaitlistResponse> join(@PathVariable String email,
                                                 @RequestBody @Valid WaitlistRequest request) {
        return ResponseEntity.ok(waitlistService.join(email, request));
    }

    @PostMapping("/{email}/{entryId}/leave")
    public ResponseEntity<Void> leave(@PathVariable String email, @PathVariable UUID entryId) {
        waitlistService.leave(email, entryId);
        return ResponseEntity.noContent().build();
    }
}
