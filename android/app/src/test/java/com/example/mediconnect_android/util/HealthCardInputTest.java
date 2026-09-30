package com.example.mediconnect_android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import org.junit.Test;

/**
 * The health card as Edit profile sends it.
 *
 * The form used to send no card at all, and the server saved that as "no
 * card": every profile save erased it. These check what goes into the body
 * in each case, and that the app refuses the same numbers the server does.
 */
public class HealthCardInputTest {

    @Test
    public void cleanDropsSpacesAndDashesAndCapitalises() {
        assertEquals("1234567890", HealthCardInput.clean(" 1234-567 890 "));
        assertEquals("DOEJ90041234", HealthCardInput.clean("doej 9004 1234"));
        assertEquals("", HealthCardInput.clean(null));
        assertEquals("", HealthCardInput.clean("   "));
    }

    @Test
    public void anEmptyFieldIsNotAProblem() {
        assertEquals(HealthCardInput.Problem.NONE, HealthCardInput.check(""));
        assertEquals(HealthCardInput.Problem.NONE, HealthCardInput.check(null));
    }

    @Test
    public void cardsFromEveryProvinceAreAccepted() {
        // PEI's eight digits, Ontario with its version code, Quebec's twelve.
        assertEquals(HealthCardInput.Problem.NONE, HealthCardInput.check("1234 5678"));
        assertEquals(HealthCardInput.Problem.NONE, HealthCardInput.check("1234-567-890-AB"));
        assertEquals(HealthCardInput.Problem.NONE, HealthCardInput.check("DOEJ 9004 1234"));
    }

    @Test
    public void otherCharactersAreRefused() {
        assertEquals(HealthCardInput.Problem.CHARACTERS, HealthCardInput.check("1234/567/890"));
        assertEquals(HealthCardInput.Problem.CHARACTERS, HealthCardInput.check("1234567890."));
    }

    @Test
    public void implausibleLengthsAreRefused() {
        assertEquals(HealthCardInput.Problem.LENGTH, HealthCardInput.check("12-34"));
        assertEquals(HealthCardInput.Problem.LENGTH, HealthCardInput.check("1234 5678 9012 3"));
        assertEquals(HealthCardInput.Problem.LENGTH, HealthCardInput.check("- - -"));
    }

    @Test
    public void aTypedCardIsSentWithItsProvince() {
        JsonObject body = new JsonObject();

        HealthCardInput.addTo(body, "1234-567-890", "BC", false);

        assertEquals("1234567890", body.get(HealthCardInput.FIELD_NUMBER).getAsString());
        assertEquals("BC", body.get(HealthCardInput.FIELD_PROVINCE).getAsString());
    }

    @Test
    public void anEmptyFieldSendsNothingWhenThereWasNoCard() {
        JsonObject body = new JsonObject();

        HealthCardInput.addTo(body, "", "ON", false);

        // Left out, so the server keeps a card this phone may not know about.
        assertFalse(body.has(HealthCardInput.FIELD_NUMBER));
        assertFalse(body.has(HealthCardInput.FIELD_PROVINCE));
    }

    @Test
    public void clearingACardSendsABlankNumber() {
        JsonObject body = new JsonObject();

        HealthCardInput.addTo(body, "  ", "ON", true);

        assertTrue(body.has(HealthCardInput.FIELD_NUMBER));
        assertEquals("", body.get(HealthCardInput.FIELD_NUMBER).getAsString());
        assertFalse(body.has(HealthCardInput.FIELD_PROVINCE));
    }

    @Test
    public void aMissingProvinceIsLeftOutRatherThanSentAsNull() {
        JsonObject body = new JsonObject();

        HealthCardInput.addTo(body, "1234567890", null, true);

        assertFalse(body.has(HealthCardInput.FIELD_PROVINCE));
        assertFalse(body.toString().contains("null"));
    }
}
