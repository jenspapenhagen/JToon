package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("TOON spec v4.3 regression tests")
public class Spec43RegressionTest {

    private static final char NBSP = (char) 0xA0;

    @Test
    @DisplayName("§5.2: throws on a header line with an unclosed bracket segment")
    void throwsOnUnclosedBracketSegment() {
        assertThrows(IllegalArgumentException.class, () -> JToon.decode("a[1:"));
    }

    @Test
    @DisplayName("§5.2: throws on a keyed header line without its colon")
    void throwsOnKeyedHeaderWithoutColon() {
        assertThrows(IllegalArgumentException.class, () -> JToon.decode("a[2:]{x}"));
    }

    @Test
    @DisplayName("§5.2: falls through to a key-value line when whitespace precedes the bracket segment (non-strict)")
    void fallsThroughWhenWhitespacePrecedesBracketSegment() {
        // Given
        final String toon = "foo [2]: bar,baz\nt\t[1]: x\nn" + NBSP + "[1]: y";

        // When
        final Object result = JToon.decode(toon, DecodeOptions.withStrict(false));

        // Then
        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("foo [2]", "bar,baz");
        expected.put("t\t[1]", "x");
        expected.put("n" + NBSP + "[1]", "y");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("§6: treats an invalid header line as a key-value line (non-strict)")
    void treatsInvalidHeaderLineAsKeyValueLine() {
        // Given
        final String toon = "[1]{a}: 1\na[2:]{x}\nb[3]:\n  - k[1]{a}: 1\n  - [1]{a}: 2\n  - [1]{a}:";

        // When
        final Object result = JToon.decode(toon, DecodeOptions.withStrict(false));

        // Then
        final Map<String, Object> firstItem = new LinkedHashMap<>();
        firstItem.put("k[1]{a}", 1L);
        final Map<String, Object> secondItem = new LinkedHashMap<>();
        secondItem.put("[1]{a}", 2L);
        final Map<String, Object> thirdItem = new LinkedHashMap<>();
        thirdItem.put("[1]{a}", new LinkedHashMap<>());

        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("[1]{a}", 1L);
        expected.put("a[2", "]{x}");
        expected.put("b", List.of(firstItem, secondItem, thirdItem));
        assertEquals(expected, result);
    }
}
