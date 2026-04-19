# MiniDev

MiniDev is an autonomous development pipeline that guides a software project from initial concept to a published state. By leveraging AI to manage the software development lifecycle, it automates critical tasks ranging from high-level planning to final deployment.

## About this Project

MiniDev is a Java-based framework designed to explore the boundaries of autonomous software engineering. By integrating structured pipeline stages with agentic workflows, it aims to create self-contained, functional software artifacts with minimal human intervention.

## Key Features

- **Autonomous Pipeline:** Automates the full lifecycle from design to release.
- **Agentic Workflow:** Intelligent task management that aligns code with design goals.
- **Iterative Development:** Continuous loop of design, implementation, and verification.
- **Modular Architecture:** Extensible stages for planning, architecture, and coding.

## Getting Started

### Prerequisites
- Java 25+
- Maven

### Build & Run
You can run the application using the Maven wrapper:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## Tech Stack
- **Language:** Java 25+
- **Framework:** Spring Boot
- **Frontend:** Thymeleaf, JavaScript (Vanilla)
- **Architecture:** Agentic Pipeline Pattern

## Project Structure

- `src/main/java`: Core logic and pipeline implementation.
- `src/test/java`: Automated unit tests and test suites.
- `docs/`: Project documentation and architecture diagrams.

## Pipeline Overview

The following diagram illustrates the lifecycle of a project within the MiniDev framework, moving through seven core stages: Planning, Architecture, Setup, Review, Coding, Testing, and Publishing.

