package com.example.basic.vm

import com.example.basic.model.BasicValue
import com.example.basic.model.BytecodeProgram
import com.example.basic.model.Instruction
import com.example.basic.model.OpCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

/**
 * VM Execution state
 */
enum class VmState {
    IDLE,
    RUNNING,
    PAUSED_DEBUG,
    WAITING_FOR_INPUT,
    TERMINATED,
    ERROR
}

/**
 * Styled text segment within a terminal line.
 */
data class TerminalSpan(
    val text: String,
    val colorIndex: Int = 0,
    val isInputEcho: Boolean = false,
    val isSystemNotice: Boolean = false,
    val isError: Boolean = false
)

/**
 * Terminal output line composed of one or more styled segments.
 */
data class TerminalLine(
    val text: String,
    val colorIndex: Int = 0, // 0 = default terminal green/cyan, 1-15 classic color palette
    val isInputEcho: Boolean = false,
    val isSystemNotice: Boolean = false,
    val isError: Boolean = false,
    val spans: List<TerminalSpan> = emptyList(),
    val endsWithNewline: Boolean = text.endsWith("\n")
) {
    companion object {
        fun appendChunk(
            current: List<TerminalLine>,
            chunk: TerminalLine
        ): List<TerminalLine> {
            if (chunk.text.isEmpty()) return current

            val result = current.toMutableList()
            val text = chunk.text
            var i = 0

            while (i < text.length) {
                val nextNewline = text.indexOf('\n', i)
                if (nextNewline == -1) {
                    val segment = text.substring(i)
                    if (segment.isNotEmpty()) {
                        appendSegment(result, segment, chunk, terminatesLine = false)
                    }
                    break
                } else {
                    val segment = text.substring(i, nextNewline)
                    appendSegment(result, segment, chunk, terminatesLine = true)
                    i = nextNewline + 1
                }
            }

            return result
        }

        private fun appendSegment(
            result: MutableList<TerminalLine>,
            segment: String,
            chunk: TerminalLine,
            terminatesLine: Boolean
        ) {
            val hasOpenLine = result.isNotEmpty() && !result.last().endsWithNewline

            if (hasOpenLine) {
                val lastLine = result.removeAt(result.size - 1)
                if (segment.isNotEmpty()) {
                    val existingSpans = if (lastLine.spans.isNotEmpty()) {
                        lastLine.spans
                    } else {
                        listOf(
                            TerminalSpan(
                                text = lastLine.text,
                                colorIndex = lastLine.colorIndex,
                                isInputEcho = lastLine.isInputEcho,
                                isSystemNotice = lastLine.isSystemNotice,
                                isError = lastLine.isError
                            )
                        )
                    }
                    val newSpan = TerminalSpan(
                        text = segment,
                        colorIndex = chunk.colorIndex,
                        isInputEcho = chunk.isInputEcho,
                        isSystemNotice = chunk.isSystemNotice,
                        isError = chunk.isError
                    )
                    val updated = lastLine.copy(
                        text = lastLine.text + segment,
                        spans = existingSpans + newSpan,
                        endsWithNewline = terminatesLine
                    )
                    result.add(updated)
                } else if (terminatesLine) {
                    val updated = lastLine.copy(endsWithNewline = true)
                    result.add(updated)
                }
            } else {
                if (segment.isNotEmpty()) {
                    val span = TerminalSpan(
                        text = segment,
                        colorIndex = chunk.colorIndex,
                        isInputEcho = chunk.isInputEcho,
                        isSystemNotice = chunk.isSystemNotice,
                        isError = chunk.isError
                    )
                    val newLine = TerminalLine(
                        text = segment,
                        colorIndex = chunk.colorIndex,
                        isInputEcho = chunk.isInputEcho,
                        isSystemNotice = chunk.isSystemNotice,
                        isError = chunk.isError,
                        spans = listOf(span),
                        endsWithNewline = terminatesLine
                    )
                    result.add(newLine)
                } else if (terminatesLine) {
                    val newLine = TerminalLine(
                        text = "",
                        colorIndex = chunk.colorIndex,
                        isInputEcho = chunk.isInputEcho,
                        isSystemNotice = chunk.isSystemNotice,
                        isError = chunk.isError,
                        spans = emptyList(),
                        endsWithNewline = true
                    )
                    result.add(newLine)
                }
            }
        }
    }
}

/**
 * Stack frame for GOSUB / RETURN
 */
data class CallFrame(
    val returnIp: Int,
    val callerLine: Int
)

/**
 * Real-time state of the on-screen 8-way joystick and fire button.
 */
data class JoystickState(
    val direction: Int = 0, // 0=Center, 1=Up, 2=Up-Right, 3=Right, 4=Down-Right, 5=Down, 6=Down-Left, 7=Left, 8=Up-Left
    val axisX: Int = 0,     // -1 (Left), 0 (Center), 1 (Right)
    val axisY: Int = 0,     // -1 (Up), 0 (Center), 1 (Down)
    val fire: Boolean = false // true when fire button is held down
)

/**
 * Shape pattern definition for retro pixel sprites (8x8 or 16x16).
 */
data class SpriteDefinition(
    val width: Int = 8,
    val height: Int = 8,
    val pattern: List<Boolean> = List(64) { false },
    val colorPattern: List<Int>? = null
)

/**
 * Virtual Machine that executes BASIC Bytecode with real-time debugging,
 * step execution, breakpoints, variable inspection, and terminal I/O.
 */
