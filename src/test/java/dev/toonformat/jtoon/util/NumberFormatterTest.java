package dev.toonformat.jtoon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NumberFormatter}.
 */
@DisplayName("NumberFormatter")
class NumberFormatterTest {

    @Test
    @DisplayName("toPlainDecimal converts scientific notation to plain decimal")
    void toPlainDecimalConvertsScientificNotation() {
        assertEquals("0.0000001", NumberFormatter.toPlainDecimal("1e-7"));
        assertEquals("0.000001", NumberFormatter.toPlainDecimal("1e-6"));
        assertEquals("1000000", NumberFormatter.toPlainDecimal("1e6"));
        assertEquals("1000000", NumberFormatter.toPlainDecimal("1E6"));
        assertEquals("1230000000", NumberFormatter.toPlainDecimal("1.23e9"));
    }

    @Test
    @DisplayName("toPlainDecimal returns plain numbers unchanged")
    void toPlainDecimalReturnsPlainNumbersUnchanged() {
        assertEquals("123", NumberFormatter.toPlainDecimal("123"));
        assertEquals("3.14159", NumberFormatter.toPlainDecimal("3.14159"));
        assertEquals("0.5", NumberFormatter.toPlainDecimal("0.5"));
        assertEquals("-42", NumberFormatter.toPlainDecimal("-42"));
    }

    @Test
    @DisplayName("toPlainDecimal handles null and empty")
    void toPlainDecimalHandlesNullAndEmpty() {
        assertNull(NumberFormatter.toPlainDecimal(null));
        assertEquals("", NumberFormatter.toPlainDecimal(""));
    }

    @Test
    @DisplayName("formatDecimal converts double to plain decimal")
    void formatDecimalConvertsDouble() {
        assertEquals("0", NumberFormatter.formatDecimal(0.0));
        assertEquals("1", NumberFormatter.formatDecimal(1.0));
        assertEquals("0.1", NumberFormatter.formatDecimal(0.1));
        assertEquals("0.0000001", NumberFormatter.formatDecimal(1e-7));
        assertEquals("1000000", NumberFormatter.formatDecimal(1000000.0));
    }

    @ParameterizedTest
    @CsvSource({
        "1.500, 1.5",
        "1.0, 1",
        "0.000001, 0.000001",
        "10.00, 10",
        "3.14159000, 3.14159",
        "123, 123"
    })
    @DisplayName("stripTrailingZeros removes unnecessary zeros")
    void stripTrailingZeros(String input, String expected) {
        assertEquals(expected, NumberFormatter.stripTrailingZeros(input));
    }

    @Test
    @DisplayName("isNumber identifies valid numbers")
    void isNumberIdentifiesValidNumbers() {
        assertTrue(NumberFormatter.isNumber("42"));
        assertTrue(NumberFormatter.isNumber("-42"));
        assertTrue(NumberFormatter.isNumber("3.14159"));
        assertTrue(NumberFormatter.isNumber("-3.14159"));
        assertTrue(NumberFormatter.isNumber("1e-7"));
        assertTrue(NumberFormatter.isNumber("1E+10"));
        assertTrue(NumberFormatter.isNumber("0.0"));
        assertTrue(NumberFormatter.isNumber("-0"));
    }

    @Test
    @DisplayName("isNumber rejects non-numbers")
    void isNumberRejectsNonNumbers() {
        assertFalse(NumberFormatter.isNumber("abc"));
        assertFalse(NumberFormatter.isNumber("12.34.56"));
        assertFalse(NumberFormatter.isNumber(""));
        assertFalse(NumberFormatter.isNumber(null));
        assertFalse(NumberFormatter.isNumber("  42  "));
        assertFalse(NumberFormatter.isNumber("42px"));
    }

    @Test
    @DisplayName("isInteger identifies valid integers")
    void isIntegerIdentifiesValidIntegers() {
        assertTrue(NumberFormatter.isInteger("42"));
        assertTrue(NumberFormatter.isInteger("-42"));
        assertTrue(NumberFormatter.isInteger("0"));
        assertTrue(NumberFormatter.isInteger("9223372036854775807"));
        assertTrue(NumberFormatter.isInteger("-9223372036854775808"));
    }

    @Test
    @DisplayName("isInteger rejects non-integers")
    void isIntegerRejectsNonIntegers() {
        assertFalse(NumberFormatter.isInteger("3.14"));
        assertFalse(NumberFormatter.isInteger("1e10"));
        assertFalse(NumberFormatter.isInteger("abc"));
        assertFalse(NumberFormatter.isInteger(""));
        assertFalse(NumberFormatter.isInteger(null));
    }
}
