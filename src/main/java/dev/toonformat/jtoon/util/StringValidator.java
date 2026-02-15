package dev.toonformat.jtoon.util;

import java.util.regex.Pattern;
import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Constants.FALSE_LITERAL;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.NULL_LITERAL;
import static dev.toonformat.jtoon.util.Constants.TRUE_LITERAL;

/**
 * Validates strings for safe unquoted usage in TOON format.
 * Follows Object Calisthenics principles with guard clauses and single-level
 * indentation.
 */
public final class StringValidator {
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:e[+-]?\\d+)?$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern OCTAL_PATTERN = Pattern.compile("^0[0-7]+$");
    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("^0\\d+$");
    private static final Pattern UNQUOTED_KEY_PATTERN = Pattern.compile("^[A-Z_][\\w.]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRUCTURAL_CHARS = Pattern.compile("[\\[\\]{}]");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\n\\r\\t]");

    private StringValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks if a string can be safely written without quotes.
     * Uses guard clauses and early returns for clarity.
     *
     * @param value     the string value to check
     * @param delimiter the delimiter being used (for validation)
     * @return true if the string can be safely written without quotes, false otherwise
     */
    public static boolean isSafeUnquoted(final String value, final String delimiter) {
        if (isNullOrEmpty(value)) {
            return false;
        }

        if (isPaddedWithWhitespace(value)) {
            return false;
        }

        if (looksLikeKeyword(value)) {
            return false;
        }

        if (looksLikeNumber(value)) {
            return false;
        }

        if (containsColon(value)) {
            return false;
        }

        if (containsQuotesOrBackslash(value)) {
            return false;
        }

        if (containsStructuralCharacters(value)) {
            return false;
        }

        if (containsControlCharacters(value)) {
            return false;
        }

        return !containsDelimiter(value, delimiter) && !startsWithListMarker(value);
    }

    /**
     * Checks if a key can be used without quotes.
     *
     * @param key the key to validate
     * @return true if the key can be used without quotes, false otherwise
     */
    public static boolean isValidUnquotedKey(final String key) {
        return UNQUOTED_KEY_PATTERN.matcher(key).matches();
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    private static boolean isPaddedWithWhitespace(final String value) {
        return !value.equals(value.trim());
    }

    private static boolean looksLikeKeyword(final String value) {
        return TRUE_LITERAL.equals(value)
            || FALSE_LITERAL.equals(value)
            || NULL_LITERAL.equals(value);
    }

    private static boolean looksLikeNumber(final String value) {
        return OCTAL_PATTERN.matcher(value).matches()
            || LEADING_ZERO_PATTERN.matcher(value).matches()
            || NUMERIC_PATTERN.matcher(value).matches();
    }

    private static boolean containsColon(final String value) {
        return value.contains(COLON);
    }

    static boolean containsQuotesOrBackslash(final String value) {
        return value.indexOf(DOUBLE_QUOTE) >= 0
            || value.indexOf(BACKSLASH) >= 0;
    }

    private static boolean containsStructuralCharacters(final String value) {
        return STRUCTURAL_CHARS.matcher(value).find();
    }

    private static boolean containsControlCharacters(final String value) {
        return CONTROL_CHARS.matcher(value).find();
    }

    private static boolean containsDelimiter(final String value, final String delimiter) {
        return value.contains(delimiter);
    }

    private static boolean startsWithListMarker(final String value) {
        return value.startsWith(LIST_ITEM_MARKER);
    }

    /**
     * Checks if a value needs quotes based on delimiter-aware validation.
     * More comprehensive than isSafeUnquoted, handles additional edge cases.
     *
     * @param value the string value to check
     * @param delimiterChar the delimiter character being used
     * @return true if the value needs quotes, false otherwise
     */
    public static boolean needsQuotes(final String value, final char delimiterChar) {
        if (value == null) {
            return true;
        }

        if (value.isEmpty()) {
            return true;
        }

        // Check for leading/trailing whitespace
        if (value.charAt(0) <= ' ' || value.charAt(value.length() - 1) <= ' ') {
            return true;
        }

        // Check for special keyword values
        final String trimmed = value.trim();
        if (trimmed.equals(TRUE_LITERAL) || trimmed.equals(FALSE_LITERAL)
            || trimmed.equals(NULL_LITERAL) || looksLikeNumber(trimmed)) {
            return true;
        }

        // Check for structural characters and delimiter
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);

            // Control characters and structural chars
            if (c < ' ' || c == ':' || c == '#' || c == '{' || c == '}'
                || c == '[' || c == ']' || c == '"' || c == '\'' || c == '-') {
                return true;
            }

            // Current delimiter
            if (c == delimiterChar) {
                return true;
            }

            // Comma when comma is the delimiter
            if (delimiterChar == ',' && c == ',') {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a key consists only of numeric characters.
     * Numeric keys must be quoted to avoid ambiguity.
     *
     * @param key the key to check
     * @return true if the key is purely numeric
     */
    public static boolean isNumericKey(final String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        for (int i = 0; i < key.length(); i++) {
            if (!Character.isDigit(key.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a string contains a hyphen character.
     * Hyphens often need special handling (e.g., "-" alone must be quoted).
     *
     * @param value the string to check
     * @return true if the string contains a hyphen
     */
    public static boolean containsHyphen(final String value) {
        if (value == null) {
            return false;
        }

        return value.indexOf('-') >= 0;
    }
}
