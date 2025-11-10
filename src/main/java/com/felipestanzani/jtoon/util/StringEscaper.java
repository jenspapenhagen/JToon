package com.felipestanzani.jtoon.util;

/**
 * Handles string escaping for TOON format.
 * Escapes special characters that need protection in quoted strings.
 */
public final class StringEscaper {

    private StringEscaper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Escapes special characters in a string.
     * Handles backslashes, quotes, and control characters.
     *
     * @param value The string to escape
     * @return The escaped string
     */
    public static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Unescapes a string and removes surrounding quotes if present.
     * Reverses the escaping applied by {@link #escape(String)}.
     *
     * @param value The string to unescape (may be quoted)
     * @return The unescaped string with quotes removed
     */
    public static String unescape(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        String unquoted = value;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            unquoted = value.substring(1, value.length() - 1);
        }

        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (char c : unquoted.toCharArray()) {
            if (escaped) {
                result.append(unescapeChar(c));
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Converts an escaped character to its unescaped form.
     *
     * @param c The character following a backslash
     * @return The unescaped character
     */
    private static char unescapeChar(char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> c;
        };
    }
}
