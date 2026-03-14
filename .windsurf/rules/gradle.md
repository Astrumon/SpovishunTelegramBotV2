---
trigger: glob
globs: ["build.gradle.kts", "settings.gradle.kts", "gradle/libs.versions.toml", "gradle/**"]
description: Rules for working with Gradle build files and the Version Catalog
---

# Gradle & Build Agent Rules

## Version Catalog

All dependency versions and aliases are defined in `gradle/libs.versions.toml`.

- **Never** add a version number inline in `build.gradle.kts`
- **Always** use `libs.something.library` alias syntax
- To add a new dependency:
  1. Add the version under `[versions]`
  2. Add the library under `[libraries]`
  3. Reference via `libs.*` alias in `build.gradle.kts`

```toml
# gradle/libs.versions.toml
[versions]
newlib = "1.2.3"

[libraries]
newlib = { module = "com.example:newlib", version.ref = "newlib" }
```

```kotlin
// build.gradle.kts
implementation(libs.newlib)
```

## Gradle Tasks

| Task                | Purpose                                |
|---------------------|----------------------------------------|
| `./gradlew runDev`  | Run with `PROFILE=dev` (in-memory DB)  |
| `./gradlew runProd` | Run with `PROFILE=prod` (PostgreSQL)   |
| `./gradlew test`    | Run all unit tests                     |
| `./gradlew build`   | Full build (compile + test + jar)      |

## Kotlin Compiler Options

- JVM target: 21
- Defined in `kotlin { jvmToolchain(21) }`
- Do not change the JVM target without updating the Docker base image

## Rules

- ❌ No hardcoded version strings in `build.gradle.kts`
- ❌ Do not add unauthenticated custom Maven repos without justification
- ✅ All new libs go through Version Catalog first
- ✅ Keep `dependencies {}` grouped: test → env → DI → DB → networking → telegram
