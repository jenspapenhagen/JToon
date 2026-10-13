# Java-Versionsstrategie

## Aktuelle Situation

**Problem:** Version 2.0.6 hat die Java-Baseline von 17 auf 21 in einem **Patch-Release** erhöht. Dies ist ein **Breaking Change** für Java-17-Nutzer.

| Version | Class-File-Major | `org.gradle.jvm.version` | Java-Baseline |
|---------|-----------------|--------------------------|---------------|
| 2.0.5   | 61 (Java 17)    | 17                       | Java 17       |
| 2.0.6   | 65 (Java 21)    | 21                       | Java 21       |
| 2.0.7   | 65 (Java 21)    | 21                       | Java 21       |

**Auswirkung:** Gradle verweigert die Resolution für Java-17-Projekte:
```
Dependency resolution is looking for a library compatible with JVM runtime version 17,
but 'dev.toonformat:jtoon:2.0.7' is only compatible with JVM runtime version 21 or newer.
```

**Betroffene Nutzer:** Projekte, die Java 17 einsetzen (z. B. epam/dm.ai mit bundled Temurin 17 JRE).

---

## Das eigentliche technisches Problem: Shortest-Digits (JDK-4511638)

**Spezifikation 4.2** erfordert die **kürzesten Round-Trip-Ziffern** für Zahlen. `Double.toString` liefert diese erst ab **JDK 19** (Bug [JDK-4511638](https://bugs.openjdk.org/browse/JDK-4511638)).

| JDK | `Double.toString(2e23)` | Spezifikationskonform? |
|-----|--------------------------|------------------------|
| 17  | `1.9999999999999998E23`   | ❌ Nein |
| 19+ | `2.0E23`                  | ✅ Ja |

**Konsequenz:** Ein simpler `--release 17` reicht **nicht** — die JDK-17-Runtime hat das falsche `Double.toString`-Verhalten.

### Lösungsmöglichkeiten

| Aufwand | Lösung | Vorteil | Nachteil |
|---------|--------|---------|----------|
| Mittel | **Ryu-Bibliothek** (empfohlen) | Bleibt auf Java 17, bewährter Algorithmus | Extra-Abhängigkeit |
| Hoch | **Shortest-Digits portieren** (Schubfach/Ryu) | Keine Extra-Abhängigkeit | Erweiterter Code, Wartung |
| Niedrig | **Baseline auf JDK 19+** | Kein Extra-Code | Kein LTS, kürzere Support-Dauer |

---

### Ryu: Die Lösung für Java 17

Ryu ist die Java-Portierung von Ulf Adams' Original-C-Implementation. Sie berechnet kürzeste Round-Trip-Ziffern und funktioniert auf **Java 8+**.

```xml
<dependency>
    <groupId>tech.uom.lib</groupId>
    <artifactId>ryu</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Verwendung:**
```java
import tech.uom.lib.ryu.Ryu;

String shortest = Ryu.doubleToString(2e23);  // → "2.0E23" ✅
```

**Vorteile:**
- ✅ Spezifikationskonform auch auf Java 17
- ✅ Bewährter Algorithmus (wird in vielen Projekten verwendet)
- ✅ Kein eigener Code zu warten
- ✅ Funktioniert auf Java 8+

---

## Zwei Optionen

### Option 1: Java-17-kompatiblen Artifact weiterhin veröffentlichen ✅ **Empfohlen**

**Vorgehen:**
- `--release 17` **plus** gepatchtem `Double.toString` (Schubfach/Ryu-Algorithmus)
- CI läuft weiterhin auf Java 21
- Zusätzlicher Java-17-Testjob als Baseline-Guard

**Vorteile:**
- Gleiche Source, gleiche Features
- Keine Breaking Changes für bestehende Nutzer
- Dependabot funktioniert wieder normal
- Semantische Versionierung wird eingehalten

**Nachteile:**
- Shortest-Digits-Algorithmus muss gepflegt werden
- Etwas mehr Code-Review-Aufwand

---

### Option 2: Beide Versionen parallel veröffentlichen

**Vorgehen:**
- **2.0.x** → Maintenance-Line für Java 17
- **3.0.0** → Java 21 mit Spezifikationskompatibilität
- Baseline-Change wird in Release Notes kommuniziert

**Vorteile:**
- Java-21-Nutzer können alle Features nutzen
- Java-17-Nutzer haben eine stabile Maintenance-Line
- Klare Trennung der Zielgruppen

**Nachteile:**
- Doppelter Maintenance-Aufwand
- Verwirrung über welche Version genommen werden sollte

---

## Empfehlung

**Option 1** ist die sicherste und nutzerfreundlichste Lösung:

1. **2.0.x** bleibt auf Java 17 (mit `--release 17`)
2. **3.0.0** führt Java 21 ein (mit klarer Kommunikation)
3. **Disclaimer** für 2.x wird hinzugefügt (siehe unten)

---

## Disclaimer für 2.x (Java 17)

> ⚠️ **Hinweis zur Spezifikationskompatibilität**
> 
> Diese Version basiert auf Java 17, das im September 2021 veröffentlicht wurde.
> Vollständige Spezifikationskompatibilität ist in dieser Version **nicht gewährleistet**.
> 
> Für spezifikationskonforme Anwendungen bitte Version 3.0.0 mit Java 21 verwenden.

---

## Version 3.0.0 — Java 21 (Spezifikationskompatibel)

### Features
- Vollständige Spezifikationskompatibilität
- Nutzung neuer Java-21-Sprachfeatures:
  - Record Patterns
  - Pattern Matching für switch
  - Sealed Classes
  - Virtual Threads (Project Loom)
- Verbesserte Performance durch moderne JVM-Features

### Breaking Changes
- **Mindestversion:** Java 21 (kein Downgrade auf 17 möglich)
- Sprachlevel auf Java 21 gesetzt
- Veraltete APIs entfernt

---

## Timeline

| Meilenstein | Zeitpunkt | Beschreibung |
|-------------|-----------|--------------|
| 2.0.8 (Hotfix) | Sofort | Rückkehr zu Java 17 mit `--release 17` |
| 2.x Maintenance | Laufend | Bugfixes für Java-17-Zielgruppe |
| 3.0.0 Entwicklung | Q1 2026 | Migration auf Java 21 |
| 3.0.0 Release | Q2 2026 | Veröffentlichung mit Spezifikationskompatibilität |
| 2.x EOL | Q4 2026 | End of Life für Java-17-Zielgruppe |

---

## Build-Konfiguration

### 2.x (Java 17)
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.release = 17
}
```

### 3.0.0 (Java 21)
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.release = 21
}
```

---

## Kommunikationsvorlage für Release Notes

### 2.0.8 (Java 17 Hotfix)
```
## 2.0.8 — Java 17 Baseline wiederhergestellt

⚠️ WICHTIG: 2.0.6 und 2.0.7 haben versehentlich die Java-Baseline auf 21 erhöht.
Dies ist ein Breaking Change für Java-17-Nutzer.

Ab 2.0.8 ist Java 17 wieder die offizielle Baseline.

Für Java-21-Nutzer: Bitte 3.0.0 verwenden (veröffentlicht in Q2 2026).
```

### 3.0.0 (Java 21)
```
## 3.0.0 — Java 21 & Spezifikationskompatibilität

Dieses Release führt Java 21 als Mindestversion ein.

Breaking Changes:
- Mindestversion: Java 21
- Sprachlevel: Java 21

Neue Features:
- Vollständige Spezifikationskompatibilität
- [weitere Features...]

Migration:
- Java-17-Nutzer: Bleiben Sie auf 2.0.x (Maintenance bis Q4 2026)
- Java-21-Nutzer: Upgrade auf 3.0.0
```

---

## Zusammenfassung

| Kriterium | 2.0.x (ab 2.0.8) | 3.0.0 |
|-----------|-------------------|-------|
| Java-Version | 17 | 21 |
| LTS | ✅ Ja | ✅ Ja |
| Spezifikationskompatibilität | ❌ Nein | ✅ Ja |
| Disclaimer | ⚠️ Erforderlich | ✅ Nicht erforderlich |
| Veröffentlichung Java | Sept. 2021 | Sept. 2023 |
| Status | Maintenance | Aktive Entwicklung |
