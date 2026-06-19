# MeaoToDo

[中文](./README.zh-CN.md) | [English](./README.en.md)

MeaoToDo is a local-first Android productivity app designed for a two-phone workflow.

- **Main phone**: quick input for Todo, Pomodoro, and expenses.
- **Spare phone**: an always-on Wi‑Fi dashboard that displays today's tasks, current focus timer, and spending overview.

## Motivation

Most productivity apps assume that the user only works with one phone. MeaoToDo is designed to make a spare Android phone useful again by turning it into a low-distraction desk dashboard.

## MVP Scope

- TodoList: create tasks, complete tasks, today view, priority, due date.
- Pomodoro: start, pause, finish, bind to task, save focus history.
- Ledger: quick expense entry, category, daily spending, monthly spending.
- Board Mode: large text, low brightness, portrait/landscape layout, anti burn-in hooks.
- Wi‑Fi Sync: LAN device discovery, manual pairing, and incremental sync framework.

## Out of Scope for MVP

- Cloud sync
- Multi-user collaboration
- AI receipt recognition
- Complex calendar features
- iOS / Web / Desktop clients

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Android NSD / LAN communication interfaces
- Local-first data model

## Two-phone Workflow

```text
Xiaomi 15 main phone
  ├─ Add Todo quickly
  ├─ Start Pomodoro sessions
  ├─ Record expenses quickly
  └─ Push incremental data over Wi‑Fi

Xiaomi 10s spare phone
  ├─ Enter Board Mode
  ├─ Show today's tasks
  ├─ Show the current Pomodoro timer
  └─ Show daily/monthly spending overview
```

## Project Layout

```text
app/src/main/java/com/kdlay/meaotodo/
  core/           App container and shared utilities
  data/           Room entities, DAO, and repositories
  domain/         Business models and use cases
  sync/           Wi‑Fi discovery, pairing, and sync protocol
  ui/             Compose screens and theme
```

## Development Roadmap

1. Set up the Android project skeleton.
2. Implement local Todo database and UI.
3. Implement Pomodoro state machine and history.
4. Implement minimal expense entry and statistics.
5. Implement Board Mode.
6. Implement Wi‑Fi discovery and manual pairing.
7. Implement incremental sync.

## Local Run

This repository does not commit the Gradle Wrapper binary jar at the initial stage. Open the project with Android Studio, or run locally:

```bash
gradle wrapper
./gradlew :app:assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Design Principles

- Local-first: Todo, Pomodoro, and Ledger must work without network access.
- Wi‑Fi-first sync: exchange only necessary incremental data on the LAN.
- Spare-phone-first dashboard: Board Mode is not just a magnified phone UI.
- Small MVP first: build a daily usable core before adding calendar, habits, or AI features.
