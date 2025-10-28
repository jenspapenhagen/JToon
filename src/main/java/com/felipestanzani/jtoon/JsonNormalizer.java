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
        // null
        if (value == null) {
            return NullNode.getInstance();
        }

        // Already a JsonNode
        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }

        // Try primitive types
        JsonNode primitiveResult = normalizePrimitive(value);
        if (primitiveResult != null) {
            return primitiveResult;
        }

        // Try big numbers
        JsonNode bigNumberResult = normalizeBigNumber(value);
        if (bigNumberResult != null) {
            return bigNumberResult;
        }

        // Try temporal types
        JsonNode temporalResult = normalizeTemporal(value);
        if (temporalResult != null) {
            return temporalResult;
        }

        // Optional → unwrap
        if (value instanceof Optional<?> optional) {
            return normalize(optional.orElse(null));
        }

        // Stream → materialize to array
        if (value instanceof Stream<?> stream) {
            return normalize(stream.toList());
        }

        // Arrays
        if (value.getClass().isArray()) {
            return normalizeArray(value);
        }

        // Try collections
        JsonNode collectionResult = normalizeCollection(value);
        if (collectionResult != null) {
            return collectionResult;
        }

        // Try Jackson's default conversion for POJOs
        try {
            return MAPPER.valueToTree(value);
        } catch (IllegalArgumentException e) {
            // Fallback for non-serializable objects
            return NullNode.getInstance();
        }
    }

    /**
     * Normalizes primitive types and their wrappers to JsonNode.
     */
    private static JsonNode normalizePrimitive(Object value) {
        if (value instanceof String string) {
            return TextNode.valueOf(string);
        }
        if (value instanceof Boolean bool) {
            return BooleanNode.valueOf(bool);
        }
        if (value instanceof Integer integer) {
            return IntNode.valueOf(integer);
        }
        if (value instanceof Long longVal) {
            return LongNode.valueOf(longVal);
        }
        if (value instanceof Double doubleVal) {
            return normalizeDouble(doubleVal);
        }
        if (value instanceof Float floatVal) {
            if (!Float.isFinite(floatVal)) {
                return NullNode.getInstance();
            }
            return FloatNode.valueOf(floatVal);
        }
        if (value instanceof Short shortVal) {
            return ShortNode.valueOf(shortVal);
        }
        if (value instanceof Byte byteVal) {
            return IntNode.valueOf(byteVal);
        }
        return null;
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
        if (value instanceof LocalDateTime localDateTime) {
            return TextNode.valueOf(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (value instanceof LocalDate localDate) {
            return TextNode.valueOf(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (value instanceof LocalTime localTime) {
            return TextNode.valueOf(localTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return TextNode.valueOf(zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return TextNode.valueOf(offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (value instanceof Instant instant) {
            return TextNode.valueOf(instant.toString());
        }
        if (value instanceof java.util.Date date) {
            return TextNode.valueOf(date.toInstant().toString());
        }
        return null;
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

        // Handle primitive arrays
        if (array instanceof int[] intArray) {
            addIntArray(arrayNode, intArray);
        } else if (array instanceof long[] longArray) {
            addLongArray(arrayNode, longArray);
        } else if (array instanceof double[] doubleArray) {
            addDoubleArray(arrayNode, doubleArray);
        } else if (array instanceof float[] floatArray) {
            addFloatArray(arrayNode, floatArray);
        } else if (array instanceof boolean[] boolArray) {
            addBooleanArray(arrayNode, boolArray);
        } else if (array instanceof byte[] byteArray) {
            addByteArray(arrayNode, byteArray);
        } else if (array instanceof short[] shortArray) {
            addShortArray(arrayNode, shortArray);
        } else if (array instanceof char[] charArray) {
            addCharArray(arrayNode, charArray);
        } else if (array instanceof Object[] objectArray) {
            addObjectArray(arrayNode, objectArray);
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
