package dev.toonformat.jtoon;

import dev.toonformat.jtoon.normalizer.JsonNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityValidationTest {

    @Nested
    @DisplayName("EncodeOptions validation")
    class EncodeOptionsValidation {
        @Test
        @DisplayName("should reject negative indent")
        void testNegativeIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(-1, Delimiter.COMMA, false, KeyFolding.OFF, 10));
        }

        @Test
        @DisplayName("should reject indent exceeding MAX_INDENT")
        void testExcessiveIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(EncodeOptions.MAX_ALLOWED_INDENT + 1, Delimiter.COMMA, false, KeyFolding.OFF, 10));
        }

        @Test
        @DisplayName("should reject null delimiter")
        void testNullDelimiter() {
            assertThrows(NullPointerException.class,
                () -> new EncodeOptions(2, null, false, KeyFolding.OFF, 10));
        }

        @Test
        @DisplayName("should reject negative flattenDepth")
        void testNegativeFlattenDepth() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.SAFE, -1));
        }

        @Test
        @DisplayName("should accept valid options")
        void testValidOptions() {
            EncodeOptions opts = new EncodeOptions(4, Delimiter.PIPE, true, KeyFolding.SAFE, 5);
            assertEquals(4, opts.indent());
            assertEquals(Delimiter.PIPE, opts.delimiter());
            assertEquals(5, opts.flattenDepth());
        }
    }

    @Nested
    @DisplayName("DecodeOptions validation")
    class DecodeOptionsValidation {
        @Test
        @DisplayName("should reject negative indent")
        void testNegativeIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new DecodeOptions(-1, Delimiter.COMMA, true, PathExpansion.OFF));
        }

        @Test
        @DisplayName("should reject indent exceeding MAX_INDENT")
        void testExcessiveIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new DecodeOptions(DecodeOptions.MAX_ALLOWED_INDENT + 1, Delimiter.COMMA, true, PathExpansion.OFF));
        }

        @Test
        @DisplayName("should reject null delimiter")
        void testNullDelimiter() {
            assertThrows(NullPointerException.class,
                () -> new DecodeOptions(2, null, true, PathExpansion.OFF));
        }
    }

    @Nested
    @DisplayName("JsonNormalizer depth limits")
    class JsonNormalizerDepthLimits {
        @Test
        @DisplayName("should have MAX_DEPTH constant")
        void testMaxDepthConstant() {
            assertEquals(512, JsonNormalizer.MAX_ALLOWED_NESTING_DEPTH);
        }
    }
}
