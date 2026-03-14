---
trigger: glob
globs: src/main/kotlin/di/**
description: Rules for working on the DI layer — Koin module registration and profile-based wiring
---

# DI Layer Agent Rules

The `di` layer wires the entire application together using Koin. It's the only place that knows about all layers simultaneously.

## Module Files

```
di/
├── ConfigModule.kt          ← AppConfig (reads .env)
├── ServiceModule.kt         ← domain services
├── PresentationModule.kt    ← TelegramBot, Commands, Controllers
├── RepositoryModule.kt      ← shared base (if any)
├── DevRepositoryModule.kt   ← MockImpl bindings (PROFILE=dev)
└── ProdRepositoryModule.kt  ← DB impl bindings (PROFILE=prod)
```

## Module Registration Pattern

```kotlin
// ServiceModule.kt
val serviceModule = module {
    single { MemberService(get()) }
    single { GroupService(get()) }
    single { AutoRegisterService(get(), get()) }
}

// DevRepositoryModule.kt
val devRepositoryModule = module {
    single<MemberRepository> { MemberRepositoryMockImpl() }
    single<GroupRepository> { GroupRepositoryMockImpl() }
    single<GroupMemberRepository> { GroupMemberRepositoryMockImpl() }
}

// ProdRepositoryModule.kt
val prodRepositoryModule = module {
    single<MemberRepository> { MemberRepositoryImpl() }
    single<GroupRepository> { GroupRepositoryImpl() }
}
```

## Profile Selection (Application.kt)

```kotlin
val repositoryModule = when (System.getenv("PROFILE")) {
    "prod" -> prodRepositoryModule
    else   -> devRepositoryModule
}
startKoin {
    modules(configModule, repositoryModule, serviceModule, presentationModule)
}
```

## Rules for Adding a New Component

**New Service:**
1. Define the class in `domain/service/`
2. Register `single { NewService(get()) }` in `ServiceModule.kt`

**New Repository:**
1. Define interface in `domain/repository/`
2. Create MockImpl in `data/memory/repository/`
3. Create DbImpl in `data/db/repository/`
4. Register in both `DevRepositoryModule.kt` and `ProdRepositoryModule.kt`

**New Command/Controller:**
1. Create both classes in `presentation/`
2. Register both with `single` in `PresentationModule.kt`

## Koin Best Practices

- Always bind to the **interface** type: `single<MemberRepository> { MemberRepositoryImpl() }`
- Use `get()` for constructor injection — never resolve manually
- Prefer `single` over `factory` for services and repos
- Keep modules focused — one module per layer
- Never use `KoinComponent` in domain or data layers — only in `Application.kt`

## Rules

- ❌ No business logic in DI modules
- ❌ No `KoinComponent` outside `Application.kt`
- ✅ All interface bindings use the interface type as the generic parameter
- ✅ Profile selection is the only conditional logic allowed here
