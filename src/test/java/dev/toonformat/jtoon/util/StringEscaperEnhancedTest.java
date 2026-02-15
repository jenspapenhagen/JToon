package dev.toonformat.jtoon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for enhanced StringEscaper methods.
 */
@DisplayName("StringEscaper - Enhanced Methods")
class StringEscaperEnhancedTest {

    @Test
    @DisplayName("escapeAndQuote wraps string in quotes")
    void escapeAndQuoteWrapsInQuotes() {
        String result = StringEscaper.escapeAndQuote("hello");
        assertEquals("\"hello\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes backslash")
    void escapeAndQuoteEscapesBackslash() {
        String result = StringEscaper.escapeAndQuote("hello\\world");
        assertEquals("\"hello\\\\world\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes double quote")
    void escapeAndQuoteEscapesDoubleQuote() {
        String result = StringEscaper.escapeAndQuote("hello\"world");
        assertEquals("\"hello\\\"world\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes newline")
    void escapeAndQuoteEscapesNewline() {
        String result = StringEscaper.escapeAndQuote("hello\nworld");
        assertEquals("\"hello\\nworld\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes carriage return")
    void escapeAndQuoteEscapesCarriageReturn() {
        String result = StringEscaper.escapeAndQuote("hello\rworld");
        assertEquals("\"hello\\rworld\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes tab")
    void escapeAndQuoteEscapesTab() {
        String result = StringEscaper.escapeAndQuote("hello\tworld");
        assertEquals("\"hello\\tworld\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes backspace")
    void escapeAndQuoteEscapesBackspace() {
        String result = StringEscaper.escapeAndQuote("hello\bworld");
        assertEquals("\"hello\\bworld\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes form feed")
    void escapeAndQuoteEscapesFormFeed() {
        String result = StringEscaper.escapeAndQuote("hello\fworld");
        assertEquals("\"hello\\fworld\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote handles multiple escape characters")
    void escapeAndQuoteHandlesMultipleEscapes() {
        String input = "line1\nline2\rline3\ttab";
        String result = StringEscaper.escapeAndQuote(input);
        assertEquals("\"line1\\nline2\\rline3\\ttab\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote returns quoted null for null input")
    void escapeAndQuoteHandlesNull() {
        String result = StringEscaper.escapeAndQuote(null);
        assertEquals("\"null\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote handles empty string")
    void escapeAndQuoteHandlesEmptyString() {
        String result = StringEscaper.escapeAndQuote("");
        assertEquals("\"\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote handles string with only escapes")
    void escapeAndQuoteHandlesOnlyEscapes() {
        String result = StringEscaper.escapeAndQuote("\\n\\r\\t");
        assertEquals("\"\\\\n\\\\r\\\\t\"", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "world", "foo_bar", "123", "a b c"})
    @DisplayName("escapeAndQuote preserves safe strings (just adds quotes)")
    void escapeAndQuotePreservesSafeStrings(String input) {
        String result = StringEscaper.escapeAndQuote(input);
        assertEquals("\"" + input + "\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes backslash at end of string")
    void escapeAndQuoteEscapesTrailingBackslash() {
        String result = StringEscaper.escapeAndQuote("path\\");
        assertEquals("\"path\\\\\"", result);
    }

    @Test
    @DisplayName("escapeAndQuote escapes all standard characters")
    void escapeAndQuoteEscapesAllStandardCharacters() {
        String input = "\" \\ \n \r \t \b \f";
        String result = StringEscaper.escapeAndQuote(input);
        assertEquals("\"\\\" \\\\ \\n \\r \\t \\b \\f\"", result);
    }
}
