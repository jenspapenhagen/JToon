package dev.toonformat.jtoon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IndentFormatter}.
 */
@DisplayName("IndentFormatter")
class IndentFormatterTest {

    @Test
    @DisplayName("getIndent returns empty string for depth 0")
    void getIndentReturnsEmptyForDepthZero() {
        assertEquals("", IndentFormatter.getIndent(0, 2));
        assertEquals("", IndentFormatter.getIndent(0, 4));
    }

    @Test
    @DisplayName("getIndent returns empty string for negative depth")
    void getIndentReturnsEmptyForNegativeDepth() {
        assertEquals("", IndentFormatter.getIndent(-1, 2));
        assertEquals("", IndentFormatter.getIndent(-5, 4));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 2, '  '",
        "1, 4, '    '",
        "2, 2, '    '",
        "2, 4, '        '",
        "3, 2, '      '",
        "3, 4, '            '"
    })
    @DisplayName("getIndent returns correct indentation")
    void getIndentReturnsCorrectIndentation(int depth, int indentSize, String expected) {
        assertEquals(expected, IndentFormatter.getIndent(depth, indentSize));
    }

    @Test
    @DisplayName("getIndent uses cache for small indent sizes")
    void getIndentUsesCache() {
        // These should all use the cache
        assertEquals("", IndentFormatter.getIndent(0, 2));
        assertEquals("  ", IndentFormatter.getIndent(1, 2));
        assertEquals("    ", IndentFormatter.getIndent(2, 2));
        assertEquals("      ", IndentFormatter.getIndent(3, 2));
        
        // Larger sizes
        assertEquals("          ", IndentFormatter.getIndent(5, 2));
        assertEquals("                    ", IndentFormatter.getIndent(10, 2));
    }

    @Test
    @DisplayName("getSpaces returns empty string for zero or negative")
    void getSpacesReturnsEmptyForZeroOrNegative() {
        assertEquals("", IndentFormatter.getSpaces(0));
        assertEquals("", IndentFormatter.getSpaces(-1));
        assertEquals("", IndentFormatter.getSpaces(-5));
    }

    @Test
    @DisplayName("getSpaces returns correct number of spaces")
    void getSpacesReturnsCorrectSpaces() {
        assertEquals(" ", IndentFormatter.getSpaces(1));
        assertEquals("  ", IndentFormatter.getSpaces(2));
        assertEquals("   ", IndentFormatter.getSpaces(3));
        assertEquals("    ", IndentFormatter.getSpaces(4));
        assertEquals("     ", IndentFormatter.getSpaces(5));
    }

    @Test
    @DisplayName("getSpaces uses cache for small sizes")
    void getSpacesUsesCache() {
        // These should all use the cache
        for (int i = 0; i < 20; i++) {
            assertEquals(" ".repeat(i), IndentFormatter.getSpaces(i));
        }
    }

    @Test
    @DisplayName("getSpaces handles large sizes")
    void getSpacesHandlesLargeSizes() {
        String large = IndentFormatter.getSpaces(100);
        assertEquals(100, large.length());
        assertTrue(large.trim().isEmpty());
    }

    @Test
    @DisplayName("getIndent handles large indent sizes")
    void getIndentHandlesLargeSizes() {
        String large = IndentFormatter.getIndent(10, 4); // 40 spaces
        assertEquals(40, large.length());
        assertTrue(large.trim().isEmpty());
    }

    @Test
    @DisplayName("same indent returns same instance from cache")
    void sameIndentReturnsSameInstance() {
        // Due to caching, identical requests should return same string
        String indent1 = IndentFormatter.getSpaces(5);
        String indent2 = IndentFormatter.getSpaces(5);
        assertEquals(indent1, indent2);
    }
}
