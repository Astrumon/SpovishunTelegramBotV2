---
trigger: always_on
---

# Commit Convention

All commits must follow this format:

```
type: short description
```

## Allowed types

| Type       | When to use                              |
|------------|------------------------------------------|
| `feat`     | New feature or command                   |
| `fix`      | Bug fix                                  |
| `refactor` | Code change without behavior change      |
| `docs`     | Documentation only                       |
| `chore`    | Tooling, dependencies, build config      |
| `test`     | Adding or updating tests                 |
| `ci`       | CI/CD pipeline changes                   |
| `build`    | Gradle or build system changes           |
| `perf`     | Performance improvement                  |

## Rules

- Description in **English**, **lowercase**, **no trailing period**
- Maximum **72 characters** total
- One **logical change** per commit — don't mix unrelated changes
- Use **imperative mood**: `add`, `fix`, `remove` — not `added`, `fixed`

## ✅ Good examples

```
feat: add /ping dota command
fix: handle null pointer in MemberRepositoryImpl
chore: add libs.versions.toml with all dependencies
refactor: extract MessageHandler routing logic
docs: update architecture overview in README
test: add unit tests for UserRepository
ci: add GitHub Actions workflow for develop branch
build: set jvmTarget to 21 in build.gradle.kts
```

## ❌ Never do

```
# Capital letter
Feat: Add ping command

# Trailing period
feat: add /ping command.

# Vague
fix: bug fix
update: changes
wip: stuff

# Mixed unrelated changes
feat: add /ping command and fix null pointer and update README
```
