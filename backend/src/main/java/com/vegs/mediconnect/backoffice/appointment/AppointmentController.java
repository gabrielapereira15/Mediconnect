package com.vegs.mediconnect.backoffice.appointment;

import com.vegs.mediconnect.backoffice.util.CustomCollectors;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.mobile.appointment.AppointmentApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentApiService appointmentApiService;

    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("patientIdValues", patientRepository.findAll(
                Sort.by("lastName", "firstName"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Patient::getId, Patient::getFullName)));
        model.addAttribute("doctorIdValues", doctorRepository.findAll(
                Sort.by("lastName", "firstName"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Doctor::getId, Doctor::getFullName)));
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("appointments", appointmentService.findAllBook()
                .stream()
                .filter(AppointmentDTO::notRemoved)
                .toList());
        return "appointment/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("appointment") final AppointmentDTO appointmentDTO) {
        return "appointment/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("appointment") @Valid final AppointmentDTO appointmentDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "appointment/add";
        }
        try {
            appointmentService.book(appointmentDTO);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("appointment.create.success"));
        } catch (ScheduleTimeUnavailable e) {
            bindingResult.addError(new FieldError("appointment", "scheduleTime", "This schedule time already in use"));
            return "appointment/add";
        } catch (Exception e) {
            bindingResult.addError(new FieldError("appointment", "scheduleTime", "Error to create appointment"));
            return "appointment/add";
        }
        return "redirect:/appointments";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final UUID id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = appointmentService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            appointmentApiService.cancelAppointment(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("appointment.delete.success"));
        }
        return "redirect:/appointments";
    }

    @PostMapping("/remove/{id}")
    public String remove(@PathVariable(name = "id") final UUID id,
                         final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = appointmentService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            appointmentApiService.removeAppointment(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("appointment.remove.success"));
        }
        return "redirect:/appointments";
    }

}
