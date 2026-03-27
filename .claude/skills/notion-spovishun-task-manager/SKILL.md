---
name: notion-spovishun-task-manager
description: Use this skill for ALL task management operations in the Spovishun project — creating tasks, updating status, reading the board, assigning branch names, or planning a sprint. Always use this skill when the user mentions "задача", "таск", "борд", "спринт", "створи задачу", "додай таск", "що в борді", "які задачі", "закрий задачу", "переведи в done", or any task/board-related request in the context of the Spovishun project. Combines Notion board operations with Spovishun-specific conventions (task numbering, branch naming, architecture layers). For general (non-Spovishun) Notion board operations, use notion-task-board-manager instead.
---

# Notion Spovishun Task Manager

This skill covers the full workflow for managing tasks on the Spovishun project Notion board — reading, creating, and updating tasks according to project conventions.

---

## Key IDs

| Resource | ID / URL |
|---|---|
| Root page | `3183462f-68a9-803a-a93a-e34eb81d2659` |
| Board collection | `3193462f-68a9-80b8-99b9-000bcbf3b536` |
| CLAUDE.md | `https://www.notion.so/31c3462f68a9819c8150ff31d729293e` |

---

## Project Conventions

### Task numbering
- Format: `feature/spovishun-N-short-description`
- `N` — next sequential number (always fetch board to find max N)
- `short-description` — maximum **3 words** in kebab-case
- Example: `feature/spovishun-16-add-user-auth`

### Task title in Notion
- Property name is **Name** (not Title) — case-sensitive
- Format: `feature/spovishun-N: task name`
- No emoji in title — emoji goes in the `icon` field

---

## Step 0: Initialization (always first)

Before any board operation, silently fetch CLAUDE.md:

```
Notion:notion-fetch(id: "https://www.notion.so/31c3462f68a9819c8150ff31d729293e")
```

Do not announce this step.

---

## Reading the Board

### Fetch all tasks
```
Notion:notion-search(
  query: "",
  data_source_url: "collection://3193462f-68a9-80b8-99b9-000bcbf3b536"
)
```

### Filter by status
After fetching, filter locally by the `Status` field:
- `Not started` — new tasks, not yet picked up
- `In progress` — active development
- `Done` — completed

---

## Creating a Task

### Step 1: Determine the next number
1. Read the board via `notion-search` on the collection
2. Find the highest N from existing task titles
3. Next number = max + 1

### Step 2: Compose task data

| Field | Value |
|---|---|
| Name | `feature/spovishun-N: task name` |
| Status | `Not started` |

### Step 3: Page content structure

Every task page must include all **five** sections:

```
## 🎯 Goal
What is the purpose of this task and what outcome is expected.

## 🌿 Branch name
feature/spovishun-{N}-short-description

## 📋 Steps
1. First step
2. Second step
3. ...

## ✅ Definition of Done
> A concrete condition — when this task is considered complete.

🤖 prompt  ← toggle (collapsible)
  A professional, detailed prompt in English for AI agents.
  Includes: task context, tech stack, files/modules, expected output, constraints and conventions.
```

> The `🤖 prompt` toggle is added at the end of the page. Always write the prompt in English, professional and precise — suitable for Claude Code or another AI agent to pick up and execute autonomously.

### Step 4: Create with icon

Pass `icon` directly inside `notion-create-pages` — no separate `API-patch-page` call needed:

```
Notion:notion-create-pages(
  parent: { type: "data_source_id", data_source_id: "3193462f-68a9-80b8-99b9-000bcbf3b536" },
  pages: [{
    properties: { "Name": "feature/spovishun-N: task name", "Status": "Not started" },
    icon: "✨",
    content: "..."
  }]
)
```

> ⚠️ Always fetch the board first to confirm exact property names — they are case-sensitive.

---

## Updating a Task

### Change status
```
Notion:notion-update-page(
  page_id: "<task-id>",
  properties: {
    "Status": "In progress"
  }
)
```

### Typical status flow
```
Not started → In progress → Done
```

---

## Board Display Format

When the user asks "show the board" or "what's in progress":

```
🔵 In progress (N)
  - feature/spovishun-12: task name
  - feature/spovishun-13: task name
📋 Not started (N)
  - feature/spovishun-14: task name
✅ Done (last 3)
  - feature/spovishun-11: task name
```

---

## Common Mistakes to Avoid

| ❌ Wrong | ✅ Correct |
|---|---|
| Emoji in task title | Only in `icon` via `notion-create-pages` |
| Missing Goal / Steps / DoD sections | Always fill in the full structure |
| Missing `🤖 prompt` toggle | Always include as a collapsible block at the end |
| Prompt toggle written in Ukrainian | Write AI prompt in English, professional tone |
| More than one task In progress | Remind the user of the one-at-a-time rule |
| Using board page ID as `data_source_id` | Fetch the board, extract the `collection://` URL |
| Separate `API-patch-page` call for icon | Pass `icon` directly in `notion-create-pages` |
| Using "Title" as property name | The correct property name is **Name** |

---

## Related Skills

- **notion-workflow-spovishun** — project initialization, CLAUDE.md
- **notion-task-to-code** — convert task to AI agent prompt
- **notion-page-builder** — detailed page content formatting
- **notion-database-manager** — board schema changes
- **notion-content-reader** — searching tasks by name
