package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.SPACE;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_PREFIX;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;
import static dev.toonformat.jtoon.util.Constants.CLOSE_BRACKET;

public final class ListItemEncoder {

    private ListItemEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void encodeObjectAsListItem(final ObjectNode obj,
                                               final LineWriter writer,
                                               final int depth,
                                               final EncodeOptions options,
                                               final int currentDepth) {
        if (currentDepth > 1024) {
            throw new IllegalArgumentException("Maximum encoding depth exceeded: 1024");
        }

        final List<String> keys = new ArrayList<>(obj.propertyNames());

        if (keys.isEmpty()) {
            writer.push(depth, LIST_ITEM_MARKER);
            return;
        }

        final Set<String> siblingKeys = new HashSet<>(keys);
        final String firstKey = keys.get(0);
        final JsonNode firstValue = obj.get(firstKey);
        encodeFirstKeyValue(firstKey, firstValue, writer, depth, options, currentDepth);

        for (int i = 1; i < keys.size(); i++) {
            final String key = keys.get(i);
            ObjectEncoder.encodeKeyValuePair(key, obj.get(key), writer, depth + 1, options, siblingKeys,
                                             Set.of(), null, null, new HashSet<>(), currentDepth);
        }
    }

    /**
     * Encodes the first key-value pair of a list item.
     * Handles special formatting for arrays and objects.
     */
private static void encodeFirstKeyValue(final String key,
                                              final JsonNode value,
                                              final LineWriter writer,
                                              final int depth,
                                              final EncodeOptions options,
                                              final int currentDepth) {
        final String encodedKey = PrimitiveEncoder.encodeKey(key);

        if (value.isValueNode()) {
            encodeFirstValueAsPrimitive(encodedKey, value, writer, depth, options);
        } else if (value.isArray()) {
            encodeFirstValueAsArray(key, encodedKey, (ArrayNode) value, writer, depth, options, currentDepth);
        } else if (value.isObject()) {
            encodeFirstValueAsObject(encodedKey, (ObjectNode) value, writer, depth, options, currentDepth);
        }
    }

    private static void encodeFirstValueAsPrimitive(final String encodedKey,
                                                     final JsonNode value,
                                                     final LineWriter writer,
                                                     final int depth,
                                                     final EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON + SPACE
                + PrimitiveEncoder.encodePrimitive(value, options.delimiter().toString()));
    }

    private static void encodeFirstValueAsArray(final String key,
                                                 final String encodedKey,
                                                 final ArrayNode arrayValue,
                                                 final LineWriter writer,
                                                 final int depth,
                                                 final EncodeOptions options,
                                                 final int currentDepth) {
        if (ArrayEncoder.isArrayOfPrimitives(arrayValue)) {
            encodeFirstArrayAsPrimitives(key, arrayValue, writer, depth, options);
        } else if (ArrayEncoder.isArrayOfObjects(arrayValue)) {
            encodeFirstArrayAsObjects(key, encodedKey, arrayValue, writer, depth, options, currentDepth);
        } else {
            encodeFirstArrayAsComplex(encodedKey, arrayValue, writer, depth, options, currentDepth);
        }
    }

    private static void encodeFirstArrayAsPrimitives(final String key,
                                                     final ArrayNode arrayValue,
                                                     final LineWriter writer,
                                                     final int depth,
                                                     final EncodeOptions options) {
        final String formatted = ArrayEncoder.formatInlineArray(arrayValue, options.delimiter().toString(), key,
                                                                options.lengthMarker());
        writer.push(depth, LIST_ITEM_PREFIX + formatted);
    }

    private static void encodeFirstArrayAsObjects(final String key,
                                                   final String encodedKey,
                                                   final ArrayNode arrayValue,
                                                   final LineWriter writer,
                                                   final int depth,
                                                   final EncodeOptions options,
                                                   final int currentDepth) {
        final List<String> header = TabularArrayEncoder.detectTabularHeader(arrayValue);
        if (!header.isEmpty()) {
            final String headerStr = PrimitiveEncoder.formatHeader(arrayValue.size(), key, header,
                                                                   options.delimiter().toString(),
                                                                   options.lengthMarker());
            writer.push(depth, LIST_ITEM_PREFIX + headerStr);
            TabularArrayEncoder.writeTabularRows(arrayValue, header, writer, depth + 2, options);
        } else {
            writer.push(depth,
                    LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);
            final int nextDepth = currentDepth + 1;
            for (JsonNode item : arrayValue) {
                if (item.isObject()) {
                    encodeObjectAsListItem((ObjectNode) item, writer, depth + 2, options, nextDepth);
                }
            }
        }
    }

    private static void encodeFirstArrayAsComplex(final String encodedKey,
                                                   final ArrayNode arrayValue,
                                                   final LineWriter writer,
                                                   final int depth,
                                                   final EncodeOptions options,
                                                   final int currentDepth) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);
        final int nextDepth = currentDepth + 1;

        for (JsonNode item : arrayValue) {
            if (item.isValueNode()) {
                writer.push(depth + 2, LIST_ITEM_PREFIX
                        + PrimitiveEncoder.encodePrimitive(item, options.delimiter().toString()));
            } else if (item.isArray() && ArrayEncoder.isArrayOfPrimitives(item)) {
                final String inline = ArrayEncoder.formatInlineArray((ArrayNode) item, options.delimiter().toString(),
                                                                     null, options.lengthMarker());
                writer.push(depth + 2, LIST_ITEM_PREFIX + inline);
            } else if (item.isObject()) {
                encodeObjectAsListItem((ObjectNode) item, writer, depth + 2, options, nextDepth);
            }
        }
    }

    private static void encodeFirstValueAsObject(final String encodedKey,
                                                 final ObjectNode nestedObj,
                                                 final LineWriter writer,
                                                 final int depth,
                                                 final EncodeOptions options,
                                                 final int currentDepth) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON);
        if (!nestedObj.isEmpty()) {
            ObjectEncoder.encodeObject(nestedObj, writer, depth + 2, options, Set.of(), null, null, new HashSet<>(), currentDepth + 1);
        }
    }
}

