# Fix Debugger Breakpoints, Superfluous Step Button, and Bytecode Sync

Resolves three core debugging and navigation issues in BASIC Studio:
1. **Breakpoints Not Working / Resume Lockup**: Fixes breakpoint detection in the VM so breakpoints work reliably during both Run and Debug, matches both editor source lines (1, 2, 3...) and BASIC program lines (10, 20, 30...), avoids getting trapped on the same line upon Resume, and automatically switches to the Debugger tab when a breakpoint is hit.
2. **Superfluous Step Button Removal**: Removes the redundant, non-functional "Step" button in `DebuggerScreen`, consolidating step execution into the dedicated execution controls toolbar.
3. **Bytecode View Persistence & Real-Time Sync**: Prevents switching to the Bytecode tab from resetting the running/paused program back to the beginning, maintains current instruction pointer and execution state, and auto-scrolls the bytecode disassembly list to keep the active instruction centered and in sync with source code.

---

## User Review & Clarified Decisions

> [!NOTE]
> Based on the Phase 1 questionnaire responses:
> - **Navigation on Breakpoint**: Automatically switch to the **Debugger** tab when a breakpoint is hit so variables, call stack, and line pointers are immediately visible.
> - **Bytecode View Scrolling**: Automatically scroll the bytecode disassembly list to keep the currently executing instruction centered and visible.
> - **Breakpoint Addressing**: Support matching both 1-based editor source line numbers (`1, 2, 3...`) and BASIC numbered lines (`10, 20, 30...`).

---

## Proposed Changes

### 1. Breakpoint Execution & VM Engine (`BasicVirtualMachine.kt`, `BasicViewModel.kt`)

#### Problem Analysis:
- `vm.execute(debug = false)` in `runProgram()` explicitly ignored breakpoints because `isDebugMode` was false.
- In `execute()`, breakpoint check only checked `breakpoints.contains(currentLine)`. If a user set a breakpoint on BASIC line `20` (e.g. `20 PRINT "HI"`), or clicked line 2 in the editor, `sourceLineNum` was never checked against breakpoints.
- When paused at a breakpoint on line `L`, resuming called `execute()` with `ip` still at the first instruction of line `L`. The while-loop immediately triggered `breakpoints.contains(currentLine)` on iteration 0 without executing any instruction, causing an infinite pause lockup on Resume.
- Multiple micro-instructions for the same source statement repeatedly triggered breakpoints if stepping or continuing.

#### Fix:
- In `BasicVirtualMachine`:
  - Support both `instruction.sourceLine` and `instruction.sourceLineNum` when checking breakpoints:
    ```kotlin
    fun isAtBreakpoint(inst: Instruction): Boolean {
        if (breakpoints.isEmpty()) return false
        if (breakpoints.contains(inst.sourceLine)) return true
        if (inst.sourceLineNum > 0 && breakpoints.contains(inst.sourceLineNum)) return true
        if (breakpoints.contains(ip)) return true
        return false
    }
    ```
  - Introduce `private var lastBreakpointLineHit: Int = -1` to track the line where the breakpoint triggered.
  - When entering `execute()`, if resuming from `lastBreakpointLineHit`, execute past the current line before checking the breakpoint on that same line again.
  - Check breakpoints during execution regardless of whether started via `runProgram()` or `startDebugging()` if breakpoints are set, or ensure `runProgram()` enables breakpoint checking when breakpoints are configured.
  - In `BasicViewModel`, add an event / state flow for `onBreakpointHit` or observe `VmState.PAUSED_DEBUG` caused by breakpoint to trigger tab switching.

---

### 2. Tab Navigation & Superfluous Step Button (`BasicStudioScreens.kt`)

#### Fix:
- **Remove Superfluous Step Button**:
  - In `DebuggerScreen`, remove the redundant `OutlinedButton` ("debugger_step_button") near the top. The global `ExecutionToolbar` docked at the bottom already handles Step, Pause/Resume, and Stop consistently across all tabs.
  - Retain or polish the IP/Source Line indicator card and "Continue" button in `DebuggerScreen`.
- **Auto-Switch to Debugger on Breakpoint**:
  - In `BasicStudioApp`, observe `vmState` or breakpoint event: when `vmState` transitions from `RUNNING` to `PAUSED_DEBUG` due to a breakpoint or pause, automatically update `activeTab = AppTab.DEBUGGER`.

---

### 3. Bytecode View Non-Resetting & Sync (`BasicViewModel.kt`, `BasicStudioScreens.kt`)

#### Problem Analysis:
- In `BasicStudioScreens.kt`:
  ```kotlin
  NavigationBarItem(
      selected = activeTab == AppTab.BYTECODE,
      onClick = {
          viewModel.compileCurrentCode()
          activeTab = AppTab.BYTECODE
      }
  )
  ```
  `viewModel.compileCurrentCode()` compiled the code and called `vm.loadProgram(program)`.
  `vm.loadProgram()` invoked `reset()`, wiping `ip = 0`, variables, call stack, and halting active execution.

#### Fix:
- Separate bytecode generation/inspection from VM reset:
  - Add `viewModel.ensureBytecodeCompiled(resetVm: Boolean = false)`:
    - If `_compiledProgram.value != null` and the source code has not changed, do nothing to the VM.
    - If compiling for inspection, compile the source into `_compiledProgram.value` without resetting `vm` if `vm.state` is `RUNNING` or `PAUSED_DEBUG`.
  - When tapping the Bytecode tab, call `ensureBytecodeCompiled(resetVm = false)` instead of `compileCurrentCode()`.
- In `BytecodeScreen`:
  - Use `val listState = rememberLazyListState()`.
  - Add `LaunchedEffect(currentIp)`:
    - When `currentIp` changes and `currentIp in 0 until p.instructions.size`, call `listState.animateScrollToItem(maxOf(0, currentIp - 2))` to keep the instruction centered and visible.
  - Display current instruction arrow indicator `▶` with glowing amber/cyan accent, matching source line information.

---

## Verification Plan

### Automated Tests
1. **Breakpoint Unit Tests (`BasicVirtualMachineTest.kt` / `DebuggerTest.kt`)**:
   - Verify setting a breakpoint on editor source line `2` pauses execution before executing line `2`.
   - Verify setting a breakpoint on BASIC line number `20` pauses execution before executing line `20`.
   - Verify calling `resume()` from a breakpoint executes past the breakpoint line and continues to subsequent instructions or the next breakpoint without locking up.
   - Verify running with breakpoints set pauses at the breakpoint.
2. **Bytecode Inspection Test**:
   - Verify inspecting bytecode while program is paused maintains `ip` and variables without resetting.
3. **Full Test Suite**:
   - Run `gradle :app:testDebugUnitTest` to ensure all existing and new tests pass.

### Manual / Applet Verification
1. Set a breakpoint on line 2 of a loop program in the editor.
2. Press "Run": confirm app automatically switches to the Debugger tab, showing line 2 paused, IP pointer, and variables.
3. Press "Resume" or "Step": verify program resumes execution and continues cleanly rather than getting stuck.
4. Open the Debugger tab: confirm the superfluous "Step" button at the top is removed.
5. While program is paused at line 2, switch to the Bytecode tab: confirm the program remains at the current instruction, does not restart from line 0, and the bytecode list auto-scrolls to highlight the current instruction.
