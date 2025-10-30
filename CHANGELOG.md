# Changelog

All notable changes to this project will be documented in this file.

This project adheres to Semantic Versioning and follows a Keep a Changelog-like format.

## [0.1.1] - 2025-10-30

### Added

- `JToon.encodeJson(String)` and `JToon.encodeJson(String, EncodeOptions)` to encode plain JSON strings directly to TOON.
- Centralized JSON parsing via `JsonNormalizer.parse(String)` to preserve separation of concerns.
- Unit tests for JSON string entry point (objects, primitive arrays, tabular arrays, custom options, error cases).
- README examples for JSON-string encoding, including a Java text block example.
- This changelog.

### Changed

- README: Expanded API docs to include `encodeJson` overloads.

## [0.1.0] - 2025-10-30

### Added

- Initial release.
- Core encoding of Java objects to TOON with automatic normalization of Java types (numbers, temporals, collections, maps, arrays, POJOs).
- Tabular array encoding for uniform arrays of objects.
- Delimiter options (comma, tab, pipe) and optional length marker.
- Comprehensive README with specification overview and examples.

[0.1.1]: https://github.com/felipestanzani/jtoon/releases/tag/v0.1.1
[0.1.0]: https://github.com/felipestanzani/jtoon/releases/tag/v0.1.0
