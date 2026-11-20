# Rota Versions

Resolved 2026-11-19. Maven artifacts were read from Maven Central `maven-metadata.xml`; only
GA versions are selected (exclude `-M*`, `-RC*`, `-beta*`, `-alpha*`, `-rc*`).

## Resolved

| Artifact | Version | Metadata URL |
| --- | --- | --- |
| `org.springframework.boot:spring-boot-starter-parent` | 4.1.2 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml |
| `ai.timefold.solver:timefold-solver-core` | 2.8.0 | https://repo1.maven.org/maven2/ai/timefold/solver/timefold-solver-core/maven-metadata.xml |
| `org.testcontainers:testcontainers` | 2.0.5 | https://repo1.maven.org/maven2/org/testcontainers/testcontainers/maven-metadata.xml |
| `org.junit.jupiter:junit-jupiter` | 6.1.3 | https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/maven-metadata.xml |

## Evidence

### Spring Boot — 4.1.2

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>4.2.0-RC2</latest>
<release>4.2.0-RC2</release>
...
<version>4.1.0</version>
<version>4.1.1</version>
<version>4.1.2</version>
<version>4.2.0-M1</version>
<version>4.2.0-M2</version>
<version>4.2.0-RC1</version>
<version>4.2.0-RC2</version>
```

`4.2.0` exists only as milestones/RCs, so the latest GA is `4.1.2`.

### Timefold Solver — 2.8.0

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ai/timefold/solver/timefold-solver-core/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>2.9.0-rc-1</latest>
<release>2.9.0-rc-1</release>
...
<version>2.8.0</version>
<version>2.9.0-rc-1</version>
```

Latest GA is `2.8.0`. The Spring Boot starter
(`ai.timefold.solver:timefold-solver-spring-boot-starter`) has the same version line; its
latest GA is also `2.8.0` (metadata lastUpdated 20261116161941).

### Testcontainers — 2.0.5

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/testcontainers/testcontainers/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>2.0.5</latest>
<release>2.0.5</release>
```

Latest GA is `2.0.5`.

### JUnit — 6.1.3

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>6.2.0-M1</latest>
<release>6.2.0-M1</release>
...
<version>6.1.0</version>
<version>6.1.1</version>
<version>6.1.2</version>
<version>6.1.3</version>
<version>6.2.0-M1</version>
```

Latest GA is `6.1.3`.

## Java — 27 (Temurin), target (not yet installed)

Samet confirmed binding **Java 27 (Temurin)** on 2026-11-20. It is not installed on this
machine yet:

- Preflight output recorded in `.agent/STATE.md`: `openjdk version "21.0.10" 2026-01-20`
  (OpenLogic OpenJDK, `C:\Program Files\OpenLogic\jdk-21.0.10.7-hotspot`).
- `java -version` and `JAVA_HOME`: OpenLogic 21.0.10.
- `C:\Users\samet\.jdks`: `ms-25.0.2`, `ms-25.0.3`, `openjdk-25`, `openjdk-25.0.2`
  (Microsoft Build of OpenJDK 25).
- No Temurin 27 install found on this machine.

Provision Temurin 27, then re-run `scripts/preflight.ps1` to confirm before building.
