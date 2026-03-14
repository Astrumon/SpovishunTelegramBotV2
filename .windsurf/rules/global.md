---
trigger: always_on
description: Global rules for all AI agents working on SpovishunTelegramBotV2
---

# SpovishunTelegramBotV2 — Global Agent Rules

## Project Identity

- **Project:** SpovishunTelegramBotV2
- **Language:** Kotlin 2.3.0 (JVM 21)
- **Build tool:** Gradle with Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`)
- **Bot:** @Spovishun_bot
- **Branches:** `develop` (default), `main` (production)
- **Architecture:** Clean Architecture — `common` / `domain` / `data` / `di` / `presentation`

## Source Root

All Kotlin source files live under `src/main/kotlin/`.

## Dependency Rules

```
presentation → domain ← data
common ← accessible from all layers
di ← wires everything, knows all layers
```

- `domain` must NOT import from `data` or `presentation`
- `data` must NOT import from `presentation`
- `common` must NOT import from any project layer
- Only `data/db/DatabaseFactory.kt` may use `Dispatchers.IO`

## Kotlin Conventions

- Use `suspend fun` for all I/O and database operations
- Prefer `data class` for domain models
- Prefer `interface` + `Impl` pattern for repositories and services
- Use `object` only for stateless singletons (Koin modules, Exposed tables)
- Extension functions → `common/extension/`
- Custom exceptions → `common/exception/`
- Result wrappers → `common/result/`

## Commit Convention

Format: `type: short description`

- Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `ci`, `build`, `perf`
- Description: lowercase English, no period, max 72 chars

Examples:
```
feat: add /leaderboard command
fix: handle null chatId in MessageHandler
refactor: extract GroupController from StartCommand
```

## Branch Convention

```
feature/spovishun-{N}-short-description
```

- Always branch from `develop`
- `short-description`: max 3 words, kebab-case
- `{N}`: next sequential task number, never reuse

## Environment & Configuration

- Config read from `.env` via `dotenv-kotlin` → `AppConfig.kt`
- Never hardcode tokens, passwords, or secrets
- `PROFILE=dev` → in-memory MockImpl repositories
- `PROFILE=prod` → Exposed DB repositories (PostgreSQL)
- `.env` is gitignored — never commit it

## DI (Koin)

- All dependencies registered in `di/` modules
- Profile selection in `Application.kt`:
  ```kotlin
  val repositoryModule = when (System.getenv("PROFILE")) {
      "prod" -> prodRepositoryModule
      else   -> devRepositoryModule
  }
  ```
- Use `single` for services and repositories
- Use `factory` only for stateless, lightweight objects

## File Naming

| Layer         | Pattern                         | Example                       |
|---------------|---------------------------------|-------------------------------|
| Domain model  | `{Entity}.kt`                   | `Member.kt`                   |
| Repository IF | `{Entity}Repository.kt`         | `MemberRepository.kt`         |
| DB impl       | `{Entity}RepositoryImpl.kt`     | `MemberRepositoryImpl.kt`     |
| Mock impl     | `{Entity}RepositoryMockImpl.kt` | `MemberRepositoryMockImpl.kt` |
| Service       | `{Entity}Service.kt`            | `MemberService.kt`            |
| Command       | `{Name}Command.kt`              | `PingCommand.kt`              |
| Controller    | `{Entity}Controller.kt`         | `GroupController.kt`          |
| Koin module   | `{Scope}Module.kt`              | `ServiceModule.kt`            |

## Never Do

- Do not use `UseCase` — use `Service` instead
- Do not put business logic in `Command` classes — delegate to `Controller`
- Do not commit `.env` or any secrets
- Do not use raw SQL strings — use Exposed DSL
- Do not create files outside the established layer structure without justification
