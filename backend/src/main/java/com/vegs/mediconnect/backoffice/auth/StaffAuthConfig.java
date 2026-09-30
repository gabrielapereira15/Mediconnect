package com.vegs.mediconnect.backoffice.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Puts the staff check in front of the back office, and nothing else.
 *
 * The mobile API carries its own token and is checked by its own
 * interceptor; the FHIR facade likewise. Listing what this guards, rather
 * than guarding everything and listing exceptions, is what keeps a new
 * mobile route from being locked out by a change made for the web.
 */
@Configuration
@RequiredArgsConstructor
public class StaffAuthConfig implements WebMvcConfigurer {

    private final StaffAuthInterceptor staffAuthInterceptor;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(staffAuthInterceptor)
                .addPathPatterns(
                        "/",
                        "/appointments/**",
                        "/doctors/**",
                        "/patients/**",
                        "/schedules/**",
                        "/scheduleTimes/**",
                        "/notifications/**",
                        "/waitlist/**")
                // The sign-in page itself, and the static files every page
                // needs before anyone has signed in.
                .excludePathPatterns(
                        "/staff/login",
                        "/staff/logout",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/error");
    }
}
