package dev.toonformat.jtoon.util;

/**
 * Handles indentation formatting for TOON output.
 * Provides consistent indentation across all encoders.
 */
public final class IndentFormatter {

    /**
     * Cache of common indent strings to avoid repeated string creation.
     */
    private static final int CACHE_SIZE = 20;
    private static final String[] INDENT_CACHE = new String[CACHE_SIZE];

    static {
        // Pre-compute common indent sizes
        for (int i = 0; i < CACHE_SIZE; i++) {
            INDENT_CACHE[i] = " ".repeat(i);
        }
    }

    private IndentFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the indentation string for the given depth and indent size.
     *
     * @param depth the nesting depth (0 = root)
     * @param indentSize number of spaces per indentation level
     * @return the indentation string
     */
    public static String getIndent(final int depth, final int indentSize) {
        if (depth <= 0) {
            return "";
        }

        final int totalSpaces = depth * indentSize;

        // Use cache for small indent sizes
        if (totalSpaces < INDENT_CACHE.length) {
            return INDENT_CACHE[totalSpaces];
        }

        return " ".repeat(totalSpaces);
    }

    /**
     * Gets the indentation string for a given total number of spaces.
     * Useful when you already know the exact space count needed.
     *
     * @param spaces the number of spaces needed
     * @return the indentation string
     */
    public static String getSpaces(final int spaces) {
        if (spaces <= 0) {
            return "";
        }

        if (spaces < INDENT_CACHE.length) {
            return INDENT_CACHE[spaces];
        }

        return " ".repeat(spaces);
    }
}
