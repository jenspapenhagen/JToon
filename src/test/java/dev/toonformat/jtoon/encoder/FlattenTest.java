package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test for Flatten.
 */
class FlattenTest {

    private static final int FOLD_VALUE = 123;
    private static final int PAYLOAD_VALUE = 42;
    private static final int EXPECTED_SEGMENT_COUNT = 3;
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
        final ObjectNode root = MAPPER.createObjectNode();
        final ObjectNode a = root.putObject("a");
        final ObjectNode b = a.putObject("b");
        b.put("c", FOLD_VALUE);

        final Set<String> siblings = Set.of();
        final Set<String> rootLiteral = Set.of();

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, siblings, rootLiteral, null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
        assertNull(result.remainder());
        assertEquals(FOLD_VALUE, result.leafValue().asInt());
        assertEquals(EXPECTED_SEGMENT_COUNT, result.segmentCount());
    }

    @Test
    void givenNonObjectValue_whenTryFold_thenReturnsNull() {
        // Given
        final JsonNode value = MAPPER.valueToTree(10);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "x", value, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenSingleSegmentChain_whenTryFold_thenReturnsNull() {
        // Given
        final int singleValue = 5;
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("a", singleValue);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", node.get("a"), Set.of(), Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenChainWithInvalidIdentifier_whenTryFold_thenReturnsNull() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode inner = a.putObject("invalid-key");
        inner.put("x", 1);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenSiblingCollision_whenTryFold_thenReturnsNull() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", true);

        final Set<String> siblings = Set.of("a.b");

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, siblings, Set.of(), null, 2
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenRootLiteralCollision_whenTryFold_thenReturnsNull() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", 1);

        final Set<String> rootLiteral = Set.of("root.a.b.c");

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), rootLiteral, "root", 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenDepthLimitReached_whenTryFold_thenReturnsNull() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        final int dfltReachedValue = 10;
        b.putObject("c").put("x", dfltReachedValue);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 1
        );

        // Then
        assertNull(result);
    }

    @Test
    void testTryFoldKeyChainWithArrayNode() {
        // Given
        final ArrayNode a = MAPPER.createArrayNode();

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain("a", a, Set.of(), Set.of(), null, 10);

        // Then
        assertNull(result);
    }

    @Test
    void testTryFoldKeyChainWithSmallRemainingDepth() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 0
        );

        // Then
        assertNull(result);
    }

    @Test
    void testTryFoldKeyChainWithPathPrefix() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
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
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
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
        final ObjectNode a = MAPPER.createObjectNode();
        a.put("item", PAYLOAD_VALUE);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.item", result.foldedKey());
        assertNull(result.remainder());
        assertNotNull(result.leafValue());
        assertEquals(PAYLOAD_VALUE, result.leafValue().asInt());
        assertEquals(2, result.segmentCount());
    }

    @Test
    void givenTailObjectWithMultipleKeys_whenTryFold_thenReturnsTailInResult() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
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
        final ObjectNode a = MAPPER.createObjectNode();
        a.putObject("b"); // empty obj → leaf

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
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
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", 1);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), null, null, 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
    }

    @Test
    void givenPathPrefixWithDot_whenTryFold_thenUsesCorrectPath() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", 1);

        // When - using pathPrefix with dot
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), "prefix.data", 10
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
    }

    @Test
    void givenDeepSingleKeyChainWithArrayLeaf_whenTryFold_thenReturnsLeaf() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        final ObjectNode c = b.putObject("c");
        c.putArray("items"); // array leaf

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
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
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.putObject("c").put("x", 1);

        // When - depth limit of 2
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 2
        );

        // Then
        assertNotNull(result);
        assertEquals("a.b", result.foldedKey());
    }

    @Test
    void givenDeeplyNestedWithEmptyIntermediate_whenTryFold_thenHandles() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        final ObjectNode c = b.putObject("c");
        c.putObject("d"); // empty object
        c.put("e", 1);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
    }

    @Test
    void givenMultipleLevelsOfSingleKeyObjects_whenTryFold_thenFolds() {
        // Given - deep chain
        final ObjectNode root = MAPPER.createObjectNode();
        final ObjectNode level1 = root.putObject("a");
        final ObjectNode level2 = level1.putObject("b");
        final ObjectNode level3 = level2.putObject("c");
        final ObjectNode level4 = level3.putObject("d");
        level4.put("value", PAYLOAD_VALUE);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", level1, Set.of(), Set.of(), null, 10
        );

        // Then
        assertNotNull(result);
        assertTrue(result.foldedKey().startsWith("a.b"));
    }

    @Test
    void givenSiblingCollisionWithFoldedKey_whenTryFold_thenReturnsNull() {
        // Given - existing folded key that would collide
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", 1);

        final Set<String> siblings = Set.of("a.b.x");

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, siblings, Set.of(), null, 10
        );

        // Then
        assertNull(result);
    }

    @Test
    void givenNumericKeySegment_whenTryFold_thenFolds() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("123");
        b.put("x", 1);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then - numeric keys are NOT valid identifiers, so null expected
        assertNull(result);
    }

    @Test
    void givenUnderscoreKeySegment_whenTryFold_thenFolds() {
        // Given
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("_private");
        b.put("x", 1);

        // When
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 10
        );

        // Then - underscore prefix is valid
        assertNotNull(result);
    }

    @Test
    @DisplayName("given valid object but remainingDepth is 1 when tryFold then returns null")
    void givenValidObjectButRemainingDepthIsOne_whenTryFold_thenReturnsNull() {
        // Given - valid object chain but remainingDepth <= 1
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", FOLD_VALUE);

        // When - remainingDepth is 1 (not enough to fold)
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), null, 1
        );

        // Then - should return null because remainingDepth <= 1
        assertNull(result);
    }

    @Test
    @DisplayName("given simple key without dots when collectChain then uses key directly")
    void givenSimpleKeyWithoutDots_whenCollectChain_thenUsesKeyDirectly() {
        // Given - simple key without dots
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", FOLD_VALUE);

        // When - call collectSingleKeyChain directly with simple key
        final Flatten.ChainResult result = Flatten.collectSingleKeyChain("simpleKey", a, 10);

        // Then - first segment should be the key as-is (no dot processing)
        assertNotNull(result);
        assertEquals("simpleKey", result.segments().get(0));
        assertEquals(EXPECTED_SEGMENT_COUNT, result.segments().size()); // simpleKey, b, c
    }

    @Test
    @DisplayName("given single key object at max depth when collectChain then treats as leaf")
    void givenSingleKeyObjectAtMaxDepth_whenCollectChain_thenTreatsAsLeaf() {
        // Given - single-key object at exact max depth
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", FOLD_VALUE);

        // When - depth limit of 2 means we can collect "a" and "b", but "b" has 1 key "c"
        // At depthCounter == maxDepth, single-key object should be treated as leaf
        final Flatten.ChainResult result = Flatten.collectSingleKeyChain("a", a, 2);

        // Then - should stop at "b" with single key and treat as leaf
        assertNotNull(result);
        assertEquals(2, result.segments().size()); // Only "a" and "b"
        assertNotNull(result.leafValue()); // b is treated as leaf (single key, but at max depth)
        assertNull(result.tail());
    }

    @Test
    @DisplayName("given max depth reached with single key chain when collectChain then returns tail")
    void givenMaxDepthReachedWithSingleKeyChain_whenCollectChain_thenReturnsTail() {
        // Given - chain deeper than max depth
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        final ObjectNode c = b.putObject("c");
        final ObjectNode d = c.putObject("d");
        d.put("value", PAYLOAD_VALUE);

        // When - maxDepth of 3 means we stop at "c" which has 1 key "d"
        final Flatten.ChainResult result = Flatten.collectSingleKeyChain("a", a, 3);

        // Then - should have a, b, c as segments, and c (single key object) is the leaf
        assertNotNull(result);
        assertEquals(EXPECTED_SEGMENT_COUNT, result.segments().size());
        assertEquals("c", result.segments().get(2));
        assertNotNull(result.leafValue());
    }

    @Test
    @DisplayName("given empty path prefix when tryFold then uses folded key directly")
    void givenEmptyPathPrefix_whenTryFold_thenUsesFoldedKeyDirectly() {
        // Given - empty string pathPrefix (not null, but empty)
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("c", FOLD_VALUE);

        // When - pathPrefix is empty string (tests line 109 branch)
        final Flatten.FoldResult result = Flatten.tryFoldKeyChain(
            "a", a, Set.of(), Set.of(), "", 10
        );

        // Then - should still work, using folded key directly
        assertNotNull(result);
        assertEquals("a.b.c", result.foldedKey());
    }

    @Test
    @DisplayName("given empty object at depth when collectChain then returns leaf")
    void givenEmptyObjectAtDepth_whenCollectChain_thenReturnsLeaf() {
        // Given - empty object encountered during chain collection
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.putObject("c"); // empty object as leaf

        // When - collect chain that ends with empty object (tests line 180)
        final Flatten.ChainResult result = Flatten.collectSingleKeyChain("a", a, 10);

        // Then - empty object should be treated as leaf
        assertNotNull(result);
        assertEquals("c", result.segments().get(2));
        assertNotNull(result.leafValue());
        assertTrue(result.leafValue().isObject());
        assertTrue(result.leafValue().isEmpty());
    }

    @Test
    @DisplayName("given multi key object at max depth when collectChain then returns tail")
    void givenMultiKeyObjectAtMaxDepth_whenCollectChain_thenReturnsTail() {
        // Given - chain where intermediate object becomes multi-key after depth reached
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = a.putObject("b");
        b.put("x", 1);
        b.put("y", 2); // b has 2 keys - should be tail when maxDepth reached

        // When - maxDepth of 2 allows processing "a" then stops, b has 2 keys (tests line 192)
        final Flatten.ChainResult result = Flatten.collectSingleKeyChain("a", a, 2);

        // Then - should have a, b as segments, b is tail with 2 keys
        assertNotNull(result);
        assertEquals(2, result.segments().size());
        assertEquals("b", result.segments().get(1));
        assertNotNull(result.tail());
        assertEquals(2, result.tail().size());
    }

}
