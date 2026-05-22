package dev.toonformat.jtoon;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Test POJOs (records) for JToon encoding tests.
 * These records cover various scenarios including simple fields, nested structures,
 * collections, and Jackson annotations.
 */
public class TestPojos {

    // ===== Simple Records =====

    /**
     * Simple person record with basic fields.
     *
     * @param name   the person's name
     * @param age    the person's age
     * @param active whether the person is active
     */
    public record Person(String name, int age, boolean active) {
    }

    /**
     * Simple product record with various numeric types.
     *
     * @param id      the product id
     * @param name    the product name
     * @param price   the product price
     * @param inStock whether the product is in stock
     */
    public record Product(int id, String name, double price, boolean inStock) {
    }

    /**
     * Record with nullable fields to test null handling.
     *
     * @param text  nullable text field
     * @param count nullable Integer field
     * @param flag  nullable Boolean field
     */
    public record NullableData(String text, Integer count, Boolean flag) {
    }

    // ===== Nested Records =====

    /**
     * Address record for nested structure tests.
     *
     * @param street  the street name
     * @param city    the city name
     * @param zipCode the zip code
     */
    public record Address(String street, String city, String zipCode) {
    }

    /**
     * Employee record containing a nested Address.
     *
     * @param name    the employee name
     * @param id      the employee id
     * @param address the employee address
     */
    public record Employee(String name, int id, Address address) {
    }

    /**
     * Deeply nested structure for testing multiple levels.
     *
     * @param name    the company name
     * @param manager the company manager
     */
    public record Company(String name, Employee manager) {
    }

    // ===== Collection Records =====

    /**
     * Record with a list of primitives.
     *
     * @param owner     the owner name
     * @param skillList the list of skills
     */
    public record Skills(String owner, List<String> skillList) {
    }

    /**
     * Record with a list of objects (for tabular format testing).
     *
     * @param name    the team name
     * @param members the list of team members
     */
    public record Team(String name, List<Person> members) {
    }

    /**
     * Record with Map fields.
     *
     * @param name     the configuration name
     * @param settings the configuration settings map
     */
    public record Configuration(String name, Map<String, Object> settings) {
    }

    /**
     * Record with empty collections.
     *
     * @param emptyList the empty list
     * @param emptyMap  the empty map
     */
    public record EmptyCollections(List<String> emptyList, Map<String, String> emptyMap) {
    }

    /**
     * Record with multiple collection types.
     *
     * @param numbers list of integers
     * @param tags    list of strings
     * @param counts  map of string to integer counts
     */
    public record MultiCollection(List<Integer> numbers, List<String> tags, Map<String, Integer> counts) {
    }

    // ===== Annotated Records =====

    /**
     * Record with @JsonProperty annotation for field name mapping.
     *
     * @param id    the product id (mapped from product_id)
     * @param name  the product name (mapped from product_name)
     * @param price the product price
     */
    public record AnnotatedProduct(
        @JsonProperty("product_id") int id,
        @JsonProperty("product_name") String name,
        double price) {
    }

    /**
     * Record with @JsonIgnore annotation to exclude fields.
     *
     * @param publicField the public data field
     * @param secretField the secret field (excluded via @JsonIgnore)
     * @param version     the data version
     */
    public record SecureData(
        String publicField,
        @JsonIgnore String secretField,
        int version) {
    }

    /**
     * Record with multiple Jackson annotations.
     *
     * @param id       the user id (mapped from user_id)
     * @param name     the user name
     * @param internal the internal field (excluded via @JsonIgnore)
     * @param active   whether the user is active (mapped from is_active)
     */
    public record ComplexAnnotated(
        @JsonProperty("user_id") int id,
        String name,
        @JsonIgnore String internal,
        @JsonProperty("is_active") boolean active) {
    }

    /**
     * Record combining nested structure with annotations.
     *
     * @param id      the employee id (mapped from emp_id)
     * @param name    the employee name (mapped from full_name)
     * @param address the employee address
     * @param ssn     the social security number (excluded via @JsonIgnore)
     */
    public record AnnotatedEmployee(
        @JsonProperty("emp_id") int id,
        @JsonProperty("full_name") String name,
        Address address,
        @JsonIgnore String ssn) {
    }

    /**
     * OrderEmployee record containing a nested Address.
     *
     * @param name    the employee name
     * @param id      the employee id
     * @param address the employee address
     */
    @JsonPropertyOrder({"id", "name"})
    public record OrderEmployee(String name, int id, Address address) {
    }

    /**
     * Class with Jackson Annotations.
     */
    public static class FullEmployee {
        public final AnnotatedEmployee employee;
        private final Map<String, String> properties;

        public FullEmployee(final AnnotatedEmployee emp, final Map<String, String> props) {
            this.employee = emp;
            this.properties = props;
        }

        @JsonAnyGetter
        public Map<String, String> getProperties() {
            return properties;
        }

        public AnnotatedEmployee employee() {
            return employee;
        }
    }

    /**
     * Record for checking the field order in the output.
     *
     * @param no                    the hotel number
     * @param hotelId               the hotel id
     * @param hotelName             the hotel name
     * @param hotelBrand            the hotel brand
     * @param hotelCategory         the hotel category
     * @param hotelPrice            the hotel price
     * @param hotelAddressDistance  the hotel address distance
     */
    public record HotelInfoLlmRerankDto(String no,
                                        String hotelId,
                                        String hotelName,
                                        String hotelBrand,
                                        String hotelCategory,
                                        String hotelPrice,
                                        String hotelAddressDistance) {
    }

    public record UserDto(Integer id, String firstName, String lastName, java.sql.Date lastLogin) {

    }

    /**
     * Custom Serializer for HotelInfoLlmRerankDto.
     */
    public static class CustomHotelInfoLlmRerankDtoSerializer extends StdSerializer<HotelInfoLlmRerankDto> {

        public CustomHotelInfoLlmRerankDtoSerializer() {
            this(null);
        }

        public CustomHotelInfoLlmRerankDtoSerializer(final Class<HotelInfoLlmRerankDto> t) {
            super(t);
        }

        @Override
        public void serialize(final HotelInfoLlmRerankDto value, final JsonGenerator jsonGenerator,
                final SerializationContext provider) {
            jsonGenerator.writeString(value.hotelId);
        }
    }

    /**
     * POJO with custom serializer.
     */
    public static class HotelInfoLlmRerankDtoWithSerializer {
        public String name;

        @JsonSerialize(using = CustomHotelInfoLlmRerankDtoSerializer.class)
        public HotelInfoLlmRerankDto hotelInfo;

        public HotelInfoLlmRerankDtoWithSerializer(final String nameVal, final HotelInfoLlmRerankDto hotelInfoVal) {
            this.name = nameVal;
            this.hotelInfo = hotelInfoVal;
        }
    }


}

