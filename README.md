# kover-coverage-plugin

A Gradle plugin that registers per-module and aggregated [Kover](https://github.com/Kotlin/kotlinx-kover) coverage tasks for multi-module Android projects.  
Designed to be applied via a Gradle **init script** — no changes to the consuming project required.

## Tasks registered

| Task | Description |
|------|-------------|
| `koverModuleCoverage_<module>` | Parses the Kover XML report for a single module and writes `Line / Branch coverage %` to a `.txt` file |
| `koverAllModuleCoverage` | Lifecycle task — depends on all per-module tasks and triggers the summary task when done |
| `koverAllModulesCoverageReport` | Aggregates all per-module `.txt` files into a single `coverageAllModulesSummary.txt` sorted table |

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
        classpath("com.github.YOUR_ORG:kover-coverage-plugin:0.0.2")
    }
}
```

## Usage

Apply the plugin in your init script and configure the extension:

```kotlin
// .gradle/init.gradle.kts
if (System.getenv("CI")?.toBoolean() == true) {
    rootProject {
        pluginManager.apply(KoverCoveragePlugin::class.java)

        extensions.findByType(KoverCoverageExtension::class.java)?.apply {
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
./gradlew koverAllModuleCoverage
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
Kover coverage summary by module
Sources: 2 module file(s)

module    | coverage
----------|----------
:lib_data |   88.10%
:lib_core |   74.32%

Average (unweighted): 81.21%
```

Modules are listed in descending order by line coverage percentage. The average is unweighted (mean of per-module percentages).

## CI example (GitLab)

```yaml
kover_coverage_report:
  only:
    refs:
      - develop
  script:
    - git clone "$INIT_REPO_AUTH" .gradle-init
    - mkdir -p "${GRADLE_USER_HOME:-$HOME/.gradle}"
    - cp .gradle-init/.gradle/init.gradle.kts "${GRADLE_USER_HOME:-$HOME/.gradle}/init.gradle.kts"
    - ./gradlew koverAllModuleCoverage
  artifacts:
    paths:
      - build/coverageAllModulesSummary.txt
```

## License

MIT
