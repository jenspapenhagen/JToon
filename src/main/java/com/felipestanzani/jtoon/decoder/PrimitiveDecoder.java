package com.felipestanzani.jtoon.decoder;

import com.felipestanzani.jtoon.util.StringEscaper;

/**
 * Handles parsing of primitive TOON values with type inference.
 *
 * <p>
 * Converts TOON scalar representations to appropriate Java types:
 * </p>
 * <ul>
 *   <li>{@code "null"} → {@code null}</li>
 *   <li>{@code "true"} / {@code "false"} → {@code Boolean}</li>
 *   <li>Numeric strings → {@code Long} or {@code Double}</li>
 *   <li>Quoted strings → {@code String} (with unescaping)</li>
 *   <li>Bare strings → {@code String}</li>
 * </ul>
 *
 * <h2>Examples:</h2>
 * <pre>{@code
 * parse("null")      → null
 * parse("true")      → true
 * parse("42")        → 42L
 * parse("3.14")      → 3.14
 * parse("\"hello\"") → "hello"
 * parse("hello")     → "hello"
 * parse("")          → "" (empty string)
 * }</pre>
 */
final class PrimitiveDecoder {

    private PrimitiveDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses a TOON primitive value and infers its type.
     *
     * @param value The string representation of the value
     * @return The parsed value as {@code Boolean}, {@code Long}, {@code Double},
     *         {@code String}, or {@code null}
     */
    static Object parse(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // Check for null literal
        if ("null".equals(value)) {
            return null;
        }

        // Check for boolean literals
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }

        // Check for quoted strings
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return StringEscaper.unescape(value);
        }

        // Try parsing as number
        try {
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
