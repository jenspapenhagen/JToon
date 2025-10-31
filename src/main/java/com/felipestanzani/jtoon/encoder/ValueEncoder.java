package com.felipestanzani.jtoon.encoder;

import com.felipestanzani.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Core encoding orchestrator for converting JsonNode values to TOON format.
 * Delegates to specialized encoders based on node type.
 */
public final class ValueEncoder {

    private ValueEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a normalized JsonNode value to TOON format.
     * 
     * @param value   The JsonNode to encode
     * @param options Encoding options (indent, delimiter, length marker)
     * @return The TOON-formatted string
     */
    public static String encodeValue(JsonNode value, EncodeOptions options) {
        // Handle primitive values directly
        if (value.isValueNode()) {
            return PrimitiveEncoder.encodePrimitive(value, options.delimiter().getValue());
        }

        // Complex values need a LineWriter for indentation
        LineWriter writer = new LineWriter(options.indent());

        if (value.isArray()) {
            ArrayEncoder.encodeArray(null, (ArrayNode) value, writer, 0, options);
        } else if (value.isObject()) {
            ObjectEncoder.encodeObject((ObjectNode) value, writer, 0, options);
        }

        return writer.toString();
    }
}
