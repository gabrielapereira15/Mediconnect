package com.vegs.mediconnect.backoffice.message;

import com.vegs.mediconnect.backoffice.auth.RequiresRole;
import com.vegs.mediconnect.backoffice.auth.StaffSession;
import com.vegs.mediconnect.backoffice.shared.Display;
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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Messages (board B07): what the clinic has sent to patients' app inbox,
 * with how many have read it, and a place to write the next one with a
 * preview of how it will look on a phone.
 */
@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessagesController {

    private final ClinicMessages messages;

    @GetMapping
    public String page(@RequestParam(required = false) UUID selected,
                       HttpServletRequest request,
                       Model model) {
        model.addAttribute("active", "messages");
        model.addAttribute("sentList", messages.sentList());
        model.addAttribute("doctors", messages.doctors());
        model.addAttribute("canSend", deskMayAct(request));
        model.addAttribute("titleMax", ClinicMessages.TITLE_MAX);
        model.addAttribute("messageMax", ClinicMessages.MESSAGE_MAX);
        model.addAttribute("earliestSend", LocalDateTime.now().plus(ClinicMessages.SOONEST)
                .withSecond(0).withNano(0).toString());

        if (selected != null && !messages.isDraft(selected)) {
            model.addAttribute("sent", messages.sent(selected));
        } else if (!model.containsAttribute("draft")) {
            model.addAttribute("draft", selected != null
                    ? messages.draft(selected)
                    : new ClinicMessages.Draft(null, ClinicMessages.Audience.ALL, null, null, "", ""));
        }
        model.addAttribute("selectedId", selected);
        return "messages/page";
    }

    /** How many people the audience reaches, for the line under the send button. */
    @GetMapping("/reach")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String reach(@RequestParam(defaultValue = "all") String audience,
                        @RequestParam(required = false) UUID doctorId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                        Model model) {
        model.addAttribute("reach", messages.reach(new ClinicMessages.Draft(null,
                ClinicMessages.Audience.of(audience), doctorId, day, "", "")));
        return "messages/page :: reach";
    }

    @PostMapping
    @RequiresRole(StaffRole.FRONT_DESK)
    public String save(@RequestParam(required = false) UUID id,
                       @RequestParam(defaultValue = "all") String audience,
                       @RequestParam(required = false) UUID doctorId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                       @RequestParam(required = false) String title,
                       @RequestParam(required = false) String message,
                       @RequestParam(defaultValue = "send") String action,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sendAt,
                       RedirectAttributes redirect) {
        var draft = new ClinicMessages.Draft(id, ClinicMessages.Audience.of(audience), doctorId, day,
                title == null ? "" : title, message == null ? "" : message);

        if ("draft".equals(action)) {
            UUID saved = messages.saveDraft(draft);
            redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("messages.draftSaved"));
            return "redirect:/messages?selected=" + saved;
        }
        try {
            if ("schedule".equals(action)) {
                int reach = messages.schedule(draft, sendAt);
                redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("messages.scheduled",
                        Display.dayAndTime(sendAt), reach));
                return "redirect:/messages";
            }
            int reached = messages.send(draft);
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("messages.sent", reached));
            return "redirect:/messages";
        } catch (IllegalArgumentException e) {
            // Keep what they wrote: a rejected message should not have to be typed again.
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
            redirect.addFlashAttribute("draft", draft);
            if (sendAt != null) {
                redirect.addFlashAttribute("sendAt", sendAt);
            }
            return "redirect:/messages" + (id == null ? "" : "?selected=" + id);
        }
    }

    /** A scheduled message back to a draft, so it can be changed before it goes. */
    @PostMapping("/{id}/unschedule")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String unschedule(@PathVariable UUID id, RedirectAttributes redirect) {
        UUID draft = messages.unschedule(id);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("messages.unscheduled"));
        return "redirect:/messages?selected=" + draft;
    }

    @PostMapping("/{id}/withdraw")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String withdraw(@PathVariable UUID id, RedirectAttributes redirect) {
        boolean draft = messages.isDraft(id);
        boolean scheduled = !draft && messages.sent(id).scheduled();
        messages.withdraw(id);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage(
                draft ? "messages.draftDiscarded" : scheduled ? "messages.scheduleCancelled" : "messages.withdrawn"));
        return "redirect:/messages";
    }

    private static boolean deskMayAct(HttpServletRequest request) {
        StaffSession session = StaffSession.of(request);
        return session != null && session.canManageAppointments();
    }
}
