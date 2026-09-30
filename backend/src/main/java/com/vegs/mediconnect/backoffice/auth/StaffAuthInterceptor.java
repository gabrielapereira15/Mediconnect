package com.vegs.mediconnect.backoffice.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Keeps the back office to the people who work at the clinic.
 *
 * There was no check at all before this: every patient's record and every
 * appointment was one URL away from anybody who found the address. Signed
 * out, a request is sent to the sign-in page rather than refused, because
 * the usual reason to be signed out is that the session expired.
 */
@Component
public class StaffAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        StaffSession session = StaffSession.of(request);
        if (session != null) {
            return true;
        }

        // Where they were going, so signing in lands them there and not on
        // a dashboard they did not ask for.
        String target = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null) {
            target = target + "?" + query;
        }

        response.sendRedirect(request.getContextPath() + "/staff/login?next="
                + URLEncoder.encode(target, StandardCharsets.UTF_8));
        return false;
    }

    /**
     * Every page needs to say who is signed in, so the session goes into the
     * model here instead of in each controller.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        if (modelAndView == null) {
            return;
        }
        StaffSession session = StaffSession.of(request);
        if (session != null) {
            modelAndView.addObject("staff", session);
        }
    }
}
