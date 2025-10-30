package com.felipestanzani.jtoon;

import com.felipestanzani.jtoon.util.StringEscaper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StringEscaper utility class.
 * Tests string escaping logic for special characters in TOON format.
 */
@Tag("unit")
public class StringEscaperTest {

    @Nested
    @DisplayName("Basic Escaping")
    class BasicEscaping {

        static Stream<Arguments> basicEscapingCases() {
            return Stream.of(
                    Arguments.of("backslashes", "path\\to\\file", "path\\\\to\\\\file"),
                    Arguments.of("double quotes", "He said \"hello\"", "He said \\\"hello\\\""),
                    Arguments.of("newlines", "line1\nline2", "line1\\nline2"),
                    Arguments.of("carriage returns", "line1\rline2", "line1\\rline2"),
                    Arguments.of("tabs", "col1\tcol2", "col1\\tcol2"));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("basicEscapingCases")
        @DisplayName("should escape basic special characters")
        void testBasicEscaping(String description, String input, String expected) {
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Combined Escaping")
    class CombinedEscaping {

        static Stream<Arguments> combinedEscapingCases() {
            return Stream.of(
                    Arguments.of("multiple special characters", "He said \"test\\path\"\nNext line",
                            "He said \\\"test\\\\path\\\"\\nNext line"),
                    Arguments.of("all control characters together", "text\n\r\t", "text\\n\\r\\t"),
                    Arguments.of("backslash before quote", "\\\"", "\\\\\\\""));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("combinedEscapingCases")
        @DisplayName("should escape combined special characters")
        void testCombinedEscaping(String description, String input, String expected) {
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty string for empty input")
        void testEmptyString() {
            assertEquals("", StringEscaper.escape(""));
        }

        @ParameterizedTest
        @DisplayName("should not modify strings without special characters")
        @ValueSource(strings = {
                "hello world",
                "Hello World 123 @#$%^&*()_+-=[]{}|;:',.<>?/",
                "Hello 世界 🌍"
        })
        void testStringsWithoutSpecialCharacters(String input) {
            assertEquals(input, StringEscaper.escape(input));
        }

        @Test
        @DisplayName("should handle consecutive backslashes")
        void testConsecutiveBackslashes() {
            String input = "\\\\\\";
            String expected = "\\\\\\\\\\\\";
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarios {

        static Stream<Arguments> realWorldScenarios() {
            return Stream.of(
                    Arguments.of("JSON string", "{\"key\": \"value\"}", "{\\\"key\\\": \\\"value\\\"}"),
                    Arguments.of("Windows file path", "C:\\Users\\Documents\\file.txt",
                            "C:\\\\Users\\\\Documents\\\\file.txt"),
                    Arguments.of("multi-line text", "Line 1\nLine 2\nLine 3", "Line 1\\nLine 2\\nLine 3"),
                    Arguments.of("SQL query", "SELECT * FROM users WHERE name = \"John\"",
                            "SELECT * FROM users WHERE name = \\\"John\\\""),
                    Arguments.of("regex pattern", "\\d+\\.\\d+", "\\\\d+\\\\.\\\\d+"));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("realWorldScenarios")
        @DisplayName("should escape real-world scenarios")
        void testRealWorldScenarios(String scenario, String input, String expected) {
            assertEquals(expected, StringEscaper.escape(input));
        }
    }
}
