package com.vegs.mediconnect.backoffice.auth;

import com.vegs.mediconnect.datasource.staff.StaffUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import com.vegs.mediconnect.backoffice.shared.Redirects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Signing in and out of the back office.
 */
@Controller
@RequiredArgsConstructor
public class StaffLoginController {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/staff/login")
    public String loginForm(@RequestParam(required = false) String next, Model model,
                            HttpServletRequest request) {
        if (StaffSession.of(request) != null) {
            return "redirect:" + safeNext(next);
        }
        model.addAttribute("next", next);
        return "staff/login";
    }

    @PostMapping("/staff/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam(required = false) String next,
                        Model model,
                        HttpServletRequest request) {
        var user = staffUserRepository.findByEmailIgnoreCase(email.trim())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()));

        if (user.isEmpty()) {
            // One message for a wrong email and a wrong password: saying
            // which is wrong tells someone guessing that the address exists.
            model.addAttribute("error", true);
            model.addAttribute("email", email);
            model.addAttribute("next", next);
            return "staff/login";
        }

        StaffSession.begin(request, user.get());
        return "redirect:" + safeNext(next);
    }

    @GetMapping("/staff/logout")
    public String logout(HttpServletRequest request) {
        StaffSession.end(request);
        return "redirect:/staff/login";
    }

    /**
     * Only ever back into this application.
     *
     * "next" comes from the URL, so an absolute one would turn the sign-in
     * page into an open redirect: a link that signs someone in and drops
     * them on somebody else's site.
     */
    private String safeNext(String next) {
        return Redirects.within(next, "/");
    }
}
