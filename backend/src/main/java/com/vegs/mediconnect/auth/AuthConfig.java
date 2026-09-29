package com.vegs.mediconnect.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the token check onto the mobile API.
 *
 * Deliberately scoped: the Thymeleaf back office in this same application is
 * an internal tool with its own separate story, and is left as it was rather
 * than being locked down as a side effect of securing the phone endpoints.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class AuthConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/mobile/patients/**",
                        "/api/mobile/appointments/**",
                        "/api/mobile/notifications/**",
                        "/api/mobile/reviews/**",
                        // The FHIR facade serves the same health data under
                        // a different shape, so it needs the same guard.
                        "/fhir/**")
                // The doctor directory is what the app shows before sign-in,
                // and photos are referenced by <img> tags that carry no header.
                .excludePathPatterns(
                        "/api/mobile/doctors/**",
                        "/auth/**",
                        // A FHIR client has to be able to discover what the
                        // server supports before it has anything to sign in
                        // with, and the directory is already public.
                        "/fhir/metadata",
                        "/fhir/Practitioner",
                        "/fhir/Practitioner/*",
                        "/fhir/PractitionerRole",
                        "/fhir/Organization/*",
                        "/fhir/Schedule",
                        "/fhir/Slot");
    }
}
