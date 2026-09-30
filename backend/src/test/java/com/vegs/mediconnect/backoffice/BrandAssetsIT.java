package com.vegs.mediconnect.backoffice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The logo and tab icon reach a browser that has not signed in yet.
 *
 * Everything in the back office is guarded unless it is listed as open, so
 * an icon put somewhere new would be answered with a redirect to sign in
 * and the sign-in page would show a broken image. Same setup as
 * BackOfficeAccessIT, so the two share one application context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class BrandAssetsIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the tab icons and logos are served before sign-in")
    void servedSignedOut() throws Exception {
        mockMvc.perform(get("/images/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/images/mediconnect.svg"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/images/mediconnect-on-deep.svg"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the sign-in page links the tab icon and the light wordmark")
    void signInPageLinksThem() throws Exception {
        mockMvc.perform(get("/staff/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/images/favicon.svg\"")))
                .andExpect(content().string(containsString("src=\"/images/mediconnect.svg\"")));
    }
}
