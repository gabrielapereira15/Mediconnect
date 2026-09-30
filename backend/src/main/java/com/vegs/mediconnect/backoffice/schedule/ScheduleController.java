package com.vegs.mediconnect.backoffice.schedule;

import com.vegs.mediconnect.backoffice.auth.RequiresRole;
import com.vegs.mediconnect.backoffice.auth.StaffSession;
import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.shared.Redirects;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Schedule (board B02): the day, doctor by doctor, with one slot open in
 * the side panel. A week view gives the shape of the next few days.
 */
@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleBoardService board;

    /**
     * The day board, or the week: every doctor's week at a glance, or one
     * doctor's week as a grid of the same slot cells as the day board.
     */
    @GetMapping
    public String schedule(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(defaultValue = "day") String view,
                           @RequestParam(required = false) String specialty,
                           @RequestParam(required = false) UUID doctor,
                           @RequestParam(required = false) UUID slot,
                           HttpServletRequest request,
                           Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        boolean week = "week".equalsIgnoreCase(view);
        LocalDate monday = ScheduleBoardService.weekStart(day);

        model.addAttribute("active", "schedule");
        model.addAttribute("date", day);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("dateLabel", week
                ? "Week of " + Display.day(monday)
                : Display.longDay(day));
        model.addAttribute("previous", day.minusDays(week ? 7 : 1));
        model.addAttribute("next", day.plusDays(week ? 7 : 1));
        model.addAttribute("isToday", week
                ? monday.isEqual(ScheduleBoardService.weekStart(LocalDate.now()))
                : day.isEqual(LocalDate.now()));
        model.addAttribute("view", week ? "week" : "day");
        model.addAttribute("specialty", specialty == null ? "" : specialty);
        model.addAttribute("specialties", board.specialties());
        model.addAttribute("doctor", doctor);
        model.addAttribute("here", here(request));
        if (week) {
            model.addAttribute("doctors", board.doctors());
            if (doctor != null) {
                model.addAttribute("doctorWeek", board.doctorWeek(day, doctor));
            } else {
                model.addAttribute("week", board.week(day, specialty));
            }
        } else {
            model.addAttribute("board", board.day(day, specialty));
        }
        if (slot != null) {
            model.addAttribute("panel", board.panel(slot, deskMayAct(request), ownDoctor(request)));
        }
        return "schedule/board";
    }

    @GetMapping("/slots/{id}")
    public String slot(@PathVariable UUID id,
                       @RequestParam(required = false) String back,
                       HttpServletRequest request,
                       Model model) {
        model.addAttribute("panel", board.panel(id, deskMayAct(request), ownDoctor(request)));
        model.addAttribute("here", Redirects.within(back, "/schedule"));
        return "schedule/slot :: slot";
    }

    @PostMapping("/slots/{id}/offer")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String offer(@PathVariable UUID id,
                        @RequestParam(required = false) String back,
                        RedirectAttributes redirect) {
        String who = board.offerToWaitlist(id);
        if (who == null) {
            redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("schedule.offer.nobody"));
        } else {
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("schedule.offer.done", who));
        }
        return "redirect:" + Redirects.within(back, "/schedule");
    }

    @PostMapping("/slots/{id}/block")
    public String block(@PathVariable UUID id,
                        @RequestParam(required = false) String back,
                        HttpServletRequest request,
                        RedirectAttributes redirect) {
        requireAgenda(request, id);
        if (board.block(id)) {
            redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("schedule.blocked"));
        } else {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, WebUtils.getMessage("schedule.block.taken"));
        }
        return "redirect:" + Redirects.within(back, "/schedule");
    }

    @PostMapping("/slots/{id}/unblock")
    public String unblock(@PathVariable UUID id,
                          @RequestParam(required = false) String back,
                          HttpServletRequest request,
                          RedirectAttributes redirect) {
        requireAgenda(request, id);
        board.unblock(id);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("schedule.unblocked"));
        return "redirect:" + Redirects.within(back, "/schedule");
    }

    @PostMapping("/slots/{id}/release")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String release(@PathVariable UUID id,
                          @RequestParam(required = false) String back,
                          RedirectAttributes redirect) {
        board.releaseHold(id);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("schedule.released"));
        return "redirect:" + Redirects.within(back, "/schedule");
    }

    /** The doctor a clinician's login belongs to, or null. */
    private static UUID ownDoctor(HttpServletRequest request) {
        StaffSession session = StaffSession.of(request);
        return session == null ? null : session.getDoctorId();
    }

    /** Blocking is part of a doctor's agenda: the desk for anyone, a doctor for their own slots. */
    private void requireAgenda(HttpServletRequest request, UUID slotId) {
        StaffSession session = StaffSession.of(request);
        if (session == null || !session.canEditAgendaOf(board.doctorOf(slotId))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    private static boolean deskMayAct(HttpServletRequest request) {
        StaffSession session = StaffSession.of(request);
        return session != null && session.canManageAppointments();
    }

    private static String here(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getRequestURI() + (query == null ? "" : "?" + query);
    }
}
