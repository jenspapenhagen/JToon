package com.felipestanzani.jtoon;

import com.fasterxml.jackson.databind.JsonNode;
import com.felipestanzani.jtoon.encoder.ValueEncoder;
import com.felipestanzani.jtoon.normalizer.JsonNormalizer;

/**
 * Main API for encoding Java objects and JSON to JToon format.
 * 
 * <p>
 * JToon is a structured text format that represents JSON-like data in a more
 * human-readable way, with support for tabular arrays and inline formatting.
 * </p>
 * 
 * <h2>Usage Examples:</h2>
 * 
 * <pre>{@code
 * // Encode a Java object with default options
 * String result = JToon.encode(myObject);
 * 
 * // Encode with custom options
 * EncodeOptions options = new EncodeOptions(4, Delimiter.PIPE, true);
 * String result = JToon.encode(myObject, options);
 * 
 * // Encode pre-parsed JSON
 * JsonNode json = objectMapper.readTree(jsonString);
 * String result = JToon.encode(json);
 * }</pre>
 */
public final class JToon {

    private JToon() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a Java object to JToon format using default options.
     * 
     * <p>
     * The object is first normalized (Java types are converted to JSON-compatible
     * representations), then encoded to JToon format.
     * </p>
     * 
     * @param input The object to encode (can be null)
     * @return The JToon-formatted string
     */
    public static String encode(Object input) {
        return encode(input, EncodeOptions.DEFAULT);
    }

    /**
     * Encodes a Java object to JToon format using custom options.
     * 
     * <p>
     * The object is first normalized (Java types are converted to JSON-compatible
     * representations), then encoded to JToon format.
     * </p>
     * 
     * @param input   The object to encode (can be null)
     * @param options Encoding options (indent, delimiter, length marker)
     * @return The JToon-formatted string
     */
    public static String encode(Object input, EncodeOptions options) {
        JsonNode normalizedValue = JsonNormalizer.normalize(input);
        return ValueEncoder.encodeValue(normalizedValue, options);
    }
}
