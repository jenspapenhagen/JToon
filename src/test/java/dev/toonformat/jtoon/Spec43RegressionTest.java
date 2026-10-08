package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("§5.2: throws when whitespace precedes the bracket segment (non-strict)")
    void fallsThroughWhenWhitespacePrecedesBracketSegment() {
        // Given
        final String toon = "foo [2]: bar,baz\nt\t[1]: x\nn" + NBSP + "[1]: y";

        // When / Then
        assertThrows(IllegalArgumentException.class,
            () -> JToon.decode(toon, DecodeOptions.withStrict(false)));
    }

    @Test
    @DisplayName("§6: throws on an invalid header line (non-strict)")
    void treatsInvalidHeaderLineAsKeyValueLine() {
        // Given
        final String toon = "[1]{a}: 1\na[2:]{x}\nb[3]:\n  - k[1]{a}: 1\n  - [1]{a}: 2\n  - [1]{a}:";

        // When / Then
        assertThrows(IllegalArgumentException.class,
            () -> JToon.decode(toon, DecodeOptions.withStrict(false)));
    }
}
