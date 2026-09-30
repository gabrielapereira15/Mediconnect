package com.example.mediconnect_android.util;

import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The health card as Edit profile reads it and sends it.
 *
 * The rules match the server's (backend HealthCard) on purpose: a number the
 * app accepts and the server then refuses would leave the patient looking
 * at an error they cannot act on from the field it is about.
 *
 * Plain Java, so the rules are unit-tested without a device.
 */
public final class HealthCardInput {

    /**
     * Card numbers run from eight characters (PEI, the Northwest Territories)
     * to twelve (Quebec, and Ontario with its version code). Spaces and
     * dashes are how the numbers are printed, not part of them.
     */
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 12;

    /** Where the clinic is, so the province most patients will pick. */
    public static final String DEFAULT_PROVINCE = "ON";

    /** Preference keys, shared with sign-in, Profile and Visit day. */
    public static final String PREF_NUMBER = "health_card_number";
    public static final String PREF_PROVINCE = "health_card_province";

    /** Body keys the server reads the card from. */
    static final String FIELD_NUMBER = "healthCardNumber";
    static final String FIELD_PROVINCE = "healthCardProvince";

    private static final Pattern PRINTED = Pattern.compile("[A-Za-z0-9 \\-]+");
    private static final Pattern SEPARATORS = Pattern.compile("[ \\-]");

    /** What is wrong with a number as typed, if anything. */
    public enum Problem {
        NONE,
        CHARACTERS,
        LENGTH
    }

    private HealthCardInput() {
    }

    /**
     * The number as the clinic stores it: capitals, without spaces or
     * dashes. Empty when nothing was typed.
     */
    public static String clean(String entered) {
        if (entered == null) {
            return "";
        }
        return SEPARATORS.matcher(entered.trim()).replaceAll("").toUpperCase(Locale.ROOT);
    }

    /** The card is optional, so an empty field is never a problem. */
    public static Problem check(String entered) {
        if (entered == null || entered.trim().isEmpty()) {
            return Problem.NONE;
        }
        if (!PRINTED.matcher(entered.trim()).matches()) {
            return Problem.CHARACTERS;
        }
        int length = clean(entered).length();
        return length < MIN_LENGTH || length > MAX_LENGTH ? Problem.LENGTH : Problem.NONE;
    }

    /**
     * Adds the card to a profile save, or leaves it out.
     *
     * Left out, the server keeps the card it has. So an empty field sends
     * nothing unless the patient emptied it themselves: this phone may
     * simply not know about a card the clinic holds, and an empty field is
     * not a request to remove it. A card the patient cleared is sent as a
     * blank number, which the server takes as "remove it".
     *
     * @param entered  the number as typed
     * @param province the province code chosen for it
     * @param hadCard  whether the form opened with a card filled in
     */
    public static void addTo(JsonObject body, String entered, String province, boolean hadCard) {
        String number = clean(entered);
        if (!number.isEmpty()) {
            body.addProperty(FIELD_NUMBER, number);
            // Gson writes a null property as null, which the server reads
            // as "not sent"; leaving it out says the same thing honestly.
            if (province != null && !province.trim().isEmpty()) {
                body.addProperty(FIELD_PROVINCE, province.trim());
            }
        } else if (hadCard) {
            body.addProperty(FIELD_NUMBER, "");
        }
    }
}
