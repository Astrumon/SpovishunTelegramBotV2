---
name: kotlin-coroutines-expert
description: Use this skill for questions about Kotlin Coroutines, Flow, suspend functions, structured concurrency, or async programming patterns. Triggers on "coroutine", "suspend", "Flow", "async", "launch", "withContext", or concurrency-related questions.
---

# Kotlin Coroutines Expert

You are an expert in Kotlin coroutines and structured concurrency. You help write correct, leak-free async code.

## Core Rules

### Dispatcher Selection
```kotlin
withContext(Dispatchers.IO)      { /* DB, network, file I/O */ }
withContext(Dispatchers.Default) { /* CPU-intensive: sorting, parsing */ }
withContext(Dispatchers.Main)    { /* UI updates (Android only) */ }
```

### Structured Concurrency
- Every coroutine must belong to a scope
- Use `coroutineScope { }` to create child scopes — parent waits for all children
- NEVER use `GlobalScope` in production — causes memory leaks
- Cancel the scope when it's no longer needed

```kotlin
// Correct: bounded scope
class BotService(private val scope: CoroutineScope) {
    fun start() {
        scope.launch { processUpdates() }
    }
}

// Wrong: unbound scope
GlobalScope.launch { processUpdates() } // DON'T DO THIS
```

### Cancellation
- Always handle `CancellationException` — never catch and swallow it
- Use `isActive` checks in long loops
- `withTimeout` throws `TimeoutCancellationException` (subclass of `CancellationException`)

```kotlin
// Correct cancellation handling
try {
    val result = withTimeout(5000L) { fetchData() }
} catch (e: TimeoutCancellationException) {
    logger.warn("Fetch timed out")
    // Don't rethrow — handle gracefully
}
```

### Error Handling
```kotlin
// For parallel tasks: use supervisorScope to isolate failures
supervisorScope {
    val job1 = async { task1() }
    val job2 = async { task2() }
    listOf(job1, job2).map { it.await() } // one failure won't cancel others
}
```

## Flow Patterns
```kotlin
// StateFlow for state
private val _users = MutableStateFlow<List<User>>(emptyList())
val users: StateFlow<List<User>> = _users.asStateFlow()

// Collect safely
scope.launch {
    users.collect { userList ->
        updateUI(userList)
    }
}
```

## Project-Specific Rules (Spovishun)
- Only `data/db/DatabaseFactory.kt` may use `Dispatchers.IO`
- All I/O and DB functions are `suspend fun`
- Coroutine context is the caller's responsibility — never impose `Dispatchers.IO` outside `DatabaseFactory`
- `TelegramBot` runs a `CoroutineScope(SupervisorJob())` — one failing command never kills the bot

## Common Mistakes
- Calling `runBlocking` from a coroutine — causes deadlock
- Ignoring `Job` references — can't cancel orphaned coroutines
- Using `.value` on StateFlow in concurrent code without synchronization
