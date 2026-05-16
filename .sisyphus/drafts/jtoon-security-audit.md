# Draft: JToon Security Vulnerability Fixes

## Audit Summary
- **Auditor**: V12 Autonomous Auditor
- **Date**: 2026-05-15
- **Total Findings**: 23 vulnerabilities

## High Severity (3)
1. **#62017**: Encoder recursion lacks cycle guards (StackOverflow on self-referential structures)
2. **#62025**: Cyclic graphs crash encoding (same issue)
3. **#62037**: Quadratic field-set copies (O(n²) DoS)

## Medium Severity (20)
- #62013: Malformed options crash encoding
- #62015: Null delimiter crashes decoding
- #62020: Numeric-like strings lose type (type confusion)
- #62021: Dotted keys become nested paths (parser differential)
- #62023: Invalid key escapes forge field names (canonicalization)
- #62028: Dotted keys collapse to aliases (key collision)
- #62029: Prefix path collisions go undetected
- #62032: Nested folded fields disappear (data loss)
- #62044: Null path conflict crashes decoder (NPE)
- #62049: Duplicate columns overwrite row values
- #62052: Lenient array headers still crash
- #62060: Unchecked indent enables memory exhaustion
- #62062: Negative indent causes decoder crashes
- #62063: Unbounded stream inputs can hang
- #62065: Quoted dotted list-item keys expand incorrectly
- #62066: List-item field insertion bypasses path expansion
- #62067: Unbounded encoder recursion (stack exhaustion)
- #62068: Unbounded parser nesting (stack exhaustion)
- #62070: Lossy double coercion corrupts numeric handling
- #62071: Nested arrays produce invalid encoded lengths

## User Request
- User wants ALL 23 vulnerabilities fixed

## Codebase Context Explored
- `JToon.java` - Main entry point, delegates to JsonNormalizer and ValueEncoder/ValueDecoder
- `EncodeOptions.java` - No validation on indent (can be negative), delimiter (can be null)
- `DecodeOptions.java` - Same validation issues
- `JsonNormalizer.java` - normalize() recursively calls itself without cycle detection or depth limits
  - Line 100: Stream.toList() - unbounded materialization
  - Line 266-267: Collection iteration without depth tracking
  - Line 276-278: Map iteration without cycle detection
- `ObjectEncoder.java` - encodeObject() recursively descends with no depth cap (line 134-135)

## Clarification Needed
- Should regression tests be added for each fix (security-focused test cases)?