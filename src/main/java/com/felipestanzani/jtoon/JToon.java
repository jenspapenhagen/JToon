package com.felipestanzani.jtoon;

import com.felipestanzani.jtoon.encoder.ValueEncoder;
import com.felipestanzani.jtoon.normalizer.JsonNormalizer;
import tools.jackson.databind.JsonNode;

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
 * 
 * // Encode a plain JSON string directly
 * String result = JToon.encodeJson("{\"id\":123,\"name\":\"Ada\"}");
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

    /**
     * Encodes a plain JSON string to TOON format using default options.
     *
     * <p>
     * This is a convenience overload that parses the JSON string and encodes it
     * without requiring callers to create a {@code JsonNode} or intermediate
     * objects.
     * </p>
     *
     * @param json The JSON string to encode (must be valid JSON)
     * @return The TOON-formatted string
     * @throws IllegalArgumentException if the input is not valid JSON
     */
    public static String encodeJson(String json) {
        return encodeJson(json, EncodeOptions.DEFAULT);
    }

    /**
     * Encodes a plain JSON string to TOON format using custom options.
     *
     * <p>
     * Parsing is delegated to
     * {@link com.felipestanzani.jtoon.normalizer.JsonNormalizer#parse(String)}
     * to maintain separation of concerns.
     * </p>
     *
     * @param json    The JSON string to encode (must be valid JSON)
     * @param options Encoding options (indent, delimiter, length marker)
     * @return The TOON-formatted string
     * @throws IllegalArgumentException if the input is not valid JSON
     */
    public static String encodeJson(String json, EncodeOptions options) {
        JsonNode parsed = JsonNormalizer.parse(json);
        return ValueEncoder.encodeValue(parsed, options);
    }
}
