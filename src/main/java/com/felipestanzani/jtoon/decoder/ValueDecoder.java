package com.felipestanzani.jtoon.decoder;

import com.felipestanzani.jtoon.DecodeOptions;
import com.felipestanzani.jtoon.PathExpansion;
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
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter
     */
    private static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter, Group 4: field spec
     */
    private static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]\\{(.+)}:");

    /**
     * Matches keyed array headers: items[2]{id,name}: or tags[3]: or data[4]{id}:
     * Captures: group(1)=key, group(2)=#marker, group(3)=delimiter, group(4)=optional field spec
     */
    private static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile("^(.+?)\\[(#?)\\d+([\\t|])?](\\{[^}]+})?:.*$");

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
            return new LinkedHashMap<>();
        }

        String trimmed = toon.trim();
        // Special case: if input is exactly "null", return null
        if ("null".equals(trimmed)) {
            return null;
        }
        
        Parser parser = new Parser(trimmed, options);
        Object result = parser.parseValue();
        // If result is null (no content), return empty object
        if (result == null) {
            return new LinkedHashMap<>();
        }
        return result;
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
                String originalKey = keyedArray.group(1).trim();
                String key = StringEscaper.unescape(originalKey);
                String arrayHeader = content.substring(keyedArray.group(1).length());

                var arrayValue = parseArray(arrayHeader, depth);
                Map<String, Object> obj = new LinkedHashMap<>();

                // Handle path expansion for array keys
                if (shouldExpandKey(originalKey)) {
                    expandPathIntoMap(obj, key, arrayValue);
                } else {
                    obj.put(key, arrayValue);
                }
                
                // Continue parsing root-level fields if at depth 0
                if (depth == 0) {
                    parseRootObjectFields(obj, depth);
                }
                
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
            Object result = PrimitiveDecoder.parse(content);
            currentLine++;
            
            // In strict mode, check if there are more primitives at root level
            if (options.strict() && depth == 0) {
                // Skip blank lines and check for more content at root level
                while (currentLine < lines.length) {
                    String nextLine = lines[currentLine];
                    if (isBlankLine(nextLine)) {
                        currentLine++;
                        continue;
                    }
                    int nextDepth = getDepth(nextLine);
                    if (nextDepth == 0) {
                        throw new IllegalArgumentException("Multiple primitives at root depth in strict mode at line " + (currentLine + 1));
                    }
                    break;
                }
            }
            
            return result;
        }

        /**
         * Extracts delimiter from array header.
         * Returns tab, pipe, or comma (default) based on header pattern.
         */
        private String extractDelimiterFromHeader(String header) {
            Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
            if (matcher.find()) {
                String delimChar = matcher.group(3);
                if (delimChar != null) {
                    if ("\t".equals(delimChar)) {
                        return "\t";
                    } else if ("|".equals(delimChar)) {
                        return "|";
                    }
                }
            }
            // Default to comma
            return delimiter;
        }

        /**
         * Extracts declared length from array header.
         * Returns the number specified in [n] or null if not found.
         */
        private Integer extractLengthFromHeader(String header) {
            Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        /**
         * Validates array length if declared in header.
         */
        private void validateArrayLength(String header, int actualLength) {
            Integer declaredLength = extractLengthFromHeader(header);
            if (declaredLength != null && declaredLength != actualLength) {
                throw new IllegalArgumentException(
                    String.format("Array length mismatch: declared %d, found %d", declaredLength, actualLength));
            }
        }

        /**
         * Parses array from header string and following lines with a specific delimiter.
         * Detects array type (tabular, list, or primitive) and routes accordingly.
         */
        private List<Object> parseArrayWithDelimiter(String header, int depth, String arrayDelimiter) {
            Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
            Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

            if (tabularMatcher.find()) {
                return parseTabularArray(header, depth, arrayDelimiter);
            }

            if (arrayMatcher.find()) {
                int headerEndIdx = arrayMatcher.end();
                String afterHeader = header.substring(headerEndIdx).trim();

                if (afterHeader.startsWith(":")) {
                    String inlineContent = afterHeader.substring(1).trim();

                    if (!inlineContent.isEmpty()) {
                        List<Object> result = parseArrayValues(inlineContent, arrayDelimiter);
                        validateArrayLength(header, result.size());
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
                        return parseListArray(depth, arrayDelimiter, header);
                    } else {
                        currentLine++;
                        List<Object> result = parseArrayValues(nextContent, arrayDelimiter);
                        validateArrayLength(header, result.size());
                        return result;
                    }
                }
                List<Object> empty = new ArrayList<>();
                validateArrayLength(header, 0);
                return empty;
            }

            if (options.strict()) {
                throw new IllegalArgumentException("Invalid array header: " + header);
            }
            return Collections.emptyList();
        }

        /**
         * Parses array from header string and following lines.
         * Detects array type (tabular, list, or primitive) and routes accordingly.
         */
        private List<Object> parseArray(String header, int depth) {
            String arrayDelimiter = extractDelimiterFromHeader(header);
            Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
            Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

            if (tabularMatcher.find()) {
                return parseTabularArray(header, depth, arrayDelimiter);
            }

            if (arrayMatcher.find()) {
                int headerEndIdx = arrayMatcher.end();
                String afterHeader = header.substring(headerEndIdx).trim();

                if (afterHeader.startsWith(":")) {
                    String inlineContent = afterHeader.substring(1).trim();

                    if (!inlineContent.isEmpty()) {
                        List<Object> result = parseArrayValues(inlineContent, arrayDelimiter);
                        validateArrayLength(header, result.size());
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
                        return parseListArray(depth, arrayDelimiter, header);
                    } else {
                        currentLine++;
                        List<Object> result = parseArrayValues(nextContent, arrayDelimiter);
                        validateArrayLength(header, result.size());
                        return result;
                    }
                }
                List<Object> empty = new ArrayList<>();
                validateArrayLength(header, 0);
                return empty;
            }

            if (options.strict()) {
                throw new IllegalArgumentException("Invalid array header: " + header);
            }
            return Collections.emptyList();
        }

        /**
         * Checks if a line is blank (empty or only whitespace).
         */
        private boolean isBlankLine(String line) {
            return line.trim().isEmpty();
        }

        /**
         * Parses tabular array format where each row contains delimiter-separated
         * values.
         * Example: items[2]{id,name}:\n 1,Ada\n 2,Bob
         */
        private List<Object> parseTabularArray(String header, int depth, String arrayDelimiter) {
            Matcher matcher = TABULAR_HEADER_PATTERN.matcher(header);
            if (!matcher.find()) {
                return new ArrayList<>();
            }

            String keysStr = matcher.group(4);
            List<String> keys = parseTabularKeys(keysStr, arrayDelimiter);

            List<Object> result = new ArrayList<>();
            currentLine++;

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                
                // Check for blank line first - handle before depth check
                if (isBlankLine(line)) {
                    if (options.strict()) {
                        throw new IllegalArgumentException("Blank line inside tabular array at line " + (currentLine + 1));
                    }
                    // In non-strict mode, skip blank lines (they have depth 0 but should be ignored)
                    currentLine++;
                    continue;
                }
                
                int lineDepth = getDepth(line);

                // If line depth is less than expected, check if it's a key-value pair at same depth as parent
                // This terminates the array (e.g., "count: 2" after tabular array)
                if (lineDepth < depth + 1) {
                    if (lineDepth == depth) {
                        String content = line.substring(depth * options.indent());
                        // Check if it's a key-value pair (unquoted colon)
                        int colonIdx = findUnquotedColon(content);
                        if (colonIdx > 0) {
                            // This is a key-value pair at same depth - terminate array
                            break;
                        }
                    }
                    break;
                }

                if (lineDepth == depth + 1) {
                    String rowContent = line.substring((depth + 1) * options.indent());
                    Map<String, Object> row = parseTabularRow(rowContent, keys, arrayDelimiter);
                    result.add(row);
                }
                currentLine++;
            }

            validateArrayLength(header, result.size());
            return result;
        }

        /**
         * Parses list array format where items are prefixed with "- ".
         * Example: items[2]:\n - item1\n - item2
         */
        private List<Object> parseListArray(int depth, String arrayDelimiter, String header) {
            List<Object> result = new ArrayList<>();
            currentLine++;

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                
                // Check for blank line first - handle before depth check
                if (isBlankLine(line)) {
                    if (options.strict()) {
                        throw new IllegalArgumentException("Blank line inside list array at line " + (currentLine + 1));
                    }
                    // In non-strict mode, skip blank lines (they have depth 0 but should be ignored)
                    currentLine++;
                    continue;
                }
                
                int lineDepth = getDepth(line);

                // If line depth is less than expected, check if it's a key-value pair at same depth as parent
                // This terminates the array
                if (lineDepth < depth + 1) {
                    if (lineDepth == depth) {
                        String content = line.substring(depth * options.indent());
                        // Check if it's a key-value pair (unquoted colon)
                        int colonIdx = findUnquotedColon(content);
                        if (colonIdx > 0) {
                            // This is a key-value pair at same depth - terminate array
                            break;
                        }
                    }
                    break;
                }

                if (lineDepth == depth + 1) {
                    String content = line.substring((depth + 1) * options.indent());

                    if (content.startsWith("-")) {
                        result.add(parseListItem(content, depth));
                    } else {
                        currentLine++;
                    }
                } else {
                    currentLine++;
                }
            }

            if (header != null) {
                validateArrayLength(header, result.size());
            }
            return result;
        }

        /**
         * Parses a single list item starting with "- ".
         * Item can be a scalar value or an object with nested fields.
         */
        private Object parseListItem(String content, int depth) {
            // Handle empty item: just "-" or "- "
            String itemContent;
            if (content.length() > 2) {
                itemContent = content.substring(2).trim();
            } else {
                itemContent = "";
            }
            
            // Handle empty item: just "-"
            if (itemContent.isEmpty()) {
                currentLine++;
                return new LinkedHashMap<>();
            }
            
            // Check for standalone array (e.g., "[2]: 1,2")
            if (itemContent.startsWith("[")) {
                // For nested arrays in list items, default to comma delimiter if not specified
                String nestedArrayDelimiter = extractDelimiterFromHeader(itemContent);
                // parseArrayWithDelimiter handles currentLine increment internally
                // For inline arrays, it increments. For multi-line arrays, parseListArray handles it.
                // We need to increment here only if it was an inline array that we just parsed
                // Actually, parseArrayWithDelimiter always handles currentLine, so we don't need to increment
                return parseArrayWithDelimiter(itemContent, depth + 1, nestedArrayDelimiter);
            }
            
            // Check for keyed array pattern (e.g., "tags[3]: a,b,c" or "data[2]{id}: ...")
            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(itemContent);
            if (keyedArray.matches()) {
                String originalKey = keyedArray.group(1).trim();
                String key = StringEscaper.unescape(originalKey);
                String arrayHeader = itemContent.substring(keyedArray.group(1).length());
                
                // For nested arrays in list items, default to comma delimiter if not specified
                String nestedArrayDelimiter = extractDelimiterFromHeader(arrayHeader);
                var arrayValue = parseArrayWithDelimiter(arrayHeader, depth + 1, nestedArrayDelimiter);
                
                Map<String, Object> item = new LinkedHashMap<>();
                item.put(key, arrayValue);
                
                // parseArrayWithDelimiter handles currentLine for inline arrays
                // For multi-line arrays, we need to make sure currentLine is correct
                // Actually, parseArrayWithDelimiter should handle it, but for keyed arrays we might need to increment
                // Let's check: if it was inline, currentLine was incremented. If multi-line, parseListArray/parseTabularArray handle it.
                // For keyed arrays that are inline, we need to increment. For multi-line, the array parser handles it.
                // For now, always increment - parseArrayWithDelimiter will have incremented for inline, and for multi-line
                // the array parsers leave currentLine at the right place, so we need to check if we're past the array
                // Actually, this is complex. Let's just increment and see if it works.
                currentLine++;
                parseListItemFields(item, depth);
                
                return item;
            }
            
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
                    
                    // Check for keyed array pattern first
                    Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(fieldContent);
                    if (keyedArray.matches()) {
                        String originalKey = keyedArray.group(1).trim();
                        String key = StringEscaper.unescape(originalKey);
                        String arrayHeader = fieldContent.substring(keyedArray.group(1).length());
                        
                        // For nested arrays in list items, default to comma delimiter if not specified
                        String nestedArrayDelimiter = extractDelimiterFromHeader(arrayHeader);
                        var arrayValue = parseArrayWithDelimiter(arrayHeader, depth + 2, nestedArrayDelimiter);
                        
                        // Handle path expansion for array keys
                        if (shouldExpandKey(originalKey)) {
                            expandPathIntoMap(item, key, arrayValue);
                        } else {
                            item.put(key, arrayValue);
                        }
                    } else {
                        int colonIdx = findUnquotedColon(fieldContent);

                        if (colonIdx > 0) {
                            String fieldKey = StringEscaper.unescape(fieldContent.substring(0, colonIdx).trim());
                            String fieldValue = fieldContent.substring(colonIdx + 1).trim();
                            
                            // Check if next line is nested
                            Object parsedValue;
                            if (currentLine + 1 < lines.length) {
                                int nextDepth = getDepth(lines[currentLine + 1]);
                                if (nextDepth > depth + 2) {
                                    currentLine++;
                                    parsedValue = parseNestedObject(depth + 2);
                                } else {
                                    // If value is empty, create empty object; otherwise parse as primitive
                                    if (fieldValue.trim().isEmpty()) {
                                        parsedValue = new LinkedHashMap<>();
                                    } else {
                                        parsedValue = PrimitiveDecoder.parse(fieldValue);
                                    }
                                }
                            } else {
                                // If value is empty, create empty object; otherwise parse as primitive
                                if (fieldValue.trim().isEmpty()) {
                                    parsedValue = new LinkedHashMap<>();
                                } else {
                                    parsedValue = PrimitiveDecoder.parse(fieldValue);
                                }
                            }
                            
                            // Handle path expansion
                            if (shouldExpandKey(fieldKey)) {
                                expandPathIntoMap(item, fieldKey, parsedValue);
                            } else {
                                item.put(fieldKey, parsedValue);
                            }
                        }
                    }
                }
                currentLine++;
            }
        }

        /**
         * Parses a tabular row into a Map using the provided keys.
         * Validates that the row uses the correct delimiter.
         */
        private Map<String, Object> parseTabularRow(String rowContent, List<String> keys, String arrayDelimiter) {
            // Validate delimiter mismatch - check if row contains wrong delimiter
            if (options.strict()) {
                validateRowDelimiter(rowContent, arrayDelimiter);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            List<Object> values = parseArrayValues(rowContent, arrayDelimiter);

            // Validate value count matches key count
            if (options.strict() && values.size() != keys.size()) {
                throw new IllegalArgumentException(
                    String.format("Tabular row value count (%d) does not match header field count (%d)", 
                        values.size(), keys.size()));
            }

            for (int i = 0; i < keys.size() && i < values.size(); i++) {
                row.put(keys.get(i), values.get(i));
            }

            return row;
        }

        /**
         * Validates that a row uses the correct delimiter.
         */
        private void validateRowDelimiter(String rowContent, String expectedDelimiter) {
            // Check for delimiter mismatches - if header declares tab, row shouldn't use comma, etc.
            char expectedChar = expectedDelimiter.charAt(0);
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < rowContent.length(); i++) {
                char c = rowContent.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inQuotes = !inQuotes;
                    continue;
                }
                if (!inQuotes) {
                    // Check for wrong delimiter
                    if (expectedChar == '\t' && c == ',') {
                        throw new IllegalArgumentException("Delimiter mismatch: header declares tab, row uses comma");
                    }
                    if (expectedChar == '|' && c == ',') {
                        throw new IllegalArgumentException("Delimiter mismatch: header declares pipe, row uses comma");
                    }
                    if (expectedChar == ',' && (c == '\t' || c == '|')) {
                        throw new IllegalArgumentException("Delimiter mismatch: header declares comma, row uses different delimiter");
                    }
                }
            }
        }

        /**
         * Parses tabular header keys from field specification.
         * Validates delimiter consistency between bracket and brace fields.
         */
        private List<String> parseTabularKeys(String keysStr, String arrayDelimiter) {
            // Validate delimiter mismatch between bracket and brace fields
            if (options.strict()) {
                validateKeysDelimiter(keysStr, arrayDelimiter);
            }

            List<String> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(keysStr, arrayDelimiter);
            for (String key : rawValues) {
                result.add(StringEscaper.unescape(key));
            }
            return result;
        }

        /**
         * Validates delimiter consistency in tabular header keys.
         */
        private void validateKeysDelimiter(String keysStr, String expectedDelimiter) {
            char expectedChar = expectedDelimiter.charAt(0);
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < keysStr.length(); i++) {
                char c = keysStr.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inQuotes = !inQuotes;
                    continue;
                }
                if (!inQuotes) {
                    // Check for wrong delimiter in keys
                    if (expectedChar == '\t' && c == ',') {
                        throw new IllegalArgumentException("Delimiter mismatch: bracket declares tab, brace fields use comma");
                    }
                    if (expectedChar == '|' && c == ',') {
                        throw new IllegalArgumentException("Delimiter mismatch: bracket declares pipe, brace fields use comma");
                    }
                    if (expectedChar == ',' && (c == '\t' || c == '|')) {
                        throw new IllegalArgumentException("Delimiter mismatch: bracket declares comma, brace fields use different delimiter");
                    }
                }
            }
        }

        /**
         * Parses array values from a delimiter-separated string.
         */
        private List<Object> parseArrayValues(String values, String arrayDelimiter) {
            List<Object> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(values, arrayDelimiter);
            for (String value : rawValues) {
                result.add(PrimitiveDecoder.parse(value));
            }
            return result;
        }

        /**
         * Splits a string by delimiter, respecting quoted sections.
         * Whitespace around delimiters is tolerated and trimmed.
         */
        private List<String> parseDelimitedValues(String input, String arrayDelimiter) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;
            char delimChar = arrayDelimiter.charAt(0);

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
                } else if (c == delimChar && !inQuotes) {
                    // Found delimiter - add current value (trimmed) and reset
                    String value = current.toString().trim();
                    result.add(value);
                    current = new StringBuilder();
                    // Skip whitespace after delimiter
                    while (i + 1 < input.length() && Character.isWhitespace(input.charAt(i + 1))) {
                        i++;
                    }
                } else {
                    current.append(c);
                }
            }

            // Add final value
            if (!current.isEmpty() || input.endsWith(arrayDelimiter)) {
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

                // Skip blank lines
                if (isBlankLine(line)) {
                    currentLine++;
                    continue;
                }

                String content = line.substring(depth * options.indent());

                Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
                if (keyedArray.matches()) {
                    String originalKey = keyedArray.group(1).trim();
                    String key = StringEscaper.unescape(originalKey);
                    String arrayHeader = content.substring(keyedArray.group(1).length());

                    var arrayValue = parseArray(arrayHeader, depth);

                    // Handle path expansion for array keys
                    if (shouldExpandKey(originalKey)) {
                        expandPathIntoMap(obj, key, arrayValue);
                    } else {
                        obj.put(key, arrayValue);
                    }
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
                    // Skip blank lines
                    if (isBlankLine(line)) {
                        currentLine++;
                        continue;
                    }
                    
                    String content = line.substring((parentDepth + 1) * options.indent());

                    // Check for keyed array
                    Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);

                    if (keyedArray.matches()) {
                        String originalKey = keyedArray.group(1).trim();
                        String key = StringEscaper.unescape(originalKey);
                        String arrayHeader = content.substring(keyedArray.group(1).length());
                        List<Object> arrayValue = parseArray(arrayHeader, parentDepth + 1);

                        // Handle path expansion for array keys
                        if (shouldExpandKey(originalKey)) {
                            expandPathIntoMap(result, key, arrayValue);
                        } else {
                            result.put(key, arrayValue);
                        }
                    } else {
                        int colonIdx = findUnquotedColon(content);

                        if (colonIdx > 0) {
                            String key = content.substring(0, colonIdx).trim();
                            String value = content.substring(colonIdx + 1).trim();

                            parseKeyValuePairIntoMap(result, key, value, depth);
                        } else {
                            // No colon found in key-value context - this is an error
                            if (options.strict()) {
                                throw new IllegalArgumentException("Missing colon in key-value context at line " + (currentLine + 1));
                            }
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
         * Checks if a key should be expanded (is a valid identifier segment).
         * Keys with dots that are valid identifiers can be expanded.
         * Quoted keys are never expanded.
         */
        private boolean shouldExpandKey(String key) {
            if (options.expandPaths() != PathExpansion.SAFE) {
                return false;
            }
            // Quoted keys should not be expanded
            if (key.trim().startsWith("\"") && key.trim().endsWith("\"")) {
                return false;
            }
            // Check if key contains dots and is a valid identifier pattern
            if (!key.contains(".")) {
                return false;
            }
            // Valid identifier: starts with letter or underscore, followed by letters, digits, underscores
            // Each segment must match this pattern
            String[] segments = key.split("\\.");
            for (String segment : segments) {
                if (!segment.matches("^[a-zA-Z_][\\w]*$")) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Expands a dotted key into nested object structure.
         */
        private void expandPathIntoMap(Map<String, Object> map, String dottedKey, Object value) {
            String[] segments = dottedKey.split("\\.");
            Map<String, Object> current = map;

            // Navigate/create nested structure
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                Object existing = current.get(segment);

                if (existing == null) {
                    // Create new nested object
                    Map<String, Object> nested = new LinkedHashMap<>();
                    current.put(segment, nested);
                    current = nested;
                } else if (existing instanceof Map) {
                    // Use existing nested object
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingMap = (Map<String, Object>) existing;
                    current = existingMap;
                } else {
                    // Conflict: existing is not a Map
                    if (options.strict()) {
                        throw new IllegalArgumentException(
                            String.format("Path expansion conflict: %s is %s, cannot expand to object",
                                segment, existing.getClass().getSimpleName()));
                    }
                    // LWW: overwrite with new nested object
                    Map<String, Object> nested = new LinkedHashMap<>();
                    current.put(segment, nested);
                    current = nested;
                }
            }

            // Set final value
            String finalSegment = segments[segments.length - 1];
            Object existing = current.get(finalSegment);

            if (existing != null && options.strict()) {
                // Check for conflicts in strict mode
                if (existing instanceof Map && !(value instanceof Map)) {
                    throw new IllegalArgumentException(
                        String.format("Path expansion conflict: %s is object, cannot set to %s",
                            finalSegment, value.getClass().getSimpleName()));
                }
                if (existing instanceof List && !(value instanceof List)) {
                    throw new IllegalArgumentException(
                        String.format("Path expansion conflict: %s is array, cannot set to %s",
                            finalSegment, value.getClass().getSimpleName()));
                }
            }

            // LWW: last write wins (always overwrite in non-strict, or if types match in strict)
            current.put(finalSegment, value);
        }

        /**
         * Parses a key-value pair at root level, creating a new Map.
         */
        private Object parseKeyValuePair(String key, String value, int depth, boolean parseRootFields) {
            String originalKey = key;
            key = StringEscaper.unescape(key);

            // Check if next line is nested (deeper indentation)
            Object parsedValue;
            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    parsedValue = parseNestedObject(depth);
                } else {
                    // If value is empty, create empty object; otherwise parse as primitive
                    if (value.trim().isEmpty()) {
                        parsedValue = new LinkedHashMap<>();
                    } else {
                        parsedValue = PrimitiveDecoder.parse(value);
                    }
                    currentLine++;
                }
            } else {
                // If value is empty, create empty object; otherwise parse as primitive
                if (value.trim().isEmpty()) {
                    parsedValue = new LinkedHashMap<>();
                } else {
                    parsedValue = PrimitiveDecoder.parse(value);
                }
                currentLine++;
            }

            Map<String, Object> obj = new LinkedHashMap<>();

            // Handle path expansion
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(obj, key, parsedValue);
            } else {
                checkPathExpansionConflict(obj, key, parsedValue);
                obj.put(key, parsedValue);
            }

            if (parseRootFields) {
                parseRootObjectFields(obj, depth);
            }
            return obj;
        }

        /**
         * Parses a key-value pair and adds it to an existing map.
         */
        private void parseKeyValuePairIntoMap(Map<String, Object> map, String key, String value, int depth) {
            String originalKey = key;
            key = StringEscaper.unescape(key);

            // Check if next line is nested
            Object parsedValue;
            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    parsedValue = parseNestedObject(depth);
                } else {
                    // If value is empty, create empty object; otherwise parse as primitive
                    if (value.trim().isEmpty()) {
                        parsedValue = new LinkedHashMap<>();
                    } else {
                        parsedValue = PrimitiveDecoder.parse(value);
                    }
                }
            } else {
                // If value is empty, create empty object; otherwise parse as primitive
                if (value.trim().isEmpty()) {
                    parsedValue = new LinkedHashMap<>();
                } else {
                    parsedValue = PrimitiveDecoder.parse(value);
                }
            }

            // Handle path expansion
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(map, key, parsedValue);
            } else {
                checkPathExpansionConflict(map, key, parsedValue);
                map.put(key, parsedValue);
            }
        }

        /**
         * Checks for path expansion conflicts when setting a non-expanded key.
         * In strict mode, throws if the key conflicts with an existing expanded path.
         */
        private void checkPathExpansionConflict(Map<String, Object> map, String key, Object value) {
            if (!options.strict()) {
                return;
            }
            
            Object existing = map.get(key);
            if (existing != null) {
                // Check for conflicts: existing is object/array but value is not
                if (existing instanceof Map && !(value instanceof Map)) {
                    throw new IllegalArgumentException(
                        String.format("Path expansion conflict: %s is object, cannot set to %s",
                            key, value.getClass().getSimpleName()));
                }
                if (existing instanceof List && !(value instanceof List)) {
                    throw new IllegalArgumentException(
                        String.format("Path expansion conflict: %s is array, cannot set to %s",
                            key, value.getClass().getSimpleName()));
                }
            }
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
         * In strict mode, validates indentation (no tabs, proper multiples).
         */
        private int getDepth(String line) {
            // Blank lines (including lines with only spaces) have depth 0
            if (isBlankLine(line)) {
                return 0;
            }

            if (options.strict()) {
                validateIndentation(line);
            }

            int depth;
            int indentSize = options.indent();
            int leadingSpaces = 0;

            // Count leading spaces
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ') {
                    leadingSpaces++;
                } else {
                    break;
                }
            }

            // Calculate depth based on indent size
            depth = leadingSpaces / indentSize;

            // In strict mode, check if it's an exact multiple
            if (options.strict() && leadingSpaces > 0
                    && leadingSpaces % indentSize != 0) {
                    throw new IllegalArgumentException(
                        String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
                            leadingSpaces, indentSize, currentLine + 1));
                }


            return depth;
        }

        /**
         * Validates indentation in strict mode.
         * Checks for tabs, mixed tabs/spaces, and non-multiple indentation.
         */
        private void validateIndentation(String line) {
            if (line.trim().isEmpty()) {
                // Blank lines are allowed (handled separately)
                return;
            }

            int indentSize = options.indent();
            int leadingSpaces = 0;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\t') {
                    throw new IllegalArgumentException(
                        String.format("Tab character used in indentation at line %d", currentLine + 1));
                } else if (c == ' ') {
                    leadingSpaces++;
                } else {
                    // Reached non-whitespace
                    break;
                }
            }

            // Check for non-multiple indentation (only if there's actual content)
            if (leadingSpaces > 0 && leadingSpaces % indentSize != 0) {
                throw new IllegalArgumentException(
                    String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
                        leadingSpaces, indentSize, currentLine + 1));
            }
        }
    }
}
