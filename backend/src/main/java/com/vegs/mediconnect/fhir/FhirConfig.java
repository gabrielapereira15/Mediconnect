package com.vegs.mediconnect.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FhirProperties.class)
public class FhirConfig {

    /**
     * Building a FhirContext is expensive — it scans the whole R4 model — so
     * it is created once and shared. HAPI documents it as thread-safe after
     * initialisation, which is what makes that safe.
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
