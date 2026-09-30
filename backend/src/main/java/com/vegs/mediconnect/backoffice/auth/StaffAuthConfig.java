package com.vegs.mediconnect.backoffice.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Puts the staff check in front of everything that is not the patient API.
 *
 * Everything is guarded unless it is listed as belonging to someone else.
 * The previous allow-list guarded the pages it named and nothing more,
 * which left a scaffolded REST API — every patient, readable and deletable
 * — and a file upload open to anybody, because nobody had thought to add
 * them to the list. The mobile API, sign-in and the FHIR facade carry
 * their own bearer token and are checked by their own interceptor, so they
 * are excluded here by prefix: a new mobile route cannot be locked out by
 * a change made for the web.
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
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // The patient's side, with its own token check.
                        "/api/mobile/**",
                        "/auth/**",
                        "/fhir/**",
                        // The sign-in page itself, and the static files
                        // every page needs before anyone has signed in.
                        "/staff/login",
                        "/staff/logout",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error");
    }
}
