package dev.toonformat.jtoon.normalizer;

import dev.toonformat.jtoon.util.ObjectMapperSingleton;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.newSetFromMap;

public final class JsonNormalizer {

    public static final ObjectMapper MAPPER = ObjectMapperSingleton.getInstance();

    private static final int MAX_DEPTH = 512;
    private static final int MAX_STREAM_ELEMENTS = 10000;

    private static final List<Function<Object, JsonNode>> NORMALIZERS = List.of(
        JsonNormalizer::tryNormalizePrimitive,
        JsonNormalizer::tryNormalizeBigNumber,
        JsonNormalizer::tryNormalizeTemporal,
        JsonNormalizer::tryNormalizeCollection,
        JsonNormalizer::tryNormalizePojo);

    private JsonNormalizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static JsonNode parse(final String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON string cannot be null");
        }
        if (json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be blank");
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    public static JsonNode normalize(final Object value) {
        return normalizeInternal(value, 0, new IdentityHashMap<>());
    }

    private static JsonNode normalizeInternal(final Object value, final int depth, final IdentityHashMap<Object, Boolean> visited) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
        if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        } else if (value instanceof Optional<?>) {
            return normalizeInternal(((Optional<?>) value).orElse(null), depth, visited);
        } else if (value instanceof Stream<?>) {
            Stream<?> stream = (Stream<?>) value;
            List<?> list = stream.limit(MAX_STREAM_ELEMENTS + 1).toList();
            if (list.size() > MAX_STREAM_ELEMENTS) {
                throw new IllegalArgumentException("Stream has more than " + MAX_STREAM_ELEMENTS + " elements");
            }
            return normalizeInternal(list, depth, visited);
        } else if (value.getClass().isArray()) {
            return normalizeArray(value, depth, visited);
        } else {
            return normalizeWithStrategy(value, depth, visited);
        }
    }

    private static JsonNode normalizeWithStrategy(final Object value, final int depth, final IdentityHashMap<Object, Boolean> visited) {
        return NORMALIZERS.stream()
            .map(normalizer -> normalizer.apply(value))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseGet(NullNode::getInstance);
    }

    private static JsonNode tryNormalizePrimitive(final Object value) {
        if (value instanceof String stringValue) {
            return StringNode.valueOf(stringValue);
        } else if (value instanceof Boolean boolValue) {
            return BooleanNode.valueOf(boolValue);
        } else if (value instanceof Integer intValue) {
            return IntNode.valueOf(intValue);
        } else if (value instanceof Long longValue) {
            return LongNode.valueOf(longValue);
        } else if (value instanceof Double doubleValue) {
            return normalizeDouble(doubleValue);
        } else if (value instanceof Float floatValue) {
            return normalizeFloat(floatValue);
        } else if (value instanceof Short shortValue) {
            return ShortNode.valueOf(shortValue);
        } else if (value instanceof Byte byteValue) {
            return IntNode.valueOf(byteValue);
        } else {
            return null;
        }
    }

    private static JsonNode normalizeDouble(final Double value) {
        if (!Double.isFinite(value)) {
            return NullNode.getInstance();
        }
        if (value == 0.0) {
            return IntNode.valueOf(0);
        }
        return tryConvertToLong(value)
            .orElseGet(() -> DoubleNode.valueOf(value));
    }

    private static JsonNode normalizeFloat(final Float value) {
        return Float.isFinite(value)
            ? FloatNode.valueOf(value)
            : NullNode.getInstance();
    }

    private static Optional<JsonNode> tryConvertToLong(final Double value) {
        if (value != Math.floor(value)) {
            return Optional.empty();
        }
        if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
            return Optional.empty();
        }
        final long longVal = value.longValue();
        return Optional.of(LongNode.valueOf(longVal));
    }

    private static JsonNode tryNormalizeBigNumber(final Object value) {
        if (value instanceof BigInteger bigInteger) {
            return normalizeBigInteger(bigInteger);
        } else if (value instanceof BigDecimal bigDecimal) {
            return DecimalNode.valueOf(bigDecimal);
        } else {
            return null;
        }
    }

    private static JsonNode normalizeBigInteger(final BigInteger value) {
        final boolean fitsInLong = value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
            && value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0;
        return fitsInLong
            ? LongNode.valueOf(value.longValue())
            : StringNode.valueOf(value.toString());
    }

    private static JsonNode tryNormalizeTemporal(final Object value) {
        if (value instanceof LocalDateTime ldt) {
            return formatTemporal(ldt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (value instanceof LocalDate ld) {
            return formatTemporal(ld, DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (value instanceof LocalTime lt) {
            return formatTemporal(lt, DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (value instanceof ZonedDateTime zonedDateTime) {
            return formatTemporal(zonedDateTime, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } else if (value instanceof OffsetDateTime offsetDateTime) {
            return formatTemporal(offsetDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else if (value instanceof Calendar calendar) {
            return StringNode.valueOf(calendar.toInstant().toString());
        } else if (value instanceof Instant instant) {
            return StringNode.valueOf(instant.toString());
        } else if (value instanceof java.sql.Timestamp timestamp) {
            return formatTemporal(timestamp.toLocalDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (value instanceof java.sql.Date date) {
            return formatTemporal(date.toLocalDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (value instanceof java.sql.Time time) {
            return formatTemporal(time.toLocalTime(), DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (value instanceof Date date) {
            return StringNode.valueOf(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()).toString());
        } else {
            return null;
        }
    }

    private static <T> JsonNode formatTemporal(final T temporal, final DateTimeFormatter formatter) {
        return StringNode.valueOf(formatter.format((java.time.temporal.TemporalAccessor) temporal));
    }

    private static JsonNode tryNormalizeCollection(final Object value) {
        if (value instanceof Collection<?>) {
            return normalizeCollection((Collection<?>) value, 0, new IdentityHashMap<>());
        } else if (value instanceof Map<?, ?>) {
            return normalizeMap((Map<?, ?>) value, 0, new IdentityHashMap<>());
        } else {
            return null;
        }
    }

    private static ArrayNode normalizeCollection(final Collection<?> collection, final int depth, final IdentityHashMap<Object, Boolean> visited) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
        if (visited.containsKey(collection)) {
            throw new IllegalArgumentException("Circular reference detected in collection");
        }
        visited.put(collection, Boolean.TRUE);
        final ArrayNode arrayNode = MAPPER.createArrayNode();
        for (Object item : collection) {
            arrayNode.add(normalizeInternal(item, depth + 1, visited));
        }
        return arrayNode;
    }

    private static ObjectNode normalizeMap(final Map<?, ?> map, final int depth, final IdentityHashMap<Object, Boolean> visited) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
        if (visited.containsKey(map)) {
            throw new IllegalArgumentException("Circular reference detected in map");
        }
        visited.put(map, Boolean.TRUE);
        final ObjectNode objectNode = MAPPER.createObjectNode();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            objectNode.set(String.valueOf(entry.getKey()), normalizeInternal(entry.getValue(), depth + 1, visited));
        }
        return objectNode;
    }

    private static JsonNode tryNormalizePojo(final Object value) {
        try {
            return MAPPER.valueToTree(value);
        } catch (Exception e) {
            return NullNode.getInstance();
        }
    }

    private static JsonNode normalizeArray(final Object array, final int depth, final IdentityHashMap<Object, Boolean> visited) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
        if (array instanceof int[] intArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < intArr.length; i++) {
                node.add(IntNode.valueOf(intArr[i]));
            }
            return node;
        } else if (array instanceof long[] longArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < longArr.length; i++) {
                node.add(LongNode.valueOf(longArr[i]));
            }
            return node;
        } else if (array instanceof double[] doubleArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < doubleArr.length; i++) {
                final double val = doubleArr[i];
                node.add(Double.isFinite(val) ? DoubleNode.valueOf(val) : NullNode.getInstance());
            }
            return node;
        } else if (array instanceof float[] floatArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < floatArr.length; i++) {
                final float val = floatArr[i];
                node.add(Float.isFinite(val) ? FloatNode.valueOf(val) : NullNode.getInstance());
            }
            return node;
        } else if (array instanceof boolean[] boolArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < boolArr.length; i++) {
                node.add(BooleanNode.valueOf(boolArr[i]));
            }
            return node;
        } else if (array instanceof byte[] byteArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < byteArr.length; i++) {
                node.add(IntNode.valueOf(byteArr[i]));
            }
            return node;
        } else if (array instanceof short[] shortArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < shortArr.length; i++) {
                node.add(ShortNode.valueOf(shortArr[i]));
            }
            return node;
        } else if (array instanceof char[] charArr) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < charArr.length; i++) {
                node.add(StringNode.valueOf(String.valueOf(charArr[i])));
            }
            return node;
        } else if (array instanceof Object[] objArr) {
            if (visited.containsKey(array)) {
                throw new IllegalArgumentException("Circular reference detected in array");
            }
            visited.put(array, Boolean.TRUE);
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i = 0; i < objArr.length; i++) {
                node.add(normalizeInternal(objArr[i], depth + 1, visited));
            }
            return node;
        } else {
            return MAPPER.createArrayNode();
        }
    }
}
