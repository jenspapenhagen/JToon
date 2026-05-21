package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DecodeOptions configuration record.
 */
@Tag("unit")
public class DecodeOptionsTest {

    private static final int CUSTOM_INDENT = 4;
    private static final int DEFAULT_MAX_DEPTH = 512;
    private static final int DEFAULT_MAX_ARRAY_STRING_SIZE = 10_000_000;

    @Nested
    @DisplayName("Default Options")
    class DefaultOptions {

        @Test
        @DisplayName("should have correct default values")
        void testDefaultValues() {
            // Given
            final DecodeOptions options = DecodeOptions.DEFAULT;

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertTrue(options.strict());
        }

        @Test
        @DisplayName("should create options with no-arg constructor")
        void testNoArgConstructor() {
            // Given
            final DecodeOptions options = new DecodeOptions();

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertTrue(options.strict());
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("withIndent should create options with custom indent")
        void testWithIndent() {
            // Given
            final DecodeOptions options = DecodeOptions.withIndent(4);

            // Then
            assertEquals(CUSTOM_INDENT, options.indent());
        }

        @Test
        @DisplayName("withDelimiter should create options with custom delimiter")
        void testWithDelimiter() {
            // Given
            final DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.PIPE);

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.PIPE, options.delimiter());
            assertTrue(options.strict());
        }

        @Test
        @DisplayName("withStrict should create options with custom strict mode")
        void testWithStrict() {
            // Given
            final DecodeOptions options = DecodeOptions.withStrict(false);

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertFalse(options.strict());
        }
    }

    @Nested
    @DisplayName("Security Limits")
    class SecurityLimits {

        @Test
        @DisplayName("should have default max depth of 512")
        void testDefaultMaxDepth() {
            // Given
            final DecodeOptions options = DecodeOptions.DEFAULT;

            // Then
            assertEquals(DEFAULT_MAX_DEPTH, options.maxDepth());
        }

        @Test
        @DisplayName("should have default max array size of 10,000,000")
        void testDefaultMaxArraySize() {
            // Given
            final DecodeOptions options = DecodeOptions.DEFAULT;

            // Then
            assertEquals(DEFAULT_MAX_ARRAY_STRING_SIZE, options.maxArraySize());
        }

        @Test
        @DisplayName("should have default max string length of 10,000,000")
        void testDefaultMaxStringLength() {
            // Given
            final DecodeOptions options = DecodeOptions.DEFAULT;

            // Then
            assertEquals(DEFAULT_MAX_ARRAY_STRING_SIZE, options.maxStringLength());
        }

        @Test
        @DisplayName("should reject maxDepth of 0")
        void testMaxDepthZero() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, 0,
                            DEFAULT_MAX_ARRAY_STRING_SIZE, DEFAULT_MAX_ARRAY_STRING_SIZE));
        }

        @Test
        @DisplayName("should reject negative maxDepth")
        void testMaxDepthNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, -1,
                            DEFAULT_MAX_ARRAY_STRING_SIZE, DEFAULT_MAX_ARRAY_STRING_SIZE));
        }

        @Test
        @DisplayName("should reject maxDepth exceeding MAX_ALLOWED_DEPTH")
        void testMaxDepthExceedsLimit() {
            final int exceededDepth = 513;
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, exceededDepth,
                            DEFAULT_MAX_ARRAY_STRING_SIZE, DEFAULT_MAX_ARRAY_STRING_SIZE));
        }

        @Test
        @DisplayName("should accept boundary maxDepth of 512")
        void testMaxDepthBoundary() {
            assertDoesNotThrow(() -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DEFAULT_MAX_DEPTH, DEFAULT_MAX_ARRAY_STRING_SIZE,
                    DEFAULT_MAX_ARRAY_STRING_SIZE));
        }

        @Test
        @DisplayName("should reject maxArraySize of 0")
        void testMaxArraySizeZero() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, DEFAULT_MAX_DEPTH,
                            0, DEFAULT_MAX_ARRAY_STRING_SIZE));
        }

        @Test
        @DisplayName("should reject negative maxArraySize")
        void testMaxArraySizeNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, DEFAULT_MAX_DEPTH,
                            -1, DEFAULT_MAX_ARRAY_STRING_SIZE));
        }

        @Test
        @DisplayName("should reject maxStringLength of 0")
        void testMaxStringLengthZero() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, DEFAULT_MAX_DEPTH,
                            DEFAULT_MAX_ARRAY_STRING_SIZE, 0));
        }

        @Test
        @DisplayName("should reject negative maxStringLength")
        void testMaxStringLengthNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, DEFAULT_MAX_DEPTH,
                            DEFAULT_MAX_ARRAY_STRING_SIZE, -1));
        }
    }

    @Nested
    @DisplayName("Custom Options")
    class CustomOptions {

        @Test
        @DisplayName("should create options with all custom values")
        void testAllCustomValues() {
            // Given
            final DecodeOptions options = new DecodeOptions(CUSTOM_INDENT, Delimiter.TAB, false,
                    PathExpansion.OFF, DecodeOptions.MAX_ALLOWED_DEPTH,
                    DecodeOptions.DEFAULT_MAX_ARRAY_SIZE, DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

            // Then
            assertEquals(CUSTOM_INDENT, options.indent());
            assertEquals(Delimiter.TAB, options.delimiter());
            assertFalse(options.strict());
        }

        @Test
        @DisplayName("should support all delimiter types")
        void testAllDelimiters() {
            // Then
            final DecodeOptions commaOpts = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            assertEquals(Delimiter.COMMA, commaOpts.delimiter());
            final DecodeOptions tabOpts = new DecodeOptions(2, Delimiter.TAB, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            assertEquals(Delimiter.TAB, tabOpts.delimiter());
            final DecodeOptions pipeOpts = new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            assertEquals(Delimiter.PIPE, pipeOpts.delimiter());
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehavior {

        @Test
        @DisplayName("should be equal when values are equal")
        void testEquality() {
            // Given
            final DecodeOptions options1 = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            final DecodeOptions options2 = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

            // Then
            assertEquals(options1, options2);
            assertEquals(options1.hashCode(), options2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when values differ")
        void testInequality() {
            // Given
            final DecodeOptions options1 = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            final DecodeOptions options2 = new DecodeOptions(4, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            final DecodeOptions options3 = new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            final DecodeOptions options4 = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

            // Then
            assertNotEquals(options1, options2);
            assertNotEquals(options1, options3);
            assertNotEquals(options1, options4);
        }

        @Test
        @DisplayName("should have meaningful toString")
        void testToString() {
            // Given
            final DecodeOptions options = new DecodeOptions(4, Delimiter.TAB, false, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

            // When
            final String str = options.toString();

            // Then
            assertTrue(str.contains("4"), "ToString should contain indent value: " + str);
            assertTrue(str.contains("TAB") || str.contains("delimiter="),
                    "ToString should contain delimiter: " + str);
            assertTrue(str.contains("false") || str.contains("strict="),
                    "ToString should contain strict value: " + str);
        }
    }
}
