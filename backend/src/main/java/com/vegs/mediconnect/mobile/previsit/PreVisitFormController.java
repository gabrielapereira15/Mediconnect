package com.vegs.mediconnect.mobile.previsit;

import com.vegs.mediconnect.auth.AuthInterceptor;
import com.vegs.mediconnect.mobile.previsit.model.PreVisitFormRequest;
import com.vegs.mediconnect.mobile.previsit.model.PreVisitFormResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The pre-appointment form for one visit.
 *
 * Sits under the appointment because that is what it belongs to, and the
 * ownership rule is the appointment's: the caller's email comes from their
 * token rather than the path, since the path carries only an id.
 */
@RestController
@RequestMapping(value = "/api/mobile/appointments/{id}/form",
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PreVisitFormController {

    private final PreVisitFormService preVisitFormService;

    @PutMapping
    public ResponseEntity<PreVisitFormResponse> submit(
            @PathVariable(name = "id") final UUID id,
            @RequestBody final PreVisitFormRequest request,
            final HttpServletRequest httpRequest) {
        Object email = httpRequest.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        return ResponseEntity.ok(
                preVisitFormService.submit(id, String.valueOf(email), request));
    }

    /** 204 when the form has not been sent yet, rather than an empty shape. */
    @GetMapping
    public ResponseEntity<PreVisitFormResponse> get(
            @PathVariable(name = "id") final UUID id,
            final HttpServletRequest httpRequest) {
        Object email = httpRequest.getAttribute(AuthInterceptor.AUTHENTICATED_EMAIL);
        return preVisitFormService.get(id, String.valueOf(email))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
