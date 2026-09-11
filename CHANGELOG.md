# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-09-11

### Fixed

- **FIX 1** — StateStore: replaced read-modify-write `_state.value = reducer(...)` with `_state.update { }` for atomic state transitions under concurrent dispatch
- **FIX 4** — Container: moved circular-dependency resolving-set check and add into an atomic operation (ConcurrentHashMap) to prevent two concurrent callers both passing the guard simultaneously
- **FIX 5** — Debouncer: marked `job` field `@Volatile` to prevent cross-coroutine write races
- **FIX 6** — FeatureFlagProvider, ConfigRegistry: replaced `mutableMapOf()` with `ConcurrentHashMap`; AppLifecycleTracker, Router: replaced `mutableListOf()` with `CopyOnWriteArrayList`
- **FIX 7** — LogDestination.write() signature extended with `timestamp: SyzygyTimestamp?` and `error: Throwable?` parameters (defaulted for backward compatibility); ConsoleLogDestination and Logger bridge updated to forward both fields
- **FIX 9** — Throttler: clock injected via `clock: () -> Long` parameter (defaults to `System::currentTimeMillis`) to allow deterministic testing
- **FIX 12** — EventBus: added KDoc documenting 64-event buffer limit; `tryEmit()` returning false now emits a warning via stderr
- **FIX 20** — EmailValidator: added KDoc noting heuristic pattern and non-RFC-5321 compliance
- **FIX 22** — FeatureFlagProvider and ConfigRegistry: unchecked casts now wrapped with try-catch that rethrows a descriptive ClassCastException

### Tests added

- StateStore: concurrent dispatch test (FIX 1)
- Throttler: fake-clock throttle test and post-cooldown execution test (FIX 9 / FIX 23)
- Container: scoped-through-parent documentation + two new scoped tests (FIX 16)
- MaxLengthValidator: under-max, exact-max, and over-max boundary tests (FIX 18)
- Logger: Foundation LogEntry bridge tests — all 5 log levels, metadata, timestamp, and error forwarding (FIX 8)

## [1.0.0] - 2026-09-05

### Added

- DI container with singleton, transient, and scoped lifetimes
- Reactive state stores with StateFlow, reducers, and selectors
- Typed event bus with scoped subscriptions and async dispatch
- Logger with log levels, formatters, and pluggable destinations
- Feature flag provider with evaluation rules, local overrides, and A/B variants
- Navigation router with deep link parsing and route guards
- Composable validation pipeline with built-in rules
- Configuration registry with environment-based switching
- App lifecycle tracker with lifecycle-aware scoping
- Scheduling utilities — debounce, throttle, delayed execution, cancellable timers

[1.0.0]: https://github.com/Syzygy-Hub/syzygy-core-android/releases/tag/1.0.0
