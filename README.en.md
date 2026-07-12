# MeaoToDo

[中文](./README.zh-CN.md) | [English](./README.en.md)

MeaoToDo is a single-device, local-first Android personal assistant. It connects tasks, calendars, focus sessions, expenses, and daily reviews into one workflow, with optional AI assistance using a user-provided API key.

## UI v2 Navigation

The app has four stable top-level destinations:

- **Today**: unified input, next-step guidance, daily status, today’s tasks, and pending actions.
- **Plan**: separate Tasks and Calendar pages.
- **Focus**: a dedicated timer with task binding, rounds, preferences, history, and immersive mode.
- **Record**: Timeline, Ledger, and Insights.

AI is integrated into the workflow rather than isolated in a separate feature. Inputs on Today are parsed locally or by the configured model into structured drafts, and data changes happen only after explicit confirmation.

## Available Today

- Tasks: smart views, custom lists, search, filters, sorting, editing, moving, duplication, and archiving.
- Calendar: week, month, and year views with date-aware task creation and editing.
- Smart capture with `#今天`, `#明天`, `!高`, `!中`, `!低`, `@list-name`, and `🍅3`.
- Focus: multi-round focus/break runs, pause/resume/skip/cancel, task binding, wheel-based preferences, history, digital/flip clocks, and landscape immersive mode.
- Ledger: precise local expense entry, real daily/monthly summaries, categories, and budget pace.
- Insights: focus recommendations, productivity pulse, streaks, seven-day trends, and real spending pace.
- Assistant: shared daily context, deterministic command parsing, editable pending actions, a daily timeline, and a global focus dock.
- BYOK AI with encrypted key storage, compatible APIs, task drafts, daily briefs, reviews, and plan adjustment.
- Optional daily brief and evening review triggers when the app context is available, limited to once per day.
- Daily request and monthly token limits; all local data changes require confirmation.
- Optional system notifications when focus or break phases finish.
- JSON backup export through the system picker and confirmation-first, ID-based safe restore; API keys are excluded.

## In Development

- Full emulator screenshot review and Xiaomi 10s / Xiaomi 15 device validation.
- A prompt regression corpus and broader provider compatibility tests.
- More complete Compose UI and Room migration instrumentation tests.
- Finer-grained export selection and future backup schema migrations.

## AI Safety Boundary

- The API key stays on the device and is protected with Android Keystore.
- Model output is parsed into structured drafts or pending actions.
- Local data changes require explicit user confirmation.
- Only the minimum context required for the selected capability is sent.
- Tasks, calendars, focus, ledger, and deterministic capture keep working offline.

## Tech Stack

- Kotlin / Java 17
- Jetpack Compose
- Room
- DataStore
- Kotlin Coroutines / Flow
- Kotlin Serialization

## Project Layout

```text
app/src/main/java/com/kdlay/meaotodo/
  core/           App container, settings, and shared utilities
  data/           Room entities, DAOs, and repositories
  domain/         Cross-module context, actions, and use cases
  ai/             AI connectivity, prompts, structured output, and validation
  ui/             Compose screens, shared components, and theme
```

## Local Run

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

## Design Principles

- Local-first: core features do not depend on a network or AI.
- Confirmation-first: assistant suggestions never mutate data invisibly.
- Connected workflows: tasks flow into focus, actual effort, timelines, and reviews.
- Small and useful: suggestions must be grounded, explainable, and reversible.
- Progressive enhancement: ship verifiable vertical slices before broad automation.
