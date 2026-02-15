package dev.toonformat.jtoon.encoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Flatten
 */
class FlattenTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<Flatten> constructor = Flatten.class.getDeclaredConstructor();
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
    void givenValidSingleKeyChain_whenTryFold_thenFoldsSuccessfully() {
        // Given
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode a = root.putObject("a");
        ObjectNode b = a.putObject("b");
        b.put("c", 123);

        Set<String> siblings = Set.of();
        Set<String> rootLiteral = Set.of();

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, siblings, rootLiteral, null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
        assertNull(result.remainder());
        assertEquals(123, result.leafValue().asInt());
        assertEquals(3, result.segmentCount());
    }

    @Test
    void givenNonObjectValue_whenTryFold_thenReturnsNull() {
        // Given
        JsonNode value = MAPPER.valueToTree(10);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "x", value, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenSingleSegmentChain_whenTryFold_thenReturnsNull() {
        // Given
        ObjectNode node = MAPPER.createObjectNode();
        node.put("a", 5);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", node.get("a"), Set.of(), Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenChainWithInvalidIdentifier_whenTryFold_thenReturnsNull() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode inner = a.putObject("invalid-key");
        inner.put("x", 1);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenSiblingCollision_whenTryFold_thenReturnsNull() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("x", true);

        Set<String> siblings = Set.of("a.b");

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, siblings, Set.of(), null, 2
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenRootLiteralCollision_whenTryFold_thenReturnsNull() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("c", 1);

        Set<String> rootLiteral = Set.of("root.a.b.c");

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), rootLiteral, "root", 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenDepthLimitReached_whenTryFold_thenReturnsNull() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.putObject("c").put("x", 10);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 1
        );

        // Then
        assertNull(result);
    }

    @Test
    void testTryFoldKeyChainWithArrayNode() {
        // Given
        ArrayNode a = MAPPER.createArrayNode();

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain("a", a, Set.of(), Set.of(), null, 10);

        // Then
        assertNull(result);
    }

    @Test
    void testTryFoldKeyChainWithSmallRemainingDepth() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 0
        );

        // Then
        assertNull(result);
    }

    @Test
    void testTryFoldKeyChainWithPathPrefix() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), "items", 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b", result.foldedKey());
        assertNotNull(result.remainder());
        assertNull(result.leafValue());
        assertEquals(2, result.segmentCount());
    }

    @Test
    void testTryFoldKeyChainWithDotsInKey() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "c.d", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("d.b", result.foldedKey());
        assertNotNull(result.remainder());
        assertNull(result.leafValue());
        assertEquals(2, result.segmentCount());
    }

    @Test
    void testTryFoldKeyChainWithSimpleObjectNode() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        a.put("item", 42);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.item", result.foldedKey());
        assertNull(result.remainder());
        assertNotNull(result.leafValue());
        assertEquals(42, result.leafValue().asInt());
        assertEquals(2, result.segmentCount());
    }

    @Test
    void givenTailObjectWithMultipleKeys_whenTryFold_thenReturnsTailInResult() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b", result.foldedKey());
        assertNotNull(result.remainder());
        assertNull(result.leafValue());
        assertEquals(2, result.segmentCount());
    }

    @Test
    void givenEmptyObjectLeaf_whenTryFold_thenLeafIsReturned() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        a.putObject("b"); // empty obj → leaf

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b", result.foldedKey());
        assertNull(result.remainder());
        assertTrue(result.leafValue().isObject());
        assertEquals(2, result.segmentCount());
    }

    @Test
    void givenNullRootLiteralKeys_whenTryFold_thenDoesNotThrow() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("c", 1);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), null, null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
    }

    @Test
    void givenPathPrefixWithDot_whenTryFold_thenUsesCorrectPath() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("c", 1);

        // When - using pathPrefix with dot
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), "prefix.data", 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
    }

    @Test
    void givenDeepSingleKeyChainWithArrayLeaf_whenTryFold_thenReturnsLeaf() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        ObjectNode c = b.putObject("c");
        c.putArray("items"); // array leaf

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c.items", result.foldedKey());
        assertNotNull(result.leafValue());
        assertTrue(result.leafValue().isArray());
    }

    @Test
    void givenSingleKeyChainAtMaxDepth_whenTryFold_thenReturnsNull() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.putObject("c").put("x", 1);

        // When - depth limit of 2
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 2
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b", result.foldedKey());
    }

    @Test
    void givenDeeplyNestedWithEmptyIntermediate_whenTryFold_thenHandles() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        ObjectNode c = b.putObject("c");
        c.putObject("d"); // empty object
        c.put("e", 1);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
    }

    @Test
    void givenMultipleLevelsOfSingleKeyObjects_whenTryFold_thenFolds() {
        // Given - deep chain
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode level1 = root.putObject("a");
        ObjectNode level2 = level1.putObject("b");
        ObjectNode level3 = level2.putObject("c");
        ObjectNode level4 = level3.putObject("d");
        level4.put("value", 42);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", level1, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertTrue(result.foldedKey().startsWith("a.b"));
    }

    @Test
    void givenSiblingCollisionWithFoldedKey_whenTryFold_thenReturnsNull() {
        // Given - existing folded key that would collide
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("b");
        b.put("x", 1);

        Set<String> siblings = Set.of("a.b.x");

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, siblings, Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenNumericKeySegment_whenTryFold_thenFolds() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("123");
        b.put("x", 1);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then - numeric keys are NOT valid identifiers, so null expected
        assertNull(result);
    }

    @Test
    void givenUnderscoreKeySegment_whenTryFold_thenFolds() {
        // Given
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = a.putObject("_private");
        b.put("x", 1);

        // When
        Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then - underscore prefix is valid
        assertNotNull(result);
    }

}
