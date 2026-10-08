package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.BasicValue
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.TerminalLine
import com.example.basic.vm.VmState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicCompilerAndVmTest {

    private val compiler = BasicCompiler()

    @Test
    fun testCompileAndRunPrint() = runBlocking {
        val code = """
            10 PRINT "HELLO WORLD"
            20 END
        """.trimIndent()

        val program = compiler.compile(code)
        assertTrue(program.instructions.isNotEmpty())

        val output = mutableListOf<String>()
        val vm = BasicVirtualMachine(
            onOutput = { output.add(it.text) },
            onStateChange = {},
            onVariablesChanged = {},
            onIpChanged = { _, _ -> },
            onClearScreen = {},
            onBeep = {}
        )

        vm.loadProgram(program)
        vm.execute(debug = false)

        assertTrue(output.any { it.contains("HELLO WORLD") })
    }

    @Test
    fun testArithmeticAndVariables() = runBlocking {
        val code = """
            10 LET A = 10
            20 LET B = 20
            30 LET C = A + B * 2
            40 PRINT "C="; C
        """.trimIndent()

        val program = compiler.compile(code)
        val vars = mutableMapOf<String, BasicValue>()
        val vm = BasicVirtualMachine(
            onOutput = {},
            onStateChange = {},
            onVariablesChanged = { vars.putAll(it) },
            onIpChanged = { _, _ -> },
            onClearScreen = {},
            onBeep = {}
        )

        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(50.0, vars["C"]?.asNumber() ?: 0.0, 0.001)
    }

    @Test
    fun testForLoopAndStepExecution() = runBlocking {
        val code = """
            10 SUM = 0
            20 FOR I = 1 TO 5
            30   SUM = SUM + I
            40 NEXT I
        """.trimIndent()

        val program = compiler.compile(code)
        val vars = mutableMapOf<String, BasicValue>()
        val vm = BasicVirtualMachine(
            onOutput = {},
            onStateChange = {},
            onVariablesChanged = { vars.putAll(it) },
            onIpChanged = { _, _ -> },
            onClearScreen = {},
            onBeep = {}
        )

        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(15.0, vars["SUM"]?.asNumber() ?: 0.0, 0.001)
    }

    @Test
    fun testGosubReturnSubroutine() = runBlocking {
        val code = """
            10 X = 5
            20 GOSUB 100
            30 PRINT "DONE"
            40 END
            100 X = X * 3
            110 RETURN
        """.trimIndent()

        val program = compiler.compile(code)
        val vars = mutableMapOf<String, BasicValue>()
        val vm = BasicVirtualMachine(
            onOutput = {},
            onStateChange = {},
            onVariablesChanged = { vars.putAll(it) },
            onIpChanged = { _, _ -> },
            onClearScreen = {},
            onBeep = {}
        )

        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(15.0, vars["X"]?.asNumber() ?: 0.0, 0.001)
    }
}
