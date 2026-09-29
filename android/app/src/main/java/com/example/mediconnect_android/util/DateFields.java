package com.example.mediconnect_android.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;

/**
 * Date-of-birth entry.
 *
 * Typing a date into a free text field is easy to get wrong — wrong order,
 * wrong separator, a month that does not exist, a birthday in the future.
 * Tapping the field opens a picker instead, which can only produce a valid
 * date, and anything typed is still validated before it is accepted.
 */
public final class DateFields {

    public static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Nobody booking an appointment was born before this. */
    private static final int EARLIEST_YEAR = 1900;

    private DateFields() {
    }

    /** Makes a field open a date picker instead of the keyboard. */
    public static void asDateOfBirth(Context context, EditText field) {
        field.setFocusable(false);
        field.setClickable(true);
        field.setOnClickListener(v -> showPicker(context, field));
    }

    private static void showPicker(Context context, EditText field) {
        Calendar start = Calendar.getInstance();
        parse(field.getText().toString()).ifPresent(date -> start.set(
                date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()));

        DatePickerDialog dialog = new DatePickerDialog(
                context,
                (view, year, month, day) ->
                        field.setText(LocalDate.of(year, month + 1, day).format(ISO)),
                start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));

        // A date of birth cannot be in the future.
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    /** Parses yyyy-MM-dd, returning empty rather than throwing. */
    public static java.util.Optional<LocalDate> parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(LocalDate.parse(value.trim(), ISO));
        } catch (DateTimeParseException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Returns null when the date is usable, or a message explaining why not,
     * so a caller can show it on the field itself.
     */
    public static String validateDateOfBirth(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Please enter a date of birth.";
        }
        java.util.Optional<LocalDate> parsed = parse(value);
        if (parsed.isEmpty()) {
            return "Use the format YYYY-MM-DD, for example 1990-04-27.";
        }
        LocalDate date = parsed.get();
        if (date.isAfter(LocalDate.now())) {
            return "A date of birth cannot be in the future.";
        }
        if (date.getYear() < EARLIEST_YEAR) {
            return "Please check the year.";
        }
        return null;
    }
}
