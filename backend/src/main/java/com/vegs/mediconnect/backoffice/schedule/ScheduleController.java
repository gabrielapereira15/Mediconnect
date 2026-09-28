package com.vegs.mediconnect.backoffice.schedule;

import com.vegs.mediconnect.backoffice.schedule_time.ScheduleTimeDTO;
import com.vegs.mediconnect.backoffice.schedule_time.ScheduleTimeService;
import com.vegs.mediconnect.backoffice.util.CustomCollectors;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Controller
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleTimeService scheduleTimeService;
    private final DoctorRepository doctorRepository;

    public ScheduleController(final ScheduleService scheduleService, ScheduleTimeService scheduleTimeService,
                              final DoctorRepository doctorRepository) {
        this.scheduleService = scheduleService;
        this.scheduleTimeService = scheduleTimeService;
        this.doctorRepository = doctorRepository;
    }

    @ModelAttribute
    public void prepareContext(final Model model) {
        var allDoctors = doctorRepository.findAll(Sort.by("lastName", "firstName"));

        List<String> times = createAllTimes();
        model.addAttribute("timesValues", times);
        model.addAttribute("doctorIdValues", allDoctors
                .stream()
                .collect(CustomCollectors.toSortedMap(Doctor::getId, Doctor::getFullName)));
    }

    private static List<String> createAllTimes() {
        List<String> times = new ArrayList<>();
        times.add("9 AM");
        times.add("10 AM");
        times.add("11 AM");
        times.add("12 PM");
        times.add("1 PM");
        times.add("2 PM");
        times.add("3 PM");
        times.add("4 PM");
        times.add("5 PM");
        return times;
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("schedules", scheduleService.findAll());
        return "schedule/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("schedule") final ScheduleDTO scheduleDTO) {
        return "schedule/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("schedule") @Valid final ScheduleDTO scheduleDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            checkUniqueConstraint(bindingResult);
            scheduleDTO.getTimes().clear();
            return "schedule/add";
        }
        var schedule = scheduleService.create(scheduleDTO);
        scheduleDTO
                .getTimes()
                .stream()
                .map(ScheduleTimeDTO::new)
                .forEach(scheduleTimeDTO -> {
                    scheduleTimeDTO.setScheduleId(schedule.getId());
                    scheduleTimeService.create(scheduleTimeDTO);
                });
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("schedule.create.success"));
        return "redirect:/schedules";
    }

    private static void checkUniqueConstraint(BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("doctorDate")) {
            bindingResult.addError(new FieldError("schedule", "date",
                    Optional.ofNullable(bindingResult.getFieldError("doctorDate"))
                            .map(FieldError::getDefaultMessage)
                            .orElse("Error")));
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id, final Model model) {
        var schedule = scheduleService.get(id);
        model.addAttribute("schedule", schedule);
        return "schedule/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id,
            @ModelAttribute("schedule") @Valid final ScheduleDTO scheduleDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            checkUniqueConstraint(bindingResult);
            return "schedule/edit";
        }
        scheduleService.update(id, scheduleDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("schedule.update.success"));
        return "redirect:/schedules";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final UUID id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = scheduleService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            scheduleService.delete(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("schedule.delete.success"));
        }
        return "redirect:/schedules";
    }

}
