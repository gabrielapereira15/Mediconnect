package com.vegs.mediconnect.mobile.patient;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The rules a provincial health card has to meet before it is stored.
 *
 * The number is what CA Baseline identifies a patient by, so a typo stored
 * here does more harm than no card at all: another system would match the
 * wrong person, or nobody. Checking the shape catches the slips a phone
 * keyboard makes without pretending to know each province's check digits.
 *
 * The app applies the same rules before it sends anything, so a patient
 * sees the problem next to the field rather than after a round trip.
 */
final class HealthCard {

    /** Every province and territory, by the two-letter code the provinces use. */
    static final Set<String> PROVINCES = Set.of(
            "AB", "BC", "MB", "NB", "NL", "NS", "NT", "NU", "ON", "PE", "QC", "SK", "YT");

    /**
     * Card numbers run from eight characters (PEI, the Northwest Territories)
     * to twelve (Quebec, and Ontario with its version code). Spaces and
     * dashes are how the numbers are printed, not part of them, so they do
     * not count.
     */
    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 12;

    private static final Pattern PRINTED = Pattern.compile("[A-Za-z0-9 \\-]+");
    private static final Pattern SEPARATORS = Pattern.compile("[ \\-]");

    private HealthCard() {
    }

    /**
     * The number as it is stored: capitals, without the spaces and dashes
     * it is printed with, so the last four read the same on every screen and
     * the FHIR identifier carries the number rather than its formatting.
     *
     * @return null when the number is blank, which removes the card
     * @throws InvalidHealthCardException when it is not a card number
     */
    static String number(String entered) {
        if (entered == null || entered.isBlank()) {
            return null;
        }
        String trimmed = entered.trim();
        if (!PRINTED.matcher(trimmed).matches()) {
            throw new InvalidHealthCardException(
                    "A health card number has only letters, numbers, spaces and dashes.");
        }
        String number = SEPARATORS.matcher(trimmed).replaceAll("").toUpperCase(Locale.ROOT);
        if (number.length() < MIN_LENGTH || number.length() > MAX_LENGTH) {
            throw new InvalidHealthCardException("A health card number has "
                    + MIN_LENGTH + " to " + MAX_LENGTH + " letters and numbers.");
        }
        return number;
    }

    /**
     * The province code in capitals.
     *
     * @return null when the province is blank
     * @throws InvalidHealthCardException when it is not a Canadian province
     *         or territory
     */
    static String province(String entered) {
        if (entered == null || entered.isBlank()) {
            return null;
        }
        String code = entered.trim().toUpperCase(Locale.ROOT);
        if (!PROVINCES.contains(code)) {
            throw new InvalidHealthCardException("The province must be one of AB, BC, MB, NB, "
                    + "NL, NS, NT, NU, ON, PE, QC, SK or YT.");
        }
        return code;
    }
}
