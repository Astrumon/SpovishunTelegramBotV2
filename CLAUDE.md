# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SpovishunTelegramBotV2 — a Kotlin Telegram bot built with Clean Architecture. Stack: Kotlin 2.3.0 (JVM 21), Gradle Kotlin DSL + Version Catalog, Koin 3.x (DI), Exposed 0.55.0 (ORM), Flyway 10.x (migrations), SQLite (dev), PostgreSQL (prod), dotenv-kotlin, SLF4J + Logback.

## Commands

```bash
./gradlew runDev           # run with PROFILE=dev (in-memory repositories)
./gradlew runProd          # run with PROFILE=prod (PostgreSQL + Flyway migrations)
./gradlew test             # run all tests
./gradlew build            # compile + test + jar
./gradlew generateMigration  # interactive: generates next versioned SQL migration file
```

To run a single test class:
```bash
./gradlew test --tests "domain.service.MemberServiceTest"
```

## Architecture

Layers and allowed dependency direction:
```
presentation → domain ← data
common ← accessible from all layers
di ← wires everything, knows all layers
```

**Hard rules per layer:**
- `domain/` — no Telegram SDK, no Exposed/JDBC, no Koin, no `Dispatchers.IO`
- `data/` — no Telegram SDK, never call services
- `common/` — pure Kotlin only, zero project imports
- `presentation/` — no Exposed/DB imports; no business logic in Command classes
- Only `data/db/DatabaseFactory.kt` may use `Dispatchers.IO`

## Key Patterns

