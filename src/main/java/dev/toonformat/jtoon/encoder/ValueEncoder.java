package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;

public final class ValueEncoder {

    private static final int MAX_ENCODE_DEPTH = 1024;

    private ValueEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String encodeValue(final JsonNode value, final EncodeOptions options) {
        if (value == null || value.isNull()) {
            return "null";
        }

        if (value.isValueNode()) {
            return PrimitiveEncoder.encodePrimitive(value, options.delimiter().toString());
        }

        final LineWriter writer = new LineWriter(options.indent());

        if (value.isArray()) {
            ArrayEncoder.encodeArray(null, (ArrayNode) value, writer, 0, options, 0);
        } else if (value.isObject()) {
            final Set<String> jsonNodes = new HashSet<>(value.propertyNames());
            ObjectEncoder.encodeObject((ObjectNode) value, writer, 0, options, jsonNodes, null, null, new HashSet<>(), 0);
        }

        return writer.toString();
    }
}
