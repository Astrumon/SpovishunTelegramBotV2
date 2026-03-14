---
trigger: glob
globs: src/main/kotlin/presentation/**
description: Rules for working on the presentation layer — TelegramBot, MessageHandler, Commands, and Controllers
---

# Presentation Layer Agent Rules

The `presentation` layer is the Telegram-facing boundary. It translates incoming updates into domain calls and sends responses back to users.

## What Lives Here

```
presentation/
├── bot/
│   ├── TelegramBot.kt             ← creates bot, starts polling
│   ├── handler/
│   │   └── MessageHandler.kt      ← routes updates to commands
│   └── commands/
│       ├── StartCommand.kt
│       ├── PingCommand.kt
│       ├── GroupCommand.kt
│       ├── MembersCommand.kt
│       └── RegisterCommand.kt
└── controller/
    ├── GroupController.kt         ← business response logic for /group
    └── MembersController.kt       ← business response logic for /members
```

## TelegramBot

- Manages `CoroutineScope(SupervisorJob())` — one failing command never kills the bot
- Calls `startPolling()` in a loop inside `runBlocking`
- `create(token)` builds the bot instance; `startPolling(bot)` starts it

## MessageHandler

- Router: receives an `Update`, extracts the command text, dispatches to the right `Command`
- Use `when` expression keyed on the command string
- Return gracefully (send "unknown command" message) for unrecognised commands
- Do NOT put any business logic here — only routing

```kotlin
when (command) {
    "/start"    -> startCommand.execute(message)
    "/ping"     -> pingCommand.execute(message)
    "/group"    -> groupCommand.execute(message)
    "/members"  -> membersCommand.execute(message)
    "/register" -> registerCommand.execute(message)
    else        -> sendUnknownCommand(bot, message.chat.id)
}
```

## Commands

- Thin layer: parse arguments, call `Controller`, send the result back to Telegram
- All commands implement a common `execute(message: Message)` signature
- Each command is injected with one Controller (one responsibility)
- Never call a `Service` directly from a command — always go through `Controller`

```kotlin
class PingCommand(
    private val bot: TelegramBot,
    private val pingController: PingController
) {
    suspend fun execute(message: Message) {
        val response = pingController.handlePing(message.chat.id)
        bot.sendMessage(message.chat.id, response)
    }
}
```

## Controllers

- Contain the "response logic" — format messages, handle edge cases, call Service
- Receive a `Service` via constructor injection
- Return `String` (message text) or a structured response type
- Handle domain exceptions and convert them to user-friendly messages

```kotlin
class GroupController(private val groupService: GroupService) {
    suspend fun handleCreate(chatId: Long, name: String): String {
        return try {
            val group = groupService.createGroup(chatId, name)
            "✅ Group '${group.name}' created!"
        } catch (e: GroupAlreadyExistsException) {
            "⚠️ Group already exists."
        }
    }
}
```

## Message Formatting Rules

- Use HTML parse mode (`ParseMode.HTML`) for rich text
- Emoji prefix for status: ✅ success, ⚠️ warning, ❌ error, ℹ️ info
- Escape HTML entities in user-provided content: `<` → `&lt;`, `>` → `&gt;`, `&` → `&amp;`
- Keep messages concise — no walls of text

## Adding a New Command — Checklist

1. Create `presentation/bot/commands/{Name}Command.kt`
2. Create `presentation/controller/{Entity}Controller.kt` (if new domain area)
3. Register both in `di/PresentationModule.kt`
4. Add routing entry in `MessageHandler.kt`
5. Ensure `domain/service/` has the required service method
6. Write unit test for the controller

## Rules

- ❌ No DB or Exposed imports in presentation layer
- ❌ No business logic in Command classes
- ❌ No direct Service calls from Commands — must go through Controller
- ✅ One Controller per domain area (Group, Members, etc.)
- ✅ Use `SupervisorJob` scope for all coroutine launches
