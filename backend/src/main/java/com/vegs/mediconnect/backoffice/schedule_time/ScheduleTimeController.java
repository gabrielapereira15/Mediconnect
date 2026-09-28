package com.vegs.mediconnect.backoffice.schedule_time;

import com.vegs.mediconnect.backoffice.util.CustomCollectors;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import com.vegs.mediconnect.datasource.schedule.Schedule;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;


@Controller
@RequestMapping("/scheduleTimes")
public class ScheduleTimeController {

    private final ScheduleTimeService scheduleTimeService;
    private final ScheduleRepository scheduleRepository;

    public ScheduleTimeController(final ScheduleTimeService scheduleTimeService,
            final ScheduleRepository scheduleRepository) {
        this.scheduleTimeService = scheduleTimeService;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("scheduleIdValues", scheduleRepository.findAll(
                Sort.by("doctor.lastName", "doctor.firstName", "date"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Schedule::getId, Schedule::getScheduleDoctor )));
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("scheduleTimes", scheduleTimeService.findAll());
        return "scheduleTime/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("scheduleTime") final ScheduleTimeDTO scheduleTimeDTO) {
        return "scheduleTime/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("scheduleTime") @Valid final ScheduleTimeDTO scheduleTimeDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            checkUniqueConstraint(bindingResult);
            return "scheduleTime/add";
        }
        scheduleTimeService.create(scheduleTimeDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("scheduleTime.create.success"));
        return "redirect:/scheduleTimes";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id, final Model model) {
        model.addAttribute("scheduleTime", scheduleTimeService.get(id));
        return "scheduleTime/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final UUID id,
            @ModelAttribute("scheduleTime") @Valid final ScheduleTimeDTO scheduleTimeDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            checkUniqueConstraint(bindingResult);
            return "scheduleTime/edit";
        }
        scheduleTimeService.update(id, scheduleTimeDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("scheduleTime.update.success"));
        return "redirect:/scheduleTimes";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final UUID id,
            final RedirectAttributes redirectAttributes) {
        scheduleTimeService.delete(id);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("scheduleTime.delete.success"));
        return "redirect:/scheduleTimes";
    }

    private static void checkUniqueConstraint(BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("scheduleDateTime")) {
            bindingResult.addError(new FieldError("scheduleTime", "time",
                    Optional.ofNullable(bindingResult.getFieldError("scheduleDateTime"))
                            .map(FieldError::getDefaultMessage)
                            .orElse("Error")));
        }
    }

}
