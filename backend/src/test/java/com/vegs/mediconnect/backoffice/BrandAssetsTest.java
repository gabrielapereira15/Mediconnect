package com.vegs.mediconnect.backoffice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The colours the back office's logo files actually paint with.
 *
 * Both wordmarks were once filled with Android resource references
 * (fill="@color/md_on_deep") pasted in from the app's vectors. A browser
 * does not know what those mean, drops them, and paints the default,
 * black: a black logo on the dark sidebar, and an all-black one on the
 * sign-in page. Nothing failed and no page errored, so it was only ever
 * going to be caught by someone looking closely.
 *
 * The files are shown through img, where the page's CSS cannot reach them,
 * so the only colours that work are literal ones. These pin each file to
 * the exact design-system values it should use, which also catches a
 * right-looking but wrong token pasted into the wrong file.
 */
class BrandAssetsTest {

    /** fill="..." attributes, and fill: declarations inside a style block. */
    private static final Pattern FILL = Pattern.compile("fill(?:=\"|\s*:\s*)([^\";}\s]+)");

    private static Set<String> fillsIn(String resource) throws IOException {
        try (InputStream in = BrandAssetsTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " is missing from the classpath");
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Set<String> fills = new TreeSet<>();
            Matcher matcher = FILL.matcher(svg);
            while (matcher.find()) {
                fills.add(matcher.group(1).toUpperCase());
            }
            return fills;
        }
    }

    @Test
    @DisplayName("the sign-in wordmark is brand teal and ink")
    void lightWordmark() throws IOException {
        assertEquals(Set.of("#0A7A80", "#0E2431"),
                fillsIn("/static/images/mediconnect.svg"));
    }

    @Test
    @DisplayName("the sidebar wordmark is light teal and on-deep")
    void onDeepWordmark() throws IOException {
        assertEquals(Set.of("#5BCACE", "#E8F4F5"),
                fillsIn("/static/images/mediconnect-on-deep.svg"));
    }

    @Test
    @DisplayName("the tab icon is the brand cross, lighter when the browser is dark")
    void favicon() throws IOException {
        assertEquals(Set.of("#0A7A80", "#5BCACE"),
                fillsIn("/static/images/favicon.svg"));
    }
}
