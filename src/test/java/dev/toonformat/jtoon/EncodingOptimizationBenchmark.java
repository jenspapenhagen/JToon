package dev.toonformat.jtoon;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            simpleObjectNode = createSimpleObject(10);
            nestedObjectNode = createNestedObject(3, 5);
            tabularArrayNode = createTabularArray(10, 5);
            primitiveArrayNode = createPrimitiveArray(20);
            complexMixedNode = createComplexMixedDocument(5);
        } else {
            simpleObjectNode = createSimpleObject(100);
            nestedObjectNode = createNestedObject(5, 20);
            tabularArrayNode = createTabularArray(100, 10);
            primitiveArrayNode = createPrimitiveArray(500);
            complexMixedNode = createComplexMixedDocument(50);
        }
    }

    private JsonNode createSimpleObject(int fieldCount) {
        Map<String, Object> obj = new HashMap<>();
        for (int i = 0; i < fieldCount; i++) {
            obj.put("field_" + i, i % 5 == 0 ? "string_" + i : (i % 3 == 0 ? i * 1.5 : i));
        }
        return JsonNormalizer.normalize(obj);
    }

    private JsonNode createNestedObject(int depth, int fieldsPerLevel) {
        Map<String, Object> current = new HashMap<>();
        Map<String, Object> root = current;
        
        for (int d = 0; d < depth; d++) {
            Map<String, Object> level = new HashMap<>();
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

    private JsonNode createTabularArray(int rowCount, int columnCount) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            Map<String, Object> row = new HashMap<>();
            for (int c = 0; c < columnCount; c++) {
                row.put("col" + c, r * columnCount + c);
            }
            rows.add(row);
        }
        Map<String, Object> obj = new HashMap<>();
        obj.put("data", rows);
        return JsonNormalizer.normalize(obj);
    }

    private JsonNode createPrimitiveArray(int count) {
        List<Object> arr = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            arr.add(i % 3 == 0 ? "value" + i : i);
        }
        Map<String, Object> obj = new HashMap<>();
        obj.put("items", arr);
        return JsonNormalizer.normalize(obj);
    }

    private JsonNode createComplexMixedDocument(int elementCount) {
        Map<String, Object> root = new HashMap<>();
        
        // Simple fields
        root.put("id", 12345);
        root.put("name", "Test Document");
        root.put("active", true);
        
        // Nested object
        Map<String, Object> metadata = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            metadata.put("meta" + i, "value" + i);
        }
        root.put("metadata", metadata);
        
        // Tabular array
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i);
            record.put("value", i * 1.5);
            record.put("label", "item" + i);
            records.add(record);
        }
        root.put("records", records);
        
        // Primitive array
        List<Integer> tags = new ArrayList<>();
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

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(EncodingOptimizationBenchmark.class.getSimpleName())
            .result("build/jmh-results/encoding-optimization-benchmark.json")
            .build();

        new Runner(opt).run();
    }
}
