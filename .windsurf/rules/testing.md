---
trigger: glob
globs: src/test/**
description: Rules for writing tests in SpovishunTelegramBotV2
---

# Testing Agent Rules

## Test Structure

```
src/test/kotlin/
├── domain/
│   └── service/       ← unit tests for Services
├── data/
│   └── repository/    ← unit tests for MockImpl repos
└── presentation/
    └── controller/    ← unit tests for Controllers
```

## What to Test

| Layer        | Test target              | Mock                         |
|--------------|--------------------------|------------------------------|
| domain       | `*Service`               | Mock repository interfaces   |
| data/memory  | `*RepositoryMockImpl`    | No mocks needed              |
| presentation | `*Controller`            | Mock services                |

**Do NOT unit test:** Koin modules, `TelegramBot`, `MessageHandler`, `DatabaseFactory`

## Test Conventions

- JUnit 5 (`@Test`, `@BeforeEach`)
- `kotlinx-coroutines-test` for `suspend fun` tests: `runTest { }`
- MockK for mocking: `mockk<MemberRepository>()`
- Arrange / Act / Assert structure
- Test names: backtick format

```kotlin
class MemberServiceTest {
    private val repo = mockk<MemberRepository>()
    private val service = MemberService(repo)

    @Test
    fun `registerMember saves and returns member`() = runTest {
        val member = Member(123L, "danylo", "Danylo", 1L)
        coEvery { repo.save(any()) } returns member

        val result = service.registerMember(123L, "danylo", 1L)

        assertEquals(member, result)
        coVerify(exactly = 1) { repo.save(any()) }
    }
}
```

## Coroutine Testing

- Always use `runTest { }` for suspend functions
- Use `coEvery` / `coVerify` (MockK) for suspending mock functions
- Advance time with `advanceUntilIdle()` for flows or timed logic

## Rules

- ❌ No real DB connections in unit tests
- ❌ No `Thread.sleep()` — use `runTest` time control
- ✅ One `@Test` function = one behaviour being verified
- ✅ Test the public API of a class, not its internals
- ✅ Use `@BeforeEach` to reset mocks between tests
