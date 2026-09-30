package com.vegs.mediconnect.backoffice.doctor;

import com.vegs.mediconnect.backoffice.auth.RequiresRole;
import com.vegs.mediconnect.backoffice.auth.StaffSession;
import com.vegs.mediconnect.backoffice.util.WebUtils;
import com.vegs.mediconnect.datasource.staff.StaffRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Doctors and their availability (board B06).
 *
 * Every doctor as a card with how full this week is; the selected one in a
 * panel with their profile, their usual week and days off, and what
 * patients have said about them. Saving the week rewrites the diary ahead
 * without moving anybody's appointment.
 */
@Controller
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorsController {

    private static final List<String> TABS = List.of("profile", "availability", "reviews");

    private final DoctorDirectory directory;
    private final AvailabilityService availability;

    @GetMapping
    public String list(@RequestParam(required = false) UUID selected,
                       @RequestParam(defaultValue = "availability") String tab,
                       HttpServletRequest request,
                       Model model) {
        model.addAttribute("active", "doctors");
        model.addAttribute("cards", directory.cards());
        if (selected != null) {
            fillPanel(selected, tab, request, model);
        }
        return "doctors/list";
    }

    @GetMapping("/{id}/panel")
    public String panel(@PathVariable UUID id,
                        @RequestParam(defaultValue = "availability") String tab,
                        HttpServletRequest request,
                        Model model) {
        fillPanel(id, tab, request, model);
        return "doctors/panel :: panel";
    }

    private void fillPanel(UUID id, String tab, HttpServletRequest request, Model model) {
        String chosen = TABS.contains(tab) ? tab : "availability";
        model.addAttribute("tab", chosen);
        model.addAttribute("profile", directory.profile(id));
        model.addAttribute("canEdit", deskMayAct(request));
        model.addAttribute("reviewCount", directory.reviews(id).size());
        switch (chosen) {
            case "reviews" -> model.addAttribute("reviews", directory.reviews(id));
            case "availability" -> {
                var editor = availability.editor(id);
                model.addAttribute("editor", editor);
                model.addAttribute("days", WeeklyHours.WEEK.stream()
                        .map(day -> DayRow.of(day, editor.hours()))
                        .toList());
                model.addAttribute("slotLengths", WeeklyHours.SLOT_LENGTHS);
                model.addAttribute("horizons", WeeklyHours.HORIZONS);
                model.addAttribute("today", LocalDate.now());
            }
            default -> { }
        }
    }

    // ---- profile ------------------------------------------------------------------

    @PostMapping("/{id}/profile")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String saveProfile(@PathVariable UUID id,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String specialty,
                              @RequestParam(required = false) String experienceInYears,
                              @RequestParam(required = false) String about,
                              RedirectAttributes redirect) {
        String problem = directory.update(id, firstName, lastName, specialty, experienceInYears, about);
        flash(redirect, problem, "doctors.profile.saved");
        return back(id, "profile");
    }

    @PostMapping("/{id}/photo")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String savePhoto(@PathVariable UUID id,
                            @RequestParam(required = false) MultipartFile photo,
                            RedirectAttributes redirect) throws IOException {
        String problem = directory.updatePhoto(id, photo);
        flash(redirect, problem, "doctors.photo.saved");
        return back(id, "profile");
    }

    @GetMapping("/new")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String newDoctor(Model model) {
        model.addAttribute("active", "doctors");
        return "doctors/new";
    }

    @PostMapping("/new")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String create(@RequestParam String firstName,
                         @RequestParam String lastName,
                         @RequestParam String specialty,
                         @RequestParam(required = false) String experienceInYears,
                         @RequestParam(required = false) String about,
                         RedirectAttributes redirect) {
        try {
            var doctor = directory.create(firstName, lastName, specialty, experienceInYears, about);
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("doctors.created"));
            return back(doctor.getId(), "availability");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
            return "redirect:/doctors/new";
        }
    }

    // ---- availability ---------------------------------------------------------------

    /**
     * The week as the form sends it: for each day a switch and up to two
     * stretches, "start1_MONDAY" to "end1_MONDAY" and so on.
     */
    @PostMapping("/{id}/availability")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String saveAvailability(@PathVariable UUID id,
                                   @RequestParam Map<String, String> form,
                                   RedirectAttributes redirect) {
        WeeklyHours hours;
        try {
            hours = read(form);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
            return back(id, "availability");
        }
        try {
            var result = availability.save(id, hours);
            String message = WebUtils.getMessage("doctors.availability.saved", result.created(), result.removed());
            if (result.keptBooked() > 0) {
                message += " " + WebUtils.getMessage("doctors.availability.kept", result.keptBooked());
            }
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, message);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
        }
        return back(id, "availability");
    }

    static WeeklyHours read(Map<String, String> form) {
        Map<DayOfWeek, List<WeeklyHours.Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : WeeklyHours.WEEK) {
            if (!"on".equals(form.get("on_" + day)) && !"true".equals(form.get("on_" + day))) {
                continue;
            }
            List<WeeklyHours.Range> ranges = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                String start = form.get("start" + i + "_" + day);
                String end = form.get("end" + i + "_" + day);
                if (blank(start) && blank(end)) {
                    continue;
                }
                if (blank(start) || blank(end)) {
                    throw new IllegalArgumentException(name(day) + ": give both a start and an end.");
                }
                try {
                    ranges.add(new WeeklyHours.Range(LocalTime.parse(start.trim()), LocalTime.parse(end.trim())));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(name(day) + ": times look like 09:00.");
                }
            }
            if (ranges.isEmpty()) {
                throw new IllegalArgumentException(name(day) + " is switched on but has no hours.");
            }
            days.put(day, ranges);
        }
        return new WeeklyHours(days, number(form.get("slotMinutes"), 30), number(form.get("weeks"), 3));
    }

    @PostMapping("/{id}/days-off")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String addDayOff(@PathVariable UUID id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) String reason,
                            RedirectAttributes redirect) {
        if (date == null) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, WebUtils.getMessage("doctors.dayOff.noDate"));
            return back(id, "availability");
        }
        try {
            int toRebook = availability.addDayOff(id, date, reason);
            redirect.addFlashAttribute(toRebook > 0 ? WebUtils.MSG_ERROR : WebUtils.MSG_SUCCESS, toRebook > 0
                    ? WebUtils.getMessage("doctors.dayOff.rebook", toRebook)
                    : WebUtils.getMessage("doctors.dayOff.added"));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
        }
        return back(id, "availability");
    }

    @PostMapping("/{id}/days-off/{dayOffId}/remove")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String removeDayOff(@PathVariable UUID id,
                               @PathVariable UUID dayOffId,
                               RedirectAttributes redirect) {
        availability.removeDayOff(dayOffId);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("doctors.dayOff.removed"));
        return back(id, "availability");
    }

    // ---- helpers ----------------------------------------------------------------------

    /** One line of the weekly-hours form, already written out. */
    public record DayRow(String key, String shortName, String fullName, boolean on,
                         String start1, String end1, String start2, String end2) {

        static DayRow of(DayOfWeek day, WeeklyHours hours) {
            List<WeeklyHours.Range> ranges = hours.on(day);
            return new DayRow(day.name(),
                    day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                    name(day),
                    !ranges.isEmpty(),
                    ranges.size() > 0 ? ranges.get(0).start().toString() : "",
                    ranges.size() > 0 ? ranges.get(0).end().toString() : "",
                    ranges.size() > 1 ? ranges.get(1).start().toString() : "",
                    ranges.size() > 1 ? ranges.get(1).end().toString() : "");
        }
    }

    private static String back(UUID id, String tab) {
        return "redirect:/doctors?selected=" + id + "&tab=" + tab;
    }

    private static void flash(RedirectAttributes redirect, String problem, String successKey) {
        if (problem == null) {
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage(successKey));
        } else {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, problem);
        }
    }

    private static boolean deskMayAct(HttpServletRequest request) {
        StaffSession session = StaffSession.of(request);
        return session != null && session.canManageAppointments();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static int number(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String name(DayOfWeek day) {
        return day.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
    }
}
