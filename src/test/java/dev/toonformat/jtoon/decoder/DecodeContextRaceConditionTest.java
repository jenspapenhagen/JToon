package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import dev.toonformat.jtoon.JToon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecodeContextRaceConditionTest {

    private static final int THREAD_COUNT = 20;
    private static final int ITERATIONS_PER_THREAD = 100;
    private static final int AWAIT_TERMINATION_SECONDS = 10;

    @Test
    @DisplayName("Should be thread-safe when decoding multiple inputs concurrently")
    @SuppressWarnings("unchecked")
    void concurrentDecoding() throws InterruptedException, ExecutionException {
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        final String toonInput = ("name: JToon\nversion: 1.0.0\ntags[3]:\n  - java\n  - json\n"
                + "  - toon\nmetadata:\n  author: dev\n  active: true").formatted();

        final List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT * ITERATIONS_PER_THREAD; i++) {
            futures.add(executor.submit(() -> JToon.decode(toonInput)));
        }

        for (Future<Object> future : futures) {
            final Object result = future.get();
            if (!(result instanceof Map)) {
                fail("Result should be a Map");
            }
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("JToon", map.get("name"));
            assertEquals("1.0.0", map.get("version"));
            assertEquals(true, ((Map<String, Object>) map.get("metadata")).get("active"));
        }

        executor.shutdown();
        if (!executor.awaitTermination(AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should handle different inputs concurrently without interference")
    @SuppressWarnings("unchecked")
    void concurrentDifferentInputs() throws InterruptedException, ExecutionException {
        final int threadCount = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final int iterationCount = 1000;

        final List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < iterationCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                final String input = "key: value" + index;
                final Map<String, Object> result = (Map<String, Object>) JToon.decode(input);
                assertEquals("value" + index, result.get("key"));
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
    }
}
