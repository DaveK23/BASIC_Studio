# BASIC Interpreter Execution Control Toolbar Plan

A dedicated, persistent execution control toolbar featuring **Step**, **Pause**, and **Stop** controls docked directly above the navigation tabs, enabling users to inspect and steer BASIC program flow seamlessly across the code editor, terminal, graphics canvas, and debugger.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed via Phase 1 clarification:
> - **Toolbar Placement**: Dedicated execution control bar docked above the bottom navigation tabs, remaining readily accessible across all active tabs (Editor, Terminal, Debugger, Bytecode).
> - **Stepping Granularity**: Step by source code line — pressing **Step** executes all underlying bytecode instructions until the interpreter reaches the next source line or completes execution.
> - **Pause Behavior**: Pressing **Pause** halts execution cooperatively, updates variables and instruction pointers, and automatically switches active view to the **Debugger** tab for immediate inspection.

---

## 1. Overview & Core Concept

- **What It Does**: Provides live playback and stepping controls for the embedded BASIC interpreter. Users can start, single-step line-by-line, pause at any moment to inspect memory/variables in the debugger, resume execution, or abort safely.
- **Target Audience / Persona**: Developers, retro computing enthusiasts, and students writing games and algorithms who need fine-grained debugging and control over long-running loops or animations.
- **Key Value**: Replaces clumsy all-or-nothing execution with interactive execution control. Eliminates the need to switch tabs or guess where an infinite loop is hanging.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Normal Run to Pause**:
   - User writes or runs a BASIC loop (e.g. game animation or sorting algorithm).
   - In the execution toolbar, user taps **Pause** (amber button).
   - Execution yields immediately, the app auto-switches to the **Debugger** tab, and highlights the current executing line and variables.
2. **Line Stepping from Zero State / Pause**:
   - User taps **Step** (cyan button) from IDLE, PAUSED, or RUNNING state.
   - If program is not running, it compiles and steps through the first executable line.
   - If paused, it executes until the next distinct source line, updating the highlighted line in both the Editor and Debugger watch tables.
3. **Resume / Continue**:
   - While paused, the toolbar highlights a **Resume** action (or Play toggle) allowing the user to continue full-speed execution.
4. **Immediate Stop**:
   - User taps **Stop** (crimson button) to cancel pending inputs, audio playback, or active execution, returning the system to `TERMINATED` / `IDLE` state.

### Visual Identity & Theme
- **Retro-Cyber Deck Aesthetic**:
  - Surface: `DarkSurface` (`#131B2E`) with subtle borders (`#1E293B`) matching the retro terminal CRT style.
  - Button styling: Pill-shaped Material 3 buttons with high-contrast status accents:
    - **Step**: Terminal Cyan (`#38BDF8`) with `Icons.Default.SkipNext` and "STEP" monospace badge.
    - **Pause / Resume**: Amber (`#FBBF24`) when running (`Icons.Default.Pause`), Green (`#4ADE80`) when paused (`Icons.Default.PlayArrow`).
    - **Stop**: Crimson (`#EF4444`) with `Icons.Default.Stop`.
  - State Indicator: Monospace status badge (`RUNNING`, `PAUSED`, `IDLE`, `INPUT`) with glowing status dot.
- **Placement & Safe Insets**:
  - Docked directly above `NavigationBar` in `Scaffold(bottomBar = { Column { ExecutionToolbar(...); NavigationBar(...) } })`.
  - Maintains strict 48.dp minimum touch target height with tactile haptic/ripple feedback.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Source-Line Stepping vs. Opcode Stepping**
  - *Chosen Approach*: Step by source line (`stepLine()`). It runs micro-instructions until the next positive source line number is reached or program halts.
  - *Why*: BASIC programmers reason in terms of statements (`10 FOR I = 1 TO 10`, `20 PRINT I`), not raw stack operations (`PUSH_CONST`, `LOAD_VAR`).
  - *Alternative Considered*: Raw bytecode stepping (reserved for the Bytecode tab and low-level disassembly).
- **Decision 2: Seamless Tab Switching on Pause**
  - *Chosen Approach*: When **Pause** is triggered, state moves to `PAUSED_DEBUG` and `activeTab` switches automatically to `AppTab.DEBUGGER` so variables and call stack are instantly visible.
  - *Why*: Reduces cognitive friction and multiple manual navigation taps when hunting bugs.
- **Decision 3: Dedicated Bar vs. Header Actions**
  - *Chosen Approach*: Dedicated bottom bar dock.
  - *Why*: Top bar space is already occupied by app title, Samples, Export APK, and screen modes. A bottom dock places playback controls within easy one-handed thumb reach on mobile devices.

---

## 4. Technical Architecture & Data Strategy

### Component Hierarchy & Interaction Flow

```
┌────────────────────────────────────────────────────────┐
│                   Scaffold Layout                      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  TopAppBar (Title, Samples, Export APK)          │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Content Area (Active Tab)                       │  │
│  │  [ Editor | Terminal / Screen | Debug | Bytecode]│  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  ExecutionToolbar (Step, Pause/Resume, Stop)     │  │
│  │  • Status badge (RUNNING / PAUSED / IDLE)        │  │
│  │  • Line Stepper (SkipNext)                       │  │
│  │  • Pause / Resume toggle                         │  │
│  │  • Terminate (Stop)                              │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  NavigationBar (Bottom tabs)                     │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### State Transitions

```
   ┌───────────┐         Step / Run          ┌───────────────┐
   │   IDLE    │ ──────────────────────────> │    RUNNING    │
   └───────────┘                             └───────────────┘
         ▲                                           │
         │ Stop                                      │ Pause
         │                                           ▼
   ┌───────────┐         Resume              ┌───────────────┐
   │TERMINATED │ <────────────────────────── │ PAUSED_DEBUG  │
   └───────────┘        (or Stop)            └───────────────┘
                                                │       ▲
                                                └──Step─┘
```

### Virtual Machine & ViewModel Additions
- **`BasicVirtualMachine.kt`**:
  - `isPauseRequested: Boolean` flag checked inside the cooperative execution loop.
  - `fun pause()`: sets flag and updates state to `VmState.PAUSED_DEBUG`.
  - `suspend fun stepLine(): Boolean`: advances until source line changes.
- **`BasicViewModel.kt`**:
  - `fun pauseProgram()`: invokes `vm.pause()`.
  - `fun stepLine()`: compiles if needed, runs `vm.stepLine()`, and refreshes variables and call stack.
  - `fun resumeExecution()`: resumes `vm.execute(debug = true)`.
- **`BasicStudioScreens.kt`**:
  - `ExecutionToolbar` composable with clear visual styling and test tags (`testTag("toolbar_step")`, `testTag("toolbar_pause")`, `testTag("toolbar_stop")`, `testTag("toolbar_resume")`).
