---
trigger: glob
globs: src/main/kotlin/data/**
description: Rules for working on the data layer — DB implementations, mock implementations, mappers, and DatabaseFactory
---

# Data Layer Agent Rules

The `data` layer provides concrete implementations of domain repository interfaces. It contains two separate implementations: in-memory mocks for `dev` and Exposed DB implementations for `prod`.

## What Lives Here

```
data/
├── mapper/            ← ResultRow → domain model mapping functions
├── memory/
│   └── repository/    ← MockImpl: in-memory Map-based repos (dev)
└── db/
    ├── DatabaseFactory.kt     ← Exposed init, dbQuery wrapper
    ├── table/                 ← Exposed Table objects
    └── repository/            ← DB implementations (prod)
```

## Exposed Tables (`data/db/table/`)

- Extend `Table` (or `IntIdTable` / `LongIdTable`)
- Column names: `snake_case`
- Table names: plural `snake_case` (e.g. `members`, `groups`)
- Always define `uniqueIndex()` for Telegram IDs

```kotlin
object Members : LongIdTable("members") {
    val telegramId = long("telegram_id").uniqueIndex()
    val username = varchar("username", 255)
    val firstName = varchar("first_name", 255)
    val groupId = long("group_id").references(Groups.id)
}
```

## DB Repositories (`data/db/repository/`)

- Implement the domain repository interface
- All DB operations wrapped in `dbQuery { }` from `DatabaseFactory`
- Map `ResultRow` → domain model using functions from `data/mapper/`
- Class name: `{Entity}RepositoryImpl`

```kotlin
class MemberRepositoryImpl : MemberRepository {
    override suspend fun findByTelegramId(telegramId: Long): Member? = dbQuery {
        Members
            .select { Members.telegramId eq telegramId }
            .singleOrNull()
            ?.toMember()
    }
}
```

## Mappers (`data/mapper/`)

- Extension functions on `ResultRow`
- One file per entity: `MemberMapper.kt`, `GroupMapper.kt`
- Naming: `ResultRow.toMember()`, `ResultRow.toGroup()`

```kotlin
fun ResultRow.toMember() = Member(
    telegramId = this[Members.telegramId],
    username = this[Members.username],
    firstName = this[Members.firstName],
    groupId = this[Members.groupId]
)
```

## Mock Repositories (`data/memory/repository/`)

- Use `mutableMapOf<Long, Entity>()` as in-memory store
- Class name: `{Entity}RepositoryMockImpl`
- Implement full domain interface — same API, no persistence
- Safe for testing — no DB required

```kotlin
class MemberRepositoryMockImpl : MemberRepository {
    private val store = mutableMapOf<Long, Member>()

    override suspend fun findByTelegramId(telegramId: Long): Member? =
        store[telegramId]

    override suspend fun save(member: Member): Member =
        member.also { store[it.telegramId] = it }
}
```

## DatabaseFactory

- Handles DB connection init (SQLite for dev-db testing, PostgreSQL for prod)
- Exposes `dbQuery` wrapper:
  ```kotlin
  suspend fun <T> dbQuery(block: () -> T): T =
      withContext(Dispatchers.IO) { transaction { block() } }
  ```
- **Only this file** may use `Dispatchers.IO` in the entire project
- Call `SchemaUtils.create()` for table creation on startup

## Rules

- ❌ Never import Telegram SDK in data layer
- ❌ Never call services from data layer
- ✅ Use `dbQuery` for every DB operation — never use bare `transaction {}`
- ✅ Always return domain models, never expose `ResultRow` outside the mapper
- ✅ MockImpl and DbImpl must implement the exact same interface
