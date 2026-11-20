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
| `org.apache.maven.wrapper:maven-wrapper-distribution` | 3.3.4 | https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/maven-metadata.xml |
| `org.apache.maven:apache-maven` | 3.10.1 | https://repo1.maven.org/maven2/org/apache/maven/apache-maven/maven-metadata.xml |
| `com.tngtech.archunit:archunit` | 1.5.1 | https://repo1.maven.org/maven2/com/tngtech/archunit/archunit/maven-metadata.xml |

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

### Maven Wrapper — 3.3.4

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>3.3.4</latest>
<release>3.3.4</release>
...
<version>3.3.3</version>
<version>3.3.4</version>
```

Latest GA is `3.3.4`. The `only-script` distribution zip was extracted and its `mvnw` /
`mvnw.cmd` committed; `.mvn/wrapper/maven-wrapper.properties` points at `apache-maven 3.10.1`.

### Apache Maven — 3.10.1

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/maven/apache-maven/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>4.0.0-rc-7</latest>
<release>4.0.0-rc-7</release>
...
<version>3.9.15</version>
<version>3.9.16</version>
<version>3.10.0-rc-1</version>
<version>3.10.0</version>
<version>3.10.1</version>
<version>4.0.0-alpha-2</version>
```

`4.0.0` exists only as alpha/beta/rc, so the latest GA is `3.10.1`. Used as the
`distributionUrl` in `.mvn/wrapper/maven-wrapper.properties`.

### ArchUnit — 1.5.1

Command:

```powershell
(Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/tngtech/archunit/archunit/maven-metadata.xml' -UseBasicParsing).Content
```

Output (relevant lines):

```xml
<latest>1.5.1</latest>
<release>1.5.1</release>
...
<version>1.5.0</version>
<version>1.5.1</version>
```

Latest GA is `1.5.1`.

## Java — Temurin 27.0.1 (installed)

Resolved per-user install: `C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot`
(recorded in `.agent/MACHINE.md`, gitignored; `scripts/env.ps1` puts it on the session PATH).

Evidence — `java -version` via `scripts/env.ps1`:

```text
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
```
