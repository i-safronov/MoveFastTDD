# MoveFast — Interval Timer

An Android interval workout timer built with **Test-Driven Development**, and **Clean Architecture** pattern from the ground up.

---

## Overview

MoveFast lets users configure and run interval workouts (reps × work duration + rest). The app handles process death gracefully — if the system kills the app mid-workout, the user returns exactly where they left off, including the paused/running state.

---

## Architecture

### Clean Architecture layers

```
presentation/       ← Compose UI, ViewModels, Navigation
domain/             ← Business logic, models, use cases, repository interfaces
data/               ← Room, DataStore, repository implementations
core/               ← Shared utilities (dispatchers, extensions)
di/                 ← Hilt DI modules
```

**Dependency rule is strictly enforced**: `presentation` → `domain` ← `data`. Domain contains zero Android imports.

### APEX Pattern

A custom MVI framework built on top of `ViewModel`:

```
Screen  ──dispatch(Executor)──▶  ViewModel.execute()  ──▶  new State
                                        │
                                 sendEffect(Effect)
                                        │
                                 ViewModel.affect()
                                        │
                               dispatch(Executor) / sendEvent(Event)
                                        │
Screen  ◀──────────────────── OnEvent { navigate / play sound / ... }
```

- **State** — immutable UI state rendered by Compose
- **Executor** — user intent or internal command
- **Effect** — async side effect (IO, DataStore, timer coroutine)
- **Event** — one-shot signal to the screen (navigation, sound)

### Screens

| Screen | ViewModel | Responsibility |
|---|---|---|
| `SetupScreen` | `SetupViewModel` | Config input, validation, draft persistence |
| `TimerScreen` | `TimerViewModel` | Countdown, phase transitions, sound, process death recovery |

---

## Key Features

### Per-field validation
`ValidateWorkoutConfigUseCase` returns `ValidationResult.Invalid(repsError, repDurationError, restDurationError)` — only the invalid fields are highlighted, not all at once.

### Three-level persistence
| Storage | What | When cleared |
|---|---|---|
| `WorkoutDraftDataStore` | User's in-progress input | When user confirms (Start) |
| Room (`WorkoutConfigRepository`) | Confirmed workout config | Never |
| `TimerStateDataStore` | Active timer snapshot | On Cancel / workout complete |

### Process death recovery
`MainActivity` reads `TimerStateDataStore` on launch and restores the full back stack:
```
TimerStateDataStore has snapshot?
  YES → [SetupScreen, TimerScreen]  ← timer resumes automatically
  NO  → [SetupScreen]
```

The timer state includes `isRunning` — if the user paused before the process died, the screen opens in paused state.

### Timer generation counter
Each `Resume` increments `timerGeneration`. Old timer coroutines check their captured generation against the current one and exit if stale — prevents double-ticking from rapid pause/resume.

### Error handling
`DataResult<T>` sealed class in domain:
- Repository wraps Room calls in `runCatching { }.toDataResult()`
- DataStore implementations silently swallow IO errors (`runCatching { }.getOrNull()`)
- `SetupViewModel` exposes `saveError: Boolean` — user sees "Failed to save" and can retry

---

## Tech Stack

| Category | Library | Version |
|---|---|---|
| UI | Jetpack Compose + Material 3 | BOM 2025.01.01 |
| Navigation | Voyager | 1.1.0-beta02 |
| DI | Hilt | 2.51.1 |
| Database | Room | 2.7.1 |
| Preferences | DataStore Preferences | 1.1.1 |
| Language | Kotlin | 2.0.0 |
| Build | AGP | 8.9.1 |

---

## Testing

### Strategy
Tests are written **alongside or before implementation** (TDD). Each layer is tested in isolation.

| Layer | Type | Tool |
|---|---|---|
| Use Cases | Unit | JUnit4 + coroutines-test |
| Repositories | Unit (Fake DAO) + Instrumented (Room in-memory) | JUnit4 + Room Testing |
| ViewModels | Unit (Fakes for all dependencies) | JUnit4 + `UnconfinedTestDispatcher` |
| Screens | Instrumented UI (Page Object pattern) | Compose Testing + Espresso |

### Test doubles
Every Android/infrastructure dependency has a fake:
- `FakeWorkoutConfigRepository` — supports `shouldFailOnSave`, `shouldFailOnLoad` flags
- `FakeWorkoutConfigDao` — in-memory DAO
- `FakeWorkoutDraftDataStore` / `FakeTimerStateDataStore` — simple in-memory implementations

### Page Object pattern
UI tests use `SetupPage` and `TimerPage` to encapsulate node interactions. Tests read as specifications:

```kotlin
page.assertRepsHasError()
    .assertRepDurationHasNoError()
    .assertRestDurationHasNoError()
```

### Key test scenarios covered
- Per-field validation (only errored field highlighted)
- Draft restoration after process death
- Timer phase transitions and generation counter (no double-tick)
- Rapid pause/resume taps
- Configuration change (screen rotation)
- Repository save failure + retry
- Back press handling on TimerScreen

---

## Project Structure

```
app/src/
├── main/
│   └── .../movefasttdd/
│       ├── core/
│       │   ├── dispatchers/DispatchersList.kt
│       │   └── extensions/ResultExtensions.kt
│       ├── data/
│       │   ├── datastore/           ← WorkoutDraftDataStore, TimerStateDataStore
│       │   ├── local/               ← Room DAO, Database, Entity
│       │   └── repository/          ← WorkoutConfigRepositoryImpl
│       ├── di/                      ← Hilt modules
│       ├── domain/
│       │   ├── model/               ← WorkoutConfig, TimerPhase, DataResult, ValidationResult
│       │   ├── repository/          ← WorkoutConfigRepository (interface)
│       │   └── use_case/            ← ValidateWorkoutConfigUseCase, BuildTimerSequenceUseCase
│       └── presentation/
│           ├── core/viewmodel/      ← APEXViewModel (MVI base)
│           ├── setup/               ← SetupScreen, SetupViewModel, SetupActions, SetupContract
│           └── timer/               ← TimerScreen, TimerViewModel, TimerActions, TimerSoundPlayer
├── test/                            ← Unit tests + Fakes
└── androidTest/                     ← Instrumented UI tests + Page Objects
```

---

## Running Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest
```
