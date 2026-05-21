package dev.toonformat.jtoon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import dev.toonformat.jtoon.encoder.ValueEncoder;
import dev.toonformat.jtoon.normalizer.JsonNormalizer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.JsonNode;

/**
 * JMH Benchmark measuring encoding throughput after optimizations.
 * Tests both small and large objects to validate performance improvements.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class EncodingOptimizationBenchmark {

    private static final int SMALL_FIELD_COUNT = 10;
    private static final int SMALL_NESTED_DEPTH = 3;
    private static final int SMALL_COLUMN_COUNT = 5;
    private static final int SMALL_ARRAY_COUNT = 20;
    private static final int SMALL_DOCUMENT_COUNT = 5;
    private static final int LARGE_FIELD_COUNT = 100;
    private static final int LARGE_NESTED_DEPTH = 5;
    private static final int LARGE_FIELDS_PER_LEVEL = 20;
    private static final int LARGE_ARRAY_COUNT = 500;
    private static final int LARGE_DOCUMENT_COUNT = 50;
    private static final int LARGE_TABULAR_COLUMNS = 10;
    private static final double DOUBLE_FACTOR = 1.5;
    private static final int TEST_ID = 12345;
    private static final int METADATA_COUNT = 5;

    @Param({"small", "large"})
    private String size;

    private JsonNode simpleObjectNode;
    private JsonNode nestedObjectNode;
    private JsonNode tabularArrayNode;
    private JsonNode primitiveArrayNode;
    private JsonNode complexMixedNode;

    @Setup
    public void setup() {
        if ("small".equals(size)) {
            simpleObjectNode = createSimpleObject(SMALL_FIELD_COUNT);
            nestedObjectNode = createNestedObject(SMALL_NESTED_DEPTH, SMALL_COLUMN_COUNT);
            tabularArrayNode = createTabularArray(SMALL_FIELD_COUNT, SMALL_COLUMN_COUNT);
            primitiveArrayNode = createPrimitiveArray(SMALL_ARRAY_COUNT);
            complexMixedNode = createComplexMixedDocument(SMALL_DOCUMENT_COUNT);
        } else {
            simpleObjectNode = createSimpleObject(LARGE_FIELD_COUNT);
            nestedObjectNode = createNestedObject(LARGE_NESTED_DEPTH, LARGE_FIELDS_PER_LEVEL);
            tabularArrayNode = createTabularArray(LARGE_FIELD_COUNT, LARGE_TABULAR_COLUMNS);
            primitiveArrayNode = createPrimitiveArray(LARGE_ARRAY_COUNT);
            complexMixedNode = createComplexMixedDocument(LARGE_DOCUMENT_COUNT);
        }
    }

    private JsonNode createSimpleObject(final int fieldCount) {
        final Map<String, Object> obj = new HashMap<>();
        for (int i = 0; i < fieldCount; i++) {
            obj.put("field_" + i,
                    i % SMALL_COLUMN_COUNT == 0 ? "string_" + i
                            : (i % SMALL_NESTED_DEPTH == 0 ? i * DOUBLE_FACTOR : i));
        }
        return JsonNormalizer.normalize(obj);
    }

    private JsonNode createNestedObject(final int depth, final int fieldsPerLevel) {
        Map<String, Object> current = new HashMap<>();
        final Map<String, Object> root = current;
        
        for (int d = 0; d < depth; d++) {
            final Map<String, Object> level = new HashMap<>();
            for (int i = 0; i < fieldsPerLevel; i++) {
                level.put("level" + d + "_field" + i, i);
            }
            if (d < depth - 1) {
                current.put("nested", level);
                current = level;
            }
        }
        return JsonNormalizer.normalize(root);
    }

    private JsonNode createTabularArray(final int rowCount, final int columnCount) {
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            final Map<String, Object> row = new HashMap<>();
            for (int c = 0; c < columnCount; c++) {
                row.put("col" + c, r * columnCount + c);
            }
            rows.add(row);
        }
        final Map<String, Object> obj = new HashMap<>();
        obj.put("data", rows);
        return JsonNormalizer.normalize(obj);
    }

    private JsonNode createPrimitiveArray(final int count) {
        final List<Object> arr = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            arr.add(i % SMALL_NESTED_DEPTH == 0 ? "value" + i : i);
        }
        final Map<String, Object> obj = new HashMap<>();
        obj.put("items", arr);
        return JsonNormalizer.normalize(obj);
    }

    private JsonNode createComplexMixedDocument(final int elementCount) {
        final Map<String, Object> root = new HashMap<>();
        
        // Simple fields
        root.put("id", TEST_ID);
        root.put("name", "Test Document");
        root.put("active", true);
        
        // Nested object
        final Map<String, Object> metadata = new HashMap<>();
        for (int i = 0; i < METADATA_COUNT; i++) {
            metadata.put("meta" + i, "value" + i);
        }
        root.put("metadata", metadata);
        
        // Tabular array
        final List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {
            final Map<String, Object> record = new HashMap<>();
            record.put("id", i);
            record.put("value", i * DOUBLE_FACTOR);
            record.put("label", "item" + i);
            records.add(record);
        }
        root.put("records", records);
        
        // Primitive array
        final List<Integer> tags = new ArrayList<>();
        for (int i = 0; i < elementCount / 2; i++) {
            tags.add(i);
        }
        root.put("tags", tags);
        
        return JsonNormalizer.normalize(root);
    }

    @Benchmark
    public String encodeSimpleObject() {
        return ValueEncoder.encodeValue(simpleObjectNode, EncodeOptions.DEFAULT);
    }

    @Benchmark
    public String encodeNestedObject() {
        return ValueEncoder.encodeValue(nestedObjectNode, EncodeOptions.DEFAULT);
    }

    @Benchmark
    public String encodeTabularArray() {
        return ValueEncoder.encodeValue(tabularArrayNode, EncodeOptions.DEFAULT);
    }

    @Benchmark
    public String encodePrimitiveArray() {
        return ValueEncoder.encodeValue(primitiveArrayNode, EncodeOptions.DEFAULT);
    }

    @Benchmark
    public String encodeComplexMixed() {
        return ValueEncoder.encodeValue(complexMixedNode, EncodeOptions.DEFAULT);
    }

    public static void main(final String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder()
            .include(EncodingOptimizationBenchmark.class.getSimpleName())
            .result("build/jmh-results/encoding-optimization-benchmark.json")
            .build();

        new Runner(opt).run();
    }
}
