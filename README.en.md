# MeaoToDo

[中文](./README.zh-CN.md) | [English](./README.en.md)

MeaoToDo is a single-device, local-first Android personal assistant. It connects tasks, focus sessions, expenses, and daily reviews into one workflow, with optional AI assistance using a user-provided API key.

## Available Today

- Todo smart views, custom lists, search, filters, sorting, calendars, and full task actions.
- Smart capture with `#今天`, `#明天`, `!高`, `!中`, `!低`, `@list-name`, and `🍅3`.
- Multi-round Pomodoro focus/break runs, task binding, history, and persisted preferences.
- Precise local expense entry, real daily/monthly summaries, categories, and budget pace.
- Focus recommendations, productivity pulse, streaks, and a seven-day trend.
- Assistant context, deterministic command parsing, pending actions, a daily timeline, and a global focus bar.
- BYOK AI with encrypted key storage, compatible APIs, task drafts, daily briefs, reviews, and plan adjustment.
- Opt-in daily brief and evening review automation, limited to one run per day.
- Editable pending actions plus daily request and monthly token limits.
- Optional system notifications when focus or break phases finish.
- JSON backup export through the system picker and confirmation-first, ID-based safe restore; API keys are excluded.

## In Development

- A prompt regression corpus and broader provider compatibility tests.
- More complete Compose UI and Room migration instrumentation tests.
- Finer-grained export selection and future backup schema migrations.

## AI Safety Boundary

- The API key stays on the device and is protected with Android Keystore.
- Model output is parsed into structured drafts or pending actions.
- Local data changes require explicit user confirmation.
- Only the minimum context required for the selected capability is sent.
- Tasks, focus, ledger, and deterministic capture keep working offline.

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
  ui/             Compose screens and theme
```

## Local Run

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## Design Principles

- Local-first: core features do not depend on a network or AI.
- Confirmation-first: assistant suggestions never mutate data invisibly.
- Connected workflows: tasks flow into focus, actual effort, timelines, and reviews.
- Small and useful: suggestions must be grounded, explainable, and reversible.
- Progressive enhancement: ship verifiable vertical slices before broad automation.
