package com.vegs.mediconnect.backoffice.patient;

import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(final PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("patients", patientService.findAll());
        return "patient/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("patient") final PatientDTO patientDTO) {
        return "patient/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("patient") @Valid final PatientDTO patientDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "patient/add";
        }
        patientService.create(patientDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("patient.create.success"));
        return "redirect:/patients";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id, final Model model) {
        model.addAttribute("patient", patientService.get(id));
        return "patient/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id,
            @ModelAttribute("patient") @Valid final PatientDTO patientDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "patient/edit";
        }
        patientService.update(id, patientDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("patient.update.success"));
        return "redirect:/patients";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final UUID id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = patientService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            patientService.delete(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("patient.delete.success"));
        }
        return "redirect:/patients";
    }

}
