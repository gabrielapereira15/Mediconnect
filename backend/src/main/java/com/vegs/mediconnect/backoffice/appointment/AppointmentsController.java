package com.vegs.mediconnect.backoffice.appointment;

import com.vegs.mediconnect.backoffice.auth.RequiresRole;
import com.vegs.mediconnect.backoffice.auth.StaffSession;
import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.shared.Redirects;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.staff.StaffRole;
import com.vegs.mediconnect.mobile.appointment.AppointmentApiService;
import com.vegs.mediconnect.mobile.appointment.CheckInNotOpenException;
import com.vegs.mediconnect.mobile.schedule.SlotNoLongerAvailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Appointments (board B03): every booking across the clinic, by tab, with
 * the one that is selected open in a side panel.
 *
 * Nothing here deletes anything. A booking is cancelled with a reason, or
 * moved; both leave a record behind, because a visit that was booked and
 * did not happen is part of the patient's history too.
 */
@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentsController {

    private final ClinicAppointmentService clinicAppointments;
    private final AppointmentApiService appointments;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // ---- the list -------------------------------------------------------------

    @GetMapping
    public String list(@RequestParam(defaultValue = "upcoming") String tab,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) UUID doctor,
                       @RequestParam(defaultValue = "any") String range,
                       @RequestParam(defaultValue = "false") boolean formPending,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) UUID selected,
                       HttpServletRequest request,
                       Model model) {
        var filters = new AppointmentsView.Filters(AppointmentsView.Tab.of(tab), q, doctor,
                AppointmentsView.Range.of(range), formPending, page);

        model.addAttribute("active", "appointments");
        model.addAttribute("filters", filters);
        model.addAttribute("tabs", AppointmentsView.Tab.values());
        model.addAttribute("ranges", AppointmentsView.Range.values());
        model.addAttribute("doctors", doctors());
        model.addAttribute("page", clinicAppointments.page(filters, LocalDateTime.now()));
        model.addAttribute("here", here(request));
        if (selected != null) {
            model.addAttribute("panel", clinicAppointments.panel(selected, deskMayAct(request)));
        }
        return "appointments/list";
    }

    /** The side panel on its own, for htmx to swap in beside the list. */
    @GetMapping("/{id}/panel")
    public String panel(@PathVariable UUID id,
                        @RequestParam(required = false) String back,
                        HttpServletRequest request,
                        Model model) {
        model.addAttribute("panel", clinicAppointments.panel(id, deskMayAct(request)));
        model.addAttribute("here", Redirects.within(back, "/appointments"));
        return "appointments/panel :: panel";
    }

    /**
     * The same list as a CSV file, with whatever filters are on.
     *
     * Staff only, like everything here; it holds names and dates of birth,
     * so the browser is told not to keep a copy.
     */
    @GetMapping("/export.csv")
    public void export(@RequestParam(defaultValue = "upcoming") String tab,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) UUID doctor,
                       @RequestParam(defaultValue = "any") String range,
                       @RequestParam(defaultValue = "false") boolean formPending,
                       HttpServletResponse response) throws IOException {
        var filters = new AppointmentsView.Filters(AppointmentsView.Tab.of(tab), q, doctor,
                AppointmentsView.Range.of(range), formPending, 0);
        var rows = clinicAppointments.export(filters, LocalDateTime.now());

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"appointments-" + filters.tab().getKey() + ".csv\"");

        PrintWriter out = response.getWriter();
        out.println("Date,Time,Patient,Date of birth,Booked for,Doctor,Status");
        for (List<String> row : rows) {
            out.println(String.join(",", row.stream().map(AppointmentsController::csv).toList()));
        }
        out.flush();
    }

    // ---- what the desk does ------------------------------------------------------

    @PostMapping("/{id}/check-in")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String checkIn(@PathVariable UUID id,
                          @RequestParam(required = false) String back,
                          RedirectAttributes redirect) {
        try {
            appointments.checkInAsClinic(id);
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("appointment.checkedIn"));
        } catch (CheckInNotOpenException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
        }
        return "redirect:" + Redirects.within(back, "/");
    }

    @PostMapping("/{id}/cancel")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String cancel(@PathVariable UUID id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String back,
                         RedirectAttributes redirect) {
        appointments.cancelAppointmentAsClinic(id, reason);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("appointments.cancelled"));
        return "redirect:" + Redirects.within(back, "/appointments");
    }

    @PostMapping("/{id}/remind")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String remind(@PathVariable UUID id,
                         @RequestParam(required = false) String back,
                         RedirectAttributes redirect) {
        String firstName = clinicAppointments.remindAboutForm(id);
        redirect.addFlashAttribute(WebUtils.MSG_SUCCESS,
                WebUtils.getMessage("appointments.reminded", firstName));
        return "redirect:" + Redirects.within(back, "/appointments");
    }

    /** Free times to move to, swapped into the panel. */
    @GetMapping("/{id}/reschedule")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String rescheduleOptions(@PathVariable UUID id,
                                    @RequestParam(required = false) String back,
                                    HttpServletRequest request,
                                    Model model) {
        model.addAttribute("panel", clinicAppointments.panel(id, deskMayAct(request)));
        model.addAttribute("slotsByDay", clinicAppointments.freeSlotsFor(id));
        model.addAttribute("here", Redirects.within(back, "/appointments"));
        return "appointments/reschedule :: reschedule";
    }

    @PostMapping("/{id}/reschedule")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String reschedule(@PathVariable UUID id,
                             @RequestParam UUID slotId,
                             @RequestParam(required = false) String back,
                             RedirectAttributes redirect) {
        try {
            Appointment moved = appointments.rescheduleAsClinic(id, slotId);
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage(
                    "appointments.moved", Display.dayAndTime(moved.getDateTime())));
            return "redirect:/appointments?selected=" + moved.getId();
        } catch (SlotNoLongerAvailableException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
            return "redirect:" + Redirects.within(back, "/appointments");
        }
    }

    // ---- a new booking --------------------------------------------------------------

    @GetMapping("/new")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String newAppointment(@RequestParam(required = false) UUID slot,
                                 @RequestParam(required = false) UUID patient,
                                 @RequestParam(required = false) UUID doctor,
                                 Model model) {
        model.addAttribute("active", "appointments");
        model.addAttribute("patients", patientRepository.findAll(Sort.by("lastName", "firstName")));
        model.addAttribute("doctors", doctors());
        model.addAttribute("selectedPatient", patient);

        if (slot != null) {
            clinicAppointments.slotLabel(slot).ifPresent(label -> {
                model.addAttribute("fixedSlot", slot);
                model.addAttribute("fixedSlotLabel", label);
            });
        } else if (doctor != null) {
            model.addAttribute("selectedDoctor", doctor);
            model.addAttribute("slotsByDay", clinicAppointments.freeSlots(doctor, ClinicAppointmentService.RESCHEDULE_DAYS));
        }
        return "appointments/new";
    }

    /** The free times for the chosen doctor, swapped into the form. */
    @GetMapping("/new/slots")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String slotsFor(@RequestParam(required = false) UUID doctor, Model model) {
        if (doctor != null) {
            model.addAttribute("slotsByDay", clinicAppointments.freeSlots(doctor, ClinicAppointmentService.RESCHEDULE_DAYS));
        }
        return "appointments/new :: slots";
    }

    @PostMapping("/new")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String book(@RequestParam(required = false) UUID patientId,
                       @RequestParam(required = false) UUID slotId,
                       @RequestParam(required = false) String note,
                       RedirectAttributes redirect) {
        if (patientId == null || slotId == null) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, WebUtils.getMessage("appointments.new.missing"));
            return "redirect:/appointments/new" + (slotId == null ? "" : "?slot=" + slotId);
        }
        try {
            Appointment booked = appointments.bookAsClinic(patientId, slotId, note);
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage(
                    "appointments.booked", booked.getPatient().getFirstName(),
                    Display.dayAndTime(booked.getDateTime())));
            return "redirect:/appointments?selected=" + booked.getId()
                    + (booked.getDateTime().toLocalDate().isEqual(java.time.LocalDate.now()) ? "&tab=today" : "");
        } catch (SlotNoLongerAvailableException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
            return "redirect:/appointments/new";
        }
    }

    // ---- helpers ---------------------------------------------------------------------

    private List<Doctor> doctors() {
        return doctorRepository.findAll(Sort.by("lastName", "firstName"));
    }

    private static boolean deskMayAct(HttpServletRequest request) {
        StaffSession session = StaffSession.of(request);
        return session != null && session.canManageAppointments();
    }

    /** This page's own address, so its forms can come back to it. */
    private static String here(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getRequestURI() + (query == null ? "" : "?" + query);
    }

    /** One CSV cell: quoted, and never read as a formula by a spreadsheet. */
    static String csv(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
