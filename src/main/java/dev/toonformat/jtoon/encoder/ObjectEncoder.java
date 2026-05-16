package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import dev.toonformat.jtoon.KeyFolding;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import static dev.toonformat.jtoon.util.Constants.DOT;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.SPACE;

public final class ObjectEncoder {

    private static final int MAX_ENCODE_DEPTH = 1024;

    private ObjectEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void encodeObject(final ObjectNode value,
                                    final LineWriter writer,
                                    final int depth,
                                    final EncodeOptions options,
                                    final Set<String> rootLiteralKeys,
                                    final String pathPrefix,
                                    final Integer remainingDepth,
                                    final Set<String> blockedKeys,
                                    final int currentDepth) {
        if (currentDepth > MAX_ENCODE_DEPTH) {
            throw new IllegalArgumentException("Maximum encoding depth exceeded: " + MAX_ENCODE_DEPTH);
        }
        final int effectiveFlattenDepth = remainingDepth != null ? remainingDepth : options.flattenDepth();

        // Single-pass collection: gather sibling keys and optionally dotted keys at root level
        final Set<String> siblings = new LinkedHashSet<>();
        if (depth == 0 && rootLiteralKeys != null) {
            rootLiteralKeys.clear();
            for (final Map.Entry<String, JsonNode> entry : value.properties()) {
                final String key = entry.getKey();
                siblings.add(key);
                if (key.contains(DOT)) {
                    rootLiteralKeys.add(key);
                }
            }
        } else {
            for (final Map.Entry<String, JsonNode> entry : value.properties()) {
                siblings.add(entry.getKey());
            }
        }

        // Encode each field
        for (final Map.Entry<String, JsonNode> entry : value.properties()) {
            encodeKeyValuePair(entry.getKey(), entry.getValue(), writer, depth, options, siblings, rootLiteralKeys,
                               pathPrefix, effectiveFlattenDepth, blockedKeys, currentDepth);
        }
    }

    public static void encodeKeyValuePair(final String key,
                                           final JsonNode value,
                                           final LineWriter writer,
                                           final int depth,
                                           final EncodeOptions options,
                                           final Set<String> siblings,
                                           final Set<String> rootLiteralKeys,
                                           final String pathPrefix,
                                           final Integer flattenDepth,
                                           final Set<String> blockedKeys,
                                           final int currentDepth
    ) {
        if (key == null) {
            return;
        }
        final String encodedKey = PrimitiveEncoder.encodeKey(key);
        final String currentPath = pathPrefix != null ? pathPrefix + DOT + key : key;
        final int effectiveFlattenDepth = flattenDepth != null && flattenDepth > 0
                ? flattenDepth
                : options.flattenDepth();
        final int remainingDepth = effectiveFlattenDepth - depth;
        EncodeOptions currentOptions = options;

        if (remainingDepth > 0
            && !siblings.isEmpty()
            && blockedKeys != null
            && !blockedKeys.contains(key)
            && KeyFolding.SAFE == currentOptions.flatten()) {
            final Flatten.FoldResult foldResult = Flatten.tryFoldKeyChain(key, value, siblings, rootLiteralKeys,
                                                                          pathPrefix, remainingDepth);
            if (foldResult != null) {
                currentOptions = flatten(key, foldResult, writer, depth, currentOptions, rootLiteralKeys, pathPrefix,
                                         blockedKeys, remainingDepth, currentDepth);
                if (currentOptions == null) {
                    return;
                }
            }
        }

        final int nextDepth = currentDepth + 1;
        if (value.isValueNode()) {
            writer.push(depth, encodedKey + COLON + SPACE
                + PrimitiveEncoder.encodePrimitive(value, currentOptions.delimiter().toString()));
        }
        if (value.isArray()) {
            ArrayEncoder.encodeArray(key, (ArrayNode) value, writer, depth, currentOptions, nextDepth);
        }
        if (value.isObject()) {
            final ObjectNode objValue = (ObjectNode) value;
            writer.push(depth, encodedKey + COLON);
            if (!objValue.isEmpty()) {
                encodeObject(objValue, writer, depth + 1, currentOptions, rootLiteralKeys, currentPath,
                             effectiveFlattenDepth, blockedKeys, nextDepth);
            }
        }
    }

