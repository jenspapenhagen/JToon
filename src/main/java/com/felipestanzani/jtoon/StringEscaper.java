package com.felipestanzani.jtoon;

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
}