**Result wrapper:** Services return `ResultContainer<T>` (wraps Kotlin's `Result<T>`). Repository interfaces also return `ResultContainer`.

**DB access:** Every DB operation uses `dbQuery { }` from `DatabaseFactory` — never a bare `transaction {}`.

**Profile-based DI:** `Application.kt` selects the repository module based on `PROFILE` env var (`dev` → MockImpl, `prod` → DB impls). All repository bindings use the interface type: `single<MemberRepository> { MemberRepositoryImpl() }`.

**Coroutines:** All I/O and DB functions are `suspend fun`. Coroutine context is the caller's responsibility — never impose `Dispatchers.IO` outside `DatabaseFactory`.

**Telegram bot:** `TelegramBot` runs a `CoroutineScope(SupervisorJob())` so one failing command never kills the bot. `MessageHandler` routes updates to commands via `when` expression — no business logic there.

**Command flow:** `Command` parses args → calls `Controller` → sends result to Telegram. Never call a `Service` directly from a `Command`.

**Role system:** `MemberRole` enum (`ADMIN`, `MODERATOR`, `MEMBER`) lives in `domain/model/`. Access checks in controllers use `MemberService.getMemberByChatAndUserId()` — DB-based, not Telegram API. `BotAdminUtils` (`presentation/util/`) calls Telegram API only during registration/auto-registration to derive the initial role for new members.

## File Naming

| Layer | Pattern | Example |
|---|---|---|
| Domain model | `{Entity}.kt` | `Member.kt` |
| Repository interface | `{Entity}Repository.kt` | `MemberRepository.kt` |
| DB impl | `{Entity}RepositoryImpl.kt` | `MemberRepositoryImpl.kt` |
| Mock impl | `{Entity}RepositoryMockImpl.kt` | `MemberRepositoryMockImpl.kt` |
| Service | `{Entity}Service.kt` | `MemberService.kt` |
| Command | `{Name}Command.kt` | `PingCommand.kt` |
| Controller | `{Entity}Controller.kt` | `GroupController.kt` |
| Koin module | `{Scope}Module.kt` | `ServiceModule.kt` |

**Never use `UseCase` — use `Service` instead.**

## Adding a New Command (Checklist)

1. Create `presentation/bot/commands/{Name}Command.kt`
2. Create `presentation/controller/{Entity}Controller.kt` (if new domain area)
3. Register both with `single` in `di/PresentationModule.kt`; inject `BotAdminUtils` if the command needs admin/role checks
4. Add routing entry in `presentation/bot/handler/MessageHandler.kt`
5. Ensure `domain/service/` has the required service method
6. Write unit test for the controller

## Adding a New Repository (Checklist)

1. Define interface in `domain/repository/`
2. Create `{Entity}RepositoryMockImpl` in `data/memory/repository/`
3. Create `{Entity}RepositoryImpl` in `data/db/repository/`
4. Register in both `di/DevRepositoryModule.kt` and `di/ProdRepositoryModule.kt`

## Database Migrations

Migrations run automatically on startup in `prod` via Flyway. Migration files live in `src/main/resources/db/migration/` (PostgreSQL only — no separate SQLite directory).

To add a migration:
1. Update the `Table` object in `data/db/table/`
2. Run `./gradlew generateMigration` and enter a description
3. Review the generated SQL
4. Commit the `Table` file and migration script together

Never edit a migration file after it has been applied to any database.

## Source Sets & Key Files

| Source set | Path | Gradle task |
|---|---|---|
| `main` | `src/main/kotlin/` | — |
| `test` (unit) | `src/test/kotlin/` | `./gradlew test` |
| `integrationTest` | `src/integrationTest/kotlin/` | `./gradlew integrationTest` |
| `e2eTest` | `src/e2eTest/kotlin/` | `./gradlew e2eTest` |

Key files:
- `src/main/kotlin/Application.kt` — `Application.run()` starts Koin + polling; `Application.initializeKoin()` can be called in isolation for in-process test setup
- `src/main/kotlin/presentation/bot/handler/MessageHandler.kt` — routes updates to commands via `when`
- `src/main/kotlin/di/DevRepositoryModule.kt` / `ProdRepositoryModule.kt` — profile-based repo wiring

Implemented commands (6): `StartCommand`, `RegisterCommand`, `PingCommand`, `GroupCommand`, `GrantRoleCommand`, `MembersCommand`

MockImpl repos (4): `MemberRepositoryMockImpl`, `ChatRepositoryMockImpl`, `GroupRepositoryMockImpl`, `GroupMemberRepositoryMockImpl`

## Testing

- JUnit 5 + MockK + `kotlinx-coroutines-test`
- Always use `runTest { }` for `suspend fun` tests
- Use `coEvery` / `coVerify` for suspending mocks
- Test names use backtick format: `` `createMember should return success when username is unique` ``
- `@BeforeTest` to `clearAllMocks()` between tests
- No real DB connections in unit tests (use MockImpl repos or H2 for integration tests)

| Layer | Test target | How to mock |
|---|---|---|
| `domain/service/` | `*Service` | `mockk<*Repository>()` |
| `data/memory/` | `*RepositoryMockImpl` | no mocks needed |
| `presentation/controller/` | `*Controller` | `mockk<*Service>()` |

Do NOT unit test: Koin modules, `TelegramBot`, `MessageHandler`, `DatabaseFactory`.

**Integration test pattern** (`BaseIntegrationTest`): real MockImpl repos → real services → real commands/controllers → only `Bot` and `BotAdminUtils` are `mockk()`. Never use Koin in integration tests — wire manually.

**e2e test env vars**: `TEST_BOT_TOKEN`, `TEST_HELPER_BOT_TOKEN`, `TEST_CHAT_ID`, `TEST_ADMINS`. Tests skip gracefully if unset.

## Gradle — Version Catalog

All dependency versions live in `gradle/libs.versions.toml`. Never add inline version strings to `build.gradle.kts`. Always add new libs via:
1. `[versions]` entry in the TOML
2. `[libraries]` entry in the TOML
3. `libs.*` alias reference in `build.gradle.kts`

Keep `dependencies {}` grouped: test → env → DI → DB → networking → telegram.

## Commit Convention

Format: `type: short description` — lowercase English, no trailing period, max 72 chars, imperative mood.

Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `ci`, `build`, `perf`

## Branch Convention

```
feature/spovishun-{N}-short-description
```

- Always branch from `develop`; `main` is production
- `{N}`: next sequential task number, never reuse
- `short-description`: max 3 words, kebab-case

## MCP Servers & Plugins

### Project-level MCP servers (`.claude/settings.json`)

| Server | Purpose | Transport |
|---|---|---|
| `context7` | Up-to-date, version-specific library docs (Exposed, Ktor, Koin, etc.) | stdio (`@upstash/context7-mcp`) |

Append `use context7` to any prompt to fetch current docs before code generation.

### User-level setup (requires credentials)

**GitHub MCP** — issues, PRs, code search:
```bash
claude mcp add-json github '{"type":"http","url":"https://api.githubcopilot.com/mcp","headers":{"Authorization":"Bearer <YOUR_GITHUB_PAT>"}}'
```

**DBHub (PostgreSQL)** — live schema inspection and SQL queries:
```bash
claude mcp add db -- npx -y @bytebase/dbhub@latest --dsn "$DATABASE_URL"
```
> Use a read-only connection string. Never store credentials in committed files.

### Plugins (user-level)

| Plugin | Purpose | Install |
|---|---|---|
| `pg@aiguide` | PostgreSQL skills + doc search MCP (`mcp.tigerdata.com`) | `claude plugin install pg@aiguide` |
| `pr-review-toolkit` | Multi-agent PR review (tests, types, errors, docs) | `claude plugin install pr-review-toolkit@claude-code-plugins` |

Marketplaces required: `timescale/pg-aiguide`, `anthropics/claude-code`.

## Environment Variables

Copy `.env.example` to `.env` (gitignored). Required variables:

| Variable | Example |
|---|---|
| `TELEGRAM_BOT_TOKEN` | `123456:ABC-DEF...` |
| `ADMINS` | `123456789,987654321` |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/spovishun` |
| `DATABASE_DRIVER` | `org.postgresql.Driver` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | `secret` |
| `PROFILE` | `dev` or `prod` |
