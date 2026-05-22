package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ValueDecoderThreadSafetyTest {

    private static final int TEST_REPETITIONS = 100;
    private static final long EXPECTED_SCORE = 42L;

    @RepeatedTest(TEST_REPETITIONS)
    @DisplayName("ValueDecoder should be thread-safe when decoding TOON strings")
    void decodeThreadSafety() {
        // Given
        final String id = UUID.randomUUID().toString();
        final String toon = "id: " + id + "\n" +
            "tags[3]: a, b, c\n" +
            "meta:\n" +
            "  active: true\n" +
            "  score: 42";

        // When
        final Object decoded = ValueDecoder.decode(toon, DecodeOptions.DEFAULT);

        // Then
        assertInstanceOf(Map.class, decoded);
        final Map<?, ?> map = (Map<?, ?>) decoded;
        assertEquals(id, map.get("id"));
        assertEquals(List.of("a", "b", "c"), map.get("tags"));

        final Map<?, ?> meta = (Map<?, ?>) map.get("meta");
        assertEquals(true, meta.get("active"));
        assertEquals(EXPECTED_SCORE, meta.get("score"));
    }
}
