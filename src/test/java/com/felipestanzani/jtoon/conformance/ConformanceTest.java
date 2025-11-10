package com.felipestanzani.jtoon.conformance;

import com.felipestanzani.jtoon.JToon;
import com.felipestanzani.jtoon.conformance.model.JsonTestCase;
import com.felipestanzani.jtoon.conformance.model.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Objects;

@Tag("unit")
public class ConformanceTest {
    @Nested
    @DisplayName("Encoding conformance tests")
    class encodeJsonTest {
        @Test
        void testJSONFile() {
            ObjectMapper mapper = new ObjectMapper();
            File dir = new File("src/test/resources/conformance/encode");

            for (File file : Objects.requireNonNull(dir.listFiles())) {
                TestFixture testSuite = mapper.readValue(file, TestFixture.class);

                for (JsonTestCase testCase : testSuite.tests()) {
                    testCase.name();
                    String input = JToon.encodeJson(testCase.input().toString());
                    assertEquals(testCase.expected(), input);
                }
            }
        }
    }
}
