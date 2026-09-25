# ADR-014: Java and Android Views instead of Kotlin and Compose

- Status: Accepted (2026-09-25)
- Decided by: you ("DON'T USE KOTLIN, USE JAVA")

## Decision

All app code is Java 17. Gradle scripts are Groovy. AGP 9's built-in Kotlin is switched off (`android.builtInKotlin=false`).

## Consequences

| Spec item | Kotlin/Compose plan | What we use |
|---|---|---|
| UI toolkit (§8, ADR-003) | Jetpack Compose | Android Views + Material Components 1.14.0. The island is one custom `View` drawn on Canvas (option B of D3, without Compose content) |
| Motion (§7.3) | `MotionScheme` springs | `androidx.dynamicanimation` `SpringAnimation`, which retargets without losing velocity. Expressive / Standard / Fluid are stiffness and damping presets. ADR-013 is moot: the Compose-only Expressive tokens can't be used |
| Settings storage | Proto DataStore | `SharedPreferences` with change listeners (DataStore is Kotlin-first) |
| DI | Hilt | Two process singletons on the `Application` (hub, settings). Revisit if the graph grows |
| Engine and geometry | Pure Kotlin modules | Pure Java module `:core` with JUnit tests |

What doesn't change: every behaviour, rule and budget in the spec, the phase gates and the ADRs.
