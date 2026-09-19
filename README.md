[![Android](https://img.shields.io/badge/Android-Kotlin-7F77DD?style=flat)](https://developer.android.com/) [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white&style=flat)](https://kotlinlang.org) [![CI](https://img.shields.io/github/actions/workflow/status/Syzygy-Hub/syzygy-core-android/ci.yml?label=ci&style=flat)](https://github.com/Syzygy-Hub/syzygy-core-android/actions/workflows/ci.yml) [![Version](https://img.shields.io/badge/version-1.2.0-D85A30?style=flat)](https://github.com/Syzygy-Hub/syzygy-core-android/releases) [![License](https://img.shields.io/badge/License-MIT-green?style=flat)](LICENSE)

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/Syzygy-Hub/.github/main/brand/assets/banners/syzygy-banner-dark-1200.png">
  <img src="https://raw.githubusercontent.com/Syzygy-Hub/.github/main/brand/assets/banners/syzygy-banner-light-1200.png" alt="Syzygy" width="600">
</picture>

# syzygy-core-android

Core infrastructure modules for the Syzygy Android ecosystem — dependency injection, state management, event bus, logging, feature flags, navigation, validation, configuration, app lifecycle, and scheduling.

---

## Modules

| Module | Description |
|---|---|
| **DI** | Thread-safe dependency injection container with singleton, transient, and scoped lifetimes |
| **State** | Reactive state stores with StateFlow, reducers, and selectors |
| **EventBus** | Typed publish/subscribe channels with scoped subscriptions and async dispatch |
| **Logging** | Log levels, formatters, pipeline routing, and pluggable destinations |
| **FeatureFlags** | Evaluation rules, flag definitions, local overrides, and A/B variant selection |
| **Navigation** | Route definitions, deep link URL parsing, navigation stack model, and route guards |
| **Validation** | Composable field validators, rule chaining, and form-level validation pipeline |
| **Configuration** | In-memory config registry, environment-based switching, and typed config access |
| **Lifecycle** | Foreground/background state tracking, lifecycle observers, and lifecycle-aware scoping |
| **Scheduling** | Debounce, throttle, delayed execution, and cancellable timers |

---

## Installation

Add JitPack to your project-level `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
implementation("com.github.Syzygy-Hub:syzygy-core-android:1.2.0")
```

---

## Requirements

- JDK 17+
- Kotlin 2.0+

---

## Dependencies

| Package | Version | Purpose |
|---|---|---|
| [syzygy-foundation-android](https://github.com/Syzygy-Hub/syzygy-foundation-android) | 1.2.0 | Foundation contracts, primitives, and shared types |
| kotlinx-coroutines-core | 1.9.0 | Reactive state via StateFlow |

---

## Ecosystem

This repo is part of the **Syzygy** cross-platform mobile ecosystem. See the [ecosystem architecture](https://github.com/Syzygy-Hub/.github/blob/main/engineering/architecture/syzygy-ecosystem.md) for how the layers fit together.

---

## License

MIT — see [LICENSE](LICENSE) for details.
