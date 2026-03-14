---
trigger: glob
globs: src/main/kotlin/domain/**
description: Rules for working on the domain layer — models, repository interfaces, and services
---

# Domain Layer Agent Rules

The `domain` layer is the heart of the application. It contains business logic, domain models, and repository interfaces. It has **zero knowledge** of Telegram, databases, or Koin.

## What Lives Here

```
domain/
├── model/          ← data classes: Member, Group, etc.
├── repository/     ← suspend interface definitions
└── service/        ← business logic (MemberService, GroupService, AutoRegisterService)
```

## Domain Models (`domain/model/`)

- Use `data class` for all models
- Fields reflect business concepts, not DB column names
- IDs are `Long` (Telegram user/chat IDs) or `Int` (internal sequences)
- No nullable fields unless the value is genuinely optional in the domain

```kotlin
// Good
data class Member(
    val telegramId: Long,
    val username: String,
    val firstName: String,
    val groupId: Long
)

// Bad — DB concerns leaking into domain
data class Member(
    val id: Int,        // internal DB id exposed
    val tg_id: Long,    // snake_case is not Kotlin
    val group_ref: Long
)
```

## Repository Interfaces (`domain/repository/`)

- Interface only — no implementation here
- All functions must be `suspend fun`
- Return domain models, never DB entities or raw rows
- Use descriptive names: `findByTelegramId`, `findAllByGroupId`, not `get`, `fetch`

```kotlin
interface MemberRepository {
    suspend fun findByTelegramId(telegramId: Long): Member?
    suspend fun findAllByGroupId(groupId: Long): List<Member>
    suspend fun save(member: Member): Member
    suspend fun delete(telegramId: Long)
}
```

## Services (`domain/service/`)

- Contains business logic and orchestration
- Receives repository interfaces via constructor (injected by Koin)
- All functions `suspend fun`
- Services may call other services only through constructor injection (no circular deps)
- Never access the Telegram SDK or DB directly

```kotlin
class MemberService(
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) {
    suspend fun registerMember(telegramId: Long, username: String, groupId: Long): Member {
        val group = groupRepository.findById(groupId)
            ?: throw GroupNotFoundException(groupId)
        return memberRepository.save(Member(telegramId, username, username, groupId))
    }
}
```

## Rules

- ❌ No Telegram SDK imports
- ❌ No Exposed / JDBC / SQL imports
- ❌ No Koin imports
- ❌ No `Dispatchers.IO` — coroutine context is the caller's responsibility
- ✅ May use `common/` utilities
- ✅ May throw custom exceptions from `common/exception/`
