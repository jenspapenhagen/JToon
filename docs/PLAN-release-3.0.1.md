# Plan: Release 3.0.1 (main) and 2.0.9 (java17)

## Scope

Two patch releases, both with an updated CHANGELOG:

- **main / 3.x** → `v3.0.1`: Jackson 3.3.0, no bundled Blackbird, configurable
  `ObjectMapper` hooks, `JsonNormalizer` switch cleanup, dependency/Gradle updates.
- **java17 / 2.x** → `v2.0.9`: same `ObjectMapper` hooks backported, Jackson 3.3.0,
  Blackbird removed, `NumberOutput` for shortest double digits, dependency/Gradle
  updates, CI uses JDK 17.

## Changes (done)

### main
- `ObjectMapperSingleton`: `setInstance`, `isInitialized`, `createDefaultMapper`,
  `createMapper(JacksonModule...)`; no Blackbird module.
- `JToon.configure(ObjectMapper)`, `JToon.createMapper(JacksonModule...)`.
- `JsonNormalizer`: pattern-matching `switch` + Checkstyle line lengths.
- `build.gradle`: jackson-databind 3.3.0, blackbird removed.
- `spotbugs-exclude.xml`: `EI_EXPOSE_STATIC_REP2`.
- `README.md`: ObjectMapper/optimizer section + API reorganisation.
- `CHANGELOG.md`: `[3.0.1] - 2026-11-14`.
- `gradle/verification-metadata.xml`: regenerated for Gradle 9.9.1 (includes plugin
  classpath artifacts so fresh CI runners verify).

### java17
- Ported the same `ObjectMapperSingleton`/`JToon` hooks (Java 17 compatible).
- `PrimitiveEncoder`: `DoubleToDecimal` → `NumberOutput` (class removed in Jackson
  3.3.0).
- `build.gradle`: jackson-databind 3.3.0, blackbird removed, `error_prone_core`
  stays 2.42.0 (newer requires JDK 21), plugin/dep updates.
- Gradle wrapper 9.9.1; `verification-metadata.xml` regenerated with a clean cache.
- `.github/workflows`: build uses JDK 17; setup-java 6.1.0; upload-artifact 7.0.1.
- `README.md` + `CHANGELOG.md` (`[2.0.9] - 2026-11-14`).

## Verification

- `./gradlew check` green on both branches (main on JDK 21+, java17 on JDK 17).
- `act pull_request -j build` green on both branches (build + tests + SBOM). Only
  act's artifact-upload step fails locally for lack of `ACTIONS_RUNTIME_TOKEN`.

## Release

1. Commit and push `main` and `java17` to `upstream` (`toon-format/toon-java`).
2. Create GitHub releases with `gh`:
   - `gh release create v3.0.1 --target main ...`
   - `gh release create v2.0.9 --target java17 ...`
3. `release.yml` derives the version from the tag, publishes to Maven Central,
   regenerates the SBOM, updates the README version, and uploads assets.

## Risks

- Tag push publishes to Maven Central (irreversible).
- `release.yml` regenerates `verification-metadata.xml`; CI does this from a clean
  environment, so it stays complete.
- README version bump is performed by CI.
