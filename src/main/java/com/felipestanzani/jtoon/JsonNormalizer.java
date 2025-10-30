package com.felipestanzani.jtoon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Normalizes Java objects to Jackson JsonNode representation.
 * Handles Java-specific types like LocalDateTime, Optional, Stream, etc.
 */
public final class JsonNormalizer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNormalizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Normalizes any Java object to a JsonNode.
     * 
     * @param value The value to normalize
     * @return The normalized JsonNode
     */
    public static JsonNode normalize(Object value) {
        return switch (value) {
            case null -> NullNode.getInstance();
            case JsonNode jsonNode -> jsonNode;
            case Optional<?> optional -> normalize(optional.orElse(null));
            case Stream<?> stream -> normalize(stream.toList());
            default -> {
                // Arrays
                if (value.getClass().isArray()) {
                    yield normalizeArray(value);
                }

                // Try primitive types
                JsonNode primitiveResult = normalizePrimitive(value);
                if (primitiveResult != null) {
                    yield primitiveResult;
                }

                // Try big numbers
                JsonNode bigNumberResult = normalizeBigNumber(value);
                if (bigNumberResult != null) {
                    yield bigNumberResult;
                }

                // Try temporal types
                JsonNode temporalResult = normalizeTemporal(value);
                if (temporalResult != null) {
                    yield temporalResult;
                }

                // Try collections
                JsonNode collectionResult = normalizeCollection(value);
                if (collectionResult != null) {
                    yield collectionResult;
                }

                // Try Jackson's default conversion for POJOs
                try {
                    yield MAPPER.valueToTree(value);
                } catch (IllegalArgumentException e) {
                    // Fallback for non-serializable objects
                    yield NullNode.getInstance();
                }
            }
        };
    }

    /**
     * Normalizes primitive types and their wrappers to JsonNode.
     */
    private static JsonNode normalizePrimitive(Object value) {
        return switch (value) {
            case String string -> TextNode.valueOf(string);
            case Boolean bool -> BooleanNode.valueOf(bool);
            case Integer integer -> IntNode.valueOf(integer);
            case Long longVal -> LongNode.valueOf(longVal);
            case Double doubleVal -> normalizeDouble(doubleVal);
            case Float floatVal -> Float.isFinite(floatVal)
                    ? FloatNode.valueOf(floatVal)
                    : NullNode.getInstance();
            case Short shortVal -> ShortNode.valueOf(shortVal);
            case Byte byteVal -> IntNode.valueOf(byteVal);
            case null, default -> null;
        };
    }

    /**
     * Normalizes Double values to JsonNode.
     * Handles special values (NaN, Infinity), zero canonicalization, and whole
     * number conversion.
     */
    private static JsonNode normalizeDouble(Double doubleVal) {
        // Handle special values
        if (!Double.isFinite(doubleVal)) {
            return NullNode.getInstance();
        }
        // Canonicalize -0 to 0
        if (doubleVal == 0.0) {
            return IntNode.valueOf(0);
        }
        // Convert whole numbers to integers for cleaner output, but only if within safe
        // range
        if (doubleVal == Math.floor(doubleVal) && !Double.isInfinite(doubleVal) &&
                doubleVal <= Long.MAX_VALUE && doubleVal >= Long.MIN_VALUE) {
            long longVal = doubleVal.longValue();
            // Verify the conversion is exact (no precision loss)
            if (longVal == doubleVal) {
                return LongNode.valueOf(longVal);
            }
        }
        return DoubleNode.valueOf(doubleVal);
    }

    /**
     * Normalizes BigInteger and BigDecimal to JsonNode.
     */
    private static JsonNode normalizeBigNumber(Object value) {
        if (value instanceof BigInteger bigInt) {
            // Try to convert to long if within safe range
            if (bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 &&
                    bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
                return LongNode.valueOf(bigInt.longValue());
            }
            // Otherwise convert to string
            return TextNode.valueOf(bigInt.toString());
        }
        if (value instanceof BigDecimal bigDec) {
            return DecimalNode.valueOf(bigDec);
        }
        return null;
    }

    /**
     * Normalizes temporal types (date/time) to JsonNode as ISO strings.
     */
    private static JsonNode normalizeTemporal(Object value) {
        return switch (value) {
            case LocalDateTime localDateTime ->
                TextNode.valueOf(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            case LocalDate localDate ->
                TextNode.valueOf(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            case LocalTime localTime ->
                TextNode.valueOf(localTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
            case ZonedDateTime zonedDateTime ->
                TextNode.valueOf(zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            case OffsetDateTime offsetDateTime ->
                TextNode.valueOf(offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            case Instant instant ->
                TextNode.valueOf(instant.toString());
            case java.util.Date date ->
                TextNode.valueOf(date.toInstant().toString());
            default -> null;
        };
    }

    /**
     * Normalizes collections (Collection and Map) to JsonNode.
     */
    private static JsonNode normalizeCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            ArrayNode arrayNode = MAPPER.createArrayNode();
            for (Object item : collection) {
                arrayNode.add(normalize(item));
            }
            return arrayNode;
        }
        if (value instanceof Map<?, ?> map) {
            ObjectNode objectNode = MAPPER.createObjectNode();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                objectNode.set(key, normalize(entry.getValue()));
            }
            return objectNode;
        }
        return null;
    }

    private static JsonNode normalizeArray(Object array) {
        ArrayNode arrayNode = MAPPER.createArrayNode();

        switch (array) {
            case int[] intArray -> addIntArray(arrayNode, intArray);
            case long[] longArray -> addLongArray(arrayNode, longArray);
            case double[] doubleArray -> addDoubleArray(arrayNode, doubleArray);
            case float[] floatArray -> addFloatArray(arrayNode, floatArray);
            case boolean[] boolArray -> addBooleanArray(arrayNode, boolArray);
            case byte[] byteArray -> addByteArray(arrayNode, byteArray);
            case short[] shortArray -> addShortArray(arrayNode, shortArray);
            case char[] charArray -> addCharArray(arrayNode, charArray);
            case Object[] objectArray -> addObjectArray(arrayNode, objectArray);
            default -> { /* No-op for unrecognized array types */ }
        }

        return arrayNode;
    }

    /**
     * Adds double array elements to an ArrayNode.
     * Handles special values (NaN, Infinity) by converting them to null.
     */
    private static void addDoubleArray(ArrayNode arrayNode, double[] array) {
        for (double val : array) {
            if (Double.isFinite(val)) {
                arrayNode.add(val);
            } else {
                arrayNode.addNull();
            }
        }
    }

    /**
     * Adds float array elements to an ArrayNode.
     * Handles special values (NaN, Infinity) by converting them to null.
     */
    private static void addFloatArray(ArrayNode arrayNode, float[] array) {
        for (float val : array) {
            if (Float.isFinite(val)) {
                arrayNode.add(val);
            } else {
                arrayNode.addNull();
            }
        }
    }

    /**
     * Adds int array elements to an ArrayNode.
     */
    private static void addIntArray(ArrayNode arrayNode, int[] array) {
        for (int val : array) {
            arrayNode.add(val);
        }
    }

    /**
     * Adds long array elements to an ArrayNode.
     */
    private static void addLongArray(ArrayNode arrayNode, long[] array) {
        for (long val : array) {
            arrayNode.add(val);
        }
    }

    /**
     * Adds boolean array elements to an ArrayNode.
     */
    private static void addBooleanArray(ArrayNode arrayNode, boolean[] array) {
        for (boolean val : array) {
            arrayNode.add(val);
        }
    }

    /**
     * Adds byte array elements to an ArrayNode.
     */
    private static void addByteArray(ArrayNode arrayNode, byte[] array) {
        for (byte val : array) {
            arrayNode.add(val);
        }
    }

    /**
     * Adds short array elements to an ArrayNode.
     */
    private static void addShortArray(ArrayNode arrayNode, short[] array) {
        for (short val : array) {
            arrayNode.add(val);
        }
    }

    /**
     * Adds char array elements to an ArrayNode.
     */
    private static void addCharArray(ArrayNode arrayNode, char[] array) {
        for (char val : array) {
            arrayNode.add(String.valueOf(val));
        }
    }

    /**
     * Adds Object array elements to an ArrayNode.
     */
    private static void addObjectArray(ArrayNode arrayNode, Object[] array) {
        for (Object val : array) {
            arrayNode.add(normalize(val));
        }
    }
}
