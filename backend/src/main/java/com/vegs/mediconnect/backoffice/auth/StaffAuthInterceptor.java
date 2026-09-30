package com.vegs.mediconnect.backoffice.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Keeps the back office to the people who work at the clinic, and each
 * part of it to the role it belongs to.
 *
 * There was no check at all before this: every patient's record and every
 * appointment was one URL away from anybody who found the address. Signed
 * out, a request is sent to the sign-in page rather than refused, because
 * the usual reason to be signed out is that the session expired. Signed in
 * with the wrong role, it is refused outright (403): a hidden link is not a
 * permission.
 */
@Component
public class StaffAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        StaffSession session = StaffSession.of(request);
        if (session == null) {
            // Where they were going, so signing in lands them there and not
            // on a dashboard they did not ask for.
            String target = request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) {
                target = target + "?" + query;
            }

            response.sendRedirect(request.getContextPath() + "/staff/login?next="
                    + URLEncoder.encode(target, StandardCharsets.UTF_8));
            return false;
        }

        RequiresRole required = requiredRole(handler);
        if (required != null && session.getRole() != required.value()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    /** The method's own requirement, else its controller's, else none. */
    static RequiresRole requiredRole(Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return null;
        }
        RequiresRole onMethod = method.getMethodAnnotation(RequiresRole.class);
        if (onMethod != null) {
            return onMethod;
        }
        return method.getBeanType().getAnnotation(RequiresRole.class);
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
