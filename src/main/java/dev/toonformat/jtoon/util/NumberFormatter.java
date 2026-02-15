package dev.toonformat.jtoon.util;

import java.math.BigDecimal;

/**
 * Formats numbers for TOON encoding.
 * Handles conversion from scientific notation to plain decimal and
 * strips trailing zeros.
 */
public final class NumberFormatter {

    private NumberFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts a number string to plain decimal format.
     * Handles scientific notation (e.g., "1e-7" -> "0.0000001").
     *
     * @param value the number string to format
     * @return the formatted number string without scientific notation
     */
    public static String toPlainDecimal(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Check if it's scientific notation
        if (value.contains("e") || value.contains("E")) {
            try {
                final double d = Double.parseDouble(value);
                return formatDecimal(d);
            } catch (NumberFormatException e) {
                return value;
            }
        }

        return value;
    }

    /**
     * Formats a double as plain decimal without scientific notation.
     *
     * @param value the double value to format
     * @return the formatted decimal string
     */
    public static String formatDecimal(final double value) {
        if (value == 0.0) {
            return "0";
        }

        // Use BigDecimal to avoid scientific notation
        final BigDecimal bd = BigDecimal.valueOf(value);
        return bd.stripTrailingZeros().toPlainString();
    }

    /**
     * Strips trailing zeros from decimal numbers while preserving necessary decimals.
     * Examples: "1.500" -> "1.5", "1.0" -> "1", "0.000001" -> "0.000001"
     *
     * @param value the decimal string to process
     * @return the string with trailing zeros removed
     */
    public static String stripTrailingZeros(final String value) {
        final int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return value;
        }

        int lastNonZero = value.length() - 1;
        while (lastNonZero > dotIndex && value.charAt(lastNonZero) == '0') {
            lastNonZero--;
        }

        if (lastNonZero == dotIndex) {
            return value.substring(0, dotIndex);
        }

        return value.substring(0, lastNonZero + 1);
    }

    /**
     * Checks if a string represents a valid number.
     * Handles integers, decimals, and scientific notation.
     *
     * @param value the string to check
     * @return true if the string is a valid number representation
     */
    public static boolean isNumber(final String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        final String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() != value.length()) {
            return false;
        }

        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if a string represents an integer number.
     *
     * @param value the string to check
     * @return true if the string is an integer
     */
    public static boolean isInteger(final String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        final String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() != value.length()) {
            return false;
        }

        try {
            Long.parseLong(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
