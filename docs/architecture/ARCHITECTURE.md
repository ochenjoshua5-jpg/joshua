# YumaPlayer Architecture Overview

This document outlines the architectural blueprint of YumaPlayer. The application is built following **Clean Architecture** principles combined with **Unidirectional Data Flow (UDF)**, ensuring high testability, feature isolation, and strict separation of concerns.

---

## 1. Core Architectural & Dependency Rules

To keep the codebase maintainable as it grows, all modules and layers must strictly follow these fundamental rules:

- **Dependencies Point Inward:** Outer layers depend on inner layers. The Domain layer is the central core of the application.
- **Inner Layers are Unaware of Outer Layers:** Core business logic has zero awareness of UI components, databases, or third-party SDKs.
- **Interface-Driven Communication:** Cross-layer interaction occurs exclusively through domain-defined interfaces.
- **Strict Layer Isolation (Neighbor-Only Rule):** Each layer communicates only with its immediate neighbor.
  - `UI / Presentation` ➔ `Domain (Use Cases / Models)`
  - `Domain` ➔ `Repository Interfaces`
  - `Data` ➔ `Repository Implementations`
  - **Rule:** The Presentation layer must **never** talk directly to the Data layer or data sources.
- **Data Model Ownership:** Every layer owns its specific models (Network DTOs, DB Entities, Domain Entities, UI States). Conversion occurs explicitly at layer boundaries via Mappers.

---

## 2. Layer Structure & Responsibilities

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│       Jetpack Compose UI  ◄──►  ViewModels (UDF)       │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                      │
│      Use Cases  │  Domain Entities  │  Interfaces      │
└───────────────────────────▲────────────────────────────┘
                            │
                            │ (Implements Interfaces)
┌───────────────────────────┴────────────────────────────┘
│                       DATA LAYER                       │
│   Repositories Impl  │  Data Sources  │  DTOs & Mappers│
└────────────────────────────────────────────────────────┘
```

### Presentation Layer (`:feature:*`, `:app`)
- **Responsibilities:** Declarative Jetpack Compose UI rendering, visual state representation, and user input handling.
- **Components:** Composables, ViewModels (`StateFlow<UiState>`), `UiIntent`, `UiEffect`.

### Domain Layer (`:core:domain`, `:core:model`)
- **Responsibilities:** Pure, framework-independent business rules, application use cases, and core data structures.
- **Components:** Use Cases / Interactors, Domain Models, Repository & Service Interfaces.

### Data Layer (`:core:data`, `:core:network`, `:core:database`)
- **Responsibilities:** Fetching, caching, persisting, and transforming data from local databases and remote network APIs.
- **Components:** Repository Implementations, Room Entities, Ktor Data Sources, Network DTOs, Mappers.

---

## 3. Request & Data Flow Lifecycle

The following diagram illustrates the complete execution path of a single data request (e.g., searching for a track or loading lyrics):

```
User Action (Click / Input)
│
▼
┌───────────────┐        UiIntent        ┌───────────────┐
│ Compose UI    ├───────────────────────►│ ViewModel     │
└──────▲────────┘                        └───────┬───────┘
       │                                         │ Invokes
       │ UiState                                 ▼
┌──────┴────────┐  Domain Model          ┌───────────────┐
│ StateFlow     │◄───────────────────────┤ UseCase       │
└───────────────┘                        └───────┬───────┘
                                         │ Calls Interface
                                         ▼
┌───────────────┐  Domain Model          ┌───────────────┐
│ RepositoryImpl├───────────────────────►│ Repository    │
└──────▲────────┘ (via Mapper)           │ (Interface)   │
       │                                 └───────────────┘
       ├────────────────────────┐
       │ DTO                    │ Entity
┌──────┴────────┐        ┌──────┴────────┐
│ Remote Source │        │ Local DB      │
│ (Network API) │        │ (Room Cache)  │
└───────────────┘        └───────────────┘
```

---

## 4. Module Graph Reference

For the complete module catalog, category definitions, layer boundaries, and dependency enforcement rules, see **[MODULES.md](MODULES.md)**.
