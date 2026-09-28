package com.vegs.mediconnect.backoffice.doctor;

import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


@Slf4j
@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(final DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("doctors", doctorService.findAll());
        return "doctor/list";
    }

    @GetMapping("/newPhoto/{id}")
    public String newPhoto(@PathVariable(name = "id") final UUID id, final Model model) {
        model.addAttribute("doctor", new DoctorNewPhotoDTO(id, null));
        return "doctor/newPhoto";
    }

    @PostMapping("/newPhoto/{id}")
    public String newPhoto(@PathVariable(name = "id") final UUID id,
                           @ModelAttribute("doctor") @Valid final DoctorNewPhotoDTO doctorDTO,
                       final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "doctor/newPhoto";
        }
        doctorService.updatePhoto(id, doctorDTO.getImage());
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("doctor.update.success"));
        return "redirect:/doctors";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("doctor") final DoctorDTO doctorDTO) {
        return "doctor/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("doctor") @Valid final DoctorDTO doctorDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "doctor/add";
        }
        doctorService.create(doctorDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("doctor.create.success"));
        return "redirect:/doctors";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id, final Model model) {
        model.addAttribute("doctor", doctorService.get(id));
        return "doctor/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id,
            @ModelAttribute("doctor") @Valid final DoctorDTO doctorDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "doctor/edit";
        }
        doctorService.update(id, doctorDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("doctor.update.success"));
        return "redirect:/doctors";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final UUID id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = doctorService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            doctorService.delete(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("doctor.delete.success"));
        }
        return "redirect:/doctors";
    }

}