```mermaid
flowchart LR

%% ── Styling ──────────────────────────────────────────────
    classDef stage fill:#3498db,stroke:#3498db,stroke-width:2px,fill-opacity:0.08,font-weight:bold
    classDef step fill:none,stroke:#85929e,stroke-width:1px
    classDef decision fill:#f39c12,stroke:#f39c12,stroke-width:2px,fill-opacity:0.12
    classDef terminal fill:#2ecc71,stroke:#2ecc71,stroke-width:2px,fill-opacity:0.12
    classDef io fill:#ffffff,stroke:#7f8c8d,stroke-width:1px,stroke-dasharray:3 3

%% ── Global Flow ──────────────────────────────────────────
    Start([Start]):::terminal --> Planning
    Planning --> Architecture
    Architecture --> Setup
    Setup --> Review
    Review --> Coding
    Coding --> Testing
    Testing --> Review
    Review --> Publish
    Publish --> End([Project Published]):::terminal

%% ═══════════════════════════════════════════════════════
%% ═══  PLANNING  ═══════════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Planning["Stage 1 · Planning"]
        direction TB
        P_IN([Entry]):::io --> P1(Generate theme):::step
        P1 --> P2(Generate game ideas from selected theme):::step
        P2 --> P3(Select strongest concept by scope and originality):::step
        P3 --> P4(Define core gameplay loop):::step
        P4 --> P5(Define player goal, failure, and progression):::step
        P5 --> P6(Write initial GDD structure):::step
        P6 --> P7(Expand GDD with mechanics, content, and constraints):::step
        P7 --> P8(Review GDD for feasibility and MVP scope):::step
        P8 --> P9{GDD good enough?}:::decision
        P9 -- No --> P10(Refine idea, scope, and unclear design parts):::step
        P10 --> P6
        P9 -- Yes --> P_OUT([Exit]):::io
    end

%% ═══════════════════════════════════════════════════════
%% ═══  ARCHITECTURE  ═══════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Architecture["Stage 2 · Architecture"]
        direction TB
        A_IN([Entry]):::io --> A1(Extract required systems from GDD):::step
        A1 --> A2(Map each GDD element to technical components):::step
        A2 --> A3(Define modules, boundaries, and responsibilities):::step
        A3 --> A4(Define data flow, state handling, and asset ownership):::step
        A4 --> A5(Describe how mechanics should be concretely implemented):::step
        A5 --> A6(Extend GDD with technical notes and implementation strategy):::step
        A6 --> A7(Review architecture for cohesion and low coupling):::step
        A7 --> A8{Architecture clear enough?}:::decision
        A8 -- No --> A9(Refactor technical design and fill missing details):::step
        A9 --> A3
        A8 -- Yes --> A_OUT([Exit]):::io
    end

%% ═══════════════════════════════════════════════════════
%% ═══  SETUP  ══════════════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Setup["Stage 3 · Setup"]
        direction TB
        S_IN([Entry]):::io --> S1(Create project root structure):::step
        S1 --> S2(Create source, test, asset, and config directories):::step
        S2 --> S3(Create initial files for modules and entrypoints):::step
        S3 --> S4(Create placeholder classes, interfaces, and adapters):::step
        S4 --> S5(Wire startup path and base project configuration):::step
        S5 --> S6(Verify project builds and starts in minimal form):::step
        S6 --> S7{Setup valid?}:::decision
        S7 -- No --> S8(Fix file structure, wiring, and missing setup parts):::step
        S8 --> S6
        S7 -- Yes --> S_OUT([Exit]):::io
    end

%% ═══════════════════════════════════════════════════════
%% ═══  REVIEW  ═════════════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Review["Stage 4 · Review"]
        direction TB
        R_IN([Entry]):::io --> R1(Inspect current codebase state):::step
        R1 --> R2(Compare codebase against GDD and technical design):::step
        R2 --> R3(Identify missing features, gaps, and inconsistencies):::step
        R3 --> R4(Create implementation ToDo list):::step
        R4 --> R6(Sort tasks into logical build order):::step
        R6 --> R7{Remaining ToDos?}:::decision
        R7 -- Yes --> R_OUT_WORK([Exit]):::io
        R7 -- No --> R_OUT_DONE([Exit]):::io
    end

%% ═══════════════════════════════════════════════════════
%% ═══  CODING  ═════════════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Coding["Stage 5 · Coding"]
        direction TB
        C_IN([Entry]):::io --> C1(Select next highest-priority ToDo):::step
        C1 --> C2(Load relevant GDD, architecture notes, and files):::step
        C2 --> C3(Implement required feature or refactor):::step
        C3 --> C4(Write or update matching unit tests):::step
        C4 --> C5(Ensure code and tests align with intended behavior):::step
        C5 --> C6(Update task state and touched components):::step
        C6 --> C8{More ToDos?}:::decision
        C8 -- Yes --> C1
        C8 -- No --> C_OUT([Exit]):::io
    end

%% ═══════════════════════════════════════════════════════
%% ═══  TESTING  ════════════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Testing["Stage 6 · Testing"]
        direction TB
        T_IN([Entry]):::io --> T1(Run static code analysis):::step
        T1 --> T2(Run unit test suite):::step
        T2 --> T3(Collect warnings, failures, and regressions):::step
        T3 --> T4(Summarize technical quality and implementation status):::step
        T4 --> T5{Tests and analysis acceptable?}:::decision
        T5 -- No --> T6(Return findings for next review cycle):::step
        T6 --> T_OUT_REVIEW([Exit]):::io
        T5 -- Yes --> T_OUT_DONE([Exit]):::io

    end

%% ═══════════════════════════════════════════════════════
%% ═══  PUBLISH  ════════════════════════════════════════
%% ═══════════════════════════════════════════════════════
    subgraph Publish["Stage 7 · Publish"]
        direction TB
        U_IN([Entry]):::io --> U1(Prepare production configuration):::step
        U1 --> U2(Clean assets, startup flow, and packaging metadata):::step
        U2 --> U3(Build final project artifact):::step
        U3 --> U4(Run final smoke test on packaged build):::step
        U4 --> U5(Verify release matches GDD-defined MVP):::step
        U5 --> U6{Publishable?}:::decision
        U6 -- No --> U7(Return issues to review backlog):::step
        U7 --> U_OUT_REVIEW([Exit]):::io
        U6 -- Yes --> U_OUT([Exit]):::io
    end


%% ── Apply classes ───────────────────────────────────────
    class Planning,Architecture,Setup,Review,Coding,Testing,Publish stage
```

## Core Philosophy

MiniDev operates on the principle of self-evolving software development. By maintaining a continuous loop between design, implementation, and verification, it ensures that project goals remain aligned with the actual codebase. This approach fosters a highly iterative process where documentation and implementation evolve in tandem.

## Detailed Stage Breakdown

- **Planning:** Conceptualization of the project theme and definition of the MVP scope through GDD generation.
- **Architecture:** Translating GDD elements into technical components, defining modules, data flow, and asset ownership.
- **Setup:** Initializing the project structure, source directories, boilerplate code, and verification of the initial build.
- **Review:** Bridging the gap between design and implementation through continuous assessment and task prioritization.
- **Coding:** Iterative feature implementation based on prioritized tasks, guided by documentation and verified with unit tests.
- **Testing:** Comprehensive validation through static analysis and automated test suites to ensure technical quality.
- **Publishing:** Finalizing configurations, building the release artifact, and performing smoke tests to verify MVP readiness.

## Contributing

Contributions are welcome! If you'd like to improve MiniDev, please fork the repository and submit a pull request.

