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
 * Terminal output line or segment with formatting color.
 */
data class TerminalLine(
    val text: String,
    val colorIndex: Int = 0, // 0 = default terminal green/cyan, 1-15 classic color palette
    val isInputEcho: Boolean = false,
    val isSystemNotice: Boolean = false,
    val isError: Boolean = false
)

/**
 * Stack frame for GOSUB / RETURN
 */
data class CallFrame(
    val returnIp: Int,
    val callerLine: Int
)

/**
 * Virtual Machine that executes BASIC Bytecode with real-time debugging,
 * step execution, breakpoints, variable inspection, and terminal I/O.
 */
class BasicVirtualMachine(
    private val onOutput: (TerminalLine) -> Unit,
    private val onStateChange: (VmState) -> Unit,
    private val onVariablesChanged: (Map<String, BasicValue>) -> Unit,
    private val onIpChanged: (currentIp: Int, sourceLine: Int) -> Unit,
    private val onClearScreen: () -> Unit,
    private val onBeep: () -> Unit
) {
    var program: BytecodeProgram? = null
        private set

    var ip = 0
        private set

    var state = VmState.IDLE
        private set

    val stack = ArrayDeque<BasicValue>()
    val variables = mutableMapOf<String, BasicValue>()
    val arrays = mutableMapOf<String, MutableMap<Int, BasicValue>>()
    val callStack = ArrayDeque<CallFrame>()

    // Debugging controls
    val breakpoints = mutableSetOf<Int>() // Line numbers or IP
    var isDebugMode = false
    var stepMode = false

    private var currentTextColor = 0
    private var pendingInputDeferred: CompletableDeferred<String>? = null
    private var pendingInputVar: String? = null
    private var isStopRequested = false

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
        pendingInputDeferred = null
        pendingInputVar = null
        currentTextColor = 0
        state = VmState.IDLE
        onStateChange(state)
        onVariablesChanged(variables.toMap())
        onIpChanged(0, 1)
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

    fun stop() {
        isStopRequested = true
        pendingInputDeferred?.cancel()
        state = VmState.TERMINATED
        onStateChange(state)
    }

    /**
     * Runs execution until completion, breakpoint, or waiting for input.
     */
    suspend fun execute(debug: Boolean = false) {
        val p = program ?: return
        isDebugMode = debug
        isStopRequested = false
        state = VmState.RUNNING
        onStateChange(state)

        while (ip < p.instructions.size && !isStopRequested) {
            val instruction = p.instructions[ip]
            val currentLine = instruction.sourceLine

            // Breakpoint check in debug mode
            if (isDebugMode && (breakpoints.contains(currentLine) || breakpoints.contains(ip))) {
                state = VmState.PAUSED_DEBUG
                onStateChange(state)
                onIpChanged(ip, currentLine)
                onVariablesChanged(variables.toMap())
                return
            }

            // Execute single instruction
            val jumpOccurred = executeInstruction(instruction, p)

            onIpChanged(ip, currentLine)

            if (state == VmState.WAITING_FOR_INPUT || state == VmState.TERMINATED || state == VmState.ERROR) {
                return
            }

            if (!jumpOccurred) {
                ip++
            }

            // Cooperative yield for UI and user abort
            if (ip % 30 == 0) {
                delay(1)
            }
        }

        if (state == VmState.RUNNING) {
            state = VmState.TERMINATED
            onStateChange(state)
            onOutput(TerminalLine("\n*** PROGRAM COMPLETED ***", colorIndex = 11, isSystemNotice = true))
        }
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

        if (!jumpOccurred) {
            ip++
        }

        onIpChanged(ip, currentLine)
        onVariablesChanged(variables.toMap())

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
                }
                OpCode.COLOR -> {
                    val c = popValue().asNumber().toInt()
                    currentTextColor = c % 16
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
            else -> BasicValue.NumberVal(0.0)
        }
        stack.addLast(result)
    }
}
