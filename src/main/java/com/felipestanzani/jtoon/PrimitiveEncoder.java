package com.felipestanzani.jtoon;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.regex.Pattern;

import static com.felipestanzani.jtoon.Constants.*;

/**
 * Encodes primitive values, strings, object keys, and formats headers.
 */
public final class PrimitiveEncoder {
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:e[+-]?\\d+)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OCTAL_PATTERN = Pattern.compile("^0\\d+$");
    private static final Pattern UNQUOTED_KEY_PATTERN = Pattern.compile("^[A-Z_][\\w.]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRUCTURAL_CHARS = Pattern.compile("[\\[\\]{}]");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\n\\r\\t]");

    private PrimitiveEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a primitive JsonNode value.
     */
    public static String encodePrimitive(JsonNode value, String delimiter) {
        if (value.isNull()) {
            return NULL_LITERAL;
        }

        if (value.isBoolean()) {
            return String.valueOf(value.asBoolean());
        }

        if (value.isNumber()) {
            return value.asText();
        }

        if (value.isTextual()) {
            return encodeStringLiteral(value.asText(), delimiter);
        }

        return NULL_LITERAL;
    }

    /**
     * Encodes a string literal, quoting if necessary.
     */
    public static String encodeStringLiteral(String value, String delimiter) {
        if (isSafeUnquoted(value, delimiter)) {
            return value;
        }

        return DOUBLE_QUOTE + escapeString(value) + DOUBLE_QUOTE;
    }

    /**
     * Escapes special characters in a string.
     */
    public static String escapeString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Checks if a string can be safely written without quotes.
     */
    public static boolean isSafeUnquoted(String value, String delimiter) {
        boolean isSafe = value != null && !value.isEmpty();

        isSafe = isSafe && !isPaddedWithWhitespace(value);
        isSafe = isSafe && !(value.equals(TRUE_LITERAL) || value.equals(FALSE_LITERAL) || value.equals(NULL_LITERAL));
        isSafe = isSafe && !isNumericLike(value);

        // Check for colon (always structural)
        isSafe = isSafe && !value.contains(COLON);

        // Check for quotes and backslash (always need escaping)
        isSafe = isSafe && !(value.indexOf(DOUBLE_QUOTE) >= 0 || value.indexOf(BACKSLASH) >= 0);

        // Check for brackets and braces (always structural)
        isSafe = isSafe && !STRUCTURAL_CHARS.matcher(value).find();

        // Check for control characters (newline, carriage return, tab - always need
        // quoting/escaping)
        isSafe = isSafe && !CONTROL_CHARS.matcher(value).find();

        // Check for the active delimiter
        isSafe = isSafe && !value.contains(delimiter);

        // Check for hyphen at start (list marker)
        isSafe = isSafe && !value.startsWith(LIST_ITEM_MARKER);

        return isSafe;
    }

    private static boolean isNumericLike(String value) {
        // Match numbers like: 42, -3.14, 1e-6, 05, etc.
        return NUMERIC_PATTERN.matcher(value).matches() || OCTAL_PATTERN.matcher(value).matches();
    }

    private static boolean isPaddedWithWhitespace(String value) {
        return !value.equals(value.trim());
    }

    /**
     * Encodes an object key, quoting if necessary.
     */
    public static String encodeKey(String key) {
        if (isValidUnquotedKey(key)) {
            return key;
        }

        return DOUBLE_QUOTE + escapeString(key) + DOUBLE_QUOTE;
    }

    private static boolean isValidUnquotedKey(String key) {
        return UNQUOTED_KEY_PATTERN.matcher(key).matches();
    }

    /**
     * Joins encoded primitive values with the specified delimiter.
     */
    public static String joinEncodedValues(List<JsonNode> values, String delimiter) {
        return values.stream()
                .map(v -> encodePrimitive(v, delimiter))
                .reduce((a, b) -> a + delimiter + b)
                .orElse("");
    }

    /**
     * Formats a header for arrays and tables.
     * 
     * @param length       Array length
     * @param key          Optional key prefix
     * @param fields       Optional field names for tabular format
     * @param delimiter    The delimiter being used
     * @param lengthMarker Whether to include # marker before length
     * @return Formatted header string
     */
    public static String formatHeader(
            int length,
            String key,
            List<String> fields,
            String delimiter,
            boolean lengthMarker) {
        StringBuilder header = new StringBuilder();

        if (key != null) {
            header.append(encodeKey(key));
        }

        // Array length with optional marker
        header.append(OPEN_BRACKET);
        if (lengthMarker) {
            header.append("#");
        }
        header.append(length);

        // Only include delimiter if it's not the default (comma)
        if (!delimiter.equals(COMMA)) {
            header.append(delimiter);
        }
        header.append(CLOSE_BRACKET);

        // Field names for tabular format
        if (fields != null && !fields.isEmpty()) {
            header.append(OPEN_BRACE);
            String quotedFields = fields.stream()
                    .map(PrimitiveEncoder::encodeKey)
                    .reduce((a, b) -> a + delimiter + b)
                    .orElse("");
            header.append(quotedFields);
            header.append(CLOSE_BRACE);
        }

        header.append(COLON);

        return header.toString();
    }
}
