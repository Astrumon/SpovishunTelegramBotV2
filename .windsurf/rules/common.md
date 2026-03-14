---
trigger: glob
globs: src/main/kotlin/common/**
description: Rules for working on the common layer — shared utilities, extensions, exceptions, and result types
---

# Common Layer Agent Rules

The `common` layer provides shared building blocks used across all other layers. It has **zero dependencies** on any other project layer — it's the foundation.

## What Lives Here

```
common/
├── exception/    ← custom exception classes
├── extension/    ← Kotlin extension functions
└── result/       ← Result/Either wrappers
```

## Custom Exceptions (`common/exception/`)

- Extend `RuntimeException` or `Exception`
- Named by domain concept, not technical cause: `MemberNotFoundException`, not `DatabaseLookupException`
- Include relevant context in the message

```kotlin
class MemberNotFoundException(val telegramId: Long) :
    RuntimeException("Member not found: telegramId=$telegramId")

class GroupAlreadyExistsException(val groupId: Long) :
    RuntimeException("Group already exists: id=$groupId")
```

## Extension Functions (`common/extension/`)

- Only pure, stateless extensions — no DI, no DB, no Telegram SDK
- Group by receiver type: `StringExtensions.kt`, `LongExtensions.kt`

```kotlin
// StringExtensions.kt
fun String.truncate(max: Int): String =
    if (length <= max) this else substring(0, max) + "..."

fun String.toTelegramMention(userId: Long): String =
    "<a href=\"tg://user?id=$userId\">$this</a>"
```

## Result Wrappers (`common/result/`)

- Use where callers genuinely need to handle both success and failure without try/catch
- Keep it simple — Kotlin's built-in `Result<T>` or a minimal sealed class

```kotlin
sealed class BotResult<out T> {
    data class Success<T>(val value: T) : BotResult<T>()
    data class Failure(val error: String) : BotResult<Nothing>()
}
```

## Rules

- ❌ No imports from `domain`, `data`, `di`, or `presentation`
- ❌ No Telegram SDK, Exposed, or Koin imports
- ✅ Pure Kotlin only
- ✅ Fully unit-testable without any mocks
