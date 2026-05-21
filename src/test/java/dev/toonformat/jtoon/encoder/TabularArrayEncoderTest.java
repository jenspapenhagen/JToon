package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class TabularArrayEncoderTest {

    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private static final EncodeOptions options = EncodeOptions.DEFAULT;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<TabularArrayEncoder> constructor = TabularArrayEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    void givenEmptyArray_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenFirstRowNotObject_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode().add(1).add(2);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenFirstObjectHasNoKeys_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        rows.add(jsonNodeFactory.objectNode()); // empty object

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenMismatchedKeyCount_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2); // missing name key

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenMissingHeaderKeyInLaterRow_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final int extraFieldValue = 42;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("age", extraFieldValue); // same size but different key set (name missing)

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenNonPrimitiveValue_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.set("name", jsonNodeFactory.objectNode()); // not a primitive

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenUniformObjectsDifferentOrder_whenDetectHeader_thenReturnsHeaderKeys() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("name", "Bob"); // order swapped
        b.put("id", 2);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertEquals(List.of("id", "name"), header);
    }

    @Test
    void givenUniformObjects_whenEncodeArrayAsTabular_thenWritesHeaderAndRows() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("name", "Bob");

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.encodeArrayOfObjectsAsTabular("users", rows, header, writer, 0, options);

        // Then
        final String expected = String.join("\n",
                "users[2]{id,name}:",
                "  1,Ada",
                "  2,Bob");
        assertEquals(expected, writer.toString());
    }

    @Test
    void givenHeaderAndRows_whenWriteTabularRows_thenWritesValuesWithIndent() {
        // Given
        final int nodeA_X = 10;
        final int nodeA_Y = 20;
        final int nodeB_X = 11;
        final int nodeB_Y = 21;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", nodeA_X);
        a.put("y", nodeA_Y);

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("x", nodeB_X);
        b.put("y", nodeB_Y);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = List.of("x", "y");
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        final String expected = String.join("\n",
                "    10,20",
                "    11,21");
        assertEquals(expected, writer.toString());
    }

    @Test
    void testDetectTabularHeaderWithEmptyRow() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithNoneObjectAsFirstItem() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        rows.add(1);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithEmptyObject() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        final ObjectNode a = jsonNodeFactory.objectNode();
        rows.add(a);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithSecondItemIsNotAnObject() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        final ObjectNode a = jsonNodeFactory.objectNode();
        rows.add(a).add(1);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithUnevenObjectInTheList() {
        // Given
        final int objA_X = 10;
        final int objA_Y = 20;
        final int objB_X = 11;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", objA_X);
        a.put("y", objA_Y);

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("x", objB_X);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = List.of("x", "y");
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        final String expected = String.join("\n",
                                      "    10,20",
                                      "    11");
        assertEquals(expected, writer.toString());
    }

    @Test
    void testDetectTabularHeaderWithUnevenObjectArrayMixInTheList() {
        // Given
        final int mixObj_X = 10;
        final int mixObj_Y = 20;
        final int mixArr1 = 11;
        final int mixArr2 = 12;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", mixObj_X);
        a.put("y", mixObj_Y);

        final ArrayNode b = jsonNodeFactory.arrayNode();
        b.add(mixArr1);
        b.add(mixArr2);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = List.of("x", "y");
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        final String expected = String.join("\n",
                                      "    10,20");
        assertEquals(expected, writer.toString());
    }
}
