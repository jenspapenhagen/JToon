package com.felipestanzani.jtoon.conformance.encode;

import com.felipestanzani.jtoon.JToon;
import com.felipestanzani.jtoon.conformance.JsonTestFile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Objects;

public class TestFixture {

    @Nested
    class encodeJsonTest {

        @Test
        void testJSONFile () {

            ObjectMapper mapper = new ObjectMapper();

            File dir = new File("src/test/resources/conformance/encode");

            for (File file : Objects.requireNonNull(dir.listFiles())) {

                JsonTestFile.TestSuite testSuite = mapper.readValue(
                        file,
                        JsonTestFile.TestSuite.class
                );

                for (JsonTestFile.TestCase testCase : testSuite.tests()) {

                    String input = JToon.encodeJson(testCase.input().toString());

                    assertEquals(testCase.expected(), input);

                }

            }

        }

    }

}
