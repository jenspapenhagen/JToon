package dev.toonformat.jtoon;

import java.util.Objects;

public record DecodeOptions(
        int indent,
        Delimiter delimiter,
        boolean strict,
        PathExpansion expandPaths) {
    public static final DecodeOptions DEFAULT = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

    public static final int MAX_INDENT = 100;

    public DecodeOptions() {
        this(2, Delimiter.COMMA, true, PathExpansion.OFF);
    }

    public DecodeOptions {
        if (indent < 0) {
            throw new IllegalArgumentException("indent must be non-negative, got: " + indent);
        }
        if (indent > MAX_INDENT) {
            throw new IllegalArgumentException("indent must be <= " + MAX_INDENT + ", got: " + indent);
        }
        delimiter = Objects.requireNonNull(delimiter, "delimiter cannot be null");
    }

    public static DecodeOptions withIndent(final int indent) {
        return new DecodeOptions(indent, Delimiter.COMMA, true, PathExpansion.OFF);
    }

    public static DecodeOptions withDelimiter(final Delimiter delimiter) {
        return new DecodeOptions(2, delimiter, true, PathExpansion.OFF);
    }

    public static DecodeOptions withStrict(final boolean strict) {
        return new DecodeOptions(2, Delimiter.COMMA, strict, PathExpansion.OFF);
    }
}
