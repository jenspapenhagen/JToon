package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import dev.toonformat.jtoon.encoder.PrimitiveEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PrimitiveDecoder utility class.
 * Tests decoding of primitive values, keys, and header formatting.
 */
@Tag("unit")
class PrimitiveDecoderTest {

    private static final long EXPECTED_LONG_VALUE = 42L;
    private static final double DELTA = 0.000001;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<PrimitiveEncoder> constructor = PrimitiveEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    void givenNullInput_whenParse_thenReturnsEmptyString() {
        // Given
        final String input = null;

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void givenEmptyInput_whenParse_thenReturnsEmptyString() {
        // Given
        final String input = "";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("", result);
    }

    @Test
    void givenNullLiteral_whenParse_thenReturnsNull() {
        // Given
        final String input = "null";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNull(result);
    }

    @Test
    void givenTrueLiteral_whenParse_thenReturnsBooleanTrue() {
        // Given
        final String input = "true";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(true, result);
    }

    @Test
    void givenFalseLiteral_whenParse_thenReturnsBooleanFalse() {
        // Given
        final String input = "false";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(false, result);
    }

    @Test
    void givenQuotedString_whenParse_thenReturnsUnescapedString() {
        // Given
        final String input = "\"hello\\nworld\"";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("hello\nworld", result);

    }

    @Test
    void givenOctalLikeNumber_whenParse_thenReturnsString() {
        // Given
        final String input = "0123"; // starts with "0" + non-decimal

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("0123", result);
    }

    @Test
    void givenZeroOrExplicitZeroFormats_whenParse_thenParsesAsNumber() {
        // Given
        final String input = "0.0";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(0L, result);  // negative/positive zero → 0L
    }

    @Test
    void givenIntegerString_whenParse_thenReturnsLong() {
        // Given
        final String input = "42";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(EXPECTED_LONG_VALUE, result);
    }

    @Test
    void givenDecimalNumber_whenParse_thenReturnsDouble() {
        // Given
        final String input = "3.14";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        final double expectedDouble = 3.14;
        assertEquals(expectedDouble, (Double) result, DELTA);
    }

    @Test
    void givenExponentNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = "1e3";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        final double exponentResult = 1000.0;
        assertEquals(exponentResult, (Long) result, DELTA);
    }

    @Test
    void givenDoubleRepresentingWholeNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = "42.0";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(EXPECTED_LONG_VALUE, result); // should convert to Long
    }

    @Test
    void givenNegativeZeroDouble_whenParse_thenReturnsZeroLong() {
        // Given
        final String input = "-0.0";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(0L, result);
    }

    @Test
    void givenOctalNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = "07";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("07", result.toString());
    }

    @Test
    void givenNumberWithLeadingZero_whenParse_thenReturnsLong() {
        // Given
        final String input = "0.7";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("0.7", result.toString());
    }

    @Test
    void givenNumberWithLeadingZeroOutsideTheOctalRange_whenParse_thenReturnsLong() {
        // Given
        final String input = "0.9";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("0.9", result.toString());
    }

    @Test
    void given08_whenParse_thenReturnsString() {
        // Given
        final String input = "08";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("08", result);
    }

    @Test
    void given09_whenParse_thenReturnsString() {
        // Given
        final String input = "09";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("09", result);
    }

    @Test
    void given00_whenParse_thenReturnsString() {
        // Given
        final String input = "00";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("00", result);
    }

    @Test
    void givenNegativeLeadingZero_whenParse_thenReturnsString() {
        // Given
        final String input = "-07";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("-07", result);
    }

    @Test
    void givenLeadingZeroDecimal_whenParse_thenReturnsNumber() {
        // Given
        final String input = "0.5";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        final double expectedDouble = 0.5;
        assertEquals(expectedDouble, (Double) result, DELTA);
    }

    @Test
    void givenLeadingZeroExponent_whenParse_thenReturnsNumber() {
        // Given — "0e1" = 0 × 10^1 = 0, which is a whole number → Long
        final String input = "0e1";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals(0L, result);
    }

    @Test
    void givenMinLongNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = String.valueOf(Long.MIN_VALUE);

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("-9223372036854775808", result.toString());
    }

    @Test
    void givenMaxLongNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = String.valueOf(Long.MAX_VALUE);

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("9223372036854775807", result.toString());
    }

    @Test
    void givenSmallerMinLongNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = String.valueOf(Long.MIN_VALUE - 1);

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("9223372036854775807", result.toString());
    }

    @Test
    void givenBiggerMaxLongNumber_whenParse_thenReturnsLong() {
        // Given
        final String input = String.valueOf(Long.MAX_VALUE + 1);

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("-9223372036854775808", result.toString());
    }


    @Test
    void givenInvalidNumber_whenParse_thenReturnsOriginalString() {
        // Given
        final String input = "123abc";

        // When
        final Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("123abc", result);
    }

    @Test
    void testing_SkipTrailingZeros() throws Exception {
        // Given
        final String input = "10.000";

        // When
        final String result = (String) invokePrivateStatic("stripTrailingZeros", new Class[]{String.class}, input);

        // Then
        assertEquals("10", result);
    }

    @Test
    void testing_SkipTrailingZeros_WithSmallNUmber() throws Exception {
        // Given
        final String input = "1.0";

        // When
        final String result = (String) invokePrivateStatic("stripTrailingZeros", new Class[]{String.class}, input);

        // Then
        assertEquals("1", result);
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = PrimitiveEncoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }
}
