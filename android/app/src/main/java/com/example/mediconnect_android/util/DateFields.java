package com.example.mediconnect_android.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Optional;

/**
 * Date-of-birth entry.
 *
 * Someone entering their own birthday knows it, and typing eight digits is
 * quicker than scrolling a picker back forty years — so the field takes the
 * numeric keypad and inserts the dashes as you go. The calendar icon stays
 * for anyone who would rather pick, and the two write the same format.
 *
 * Typed input is checked as soon as the field is left, not only on submit,
 * so an impossible date is caught next to the field that caused it.
 */
public final class DateFields {

    public static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Nobody booking an appointment was born before this. */
    private static final int EARLIEST_YEAR = 1900;

    private DateFields() {
    }

    /**
     * Wires up a date-of-birth field: numeric entry with auto-inserted
     * dashes, a picker on the layout's end icon, and validation on blur.
     */
    public static void asDateOfBirth(Context context, TextInputLayout layout, EditText field) {
        field.addTextChangedListener(new DashInserter(field));

        layout.setEndIconOnClickListener(v -> showPicker(context, layout, field));

        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                return;
            }
            // Empty is the starting state, not a mistake to point at; the
            // submit check is what insists on a value.
            String text = field.getText() == null ? "" : field.getText().toString();
            layout.setError(text.trim().isEmpty() ? null : validateDateOfBirth(text));
        });
    }

    private static void showPicker(Context context, TextInputLayout layout, EditText field) {
        Calendar start = Calendar.getInstance();
        parse(field.getText().toString()).ifPresent(date -> start.set(
                date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()));

        DatePickerDialog dialog = new DatePickerDialog(
                context,
                (view, year, month, day) -> {
                    field.setText(LocalDate.of(year, month + 1, day).format(ISO));
                    layout.setError(null);
                },
                start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));

        // A date of birth cannot be in the future.
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    /** Parses yyyy-MM-dd, returning empty rather than throwing. */
    public static Optional<LocalDate> parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim(), ISO));
        } catch (DateTimeParseException e) {
            return Optional.empty();
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
        Optional<LocalDate> parsed = parse(value);
        if (parsed.isEmpty()) {
            // Covers both a half-typed date and a real-looking but impossible
            // one such as 1990-02-31, which LocalDate refuses outright.
            return "Enter a real date as YYYY-MM-DD, for example 1990-04-27.";
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

    /**
     * Keeps the dashes in yyyy-MM-dd so the keypad only has to produce
     * digits. It rewrites the whole field on each change, which would
     * re-enter itself, so it ignores its own edits.
     */
    private static final class DashInserter implements TextWatcher {

        private final EditText field;
        private boolean editing;

        private DashInserter(EditText field) {
            this.field = field;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (editing) {
                return;
            }
            String digits = s.toString().replaceAll("[^0-9]", "");
            if (digits.length() > 8) {
                digits = digits.substring(0, 8);
            }

            StringBuilder formatted = new StringBuilder(digits);
            if (digits.length() > 6) {
                formatted.insert(6, '-');
            }
            if (digits.length() > 4) {
                formatted.insert(4, '-');
            }

            String result = formatted.toString();
            if (result.equals(s.toString())) {
                return;
            }

            editing = true;
            field.setText(result);
            field.setSelection(result.length());
            editing = false;
        }
    }
}
