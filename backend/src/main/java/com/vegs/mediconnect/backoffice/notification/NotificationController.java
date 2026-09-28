package com.vegs.mediconnect.backoffice.notification;

import com.vegs.mediconnect.backoffice.util.CustomCollectors;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final PatientRepository patientRepository;

    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("patientsValues", patientRepository.findAll(
                        Sort.by("lastName", "firstName"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Patient::getId,
                        patient -> String.format("%s (%s)", patient.getFullName(), patient.getEmail()))));
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("notifications", notificationService.findAll());
        return "notification/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("notification") final NotificationDTO notificationDTO) {
        return "notification/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("notification") @Valid final NotificationDTO notificationDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "notification/add";
        }
        notificationService.create(notificationDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("notification.create.success"));
        return "redirect:/notifications";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final UUID id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = notificationService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            notificationService.delete(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("notification.delete.success"));
        }
        return "redirect:/notifications";
    }

}
