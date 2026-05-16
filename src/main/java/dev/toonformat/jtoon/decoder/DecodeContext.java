package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;

public class DecodeContext {

    protected String[] lines;
    protected DecodeOptions options;
    protected Delimiter delimiter;
    protected int currentLine;
    protected int depth;

    public static final int MAX_DECODE_DEPTH = 1024;

    public DecodeContext() {
        this.depth = 0;
    }

    public void incrementDepth() {
        this.depth++;
        if (this.depth > MAX_DECODE_DEPTH) {
            throw new IllegalArgumentException("Maximum nesting depth exceeded: " + MAX_DECODE_DEPTH);
        }
    }

    public void decrementDepth() {
        if (this.depth > 0) {
            this.depth--;
        }
    }
}