class BasicVirtualMachine(
    private val onOutput: (TerminalLine) -> Unit = {},
    private val onStateChange: (VmState) -> Unit = {},
    private val onVariablesChanged: (Map<String, BasicValue>) -> Unit = {},
    private val onIpChanged: (currentIp: Int, sourceLine: Int) -> Unit = { _, _ -> },
    private val onClearScreen: () -> Unit = {},
    private val onBeep: () -> Unit = {},
    private val onGraphicsStateChanged: (com.example.basic.graphics.RetroGraphicsState) -> Unit = {},
    private val onSound: (freqHz: Double, durationMs: Long, waveType: Int) -> Unit = { _, _, _ -> },
    private val onSfx: (preset: Int) -> Unit = {},
    private val onBreakpointHit: (line: Int) -> Unit = {}
) {
    var program: BytecodeProgram? = null
        private set

    var ip = 0
        private set

    var state = VmState.IDLE
        private set

    var lastError: String? = null
        private set

    val stack = ArrayDeque<BasicValue>()
    val variables = mutableMapOf<String, BasicValue>()
    val arrays = mutableMapOf<String, MutableMap<Int, BasicValue>>()
    val callStack = ArrayDeque<CallFrame>()

    // Retro Graphics & Sprites State
    var graphicsState = com.example.basic.graphics.RetroGraphicsState()
        private set

    // Sprite pattern definition table (supports sharing pattern definitions across multiple sprites)
    private val spriteDefinitions = mutableMapOf<Int, SpriteDefinition>()

    // Controller (8-way joystick and fire button) State
    var joystickState = JoystickState()
        private set

    fun updateJoystick(direction: Int, axisX: Int, axisY: Int) {
        joystickState = joystickState.copy(direction = direction, axisX = axisX, axisY = axisY)
    }

    fun updateFireButton(pressed: Boolean) {
        joystickState = joystickState.copy(fire = pressed)
    }

    // Debugging controls
    val breakpoints = mutableSetOf<Int>() // Line numbers or IP
    var isDebugMode = false
    var stepMode = false

    // Breakpoint re-entry tracking (prevents lockup on Resume)
    private var lastBreakpointLineHit: Int = -1
    private var lastBreakpointBasicLineHit: Int = -1
    private var lastBreakpointIpHit: Int = -1

    private var currentTextColor = 0
    private var pendingInputDeferred: CompletableDeferred<String>? = null
    private var pendingInputVar: String? = null
    private var isStopRequested = false
    private var isPauseRequested = false

    fun loadProgram(p: BytecodeProgram) {
        program = p
        reset()
    }

    fun reset() {
        ip = 0
        stack.clear()
        variables.clear()
        arrays.clear()
        callStack.clear()
        isStopRequested = false
        isPauseRequested = false
        pendingInputDeferred = null
        lastError = null
        state = VmState.IDLE
        graphicsState = com.example.basic.graphics.RetroGraphicsState()
        joystickState = JoystickState()
        spriteDefinitions.clear()
        onGraphicsStateChanged(graphicsState)
        pendingInputVar = null
        currentTextColor = 0
        lastBreakpointLineHit = -1
        lastBreakpointBasicLineHit = -1
        lastBreakpointIpHit = -1
        state = VmState.IDLE
        onStateChange(state)
        onVariablesChanged(variables.toMap())
        onIpChanged(0, 1)
    }

    fun isAtBreakpoint(instruction: Instruction, currentIp: Int): Boolean {
        if (breakpoints.isEmpty()) return false

        val matchesEditorLine = instruction.sourceLine > 0 && breakpoints.contains(instruction.sourceLine)
        val matchesBasicLine = instruction.sourceLineNum > 0 && breakpoints.contains(instruction.sourceLineNum)

        if (!matchesEditorLine && !matchesBasicLine) {
            return false
        }

        // If we previously paused on this line and haven't left it yet, don't re-trigger immediately
        if (matchesEditorLine && instruction.sourceLine == lastBreakpointLineHit) {
            return false
        }
        if (matchesBasicLine && instruction.sourceLineNum == lastBreakpointBasicLineHit) {
            return false
        }

        return true
    }

    fun toggleBreakpoint(lineOrIp: Int) {
        if (breakpoints.contains(lineOrIp)) {
            breakpoints.remove(lineOrIp)
        } else {
            breakpoints.add(lineOrIp)
        }
    }

    fun provideInput(text: String) {
        onOutput(TerminalLine(text = "$text\n", colorIndex = 14, isInputEcho = true))
        val varName = pendingInputVar
        val def = pendingInputDeferred
        pendingInputDeferred = null
        pendingInputVar = null

        if (varName != null) {
            val num = text.trim().toDoubleOrNull()
            val value = if (num != null && !varName.endsWith("$")) {
                BasicValue.NumberVal(num)
            } else {
                BasicValue.StringVal(text.trim())
            }
            variables[varName] = value
            onVariablesChanged(variables.toMap())
        }

        def?.complete(text)
    }

    fun pause() {
        if (state == VmState.RUNNING) {
            isPauseRequested = true
        }
    }

    fun stop() {
        isStopRequested = true
        isPauseRequested = false
        pendingInputDeferred?.cancel()
        state = VmState.TERMINATED
        onStateChange(state)
    }

    /**
     * Runs execution until completion, breakpoint, or waiting for input.
     */
    suspend fun execute(debug: Boolean = false) {
        val p = program ?: return
        isDebugMode = debug || breakpoints.isNotEmpty()
        isStopRequested = false
        isPauseRequested = false
        state = VmState.RUNNING
        onStateChange(state)

        while (ip < p.instructions.size && !isStopRequested && !isPauseRequested) {
            val instruction = p.instructions[ip]
            val currentLine = instruction.sourceLine

            // Breakpoint check
            if ((isDebugMode || breakpoints.isNotEmpty()) && isAtBreakpoint(instruction, ip)) {
                lastBreakpointLineHit = instruction.sourceLine
                lastBreakpointBasicLineHit = instruction.sourceLineNum
                lastBreakpointIpHit = ip
                state = VmState.PAUSED_DEBUG
                onStateChange(state)
                onIpChanged(ip, currentLine)
                onVariablesChanged(variables.toMap())
                val displayLine = if (instruction.sourceLineNum > 0) instruction.sourceLineNum else currentLine
                onBreakpointHit(displayLine)
                onOutput(TerminalLine("\n*** BREAKPOINT HIT AT LINE $displayLine ***\n", colorIndex = 14, isSystemNotice = true))
                return
            }

            // Execute single instruction
            val jumpOccurred = executeInstruction(instruction, p)

            // When line changes or jump occurs, clear the last breakpoint line hit
            if (jumpOccurred) {
                lastBreakpointLineHit = -1
                lastBreakpointBasicLineHit = -1
                lastBreakpointIpHit = -1
            } else {
                val nextInst = p.instructions.getOrNull(ip + 1)
                if (nextInst != null) {
                    if (nextInst.sourceLine != lastBreakpointLineHit) {
                        lastBreakpointLineHit = -1
                    }
                    if (nextInst.sourceLineNum != lastBreakpointBasicLineHit) {
                        lastBreakpointBasicLineHit = -1
                    }
                }
                if (ip + 1 != lastBreakpointIpHit) {
                    lastBreakpointIpHit = -1
                }
            }

            onIpChanged(ip, currentLine)

            if (state == VmState.WAITING_FOR_INPUT || state == VmState.TERMINATED || state == VmState.ERROR) {
                return
            }

            if (!jumpOccurred) {
                ip++
            }

            if (isPauseRequested) {
                break
            }

            // Cooperative yield for UI and user abort
            if (ip % 30 == 0) {
                delay(1)
            }
        }

        if (isPauseRequested) {
            isPauseRequested = false
            state = VmState.PAUSED_DEBUG
            onStateChange(state)
            val currentInstructionLine = p.instructions.getOrNull(ip)?.sourceLine ?: -1
            onIpChanged(ip, currentInstructionLine)
            onVariablesChanged(variables.toMap())
            return
        }

        if (state == VmState.RUNNING) {
            state = VmState.TERMINATED
            onStateChange(state)
            onOutput(TerminalLine("\n*** PROGRAM COMPLETED ***", colorIndex = 11, isSystemNotice = true))
        }
    }

    /**
     * Executes instructions until the next source line is reached, or program completes/pauses.
     */
    suspend fun stepLine(): Boolean {
        val p = program ?: return false
        if (ip >= p.instructions.size) {
            state = VmState.TERMINATED
            onStateChange(state)
            return false
        }

        val startLine = p.instructions.getOrNull(ip)?.sourceLine ?: -1
        var stepsCount = 0

        while (ip < p.instructions.size && !isStopRequested) {
            val instruction = p.instructions[ip]
            val currentLine = instruction.sourceLine

            // If we hit a breakpoint on a DIFFERENT line while stepping, stop there
            if (stepsCount > 0 && isAtBreakpoint(instruction, ip)) {
                lastBreakpointLineHit = instruction.sourceLine
                lastBreakpointBasicLineHit = instruction.sourceLineNum
                lastBreakpointIpHit = ip
                state = VmState.PAUSED_DEBUG
                onStateChange(state)
                onIpChanged(ip, currentLine)
                onVariablesChanged(variables.toMap())
                val displayLine = if (instruction.sourceLineNum > 0) instruction.sourceLineNum else currentLine
                onBreakpointHit(displayLine)
                return true
            }

            val jumpOccurred = executeInstruction(instruction, p)

            if (!jumpOccurred) {
                ip++
            }
            stepsCount++

            onIpChanged(ip, currentLine)
            onVariablesChanged(variables.toMap())

            if (state == VmState.WAITING_FOR_INPUT || state == VmState.TERMINATED || state == VmState.ERROR) {
                return false
            }

            if (ip >= p.instructions.size) {
                state = VmState.TERMINATED
                onStateChange(state)
                onOutput(TerminalLine("\n*** PROGRAM COMPLETED ***", colorIndex = 11, isSystemNotice = true))
                return false
            }

            val nextLine = p.instructions.getOrNull(ip)?.sourceLine ?: -1
            if (nextLine != startLine && nextLine > 0) {
                lastBreakpointLineHit = -1
                lastBreakpointBasicLineHit = -1
                lastBreakpointIpHit = -1
                break
            }

            if (stepsCount % 100 == 0) {
                delay(1)
            }
        }

        state = VmState.PAUSED_DEBUG
        onStateChange(state)
        return ip < p.instructions.size
    }

    /**
     * Executes a single instruction and pauses immediately (Step Into/Over).
     */
    suspend fun step(): Boolean {
        val p = program ?: return false
        if (ip >= p.instructions.size) {
            state = VmState.TERMINATED
            onStateChange(state)
            return false
        }

        val instruction = p.instructions[ip]
        val currentLine = instruction.sourceLine
        val jumpOccurred = executeInstruction(instruction, p)

        if (jumpOccurred) {
            lastBreakpointLineHit = -1
            lastBreakpointBasicLineHit = -1
            lastBreakpointIpHit = -1
        } else {
            val nextInst = p.instructions.getOrNull(ip + 1)
            if (nextInst != null && nextInst.sourceLine != lastBreakpointLineHit) {
                lastBreakpointLineHit = -1
                lastBreakpointBasicLineHit = -1
            }
            if (ip + 1 != lastBreakpointIpHit) {
                lastBreakpointIpHit = -1
            }
            ip++
        }

        onIpChanged(ip, currentLine)
        onVariablesChanged(variables.toMap())

        if (ip >= p.instructions.size && state != VmState.TERMINATED) {
            state = VmState.TERMINATED
            onStateChange(state)
            onOutput(TerminalLine("\n*** PROGRAM COMPLETED ***", colorIndex = 11, isSystemNotice = true))
            return false
        }

        if (state == VmState.RUNNING) {
            state = VmState.PAUSED_DEBUG
            onStateChange(state)
        }

        return ip < p.instructions.size && state != VmState.TERMINATED && state != VmState.ERROR
    }

    private suspend fun executeInstruction(inst: Instruction, p: BytecodeProgram): Boolean {
        try {
            when (inst.op) {
                OpCode.NOP -> {}
                OpCode.PUSH_CONST -> {
                    val value = p.constants.getOrElse(inst.argInt) { BasicValue.NumberVal(0.0) }
                    stack.addLast(value)
                }
                OpCode.POP -> if (stack.isNotEmpty()) stack.removeLast()
                OpCode.DUP -> if (stack.isNotEmpty()) stack.addLast(stack.last())

                OpCode.LOAD_VAR -> {
                    val name = inst.argString
                    if (name.startsWith("__FN_")) {
                        // Handle built-in function invocation
                        handleBuiltinFunction(name.substring(5))
                    } else {
                        val value = variables[name] ?: if (name.endsWith("$")) BasicValue.StringVal("") else BasicValue.NumberVal(0.0)
                        stack.addLast(value)
                    }
                }
                OpCode.STORE_VAR -> {
                    val value = if (stack.isNotEmpty()) stack.removeLast() else BasicValue.NumberVal(0.0)
                    variables[inst.argString] = value
                    if (!inst.argString.startsWith("__")) {
                        onVariablesChanged(variables.toMap())
                    }
                }

                // Arithmetic
                OpCode.ADD -> {
                    val b = popValue()
                    val a = popValue()
                    if (a is BasicValue.StringVal || b is BasicValue.StringVal) {
                        stack.addLast(BasicValue.StringVal(a.toString() + b.toString()))
                    } else {
                        stack.addLast(BasicValue.NumberVal(a.asNumber() + b.asNumber()))
                    }
                }
                OpCode.SUB -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.NumberVal(a - b))
                }
                OpCode.MUL -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.NumberVal(a * b))
                }
                OpCode.DIV -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    if (b == 0.0) throw ArithmeticException("Division by zero at line ${inst.sourceLine}")
                    stack.addLast(BasicValue.NumberVal(a / b))
                }
                OpCode.MOD -> {
                    val b = popValue().asNumber().toLong()
                    val a = popValue().asNumber().toLong()
                    if (b == 0L) throw ArithmeticException("Modulo by zero at line ${inst.sourceLine}")
                    stack.addLast(BasicValue.NumberVal((a % b).toDouble()))
                }
                OpCode.POW -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.NumberVal(a.pow(b)))
                }
                OpCode.NEG -> {
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.NumberVal(-a))
                }

                // Comparison & Logic
                OpCode.EQ -> {
                    val b = popValue()
                    val a = popValue()
                    stack.addLast(BasicValue.BooleanVal(a.toString() == b.toString()))
                }
                OpCode.NEQ -> {
                    val b = popValue()
                    val a = popValue()
                    stack.addLast(BasicValue.BooleanVal(a.toString() != b.toString()))
                }
                OpCode.LT -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.BooleanVal(a < b))
                }
                OpCode.LTE -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.BooleanVal(a <= b))
                }
                OpCode.GT -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.BooleanVal(a > b))
                }
                OpCode.GTE -> {
                    val b = popValue().asNumber()
                    val a = popValue().asNumber()
                    stack.addLast(BasicValue.BooleanVal(a >= b))
                }
                OpCode.AND -> {
                    val b = popValue().toBoolean()
                    val a = popValue().toBoolean()
                    stack.addLast(BasicValue.BooleanVal(a && b))
                }
                OpCode.OR -> {
                    val b = popValue().toBoolean()
                    val a = popValue().toBoolean()
                    stack.addLast(BasicValue.BooleanVal(a || b))
                }
                OpCode.NOT -> {
                    val a = popValue().toBoolean()
                    stack.addLast(BasicValue.BooleanVal(!a))
                }

                // Control flow
                OpCode.JUMP -> {
                    ip = inst.argInt
                    return true
                }
                OpCode.JUMP_IF_FALSE -> {
                    val cond = popValue().toBoolean()
                    if (!cond) {
                        ip = inst.argInt
                        return true
                    }
                }
                OpCode.JUMP_IF_TRUE -> {
                    val cond = popValue().toBoolean()
                    if (cond) {
                        ip = inst.argInt
                        return true
                    }
                }

                // Subroutines
                OpCode.GOSUB -> {
                    callStack.addLast(CallFrame(returnIp = ip + 1, callerLine = inst.sourceLine))
                    ip = inst.argInt
                    return true
                }
                OpCode.RETURN -> {
                    if (callStack.isEmpty()) {
                        throw IllegalStateException("RETURN without GOSUB at line ${inst.sourceLine}")
                    }
                    val frame = callStack.removeLast()
                    ip = frame.returnIp
                    return true
                }

                // I/O & Terminal
                OpCode.PRINT -> {
                    val value = popValue()
                    onOutput(TerminalLine(text = "${value.asDisplayString()}\n", colorIndex = currentTextColor))
                }
                OpCode.PRINT_SEMI -> {
                    val value = popValue()
                    onOutput(TerminalLine(text = value.asDisplayString(), colorIndex = currentTextColor))
                }
                OpCode.PRINT_COMMA -> {
                    val value = popValue()
                    onOutput(TerminalLine(text = "${value.asDisplayString()}\t", colorIndex = currentTextColor))
                }
                OpCode.PRINT_NEWLINE -> {
                    onOutput(TerminalLine(text = "\n", colorIndex = currentTextColor))
                }
                OpCode.INPUT -> {
                    if (inst.argInt >= 0 && inst.argInt < p.constants.size) {
                        val prompt = p.constants[inst.argInt].toString()
                        onOutput(TerminalLine(text = "$prompt ", colorIndex = 11))
                    } else {
                        onOutput(TerminalLine(text = "? ", colorIndex = 11))
                    }
                    pendingInputVar = inst.argString
                    val deferred = CompletableDeferred<String>()
                    pendingInputDeferred = deferred
                    state = VmState.WAITING_FOR_INPUT
                    onStateChange(state)

                    // Await input resolution
                    deferred.await()
                    state = if (isDebugMode) VmState.PAUSED_DEBUG else VmState.RUNNING
                    onStateChange(state)
                }
                OpCode.CLS -> {
                    onClearScreen()
                    if (graphicsState.mode > 0) {
                        graphicsState = graphicsState.copy(
                            pixels = IntArray(320 * 200) { graphicsState.backgroundColor },
                            revision = graphicsState.revision + 1
                        )
                        onGraphicsStateChanged(graphicsState)
                    }
                }
                OpCode.COLOR -> {
                    val c = popValue().asNumber().toInt()
                    currentTextColor = c % 16
                    graphicsState = graphicsState.copy(currentColor = c.coerceIn(0, 15))
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.SCREEN -> {
                    val mode = popValue().asNumber().toInt()
                    graphicsState = graphicsState.copy(mode = mode, revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.PSET -> {
                    val cArg = popValue().asNumber().toInt()
                    val y = popValue().asNumber().toInt()
                    val x = popValue().asNumber().toInt()
                    val color = if (cArg >= 0) cArg % 16 else graphicsState.currentColor
                    if (x in 0 until 320 && y in 0 until 200) {
                        graphicsState.pixels[y * 320 + x] = color
                        graphicsState = graphicsState.copy(revision = graphicsState.revision + 1)
                        onGraphicsStateChanged(graphicsState)
                    }
                }
                OpCode.DRAW_LINE -> {
                    val cArg = popValue().asNumber().toInt()
                    val y2 = popValue().asNumber().toInt()
                    val x2 = popValue().asNumber().toInt()
                    val y1 = popValue().asNumber().toInt()
                    val x1 = popValue().asNumber().toInt()
                    val color = if (cArg >= 0) cArg % 16 else graphicsState.currentColor
                    drawLineBresenham(x1, y1, x2, y2, color)
                    graphicsState = graphicsState.copy(revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.DRAW_RECT -> {
                    val fill = popValue().asNumber().toInt()
                    val cArg = popValue().asNumber().toInt()
                    val y2 = popValue().asNumber().toInt()
                    val x2 = popValue().asNumber().toInt()
                    val y1 = popValue().asNumber().toInt()
                    val x1 = popValue().asNumber().toInt()
                    val color = if (cArg >= 0) cArg % 16 else graphicsState.currentColor
                    drawRectBresenham(x1, y1, x2, y2, color, fill != 0)
                    graphicsState = graphicsState.copy(revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.DRAW_CIRCLE -> {
                    val fill = popValue().asNumber().toInt()
                    val cArg = popValue().asNumber().toInt()
                    val radius = popValue().asNumber().toInt()
                    val y = popValue().asNumber().toInt()
                    val x = popValue().asNumber().toInt()
                    val color = if (cArg >= 0) cArg % 16 else graphicsState.currentColor
                    drawCircleBresenham(x, y, radius, color, fill != 0)
                    graphicsState = graphicsState.copy(revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.SOUND -> {
                    val waveType = popValue().asNumber().toInt().coerceIn(0, 4)
                    val durationMs = popValue().asNumber().toLong().coerceAtLeast(1)
                    val freqHz = popValue().asNumber().coerceAtLeast(20.0)
                    onSound(freqHz, durationMs, waveType)
                }
                OpCode.SFX -> {
                    val preset = popValue().asNumber().toInt()
                    onSfx(preset)
                }
                OpCode.SPRITEDEF -> {
                    val patRaw = popValue().toString()
                    val size = popValue().asNumber().toInt().let { if (it == 16) 16 else 8 }
                    val id = popValue().asNumber().toInt().coerceAtLeast(0)

                    val bits = parseSpritePattern(patRaw, size)
                    val def = SpriteDefinition(width = size, height = size, pattern = bits, colorPattern = null)
                    spriteDefinitions[id] = def

                    // Update any active sprites currently using this bitmap definition
                    val updatedSprites = graphicsState.sprites.toMutableMap()
                    var changed = false
                    for ((sId, spr) in updatedSprites) {
                        if (spr.patternDefId == id || (spr.patternDefId == null && sId == id)) {
                            updatedSprites[sId] = spr.copy(width = size, height = size, pattern = bits, colorPattern = null, patternDefId = id)
                            changed = true
                        }
                    }
                    if (changed) {
                        graphicsState = graphicsState.copy(sprites = updatedSprites, revision = graphicsState.revision + 1)
                        onGraphicsStateChanged(graphicsState)
                    }
                }
                OpCode.SPRITEDEFMULTI -> {
                    val argCount = inst.argInt
                    val id: Int
                    val explicitSize: Int?
                    val hexPattern: String
                    val maskId: Int

                    if (argCount == 4) {
                        val val4 = popValue()
                        val val3 = popValue()
                        val val2 = popValue()
                        val val1 = popValue()
                        id = val1.asNumber().toInt().coerceAtLeast(0)

                        if (val3 is BasicValue.StringVal) {
                            explicitSize = val2.asNumber().toInt()
                            hexPattern = val3.toString()
                            maskId = val4.asNumber().toInt()
                        } else if (val4 is BasicValue.StringVal) {
                            explicitSize = val2.asNumber().toInt()
                            maskId = val3.asNumber().toInt()
                            hexPattern = val4.toString()
                        } else if (val2 is BasicValue.StringVal) {
                            hexPattern = val2.toString()
                            maskId = val3.asNumber().toInt()
                            explicitSize = val4.asNumber().toInt()
                        } else {
                            explicitSize = val2.asNumber().toInt()
                            hexPattern = val3.toString()
                            maskId = val4.asNumber().toInt()
                        }
                    } else {
                        // 3 arguments
                        val val3 = popValue()
                        val val2 = popValue()
                        val val1 = popValue()
                        id = val1.asNumber().toInt().coerceAtLeast(0)
                        explicitSize = null

                        if (val2 is BasicValue.StringVal) {
                            hexPattern = val2.toString()
                            maskId = val3.asNumber().toInt()
                        } else if (val3 is BasicValue.StringVal) {
                            maskId = val2.asNumber().toInt()
                            hexPattern = val3.toString()
                        } else {
                            hexPattern = val2.toString()
                            maskId = val3.asNumber().toInt()
                        }
                    }

                    // Retrieve monochrome sprite definition to use as transparency mask
                    val maskDef = spriteDefinitions[maskId]
                        ?: throw IllegalArgumentException("Transparency mask sprite definition $maskId is not defined")

                    val maskW = maskDef.width
                    val maskH = maskDef.height
                    val maskPixelCount = maskW * maskH

                    // Validate explicit size if passed
                    if (explicitSize != null && explicitSize > 0 && (explicitSize != maskW || explicitSize != maskH)) {
                        throw IllegalArgumentException("Dimensions mismatch: Specified size $explicitSize does not match transparency mask dimensions (${maskW}x${maskH})")
                    }

                    // Check multiline / delimited format
                    val cleanPat = hexPattern.trim().trim('"', '\'')
                    val hasLines = cleanPat.contains('\n') || cleanPat.contains('/')
                    if (hasLines) {
                        val delim = if (cleanPat.contains('\n')) '\n' else '/'
                        val rows = cleanPat.split(delim).map { it.trim().replace(" ", "").replace("\t", "").replace("\r", "") }.filter { it.isNotEmpty() }
                        if (rows.size != maskH) {
                            throw IllegalArgumentException("Dimensions mismatch: Colour definition row count (${rows.size}) does not match transparency mask height ($maskH)")
                        }
                        for ((idx, row) in rows.withIndex()) {
                            if (row.length != maskW) {
                                throw IllegalArgumentException("Dimensions mismatch: Colour definition row $idx width (${row.length}) does not match transparency mask width ($maskW)")
                            }
                        }
                    }

                    // Clean and extract hex digits
                    var hexBody = cleanPat
                    if (hexBody.startsWith("0x", ignoreCase = true) || hexBody.startsWith("&H", ignoreCase = true)) {
                        hexBody = hexBody.substring(2)
                    }
                    val hexChars = hexBody.filter { it.isLetterOrDigit() }

                    if (hexChars.length != maskPixelCount) {
                        throw IllegalArgumentException("Dimensions mismatch: Colour definition pixel count (${hexChars.length}) does not match transparency mask pixel count ($maskPixelCount)")
                    }

                    val hexColors = ArrayList<Int>(maskPixelCount)
                    for (ch in hexChars) {
                        val col = ch.digitToIntOrNull(16)
                            ?: throw IllegalArgumentException("Invalid hex digit '$ch' in SPRITEDEFMULTI colour pattern")
                        hexColors.add(col)
                    }

                    val def = SpriteDefinition(
                        width = maskW,
                        height = maskH,
                        pattern = maskDef.pattern,
                        colorPattern = hexColors
                    )
                    spriteDefinitions[id] = def

                    // Update any active sprites currently using this bitmap definition
                    val updatedSprites = graphicsState.sprites.toMutableMap()
                    var changed = false
                    for ((sId, spr) in updatedSprites) {
                        if (spr.patternDefId == id || (spr.patternDefId == null && sId == id)) {
                            updatedSprites[sId] = spr.copy(
                                width = maskW,
                                height = maskH,
                                pattern = maskDef.pattern,
                                colorPattern = hexColors,
                                patternDefId = id
                            )
                            changed = true
                        }
                    }
                    if (changed) {
                        graphicsState = graphicsState.copy(sprites = updatedSprites, revision = graphicsState.revision + 1)
                        onGraphicsStateChanged(graphicsState)
                    }
                }
                OpCode.SPRITE -> {
                    val colorArg = popValue().asNumber().toInt()
                    val patArg = popValue().asNumber().toInt()
                    val y = popValue().asNumber().toInt()
                    val x = popValue().asNumber().toInt()
                    val id = popValue().asNumber().toInt().coerceIn(0, 15)

                    val updatedSprites = graphicsState.sprites.toMutableMap()
                    val currentSprite = updatedSprites[id] ?: com.example.basic.graphics.Sprite(id = id)

                    // Apply pattern definition from spriteDefinitions if patArg is valid
                    val targetPatId = if (patArg >= 0) patArg else (currentSprite.patternDefId ?: id)
                    val patternDef = if (targetPatId >= 0) spriteDefinitions[targetPatId] else null
                    val finalPattern = patternDef?.pattern ?: currentSprite.pattern
                    val finalWidth = patternDef?.width ?: currentSprite.width
                    val finalHeight = patternDef?.height ?: currentSprite.height
                    val finalColor = if (colorArg >= 0) colorArg % 16 else currentSprite.colorIndex
                    val finalColorPattern = if (patternDef != null) patternDef.colorPattern else currentSprite.colorPattern

                    updatedSprites[id] = currentSprite.copy(
                        x = x,
                        y = y,
                        width = finalWidth,
                        height = finalHeight,
                        pattern = finalPattern,
                        colorIndex = finalColor,
                        colorPattern = finalColorPattern,
                        patternDefId = targetPatId,
                        visible = true
                    )
                    graphicsState = graphicsState.copy(sprites = updatedSprites, revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.SPRITEMOVE -> {
                    val dy = popValue().asNumber().toInt()
                    val dx = popValue().asNumber().toInt()
                    val id = popValue().asNumber().toInt().coerceIn(0, 15)

                    val updatedSprites = graphicsState.sprites.toMutableMap()
                    val currentSprite = updatedSprites[id] ?: com.example.basic.graphics.Sprite(id = id)
                    updatedSprites[id] = currentSprite.copy(
                        x = currentSprite.x + dx,
                        y = currentSprite.y + dy
                    )
                    graphicsState = graphicsState.copy(sprites = updatedSprites, revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.SPRITEPOS -> {
                    val y = popValue().asNumber().toInt()
                    val x = popValue().asNumber().toInt()
                    val id = popValue().asNumber().toInt().coerceIn(0, 15)

                    val updatedSprites = graphicsState.sprites.toMutableMap()
                    val currentSprite = updatedSprites[id] ?: com.example.basic.graphics.Sprite(id = id)
                    updatedSprites[id] = currentSprite.copy(
                        x = x,
                        y = y
                    )
                    graphicsState = graphicsState.copy(sprites = updatedSprites, revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.SPRITESHOW -> {
                    val visible = popValue().asNumber().toInt() != 0
                    val id = popValue().asNumber().toInt().coerceIn(0, 15)

                    val updatedSprites = graphicsState.sprites.toMutableMap()
                    val currentSprite = updatedSprites[id] ?: com.example.basic.graphics.Sprite(id = id)
                    updatedSprites[id] = currentSprite.copy(visible = visible)
                    graphicsState = graphicsState.copy(sprites = updatedSprites, revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.SPRITEHIT -> {
                    val id2 = popValue().asNumber().toInt().coerceIn(0, 15)
                    val id1 = popValue().asNumber().toInt().coerceIn(0, 15)
                    val s1 = graphicsState.sprites[id1]
                    val s2 = graphicsState.sprites[id2]
                    val hits = if (s1 != null && s2 != null) s1.hits(s2) else false
                    stack.addLast(BasicValue.BooleanVal(hits))
                }
                OpCode.JOYSTICK -> {
                    val enabled = popValue().asNumber().toInt() != 0
                    graphicsState = graphicsState.copy(showJoystick = enabled, revision = graphicsState.revision + 1)
                    onGraphicsStateChanged(graphicsState)
                }
                OpCode.BEEP -> {
                    onBeep()
                }
                OpCode.SLEEP -> {
                    val ms = popValue().asNumber().toLong().coerceAtLeast(0)
                    delay(ms.coerceAtMost(5000))
                }

                // Arrays
                OpCode.DIM_ARRAY -> {
                    val size = popValue().asNumber().toInt()
                    arrays[inst.argString] = mutableMapOf()
                }
                OpCode.STORE_ARRAY -> {
                    val value = popValue()
                    val index = popValue().asNumber().toInt()
                    val arr = arrays.getOrPut(inst.argString) { mutableMapOf() }
                    arr[index] = value
                }
                OpCode.LOAD_ARRAY -> {
                    val index = popValue().asNumber().toInt()
                    val arr = arrays[inst.argString]
                    val value = arr?.get(index) ?: BasicValue.NumberVal(0.0)
                    stack.addLast(value)
                }

                OpCode.HALT -> {
                    state = VmState.TERMINATED
                    onStateChange(state)
                    return false
                }
            }
        } catch (e: Exception) {
            state = VmState.ERROR
            lastError = e.message
            onStateChange(state)
            onOutput(TerminalLine(text = "\n[RUNTIME ERROR at line ${inst.sourceLine}]: ${e.message}\n", isError = true))
            return false
        }

        return false
    }

    private fun popValue(): BasicValue {
        return if (stack.isNotEmpty()) stack.removeLast() else BasicValue.NumberVal(0.0)
    }

    private fun handleBuiltinFunction(fn: String) {
        val arg = popValue()
        val num = arg.asNumber()
        val str = arg.toString()
        val result = when (fn.uppercase(Locale.ROOT)) {
            "INT" -> BasicValue.NumberVal(kotlin.math.floor(num))
            "ABS" -> BasicValue.NumberVal(abs(num))
            "SQR" -> BasicValue.NumberVal(sqrt(num.coerceAtLeast(0.0)))
            "RND" -> BasicValue.NumberVal(Random.nextDouble() * num)
            "SIN" -> BasicValue.NumberVal(sin(num))
            "COS" -> BasicValue.NumberVal(cos(num))
            "TAN" -> BasicValue.NumberVal(tan(num))
            "LEN" -> BasicValue.NumberVal(str.length.toDouble())
            "STR$" -> BasicValue.StringVal(arg.asDisplayString())
            "VAL" -> BasicValue.NumberVal(str.toDoubleOrNull() ?: 0.0)
            "CHR$" -> BasicValue.StringVal(num.toInt().toChar().toString())
            "ASC" -> BasicValue.NumberVal(if (str.isNotEmpty()) str[0].code.toDouble() else 0.0)
            "JOY" -> {
                val mode = num.toInt()
                val joyVal = when (mode) {
                    1 -> joystickState.axisX.toDouble()
                    2 -> joystickState.axisY.toDouble()
                    else -> joystickState.direction.toDouble()
                }
                BasicValue.NumberVal(joyVal)
            }
            "FIRE" -> {
                BasicValue.NumberVal(if (joystickState.fire) 1.0 else 0.0)
            }
            else -> BasicValue.NumberVal(0.0)
        }
        stack.addLast(result)
    }

    private fun drawLineBresenham(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        var x0 = x1
        var y0 = y1
        val dx = kotlin.math.abs(x2 - x0)
        val dy = -kotlin.math.abs(y2 - y0)
        val sx = if (x0 < x2) 1 else -1
        val sy = if (y0 < y2) 1 else -1
        var err = dx + dy

        while (true) {
            if (x0 in 0 until 320 && y0 in 0 until 200) {
                graphicsState.pixels[y0 * 320 + x0] = color
            }
            if (x0 == x2 && y0 == y2) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x0 += sx
            }
            if (e2 <= dx) {
                err += dx
                y0 += sy
            }
        }
    }

    private fun drawRectBresenham(x1: Int, y1: Int, x2: Int, y2: Int, color: Int, fill: Boolean) {
        val minX = kotlin.math.min(x1, x2).coerceIn(0, 319)
        val maxX = kotlin.math.max(x1, x2).coerceIn(0, 319)
        val minY = kotlin.math.min(y1, y2).coerceIn(0, 199)
        val maxY = kotlin.math.max(y1, y2).coerceIn(0, 199)

        if (fill) {
            for (y in minY..maxY) {
                for (x in minX..maxX) {
                    graphicsState.pixels[y * 320 + x] = color
                }
            }
        } else {
            for (x in minX..maxX) {
                graphicsState.pixels[minY * 320 + x] = color
                graphicsState.pixels[maxY * 320 + x] = color
            }
            for (y in minY..maxY) {
                graphicsState.pixels[y * 320 + minX] = color
                graphicsState.pixels[y * 320 + maxX] = color
            }
        }
    }

    private fun drawCircleBresenham(xc: Int, yc: Int, r: Int, color: Int, fill: Boolean) {
        if (r <= 0) {
            if (xc in 0 until 320 && yc in 0 until 200) {
                graphicsState.pixels[yc * 320 + xc] = color
            }
            return
        }

        if (fill) {
            for (dy in -r..r) {
                val curY = yc + dy
                if (curY !in 0 until 200) continue
                val dxMax = kotlin.math.sqrt((r * r - dy * dy).toDouble()).toInt()
                val minX = (xc - dxMax).coerceIn(0, 319)
                val maxX = (xc + dxMax).coerceIn(0, 319)
                for (x in minX..maxX) {
                    graphicsState.pixels[curY * 320 + x] = color
                }
            }
        } else {
            var x = 0
            var y = r
            var d = 3 - 2 * r

            fun putCirclePixel(px: Int, py: Int) {
                if (px in 0 until 320 && py in 0 until 200) {
                    graphicsState.pixels[py * 320 + px] = color
                }
            }

            fun plotCirclePoints(cx: Int, cy: Int, px: Int, py: Int) {
                putCirclePixel(cx + px, cy + py)
                putCirclePixel(cx - px, cy + py)
                putCirclePixel(cx + px, cy - py)
                putCirclePixel(cx - px, cy - py)
                putCirclePixel(cx + py, cy + px)
                putCirclePixel(cx - py, cy + px)
                putCirclePixel(cx + py, cy - px)
                putCirclePixel(cx - py, cy - px)
            }

            plotCirclePoints(xc, yc, x, y)
            while (y >= x) {
                x++
                if (d > 0) {
                    y--
                    d += 4 * (x - y) + 10
                } else {
                    d += 4 * x + 6
                }
                plotCirclePoints(xc, yc, x, y)
            }
        }
    }

    /**
     * Parses a sprite pattern string into a flat bitmask (true = solid, false = transparent).
     * Supports:
     * 1. Comma-separated byte values (e.g. "60, 126, 255, 219, ...")
     * 2. Multiline ASCII art (lines separated by '\n' or '/')
     * 3. Hexadecimal strings (explicit 0x/&H prefix or 16/64 hex chars with A-F)
     * 4. Single-row bitmask repeated across all rows (e.g. "11111111")
     * 5. Character-per-pixel bitmask ('1', '#', '*', 'X', '@' = solid, '0', '.', '-' = transparent)
     */
    private fun parseSpritePattern(patRaw: String, size: Int): List<Boolean> {
        val clean = patRaw.trim().trim('"', '\'')
        val requiredBits = size * size

        // Format 1: Comma-separated numeric byte values e.g. "60, 126, 255, 219, ..."
        if (clean.contains(',')) {
            val parts = clean.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val allNumbers = parts.isNotEmpty() && parts.all { part ->
                part.toIntOrNull() != null ||
                (part.startsWith("&H", ignoreCase = true) && part.substring(2).toIntOrNull(16) != null) ||
                (part.startsWith("0x", ignoreCase = true) && part.substring(2).toIntOrNull(16) != null)
            }
            if (allNumbers) {
                val bits = ArrayList<Boolean>(requiredBits)
                for (part in parts) {
                    val num = when {
                        part.startsWith("&H", ignoreCase = true) || part.startsWith("0x", ignoreCase = true) ->
                            part.substring(2).toIntOrNull(16) ?: 0
                        else -> part.toIntOrNull() ?: 0
                    }
                    for (b in (size - 1) downTo 0) {
                        if (bits.size < requiredBits) {
                            bits.add(((num shr b) and 1) == 1)
                        }
                    }
                }
                while (bits.size < requiredBits) {
                    bits.add(false)
                }
                return bits.take(requiredBits)
            }
        }

        // Format 2: Multiline ASCII visual pattern (lines separated by \n or /)
        if (clean.contains('\n') || clean.contains('/')) {
            val delim = if (clean.contains('\n')) '\n' else '/'
            val lines = clean.split(delim).map { it.trim() }.filter { it.isNotEmpty() }
            val bits = ArrayList<Boolean>(requiredBits)
            for (line in lines) {
                val rowClean = line.replace(" ", "").replace("\t", "").replace("\r", "")
                for (x in 0 until size) {
                    if (x < rowClean.length) {
                        val c = rowClean[x]
                        bits.add(c == '1' || c == '#' || c == '*' || c == 'X' || c == 'x' || c == '@' || c == '+')
                    } else {
                        bits.add(false)
                    }
                }
            }
            while (bits.size < requiredBits) {
                bits.add(false)
            }
            return bits.take(requiredBits)
        }

        val noSpaces = clean.replace(" ", "").replace("\t", "").replace("\r", "")

        // Format 3: Explicit or detected Hexadecimal bitmask (starts with 0x/&H or has hex letters A-F with matching length)
        val isExplicitHex = noSpaces.startsWith("0x", ignoreCase = true) || noSpaces.startsWith("&H", ignoreCase = true)
        val hexBody = if (isExplicitHex) noSpaces.substring(2) else noSpaces
        val hasHexLetter = hexBody.any { it in 'a'..'f' || it in 'A'..'F' }
        val isAllHex = hexBody.isNotEmpty() && hexBody.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }

        if (isExplicitHex || (hasHexLetter && isAllHex && hexBody.length == requiredBits / 4)) {
            val bits = ArrayList<Boolean>(requiredBits)
            for (ch in hexBody) {
                val hexVal = ch.digitToIntOrNull(16) ?: 0
                for (b in 3 downTo 0) {
                    if (bits.size < requiredBits) {
                        bits.add(((hexVal shr b) and 1) == 1)
                    }
                }
            }
            while (bits.size < requiredBits) {
                bits.add(false)
            }
            return bits.take(requiredBits)
        }

        // Format 4: Single-row pattern repeated for all rows (e.g. "11111111" for 8x8)
        if (noSpaces.length == size && noSpaces.all { it in "01.#*Xx@+-_ " }) {
            val bits = ArrayList<Boolean>(requiredBits)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val c = noSpaces[x]
                    bits.add(c == '1' || c == '#' || c == '*' || c == 'X' || c == 'x' || c == '@' || c == '+')
                }
            }
            return bits
        }

        // Format 5: Standard character-per-pixel bitmask (1, #, *, X = solid; 0, ., - = transparent)
        val bits = ArrayList<Boolean>(requiredBits)
        for (c in noSpaces) {
            if (bits.size < requiredBits) {
                bits.add(c == '1' || c == '#' || c == '*' || c == 'X' || c == 'x' || c == '@' || c == '+')
            }
        }
        while (bits.size < requiredBits) {
            bits.add(false)
        }
        return bits.take(requiredBits)
    }
}
