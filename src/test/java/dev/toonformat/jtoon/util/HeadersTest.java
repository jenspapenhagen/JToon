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
}
