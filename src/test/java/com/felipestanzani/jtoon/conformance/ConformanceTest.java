package com.felipestanzani.jtoon.conformance;

import com.felipestanzani.jtoon.DecodeOptions;
import com.felipestanzani.jtoon.Delimiter;
import com.felipestanzani.jtoon.EncodeOptions;
import com.felipestanzani.jtoon.JToon;
import com.felipestanzani.jtoon.conformance.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@Tag("unit")
public class ConformanceTest {
    @Nested
    @DisplayName("Encoding conformance tests")
    class encodeJsonTest {
        private final ObjectMapper mapper = new ObjectMapper();

        @TestFactory
        Stream<DynamicTest> testJSONFile() {
            File directory = new File("src/test/resources/conformance/encode");
            return loadTestFixtures(directory)
                    .flatMap(this::createTestsFromFixture);
        }

        private Stream<EncodeTestFile> loadTestFixtures(File directory) {
            File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                    .map(this::parseFixture);
        }

        private EncodeTestFile parseFixture(File file) {
            try {
                EncodeTestFixture fixture = mapper.readValue(file, EncodeTestFixture.class);
                return new EncodeTestFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private Stream<DynamicTest> createTestsFromFixture(EncodeTestFile fixtureWithFile) {
            File file = fixtureWithFile.file();
            EncodeTestFixture fixture = fixtureWithFile.fixture();
            return fixture.tests().stream()
                    .map(testCase -> createDynamicTest(file, testCase));
        }

        private DynamicTest createDynamicTest(File file, JsonEncodeTestCase testCase) {
            String displayName = file.getName() + " - " + testCase.name();
            return DynamicTest.dynamicTest(displayName, () -> executeTestCase(testCase));
        }

        private void executeTestCase(JsonEncodeTestCase testCase) {
            EncodeOptions options = parseOptions(testCase.options());
            String jsonInput = mapper.writeValueAsString(testCase.input());
            String actual = JToon.encodeJson(jsonInput, options);
            assertEquals(testCase.expected(), actual);
        }

        private EncodeOptions parseOptions(JsonEncodeTestOptions options) {
            if (options == null) {
                return EncodeOptions.DEFAULT;
            }

            int indent = options.indent() != null ? options.indent() : 2;

            Delimiter delimiter = Delimiter.COMMA;
            if (options.delimiter() != null) {
                String delimiterValue = options.delimiter();
                delimiter = switch (delimiterValue) {
                    case "\t" -> Delimiter.TAB;
                    case "|" -> Delimiter.PIPE;
                    case "," -> Delimiter.COMMA;
                    default -> delimiter;
                };
            }

            boolean lengthMarker = options.lengthMarker() != null && "#".equals(options.lengthMarker());

            return new EncodeOptions(indent, delimiter, lengthMarker);
        }

        private record EncodeTestFile(File file, EncodeTestFixture fixture) {
        }
    }

    @Nested
    @DisplayName("Decoding conformance tests")
    class decodeJsonTest {
        private final ObjectMapper mapper = new ObjectMapper();

        @TestFactory
        Stream<DynamicTest> testJSONFile() {
            File directory = new File("src/test/resources/conformance/decode");
            return loadTestFixtures(directory)
                    .flatMap(this::createTestsFromFixture);
        }

        private Stream<DecodeTestFile> loadTestFixtures(File directory) {
            File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                    .map(this::parseFixture);
        }

        private DecodeTestFile parseFixture(File file) {
            try {
                var fixture = mapper.readValue(file, DecodeTestFixture.class);
                return new DecodeTestFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private Stream<DynamicTest> createTestsFromFixture(DecodeTestFile decodeFile) {
            File file = decodeFile.file();
            var fixture = decodeFile.fixture();
            return fixture.tests().stream()
                    .map(testCase -> createDynamicTest(file, testCase));
        }

        private DynamicTest createDynamicTest(File file, JsonDecodeTestCase testCase) {
            String displayName = file.getName() + " - " + testCase.name();
            return DynamicTest.dynamicTest(displayName, () -> executeTestCase(testCase));
        }

        private void executeTestCase(JsonDecodeTestCase testCase) {
            var options = parseOptions(testCase.options());
            String toonInput = testCase.input().asString();

            if (Boolean.TRUE.equals(testCase.shouldError())) {
                Object actual;
                try {
                    actual = JToon.decode(toonInput, options);
                } catch (IllegalArgumentException e) {
                    return;
                }
                String actualJson = mapper.writeValueAsString(actual);
                fail("Expected IllegalArgumentException but got result: " + actualJson);
            } else {
                Object actual = JToon.decode(toonInput, options);
                if (testCase.expected() == null || testCase.expected().isNull()) {
                    assertNull(actual, "Expected null but got: " + actual);
                } else {
                    String actualJson = mapper.writeValueAsString(actual);
                    String expectedJson = mapper.writeValueAsString(testCase.expected());
                    assertEquals(expectedJson, actualJson);
                }
            }
        }

        private DecodeOptions parseOptions(JsonDecodeTestOptions options) {
            if (options == null) {
                return DecodeOptions.DEFAULT;
            }

            int indent = options.indent() != null ? options.indent() : 2;

            Delimiter delimiter = Delimiter.COMMA;
            if (options.delimiter() != null) {
                String delimiterValue = options.delimiter();
                delimiter = switch (delimiterValue) {
                    case "\t" -> Delimiter.TAB;
                    case "|" -> Delimiter.PIPE;
                    case "," -> Delimiter.COMMA;
                    default -> delimiter;
                };
            }

            boolean strict = options.strict() != null ? options.strict() : true;

            return new DecodeOptions(indent, delimiter, strict);
        }

        private record DecodeTestFile(File file, DecodeTestFixture fixture) {
        }
    }
}
