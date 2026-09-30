package com.vegs.mediconnect.backoffice.waitlist;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Waitlist (board B05): everyone waiting for something earlier, the freed
 * slots being held for somebody, and the desk's own offers.
 */
@Controller
@RequestMapping("/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistBoard board;

    @GetMapping
    public String list(@RequestParam(defaultValue = "waiting") String tab,
                       @RequestParam(required = false) UUID offer,
                       HttpServletRequest request,
                       Model model) {
        var chosen = WaitlistBoard.Tab.of(tab);
        model.addAttribute("active", "waitlist");
        model.addAttribute("tab", chosen);
        model.addAttribute("tabs", WaitlistBoard.Tab.values());
        model.addAttribute("board", board.board(chosen));
        model.addAttribute("canAct", deskMayAct(request));
        if (offer != null && deskMayAct(request)) {
            model.addAttribute("options", board.offerOptions(offer));
        }
        return "waitlist/list";
    }

    /** Earlier free times for one person, swapped into the panel. */
    @GetMapping("/{id}/offer")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String offerOptions(@PathVariable UUID id, Model model) {
        model.addAttribute("options", board.offerOptions(id));
        return "waitlist/offer :: offer";
    }

    @PostMapping("/{id}/offer")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String offer(@PathVariable UUID id,
                        @RequestParam(required = false) UUID slotId,
                        RedirectAttributes redirect) {
        if (slotId == null) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, WebUtils.getMessage("waitlist.offer.pick"));
            return "redirect:/waitlist?offer=" + id;
        }
        String problem = board.offer(id, slotId);
        if (problem == null) {
            redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("waitlist.offer.done"));
            return "redirect:/waitlist?tab=offered";
        }
        redirect.addFlashAttribute(WebUtils.MSG_ERROR, problem);
        return "redirect:/waitlist?offer=" + id;
    }

    @PostMapping("/{id}/end-offer")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String endOffer(@PathVariable UUID id, RedirectAttributes redirect) {
        board.endOffer(id);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("schedule.released"));
        return "redirect:/waitlist?tab=offered";
    }

    @PostMapping("/{id}/withdraw")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String withdraw(@PathVariable UUID id,
                           @RequestParam(defaultValue = "waiting") String tab,
                           RedirectAttributes redirect) {
        board.withdraw(id);
        redirect.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("waitlist.withdrawn"));
        return "redirect:/waitlist?tab=" + WaitlistBoard.Tab.of(tab).getKey();
    }

    @GetMapping("/new")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String newEntry(Model model) {
        model.addAttribute("active", "waitlist");
        model.addAttribute("candidates", board.candidates());
        model.addAttribute("today", LocalDate.now());
        return "waitlist/new";
    }

    @PostMapping("/new")
    @RequiresRole(StaffRole.FRONT_DESK)
    public String add(@RequestParam(required = false) UUID appointmentId,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableFrom,
                      RedirectAttributes redirect) {
        if (appointmentId == null) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, WebUtils.getMessage("waitlist.new.pick"));
            return "redirect:/waitlist/new";
        }
        String problem = board.add(appointmentId, availableFrom);
        if (problem != null) {
            redirect.addFlashAttribute(WebUtils.MSG_ERROR, problem);
            return "redirect:/waitlist/new";
        }
        redirect.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("waitlist.added"));
        return "redirect:/waitlist";
    }

    private static boolean deskMayAct(HttpServletRequest request) {
        StaffSession session = StaffSession.of(request);
        return session != null && session.canManageAppointments();
    }
}
