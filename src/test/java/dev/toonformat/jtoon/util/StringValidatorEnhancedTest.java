package dev.toonformat.jtoon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for enhanced StringValidator methods.
 */
@DisplayName("StringValidator - Enhanced Methods")
class StringValidatorEnhancedTest {

    @ParameterizedTest
    @ValueSource(strings = {"-", "hello-world", "foo-bar-baz"})
    @DisplayName("needsQuotes returns true for strings with hyphen")
    void needsQuotesReturnsTrueForHyphen(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "world", "foo_bar", "abc123"})
    @DisplayName("needsQuotes returns false for safe strings")
    void needsQuotesReturnsFalseForSafeStrings(String input) {
        assertFalse(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "   "})
    @DisplayName("needsQuotes returns true for empty or whitespace-only")
    void needsQuotesReturnsTrueForEmptyOrWhitespace(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {" hello", "hello ", " hello ", "\thello", "hello\t"})
    @DisplayName("needsQuotes returns true for strings with leading/trailing whitespace")
    void needsQuotesReturnsTrueForPaddedStrings(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello world", "foo bar", "a b c"})
    @DisplayName("needsQuotes returns false for internal spaces without leading/trailing")
    void needsQuotesReturnsFalseForInternalSpaces(String input) {
        assertFalse(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "null"})
    @DisplayName("needsQuotes returns true for keyword-like strings (lowercase)")
    void needsQuotesReturnsTrueForKeywordsLowercase(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRUE", "FALSE", "NULL", "True", "False", "Null"})
    @DisplayName("needsQuotes returns false for uppercase keywords (trim handles them differently)")
    void needsQuotesReturnsFalseForUppercaseKeywords(String input) {
        assertFalse(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "-42", "3.14", "1e10", "0.5", "-0"})
    @DisplayName("needsQuotes returns true for numeric strings")
    void needsQuotesReturnsTrueForNumericStrings(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a:b", "key:value", "foo:bar"})
    @DisplayName("needsQuotes returns true for strings with colon")
    void needsQuotesReturnsTrueForColon(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{key}", "[array]", "{foo}", "[bar]"})
    @DisplayName("needsQuotes returns true for strings with structural characters")
    void needsQuotesReturnsTrueForStructuralChars(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @Test
    @DisplayName("needsQuotes returns true for null input")
    void needsQuotesReturnsTrueForNull() {
        assertTrue(StringValidator.needsQuotes(null, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a|b", "foo|bar"})
    @DisplayName("needsQuotes returns true when delimiter is pipe and string contains pipe")
    void needsQuotesReturnsTrueForPipeDelimiter(String input) {
        assertTrue(StringValidator.needsQuotes(input, '|'));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b", "foo,bar"})
    @DisplayName("needsQuotes returns true when delimiter is comma and string contains comma")
    void needsQuotesReturnsTrueForCommaDelimiter(String input) {
        assertTrue(StringValidator.needsQuotes(input, ','));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b", "foo,bar"})
    @DisplayName("needsQuotes returns false when delimiter is pipe and string contains comma")
    void needsQuotesReturnsFalseWhenDifferentDelimiter(String input) {
        assertFalse(StringValidator.needsQuotes(input, '|'));
    }

    @Test
    @DisplayName("isNumericKey returns true for purely numeric strings")
    void isNumericKeyReturnsTrueForNumeric() {
        assertTrue(StringValidator.isNumericKey("123"));
        assertTrue(StringValidator.isNumericKey("0"));
        assertTrue(StringValidator.isNumericKey("999999"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12a", "a12", "1a2", "abc", "hello", "123.456"})
    @DisplayName("isNumericKey returns false for non-numeric strings")
    void isNumericKeyReturnsFalseForNonNumeric(String input) {
        assertFalse(StringValidator.isNumericKey(input));
    }

    @Test
    @DisplayName("isNumericKey returns false for null")
    void isNumericKeyReturnsFalseForNull() {
        assertFalse(StringValidator.isNumericKey(null));
    }

    @Test
    @DisplayName("containsHyphen returns true for strings with hyphen")
    void containsHyphenReturnsTrue() {
        assertTrue(StringValidator.containsHyphen("-"));
        assertTrue(StringValidator.containsHyphen("hello-world"));
        assertTrue(StringValidator.containsHyphen("-prefix"));
        assertTrue(StringValidator.containsHyphen("suffix-"));
    }

    @Test
    @DisplayName("containsHyphen returns false for strings without hyphen")
    void containsHyphenReturnsFalse() {
        assertFalse(StringValidator.containsHyphen("hello"));
        assertFalse(StringValidator.containsHyphen("world"));
        assertFalse(StringValidator.containsHyphen(""));
    }

    @Test
    @DisplayName("containsHyphen returns false for null")
    void containsHyphenReturnsFalseForNull() {
        assertFalse(StringValidator.containsHyphen(null));
    }
}
