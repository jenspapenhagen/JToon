package com.felipestanzani.jtoon.conformance;

import com.felipestanzani.jtoon.Delimiter;
import com.felipestanzani.jtoon.EncodeOptions;
import com.felipestanzani.jtoon.JToon;
import com.felipestanzani.jtoon.conformance.model.JsonTestCase;
import com.felipestanzani.jtoon.conformance.model.JsonTestOptions;
import com.felipestanzani.jtoon.conformance.model.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@Tag("unit")
public class ConformanceTest {
    @Nested
    @DisplayName("Encoding conformance tests")
    class encodeJsonTest {
        @TestFactory
        Stream<DynamicTest> testJSONFile() {
            File directory = new File("src/test/resources/conformance/encode");
            return loadTestFixtures(directory)
                    .flatMap(this::createTestsFromFixture);
        }

        private Stream<TestFixtureWithFile> loadTestFixtures(File directory) {
            ObjectMapper mapper = new ObjectMapper();
            File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                    .map(file -> parseFixture(mapper, file));
        }

        private TestFixtureWithFile parseFixture(ObjectMapper mapper, File file) {
            try {
                TestFixture fixture = mapper.readValue(file, TestFixture.class);
                return new TestFixtureWithFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private Stream<DynamicTest> createTestsFromFixture(TestFixtureWithFile fixtureWithFile) {
            File file = fixtureWithFile.file();
            TestFixture fixture = fixtureWithFile.fixture();
            return fixture.tests().stream()
                    .map(testCase -> createDynamicTest(file, testCase));
        }

        private DynamicTest createDynamicTest(File file, JsonTestCase testCase) {
            String displayName = file.getName() + " - " + testCase.name();
            return DynamicTest.dynamicTest(displayName, () -> executeTestCase(testCase));
        }

        private void executeTestCase(JsonTestCase testCase) {
            EncodeOptions options = parseOptions(testCase.options());
            String actual = JToon.encodeJson(testCase.input().toString(), options);
            assertEquals(testCase.expected(), actual);
        }

        private EncodeOptions parseOptions(JsonTestOptions options) {
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

        private record TestFixtureWithFile(File file, TestFixture fixture) {
        }
    }
}
