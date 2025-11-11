package com.felipestanzani.jtoon.decoder;

import com.felipestanzani.jtoon.DecodeOptions;
import com.felipestanzani.jtoon.util.StringEscaper;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main decoder for converting TOON-formatted strings to Java objects.
 *
 * <p>
 * Implements a line-by-line parser with indentation-based depth tracking.
 * Delegates primitive type inference to {@link PrimitiveDecoder}.
 * </p>
 *
 * <h2>Parsing Strategy:</h2>
 * <ul>
 * <li>Split input into lines</li>
 * <li>Track current line position and indentation depth</li>
 * <li>Use regex patterns to detect structure (arrays, objects, primitives)</li>
 * <li>Recursively process nested structures</li>
 * </ul>
 *
 * @see DecodeOptions
 * @see PrimitiveDecoder
 */
public final class ValueDecoder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Matches standalone array headers: [3], [#2], [3\t], [2|]
     */
    private static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)\\d+[\\t|]?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:
     */
    private static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)\\d+[\\t|]?]\\{(.+)}:");

    /**
     * Matches keyed array headers: items[2]{id,name}: or tags[3]: or data[4]{id}:
     * Captures: group(1)=key, group(2)=#marker, group(3)=optional field spec
     */
    private static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile("^(.+?)\\[(#?)\\d+[\\t|]?](\\{[^}]+})?:.*$");

    private ValueDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decodes a TOON-formatted string to a Java object.
     *
     * @param toon    TOON-formatted input string
     * @param options parsing options (delimiter, indentation, strict mode)
     * @return parsed object (Map, List, primitive, or null)
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static Object decode(String toon, DecodeOptions options) {
        if (toon == null || toon.trim().isEmpty()) {
            return null;
        }

        String trimmed = toon.trim();
        Parser parser = new Parser(trimmed, options);
        return parser.parseValue();
    }

    /**
     * Decodes a TOON-formatted string directly to a JSON string using custom
     * options.
     *
     * <p>
     * This is a convenience method that decodes TOON to Java objects and then
     * serializes them to JSON.
     * </p>
     *
     * @param toon    The TOON-formatted string to decode
     * @param options Decoding options (indent, delimiter, strict mode)
     * @return JSON string representation
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static String decodeToJson(String toon, DecodeOptions options) {
        try {
            Object decoded = ValueDecoder.decode(toon, options);
            return OBJECT_MAPPER.writeValueAsString(decoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert decoded value to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Inner parser class managing line-by-line parsing state.
     * Maintains currentLine index and uses recursive descent for nested structures.
     */
    private static class Parser {
        private final String[] lines;
        private final DecodeOptions options;
        private final String delimiter;
        private int currentLine = 0;

        Parser(String toon, DecodeOptions options) {
            this.lines = toon.split("\n", -1);
            this.options = options;
            this.delimiter = options.delimiter().getValue();
        }

        /**
         * Parses the current line at root level (depth 0).
         * Routes to appropriate handler based on line content.
         */
        Object parseValue() {
            if (currentLine >= lines.length) {
                return null;
            }

            String line = lines[currentLine];
            int depth = getDepth(line);

            if (depth > 0) {
                if (options.strict()) {
                    throw new IllegalArgumentException("Unexpected indentation at line " + currentLine);
                }
                return null;
            }

            String content = line.substring(depth * options.indent());

            // Handle standalone arrays: [2]:
            if (content.startsWith("[")) {
                return parseArray(content, depth);
            }

            // Handle keyed arrays: items[2]{id,name}:
            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
            if (keyedArray.matches()) {
                String key = StringEscaper.unescape(keyedArray.group(1).trim());
                String arrayHeader = content.substring(keyedArray.group(1).length());

                var arrayValue = parseArray(arrayHeader, depth);
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put(key, arrayValue);
                return obj;
            }

            // Handle key-value pairs: name: Ada
            int colonIdx = findUnquotedColon(content);
            if (colonIdx > 0) {
                String key = content.substring(0, colonIdx).trim();
                String value = content.substring(colonIdx + 1).trim();
                return parseKeyValuePair(key, value, depth, depth == 0);
            }

            // Bare scalar value
            currentLine++;
            return PrimitiveDecoder.parse(content);
        }

        /**
         * Parses array from header string and following lines.
         * Detects array type (tabular, list, or primitive) and routes accordingly.
         */
        private List<Object> parseArray(String header, int depth) {
            Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
            Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

            if (tabularMatcher.find()) {
                return parseTabularArray(header, depth);
            }

            if (arrayMatcher.find()) {
                int headerEndIdx = arrayMatcher.end();
                String afterHeader = header.substring(headerEndIdx).trim();

                if (afterHeader.startsWith(":")) {
                    String inlineContent = afterHeader.substring(1).trim();

                    if (!inlineContent.isEmpty()) {
                        List<Object> result = parseArrayValues(inlineContent);
                        currentLine++;
                        return result;
                    }
                }

                currentLine++;
                if (currentLine < lines.length) {
                    String nextLine = lines[currentLine];
                    int nextDepth = getDepth(nextLine);
                    String nextContent = nextLine.substring(nextDepth * options.indent());

                    if (nextContent.startsWith("- ")) {
                        currentLine--;
                        return parseListArray(depth);
                    } else {
                        currentLine++;
                        return parseArrayValues(nextContent);
                    }
                }
                return new ArrayList<>();
            }

            if (options.strict()) {
                throw new IllegalArgumentException("Invalid array header: " + header);
            }
            return Collections.emptyList();
        }

        /**
         * Parses tabular array format where each row contains delimiter-separated
         * values.
         * Example: items[2]{id,name}:\n 1,Ada\n 2,Bob
         */
        private List<Object> parseTabularArray(String header, int depth) {
            Matcher matcher = TABULAR_HEADER_PATTERN.matcher(header);
            if (!matcher.find()) {
                return new ArrayList<>();
            }

            String keysStr = matcher.group(2);
            List<String> keys = parseTabularKeys(keysStr);

            List<Object> result = new ArrayList<>();
            currentLine++;

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 1) {
                    break;
                }

                if (lineDepth == depth + 1) {
                    String rowContent = line.substring((depth + 1) * options.indent());
                    Map<String, Object> row = parseTabularRow(rowContent, keys);
                    result.add(row);
                }
                currentLine++;
            }

            return result;
        }

        /**
         * Parses list array format where items are prefixed with "- ".
         * Example: items[2]:\n - item1\n - item2
         */
        private List<Object> parseListArray(int depth) {
            List<Object> result = new ArrayList<>();
            currentLine++;

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 1) {
                    break;
                }

                if (lineDepth == depth + 1) {
                    String content = line.substring((depth + 1) * options.indent());

                    if (content.startsWith("- ")) {
                        result.add(parseListItem(content, depth));
                    } else {
                        currentLine++;
                    }
                } else {
                    currentLine++;
                }
            }

            return result;
        }

        /**
         * Parses a single list item starting with "- ".
         * Item can be a scalar value or an object with nested fields.
         */
        private Object parseListItem(String content, int depth) {
            String itemContent = content.substring(2).trim();
            int colonIdx = findUnquotedColon(itemContent);

            // Simple scalar: - value
            if (colonIdx <= 0) {
                currentLine++;
                return PrimitiveDecoder.parse(itemContent);
            }

            // Object item: - key: value
            String key = StringEscaper.unescape(itemContent.substring(0, colonIdx).trim());
            String value = itemContent.substring(colonIdx + 1).trim();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put(key, PrimitiveDecoder.parse(value));

            currentLine++;
            parseListItemFields(item, depth);

            return item;
        }

        /**
         * Parses additional fields for a list item object.
         */
        private void parseListItemFields(Map<String, Object> item, int depth) {
            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 2) {
                    break;
                }

                if (lineDepth == depth + 2) {
                    String fieldContent = line.substring((depth + 2) * options.indent());
                    int colonIdx = findUnquotedColon(fieldContent);

                    if (colonIdx > 0) {
                        String fieldKey = StringEscaper.unescape(fieldContent.substring(0, colonIdx).trim());
                        String fieldValue = fieldContent.substring(colonIdx + 1).trim();
                        item.put(fieldKey, PrimitiveDecoder.parse(fieldValue));
                    }
                }
                currentLine++;
            }
        }

        /**
         * Parses a tabular row into a Map using the provided keys.
         */
        private Map<String, Object> parseTabularRow(String rowContent, List<String> keys) {
            Map<String, Object> row = new LinkedHashMap<>();
            List<Object> values = parseArrayValues(rowContent);

            for (int i = 0; i < keys.size() && i < values.size(); i++) {
                row.put(keys.get(i), values.get(i));
            }

            return row;
        }

        /**
         * Parses tabular header keys from field specification.
         */
        private List<String> parseTabularKeys(String keysStr) {
            List<String> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(keysStr);
            for (String key : rawValues) {
                result.add(StringEscaper.unescape(key));
            }
            return result;
        }

        /**
         * Parses array values from a delimiter-separated string.
         */
        private List<Object> parseArrayValues(String values) {
            List<Object> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(values);
            for (String value : rawValues) {
                result.add(PrimitiveDecoder.parse(value));
            }
            return result;
        }

        /**
         * Splits a string by delimiter, respecting quoted sections.
         */
        private List<String> parseDelimitedValues(String input) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escaped) {
                    current.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    current.append(c);
                    escaped = true;
                } else if (c == '"') {
                    current.append(c);
                    inQuotes = !inQuotes;
                } else if (c == delimiter.charAt(0) && !inQuotes) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }

            if (!current.isEmpty() || input.endsWith(String.valueOf(delimiter))) {
                result.add(current.toString().trim());
            }

            return result;
        }

        /**
         * Parses additional key-value pairs at root level.
         */
        private void parseRootObjectFields(Map<String, Object> obj, int depth) {
            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth != depth) {
                    return;
                }

                String content = line.substring(depth * options.indent());

                Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
                if (keyedArray.matches()) {
                    String key = StringEscaper.unescape(keyedArray.group(1).trim());
                    String arrayHeader = content.substring(keyedArray.group(1).length());

                    var arrayValue = parseArray(arrayHeader, depth);

                    obj.put(key, arrayValue);
                } else {
                    int colonIdx = findUnquotedColon(content);
                    if (colonIdx > 0) {
                        String key = content.substring(0, colonIdx).trim();
                        String value = content.substring(colonIdx + 1).trim();

                        parseKeyValuePairIntoMap(obj, key, value, depth);

                        currentLine++;
                    } else {
                        return;
                    }
                }
            }
        }

        /**
         * Parses nested object starting at currentLine.
         */
        private Map<String, Object> parseNestedObject(int parentDepth) {
            Map<String, Object> result = new LinkedHashMap<>();

            while (currentLine < lines.length) {
                String line = lines[currentLine];

                int depth = getDepth(line);

                if (depth <= parentDepth) {
                    return result;
                }

                if (depth == parentDepth + 1) {
                    String content = line.substring((parentDepth + 1) * options.indent());

                    // Check for keyed array
                    Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);

                    if (keyedArray.matches()) {
                        String key = StringEscaper.unescape(keyedArray.group(1).trim());
                        String arrayHeader = content.substring(keyedArray.group(1).length());
                        List<Object> arrayValue = parseArray(arrayHeader, parentDepth + 1);
                        result.put(key, arrayValue);
                    } else {
                        int colonIdx = findUnquotedColon(content);

                        if (colonIdx > 0) {
                            String key = content.substring(0, colonIdx).trim();
                            String value = content.substring(colonIdx + 1).trim();

                            parseKeyValuePairIntoMap(result, key, value, depth);
                        }
                        currentLine++;
                    }
                } else {
                    // Depth is greater than parentDepth + 1, skip this line
                    currentLine++;
                }
            }

            return result;
        }

        /**
         * Parses a key-value pair at root level, creating a new Map.
         */
        private Object parseKeyValuePair(String key, String value, int depth, boolean parseRootFields) {
            key = StringEscaper.unescape(key);

            // Check if next line is nested (deeper indentation)
            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put(key, parseNestedObject(depth));

                    if (parseRootFields) {
                        parseRootObjectFields(obj, depth);
                    }
                    return obj;
                }
            }

            // Simple key-value pair
            currentLine++;
            Object parsedValue = PrimitiveDecoder.parse(value);
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put(key, parsedValue);

            if (parseRootFields) {
                parseRootObjectFields(obj, depth);
            }
            return obj;
        }

        /**
         * Parses a key-value pair and adds it to an existing map.
         */
        private void parseKeyValuePairIntoMap(Map<String, Object> map, String key, String value, int depth) {
            key = StringEscaper.unescape(key);

            // Check if next line is nested
            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    map.put(key, parseNestedObject(depth));
                    return;
                }
            }

            map.put(key, PrimitiveDecoder.parse(value));
        }

        /**
         * Finds the index of the first unquoted colon in a line.
         * Critical for handling quoted keys like "order:id": value.
         */
        private int findUnquotedColon(String content) {
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);

                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ':' && !inQuotes) {
                    return i;
                }
            }

            return -1;
        }

        /**
         * Calculates indentation depth (nesting level) of a line.
         * Counts leading spaces in multiples of the configured indent size.
         */
        private int getDepth(String line) {
            int depth = 0;
            int indentSize = options.indent();

            for (int i = 0; i < line.length(); i += indentSize) {
                if (i + indentSize <= line.length()
                        && line.substring(i, i + indentSize).equals(" ".repeat(indentSize))) {
                    depth++;
                } else {
                    break;
                }
            }

            return depth;
        }
    }
}