    /**
     * Extract to flatten methode for better maintenance.
     *
     * @param key             the key name
     * @param foldResult      the result of the folding
     * @param writer          the LineWriter for accumulating output
     * @param depth           the current indentation depth
     * @param options         encoding options
     * @param rootLiteralKeys optional set of dotted keys at the root level to avoid collisions
     * @param pathPrefix      optional parent dotted path (for absolute collision checks)
     * @param blockedKeys     contains only keys that have undergone a successful flattening
     * @param remainingDepth  the depth that remind to the limit
     * @return EncodeOptions changes for Case 2
     */
private static EncodeOptions flatten(final String key,
                                          final Flatten.FoldResult foldResult,
                                          final LineWriter writer,
                                          final int depth,
                                          final EncodeOptions options,
                                          final Set<String> rootLiteralKeys,
                                          final String pathPrefix,
                                          final Set<String> blockedKeys,
                                          final int remainingDepth,
                                          final int currentDepth) {
        final String foldedKey = foldResult.foldedKey();
        EncodeOptions currentOptions = options;

        blockedKeys.add(key);
        blockedKeys.add(foldedKey);

        final String encodedFoldedKey = PrimitiveEncoder.encodeKey(foldedKey);
        final JsonNode remainder = foldResult.remainder();

        if (remainder == null) {
            handleFullyFoldedLeaf(foldResult, writer, depth, currentOptions, encodedFoldedKey, currentDepth);
            return null;
        }

        if (remainder.isObject()) {
            writer.push(depth, indentedLine(depth, encodedFoldedKey + COLON, currentOptions.indent()));

            final String foldedPath = pathPrefix != null ? String.join(DOT, pathPrefix, foldedKey) : foldedKey;
            int newRemainingDepth = remainingDepth - foldResult.segmentCount();

            if (newRemainingDepth <= 0) {
                newRemainingDepth = -1;
                currentOptions = new EncodeOptions(currentOptions.indent(), currentOptions.delimiter(),
                                                   currentOptions.lengthMarker(), KeyFolding.OFF,
                                                   currentOptions.flattenDepth());
            }

            encodeObject((ObjectNode) remainder, writer, depth + 1, currentOptions, rootLiteralKeys, foldedPath,
                         newRemainingDepth, blockedKeys, currentDepth + 1);
            return null;
        }

        return currentOptions;
    }

    private static void handleFullyFoldedLeaf(final Flatten.FoldResult foldResult,
                                              final LineWriter writer,
                                              final int depth,
                                              final EncodeOptions options,
                                              final String encodedFoldedKey,
                                              final int currentDepth) {
        final JsonNode leaf = foldResult.leafValue();
        final int nextDepth = currentDepth + 1;

        if (leaf.isValueNode()) {
            writer.push(depth,
                indentedLine(depth,
                    encodedFoldedKey + COLON + SPACE
                        + PrimitiveEncoder.encodePrimitive(leaf, options.delimiter().toString()),
                    options.indent()));
            return;
        }

        if (leaf.isArray()) {
            ArrayEncoder.encodeArray(foldResult.foldedKey(), (ArrayNode) leaf, writer, depth, options, nextDepth);
            return;
        }

        if (leaf.isObject()) {
            writer.push(depth, indentedLine(depth, encodedFoldedKey + COLON, options.indent()));
            if (!leaf.isEmpty()) {
                encodeObject((ObjectNode) leaf, writer, depth + 1, options, null, null, null, null, nextDepth);
            }
        }
    }

    private static String indentedLine(final int depth, final String content, final int indentSize) {
        return "%s%s".formatted(" ".repeat(indentSize * depth), content);
    }
}
