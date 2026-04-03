# kover-coverage-plugin

A Gradle plugin that registers per-module and aggregated [Kover](https://github.com/Kotlin/kotlinx-kover) coverage tasks for multi-module Android projects.  
Designed to be applied via a Gradle **init script** — no changes to the consuming project required.

## Tasks registered

| Task | Description |
|------|-------------|
| `koverModuleCoverage_<module>` | Parses the Kover XML report for a single module and writes `Line / Branch coverage %` to a `.txt` file |
| `koverAllModulesCoverageReport` | Aggregates all per-module `.txt` files into a single `coverageAllModulesSummary.txt` |

## Requirements

- Gradle 8+
- [Kover Gradle Plugin](https://github.com/Kotlin/kotlinx-kover) `0.9.1+` already applied to the consuming project

## Installation via JitPack

Add to your init script:

```kotlin
// .gradle/init.gradle.kts
initscript {
    repositories {
        maven(url = "https://jitpack.io")
        mavenCentral()
    }
    dependencies {
        classpath("com.github.YOUR_ORG:kover-coverage-plugin:0.0.1")
    }
}
```

## Usage

Apply the plugin in your init script and configure the extension:

```kotlin
// .gradle/init.gradle.kts
if (System.getenv("CI")?.toBoolean() == true) {
    rootProject {
        pluginManager.apply(com.example.ci.kover.KoverCoveragePlugin::class.java)

        extensions.findByType(com.example.ci.kover.KoverCoverageExtension::class.java)?.apply {
            excludedProjects.set(setOf(
                ":app_microbenchmark",
                ":app_baseline",
                // ... other modules to exclude
            ))
        }
    }
}
```

Then run:

```bash
./gradlew koverAllModulesCoverageReport
```

The summary report is written to `build/coverageAllModulesSummary.txt`.

## Extension properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `excludedProjects` | `SetProperty<String>` | `emptySet()` | Module paths to exclude from coverage (e.g. benchmark, demo, test-only modules) |
| `taskPrefix` | `Property<String>` | `"koverModuleCoverage_"` | Prefix for per-module task names |
| `filePrefix` | `Property<String>` | `"coverage_"` | Prefix for per-module output file names |

## Output format

**Per-module file** (`build/coverage_<module>.txt`):
```
Line coverage:   74.32% (1523/2049)
Branch coverage: 61.05% (418/685)
```

**Summary file** (`build/coverageAllModulesSummary.txt`):
```
=== :lib_core ===
Line coverage:   74.32% (1523/2049)
Branch coverage: 61.05% (418/685)

=== :lib_data ===
Line coverage:   88.10% (742/842)
Branch coverage: 79.31% (230/290)
```

## CI example (GitLab)

```yaml
kover_coverage_report:
  extends: .analyse
  only:
    refs:
      - develop
  script:
    - git clone "$INIT_REPO_AUTH" .gradle-init
    - mkdir -p "${GRADLE_USER_HOME:-$HOME/.gradle}"
    - cp .gradle-init/.gradle/init.gradle.kts "${GRADLE_USER_HOME:-$HOME/.gradle}/init.gradle.kts"
    - ./gradlew koverAllModulesCoverageReport
  artifacts:
    paths:
      - build/coverageAllModulesSummary.txt
```

## License

MIT

