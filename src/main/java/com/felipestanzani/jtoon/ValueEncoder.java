package com.felipestanzani.jtoon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.felipestanzani.jtoon.Constants.*;

/**
 * Core encoding engine for converting JsonNode values to JToon format.
 */
public final class ValueEncoder {

    private ValueEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a normalized JsonNode value to JToon format.
     */
    public static String encodeValue(JsonNode value, EncodeOptions options) {
        if (value.isValueNode()) {
            return PrimitiveEncoder.encodePrimitive(value, options.delimiter().getValue());
        }

        LineWriter writer = new LineWriter(options.indent());

        if (value.isArray()) {
            encodeArray(null, (ArrayNode) value, writer, 0, options);
        } else if (value.isObject()) {
            encodeObject((ObjectNode) value, writer, 0, options);
        }

        return writer.toString();
    }

    // Object encoding

    private static void encodeObject(ObjectNode value, LineWriter writer, int depth, EncodeOptions options) {
        Iterator<String> fieldNames = value.fieldNames();

        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode fieldValue = value.get(key);
            encodeKeyValuePair(key, fieldValue, writer, depth, options);
        }
    }

    private static void encodeKeyValuePair(String key, JsonNode value, LineWriter writer, int depth,
            EncodeOptions options) {
        String encodedKey = PrimitiveEncoder.encodeKey(key);

        if (value.isValueNode()) {
            writer.push(depth, encodedKey + COLON + SPACE
                    + PrimitiveEncoder.encodePrimitive(value, options.delimiter().getValue()));
        } else if (value.isArray()) {
            encodeArray(key, (ArrayNode) value, writer, depth, options);
        } else if (value.isObject()) {
            ObjectNode objValue = (ObjectNode) value;
            if (objValue.isEmpty()) {
                writer.push(depth, encodedKey + COLON);
            } else {
                writer.push(depth, encodedKey + COLON);
                encodeObject(objValue, writer, depth + 1, options);
            }
        }
    }

    // Array encoding

    private static void encodeArray(String key, ArrayNode value, LineWriter writer, int depth, EncodeOptions options) {
        if (value.isEmpty()) {
            String header = PrimitiveEncoder.formatHeader(0, key, null, options.delimiter().getValue(),
                    options.lengthMarker());
            writer.push(depth, header);
            return;
        }

        // Primitive array
        if (isArrayOfPrimitives(value)) {
            encodeInlinePrimitiveArray(key, value, writer, depth, options);
            return;
        }

        // Array of arrays (all primitives)
        if (isArrayOfArrays(value)) {
            boolean allPrimitiveArrays = StreamSupport.stream(value.spliterator(), false)
                    .filter(JsonNode::isArray)
                    .allMatch(ValueEncoder::isArrayOfPrimitives);

            if (allPrimitiveArrays) {
                encodeArrayOfArraysAsListItems(key, value, writer, depth, options);
                return;
            }
        }

        // Array of objects
        if (isArrayOfObjects(value)) {
            var header = detectTabularHeader(value);
            if (!header.isEmpty()) {
                encodeArrayOfObjectsAsTabular(key, value, header, writer, depth, options);
            } else {
                encodeMixedArrayAsListItems(key, value, writer, depth, options);
            }
            return;
        }

        // Mixed array: fallback to expanded format
        encodeMixedArrayAsListItems(key, value, writer, depth, options);
    }

    // Type detection

    private static boolean isArrayOfPrimitives(JsonNode array) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (!item.isValueNode()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isArrayOfArrays(JsonNode array) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (!item.isArray()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isArrayOfObjects(JsonNode array) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (!item.isObject()) {
                return false;
            }
        }
        return true;
    }

    // Primitive array encoding (inline)

    private static void encodeInlinePrimitiveArray(String prefix, ArrayNode values, LineWriter writer, int depth,
            EncodeOptions options) {
        String formatted = formatInlineArray(values, options.delimiter().getValue(), prefix, options.lengthMarker());
        writer.push(depth, formatted);
    }

    private static String formatInlineArray(ArrayNode values, String delimiter, String prefix, boolean lengthMarker) {
        List<JsonNode> valueList = new ArrayList<>();
        values.forEach(valueList::add);

        String header = PrimitiveEncoder.formatHeader(values.size(), prefix, null, delimiter, lengthMarker);
        String joinedValue = PrimitiveEncoder.joinEncodedValues(valueList, delimiter);

        // Only add space if there are values
        if (values.isEmpty()) {
            return header;
        }
        return header + SPACE + joinedValue;
    }

    // Array of arrays (expanded format)

    private static void encodeArrayOfArraysAsListItems(String prefix, ArrayNode values, LineWriter writer, int depth,
            EncodeOptions options) {
        String header = PrimitiveEncoder.formatHeader(values.size(), prefix, null, options.delimiter().getValue(),
                options.lengthMarker());
        writer.push(depth, header);

        for (JsonNode arr : values) {
            if (arr.isArray() && isArrayOfPrimitives(arr)) {
                String inline = formatInlineArray((ArrayNode) arr, options.delimiter().getValue(), null,
                        options.lengthMarker());
                writer.push(depth + 1, LIST_ITEM_PREFIX + inline);
            }
        }
    }

    // Array of objects (tabular format)

    private static void encodeArrayOfObjectsAsTabular(String prefix, ArrayNode rows, List<String> header,
            LineWriter writer, int depth, EncodeOptions options) {
        String headerStr = PrimitiveEncoder.formatHeader(rows.size(), prefix, header, options.delimiter().getValue(),
                options.lengthMarker());
        writer.push(depth, headerStr);

        writeTabularRows(rows, header, writer, depth + 1, options);
    }

    private static List<String> detectTabularHeader(ArrayNode rows) {
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }

        JsonNode firstRow = rows.get(0);
        if (!firstRow.isObject()) {
            return new ArrayList<>();
        }

        ObjectNode firstObj = (ObjectNode) firstRow;
        List<String> firstKeys = new ArrayList<>();
        firstObj.fieldNames().forEachRemaining(firstKeys::add);

        if (firstKeys.isEmpty()) {
            return new ArrayList<>();
        }

        if (isTabularArray(rows, firstKeys)) {
            return firstKeys;
        }

        return new ArrayList<>();
    }

    private static boolean isTabularArray(ArrayNode rows, List<String> header) {
        for (JsonNode row : rows) {
            if (!row.isObject()) {
                return false;
            }

            ObjectNode obj = (ObjectNode) row;
            List<String> keys = new ArrayList<>();
            obj.fieldNames().forEachRemaining(keys::add);

            // All objects must have the same keys (but order can differ)
            if (keys.size() != header.size()) {
                return false;
            }

            // Check that all header keys exist in the row and all values are primitives
            for (String key : header) {
                if (!obj.has(key)) {
                    return false;
                }
                if (!obj.get(key).isValueNode()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void writeTabularRows(ArrayNode rows, List<String> header, LineWriter writer, int depth,
            EncodeOptions options) {
        for (JsonNode row : rows) {
            ObjectNode obj = (ObjectNode) row;
            List<JsonNode> values = new ArrayList<>();
            for (String key : header) {
                values.add(obj.get(key));
            }
            String joinedValue = PrimitiveEncoder.joinEncodedValues(values, options.delimiter().getValue());
            writer.push(depth, joinedValue);
        }
    }

    // Mixed array encoding (expanded format)

    private static void encodeMixedArrayAsListItems(String prefix, ArrayNode items, LineWriter writer, int depth,
            EncodeOptions options) {
        String header = PrimitiveEncoder.formatHeader(items.size(), prefix, null, options.delimiter().getValue(),
                options.lengthMarker());
        writer.push(depth, header);

        for (JsonNode item : items) {
            if (item.isValueNode()) {
                // Direct primitive as list item
                writer.push(depth + 1,
                        LIST_ITEM_PREFIX + PrimitiveEncoder.encodePrimitive(item, options.delimiter().getValue()));
            } else if (item.isArray()) {
                // Direct array as list item
                if (isArrayOfPrimitives(item)) {
                    String inline = formatInlineArray((ArrayNode) item, options.delimiter().getValue(), null,
                            options.lengthMarker());
                    writer.push(depth + 1, LIST_ITEM_PREFIX + inline);
                }
            } else if (item.isObject()) {
                // Object as list item
                encodeObjectAsListItem((ObjectNode) item, writer, depth + 1, options);
            }
        }
    }

    private static void encodeObjectAsListItem(ObjectNode obj, LineWriter writer, int depth, EncodeOptions options) {
        List<String> keys = new ArrayList<>();
        obj.fieldNames().forEachRemaining(keys::add);

        if (keys.isEmpty()) {
            writer.push(depth, LIST_ITEM_MARKER);
            return;
        }

        // First key-value on the same line as "- "
        String firstKey = keys.getFirst();
        JsonNode firstValue = obj.get(firstKey);
        encodeFirstKeyValue(firstKey, firstValue, writer, depth, options);

        // Remaining keys on indented lines
        for (int i = 1; i < keys.size(); i++) {
            String key = keys.get(i);
            encodeKeyValuePair(key, obj.get(key), writer, depth + 1, options);
        }
    }

    private static void encodeFirstKeyValue(String key, JsonNode value, LineWriter writer, int depth,
            EncodeOptions options) {
        String encodedKey = PrimitiveEncoder.encodeKey(key);

        if (value.isValueNode()) {
            encodeFirstValueAsPrimitive(encodedKey, value, writer, depth, options);
        } else if (value.isArray()) {
            encodeFirstValueAsArray(key, encodedKey, (ArrayNode) value, writer, depth, options);
        } else if (value.isObject()) {
            encodeFirstValueAsObject(encodedKey, (ObjectNode) value, writer, depth, options);
        }
    }

    private static void encodeFirstValueAsPrimitive(String encodedKey, JsonNode value, LineWriter writer, int depth,
            EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON + SPACE
                + PrimitiveEncoder.encodePrimitive(value, options.delimiter().getValue()));
    }

    private static void encodeFirstValueAsArray(String key, String encodedKey, ArrayNode arrayValue, LineWriter writer,
            int depth, EncodeOptions options) {
        if (isArrayOfPrimitives(arrayValue)) {
            encodeFirstArrayAsPrimitives(key, arrayValue, writer, depth, options);
        } else if (isArrayOfObjects(arrayValue)) {
            encodeFirstArrayAsObjects(key, encodedKey, arrayValue, writer, depth, options);
        } else {
            encodeFirstArrayAsComplex(encodedKey, arrayValue, writer, depth, options);
        }
    }

    private static void encodeFirstArrayAsPrimitives(String key, ArrayNode arrayValue, LineWriter writer, int depth,
            EncodeOptions options) {
        String formatted = formatInlineArray(arrayValue, options.delimiter().getValue(), key, options.lengthMarker());
        writer.push(depth, LIST_ITEM_PREFIX + formatted);
    }

    private static void encodeFirstArrayAsObjects(String key, String encodedKey, ArrayNode arrayValue,
            LineWriter writer, int depth, EncodeOptions options) {
        List<String> header = detectTabularHeader(arrayValue);
        if (!header.isEmpty()) {
            String headerStr = PrimitiveEncoder.formatHeader(arrayValue.size(), key, header,
                    options.delimiter().getValue(), options.lengthMarker());
            writer.push(depth, LIST_ITEM_PREFIX + headerStr);
            writeTabularRows(arrayValue, header, writer, depth + 1, options);
        } else {
            writer.push(depth,
                    LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);
            for (JsonNode item : arrayValue) {
                if (item.isObject()) {
                    encodeObjectAsListItem((ObjectNode) item, writer, depth + 1, options);
                }
            }
        }
    }

    private static void encodeFirstArrayAsComplex(String encodedKey, ArrayNode arrayValue, LineWriter writer, int depth,
            EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);

        for (JsonNode item : arrayValue) {
            if (item.isValueNode()) {
                writer.push(depth + 1, LIST_ITEM_PREFIX
                        + PrimitiveEncoder.encodePrimitive(item, options.delimiter().getValue()));
            } else if (item.isArray() && isArrayOfPrimitives(item)) {
                String inline = formatInlineArray((ArrayNode) item, options.delimiter().getValue(), null,
                        options.lengthMarker());
                writer.push(depth + 1, LIST_ITEM_PREFIX + inline);
            } else if (item.isObject()) {
                encodeObjectAsListItem((ObjectNode) item, writer, depth + 1, options);
            }
        }
    }

    private static void encodeFirstValueAsObject(String encodedKey, ObjectNode nestedObj, LineWriter writer, int depth,
            EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON);
        if (!nestedObj.isEmpty()) {
            encodeObject(nestedObj, writer, depth + 2, options);
        }
    }
}
