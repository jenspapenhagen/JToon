package dev.toonformat.jtoon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Headers}.
 */
@DisplayName("Headers")
class HeadersTest {

    @Test
    @DisplayName("constructor throws UnsupportedOperationException")
    void constructorThrowsException() throws Exception {
        Constructor<Headers> constructor = Headers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
    }

    @Test
    @DisplayName("ARRAY_HEADER_PATTERN matches array headers")
    void arrayHeaderPatternMatches() {
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[3]").matches());
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[#2]").matches());
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[3\t]").matches());
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[2|]").matches());
    }

    @Test
    @DisplayName("TABULAR_HEADER_PATTERN matches tabular headers")
    void tabularHeaderPatternMatches() {
        assertNotNull(Headers.TABULAR_HEADER_PATTERN.matcher("[2]{id,name,role}:").matches());
        assertNotNull(Headers.TABULAR_HEADER_PATTERN.matcher("[#3]{a,b,c}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN matches keyed arrays")
    void keyedArrayPatternMatches() {
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("items[2]{id,name}:").matches());
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("tags[3]:").matches());
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("data[4]{id}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN matches quoted keys with spaces")
    void keyedArrayPatternQuotedKeyWithSpaces() {
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("\"my items\"[3]:").matches());
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("\"user name\"[2]{id,name}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN matches quoted keys with escaped quotes")
    void keyedArrayPatternEscapedQuotes() {
        // Key containing escaped quotes: "name\"with\"quotes"
        assertTrue(Headers.KEYED_ARRAY_PATTERN.matcher("\"name\\\"with\\\"quotes\"[3]:").matches());
        assertTrue(Headers.KEYED_ARRAY_PATTERN.matcher("\"key\\\"word\"[2]{a,b}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN does not match malformed patterns")
    void keyedArrayPatternNoMatch() {
        // Missing colon
        assertFalse(Headers.KEYED_ARRAY_PATTERN.matcher("items[3]").matches());
        // Missing brackets
        assertFalse(Headers.KEYED_ARRAY_PATTERN.matcher("items:").matches());
        // Negative length
        assertFalse(Headers.KEYED_ARRAY_PATTERN.matcher("items[-1]:").matches());
    }
}
