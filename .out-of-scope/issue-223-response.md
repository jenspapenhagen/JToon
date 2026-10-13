# Antwort-Entwurf für Issue #223

## Zusammenfassung

Danke für das detaillierte Feedback! Du hast völlig recht — die Baseline-Erhöhung von Java 17 auf 21 in einem Patch-Release (2.0.6) war ein Fehler und verstößt gegen semantische Versionierung.

## Das technische Problem

Die Spezifikation 4.2 erfordert **kürzeste Round-Trip-Ziffern** für Zahlen. `Double.toString` liefert diese erst ab **JDK 19** ([JDK-4511638](https://bugs.openjdk.org/browse/JDK-4511638)):

| JDK | `Double.toString(2e23)` | Spezifikationskonform? |
|-----|--------------------------|------------------------|
| 17  | `1.9999999999999998E23`   | ❌ Nein |
| 19+ | `2.0E23`                  | ✅ Ja |

Ein simples `--release 17` reicht also **nicht** — die JDK-17-Runtime hat das falsche Verhalten.

## Unsere Entscheidung

Wir werden **Option 1** umsetzen: Die Java-17-Baseline wird in 2.0.8 wiederhergestellt, **inklusive einem gepatchten `Double.toString`** (Schubfach/Ryu-Algorithmus).

### Konkrete Maßnahmen

1. **2.0.8 (Hotfix):**
   - Rückkehr zu `--release 17`
   - **Shortest-Digits-Patch** für `Double.toString` (Schubfach/Ryu)
   - Java-17-Testjob in CI als Baseline-Guard
   - Release Notes warnen vor dem Breaking Change in 2.0.6/2.0.7

2. **2.0.x (Maintenance-Line):**
   - Bleibt auf Java 17 (mit Shortest-Digits-Patch)
   - Bugfixes und Sicherheitsupdates weiterhin
   - EOL: Q4 2026

3. **3.0.0 (Q2 2026):**
   - Java 21 als Mindestversion
   - Vollständige Spezifikationskompatibilität (native `Double.toString`)
   - Klare Migration-Dokumentation

## Disclaimer für 2.x

> ⚠️ Diese Version basiert auf Java 17 (September 2021). Vollständige Spezifikationskompatibilität ist nicht gewährleistet. Für spezifikationskonforme Anwendungen bitte 3.0.0 mit Java 21 verwenden.

## Timeline

| Version | Java | Zeitpunkt |
|---------|------|-----------|
| 2.0.8   | 17   | Sofort (Hotfix) |
| 3.0.0   | 21   | Q2 2026 |

## Rückmeldung

Kannst du bitte bestätigen, ob das für euer Setup (epam/dm.ai mit Temurin 17 JRE) funktioniert? Wir würden gerne einen Java-17-Test gegen eure Suite lassen.

---

*Dieser Entwurf kann direkt als Antwort auf Issue #223 verwendet werden.*
