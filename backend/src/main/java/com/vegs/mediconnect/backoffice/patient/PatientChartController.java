package com.vegs.mediconnect.backoffice.patient;

import com.vegs.mediconnect.backoffice.auth.RequiresRole;
import com.vegs.mediconnect.datasource.staff.StaffRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Patients and their charts (board B04).
 *
 * A clinician's, not the front desk's: the whole controller is closed to
 * any other role on the server, not only hidden from the navigation.
 * Nothing here edits what the patient wrote — the record is theirs, kept
 * in the app — and nothing deletes a patient.
 */
@Controller
@RequestMapping("/patients")
@RequiredArgsConstructor
@RequiresRole(StaffRole.CLINICIAN)
public class PatientChartController {

    private static final List<String> TABS = List.of("overview", "appointments", "forms", "record");

    private final PatientChartService charts;

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("active", "patients");
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("patients", charts.search(q));
        return "patients/list";
    }

    @GetMapping("/{id}")
    public String chart(@PathVariable UUID id,
                        @RequestParam(defaultValue = "overview") String tab,
                        Model model) {
        model.addAttribute("active", "patients");
        model.addAttribute("tab", TABS.contains(tab) ? tab : "overview");
        model.addAttribute("chart", charts.chart(id));
        return "patients/chart";
    }

    /** The PS-CA document, downloaded; not kept in the browser's cache. */
    @GetMapping("/{id}/summary.json")
    public ResponseEntity<String> summary(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("patient-summary-" + id + ".json").build().toString())
                .body(charts.summaryDocument(id));
    }
}
