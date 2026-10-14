package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.BasicValue
import com.example.basic.ui.BasicViewModel
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.VmState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebuggerBreakpointTest {

    @Test
    fun testBreakpointBySourceLineNumber() = runBlocking {
        val code = """
            10 LET A = 10
            20 LET B = 20
            30 LET C = 30
        """.trimIndent()

        val compiler = BasicCompiler()
        val program = compiler.compile(code)
        var hitCount = 0
        var hitLine = -1

        val vm = BasicVirtualMachine(
            onBreakpointHit = { line ->
                hitCount++
                hitLine = line
            }
        )
        vm.loadProgram(program)
        // Line 2 in the editor is "20 LET B = 20"
        vm.toggleBreakpoint(2)

        // Run execution
        vm.execute()

        // Should pause at line 2 before executing LET B = 20
        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(1, hitCount)
        assertEquals(BasicValue.NumberVal(10.0), vm.variables["A"])
        assertEquals(null, vm.variables["B"]) // B should NOT have executed yet

        // Resume execution
        vm.execute()

        // Should now run to completion without getting stuck on line 2
        assertEquals(VmState.TERMINATED, vm.state)
        assertEquals(BasicValue.NumberVal(20.0), vm.variables["B"])
        assertEquals(BasicValue.NumberVal(30.0), vm.variables["C"])
    }

    @Test
    fun testBreakpointByBasicLineNumber() = runBlocking {
        val code = """
            10 LET X = 100
            20 LET Y = 200
            30 LET Z = 300
        """.trimIndent()

        val compiler = BasicCompiler()
        val program = compiler.compile(code)
        var hitCount = 0

        val vm = BasicVirtualMachine(
            onBreakpointHit = {
                hitCount++
            }
        )
        vm.loadProgram(program)
        // BASIC line number 20
        vm.toggleBreakpoint(20)

        vm.execute()

        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(1, hitCount)
        assertEquals(BasicValue.NumberVal(100.0), vm.variables["X"])
        assertEquals(null, vm.variables["Y"])

        // Resume to completion
        vm.execute()
        assertEquals(VmState.TERMINATED, vm.state)
        assertEquals(BasicValue.NumberVal(200.0), vm.variables["Y"])
        assertEquals(BasicValue.NumberVal(300.0), vm.variables["Z"])
    }

    @Test
    fun testBreakpointInLoopHitsEveryIteration() = runBlocking {
        val code = """
            10 LET SUM = 0
            20 FOR I = 1 TO 3
            30   LET SUM = SUM + I
            40 NEXT I
        """.trimIndent()

        val compiler = BasicCompiler()
        val program = compiler.compile(code)
        var hitCount = 0

        val vm = BasicVirtualMachine(
            onBreakpointHit = {
                hitCount++
            }
        )
        vm.loadProgram(program)
        // Breakpoint on line 3 (sourceLine 3 or basicLineNum 30)
        vm.toggleBreakpoint(3)

        // Iteration 1
        vm.execute()
        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(1, hitCount)
        assertEquals(BasicValue.NumberVal(1.0), vm.variables["I"])
        assertEquals(BasicValue.NumberVal(0.0), vm.variables["SUM"])

        // Iteration 2
        vm.execute()
        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(2, hitCount)
        assertEquals(BasicValue.NumberVal(2.0), vm.variables["I"])
        assertEquals(BasicValue.NumberVal(1.0), vm.variables["SUM"])

        // Iteration 3
        vm.execute()
        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(3, hitCount)
        assertEquals(BasicValue.NumberVal(3.0), vm.variables["I"])
        assertEquals(BasicValue.NumberVal(3.0), vm.variables["SUM"])

        // Completion
        vm.execute()
        assertEquals(VmState.TERMINATED, vm.state)
        assertEquals(3, hitCount)
        assertEquals(BasicValue.NumberVal(6.0), vm.variables["SUM"])
    }

    @Test
    fun testEnsureBytecodeCompiledDoesNotResetRunningVm() = runBlocking {
        val viewModel = BasicViewModel()
        viewModel.updateCode("""
            10 LET COUNT = 42
            20 SLEEP 100
        """.trimIndent())

        // Compile and step to execute first line
        assertTrue(viewModel.compileCurrentCode())
        assertTrue(viewModel.vm.stepLine())

        assertEquals(VmState.PAUSED_DEBUG, viewModel.vm.state)
        assertEquals(BasicValue.NumberVal(42.0), viewModel.vm.variables["COUNT"])
        val currentIp = viewModel.vm.ip
        assertTrue(currentIp > 0)

        // Switching to Bytecode view calls ensureBytecodeCompiled(resetVm = false)
        val success = viewModel.ensureBytecodeCompiled(resetVm = false)
        assertTrue(success)

        // VM state and instruction pointer must remain preserved!
        assertEquals(VmState.PAUSED_DEBUG, viewModel.vm.state)
        assertEquals(currentIp, viewModel.vm.ip)
        assertEquals(BasicValue.NumberVal(42.0), viewModel.vm.variables["COUNT"])
    }
}
