package com.example.basic.model

/**
 * Supported BASIC Value types: Double (numbers), String (text), Boolean
 */
sealed class BasicValue {
    data class NumberVal(val value: Double) : BasicValue() {
        override fun toString(): String {
            return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        }
    }
    data class StringVal(val value: String) : BasicValue() {
        override fun toString(): String = value
    }
    data class BooleanVal(val value: Boolean) : BasicValue() {
        override fun toString(): String = if (value) "TRUE" else "FALSE"
    }

    fun toBoolean(): Boolean = when (this) {
        is BooleanVal -> value
        is NumberVal -> value != 0.0
        is StringVal -> value.isNotEmpty() && value != "0"
    }

    fun asNumber(): Double = when (this) {
        is NumberVal -> value
        is StringVal -> value.toDoubleOrNull() ?: 0.0
        is BooleanVal -> if (value) 1.0 else 0.0
    }

    fun asDisplayString(): String = toString()
}

/**
 * Bytecode Opcodes executed by the virtual machine.
 */
enum class OpCode {
    NOP,
    // Constants / Stack
    PUSH_CONST,    // arg: constant table index
    POP,           // pop top of stack
    DUP,           // duplicate top of stack

    // Variables
    LOAD_VAR,      // arg: variable name index
    STORE_VAR,     // arg: variable name index

    // Arithmetic
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    POW,
    NEG,

    // Comparison & Logical
    EQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
    AND,
    OR,
    NOT,

    // Control flow
    JUMP,          // unconditional jump to instruction offset
    JUMP_IF_FALSE, // jump if top is false (pops condition)
    JUMP_IF_TRUE,  // jump if top is true (pops condition)

    // Subroutines
    GOSUB,         // push return IP and jump
    RETURN,        // pop return IP and jump

    // I/O & System
    PRINT,         // pop and output to terminal with newline
    PRINT_SEMI,    // pop and output to terminal without newline
    PRINT_COMMA,   // pop and output with tab spacing
    PRINT_NEWLINE, // output newline
    INPUT,         // wait for user input from terminal emulator into variable (arg: var index, optional prompt)
    CLS,           // clear terminal screen
    COLOR,         // set text color (arg or top of stack)
    BEEP,          // beep tone / sound cue
    SLEEP,         // delay in ms (pop from stack)

    // Array / List Operations
    LOAD_ARRAY,    // arg: array name index; pops index expression
    STORE_ARRAY,   // arg: array name index; pops value, pops index
    DIM_ARRAY,     // dimension array with size

    // Program lifecycle
    HALT           // terminate execution
}

/**
 * A single bytecode instruction with source line mapping for real-time debugging.
 */
data class Instruction(
    val op: OpCode,
    val argInt: Int = 0,
    val argString: String = "",
    val sourceLine: Int = -1,    // 1-based original source code line
    val sourceLineNum: Int = -1  // BASIC line number (e.g., 10, 20) if available
) {
    fun toDisassembly(index: Int, constants: List<BasicValue>): String {
        val argDisplay = when (op) {
            OpCode.PUSH_CONST -> if (argInt in constants.indices) " [${constants[argInt]}]" else " #$argInt"
            OpCode.LOAD_VAR, OpCode.STORE_VAR -> " '$argString'"
            OpCode.JUMP, OpCode.JUMP_IF_FALSE, OpCode.JUMP_IF_TRUE, OpCode.GOSUB -> " @$argInt"
            OpCode.INPUT -> " var='$argString' prompt='$argInt'"
            OpCode.DIM_ARRAY, OpCode.LOAD_ARRAY, OpCode.STORE_ARRAY -> " '$argString'"
            OpCode.COLOR -> " #$argInt"
            else -> ""
        }
        val lineInfo = if (sourceLine > 0) " (line $sourceLine)" else ""
        return String.format("%04d: %-14s%-16s%s", index, op.name, argDisplay, lineInfo)
    }
}

/**
 * Bytecode Program container with metadata, symbol tables, and instructions.
 */
data class BytecodeProgram(
    val instructions: List<Instruction>,
    val constants: List<BasicValue>,
    val sourceCode: String = "",
    val lineToIpMap: Map<Int, Int> = emptyMap() // Map source line -> first instruction IP
) {
    fun disassemble(): String {
        val sb = StringBuilder()
        sb.appendLine("; --- BASIC BYTECODE DISASSEMBLY ---")
        sb.appendLine("; Total Instructions: ${instructions.size}")
        sb.appendLine("; Constants Pool (${constants.size}):")
        constants.forEachIndexed { i, c ->
            sb.appendLine(";   #$i = $c (${c::class.simpleName})")
        }
        sb.appendLine("; ----------------------------------")
        instructions.forEachIndexed { index, inst ->
            sb.appendLine(inst.toDisassembly(index, constants))
        }
        return sb.toString()
    }
}
