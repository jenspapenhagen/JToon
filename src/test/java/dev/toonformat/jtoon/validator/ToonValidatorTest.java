package dev.toonformat.jtoon.validator;

import static org.junit.jupiter.api.Assertions.*;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ToonValidator} — structural and conformance validation.
 */
@Tag("unit")
class ToonValidatorTest {

    @Test
    void validToon_passesValidation() {
        // Given
        final String toon = "id: 123\nname: Ada\nactive: true";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertTrue(result.valid());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void validToon_withTabularArray_passes() {
        // Given
        final String toon = "items[2]{id,name}:\n  1,Alice\n  2,Bob";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertTrue(result.valid());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void nullInput_passesValidation() {
        // Given
        final String toon = null;

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertTrue(result.valid());
    }

    @Test
    void blankInput_passesValidation() {
        // Given
        final String toon = "   ";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertTrue(result.valid());
    }

    @Test
    void invalidStructure_failsValidation() {
        // Given — bad indentation
        final String toon = "  badIndent";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertFalse(result.valid());
        assertFalse(result.issues().isEmpty());
        assertTrue(result.issues().get(0).contains("Structural error"));
    }

    @Test
    void trailingSpaces_detected() {
        // Given
        final String toon = "id: 123 \nname: Ada";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("Trailing space")));
    }

    @Test
    void trailingNewline_detected() {
        // Given
        final String toon = "id: 123\n";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("Trailing newline")));
    }

    @Test
    void multipleTrailingSpaces_allDetected() {
        // Given — trailing spaces on multiple lines
        final String toon = "id: 123 \nname: Ada \nactive: true";

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon);

        // Then
        assertFalse(result.valid());
        assertEquals(2, result.issues().stream().filter(i -> i.contains("Trailing space")).count());
    }

    @Test
    void isValid_returnsTrueForValidToon() {
        // Given
        final String toon = "key: value";

        // Then
        assertTrue(ToonValidator.isValid(toon));
    }

    @Test
    void isValid_returnsFalseForInvalidToon() {
        // Given
        final String toon = "  badIndent";

        // Then
        assertFalse(ToonValidator.isValid(toon));
    }

    @Test
    void validate_withCustomOptions() {
        // Given — pipe-delimited valid TOON
        final String toon = "items[2|]{a|b}:\n  1|x\n  2|y";
        final DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.PIPE);

        // When
        final ToonValidator.ValidationResult result = ToonValidator.validate(toon, options);

        // Then
        assertTrue(result.valid());
    }
}
