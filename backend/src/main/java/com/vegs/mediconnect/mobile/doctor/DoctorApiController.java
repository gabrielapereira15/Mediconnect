package com.vegs.mediconnect.mobile.doctor;

import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.mobile.doctor.model.DoctorResponse;
import com.vegs.mediconnect.mobile.doctor.model.DoctorSimpleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/mobile/doctors", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DoctorApiController {

    private final DoctorApiService doctorApiService;

    @GetMapping
    public ResponseEntity<List<DoctorSimpleResponse>> getAllDoctors() {
        return ResponseEntity.ok(doctorApiService.getDoctors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctor(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(doctorApiService.getDoctorDetails(id));
    }

    @GetMapping("/photo/{id}")
    public ResponseEntity<Resource> getProfilePhoto(@PathVariable(name = "id") final UUID id) {
        Doctor doctor = doctorApiService.getDoctor(id);

        ByteArrayResource resource = new ByteArrayResource(doctor.getProfilePhoto());
        return ResponseEntity.ok()
                .contentType(getMediaType(doctor))
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("profile-photo")
                                .build().toString())
                .body(resource);
    }

    private MediaType getMediaType(Doctor doctor) {
        try {
            return MediaType.parseMediaType(doctor.getProfilePhotoExtension());
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

}
