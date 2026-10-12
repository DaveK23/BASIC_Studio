package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.BasicValue
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.VmState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionControlTest {

    @Test
    fun testStepBySourceLine() = runBlocking {
        val code = """
            10 LET A = 1
            20 LET B = 2
            30 LET C = A + B
        """.trimIndent()

        val compiler = BasicCompiler()
        val program = compiler.compile(code)
        assertTrue(program.instructions.isNotEmpty())

        val vm = BasicVirtualMachine()
        vm.loadProgram(program)

        assertEquals(VmState.IDLE, vm.state)

        // Step 1: line 10 should execute
        val step1 = vm.stepLine()
        assertTrue(step1)
        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(BasicValue.NumberVal(1.0), vm.variables["A"])

        // Step 2: line 20 should execute
        val step2 = vm.stepLine()
        assertTrue(step2)
        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        assertEquals(BasicValue.NumberVal(2.0), vm.variables["B"])

        // Step 3: line 30 should execute
        val step3 = vm.stepLine()
        assertEquals(BasicValue.NumberVal(3.0), vm.variables["C"])
    }

    @Test
    fun testPauseAndResume() = runBlocking {
        val code = """
            10 LET X = 0
            20 FOR I = 1 TO 1000
            30   LET X = X + 1
            40 NEXT I
        """.trimIndent()

        val compiler = BasicCompiler()
        val program = compiler.compile(code)
        assertTrue(program.instructions.isNotEmpty())

        val vm = BasicVirtualMachine()
        vm.loadProgram(program)

        val job = launch(Dispatchers.Default) {
            vm.execute(debug = true)
        }

        // Wait a brief moment for loop to start running
        delay(10)
        vm.pause()
        job.join()

        assertEquals(VmState.PAUSED_DEBUG, vm.state)
        val pausedX = (vm.variables["X"] as? BasicValue.NumberVal)?.value ?: 0.0
        assertTrue("X should have made progress before pause", pausedX > 0.0)

        // Now resume execution to completion
        vm.execute(debug = true)
        assertEquals(VmState.TERMINATED, vm.state)
        assertEquals(BasicValue.NumberVal(1000.0), vm.variables["X"])
    }

    @Test
    fun testStopExecution() = runBlocking {
        val code = """
            10 LET COUNT = 0
            20 LET COUNT = COUNT + 1
            30 GOTO 20
        """.trimIndent()

        val compiler = BasicCompiler()
        val program = compiler.compile(code)
        assertTrue(program.instructions.isNotEmpty())

        val vm = BasicVirtualMachine()
        vm.loadProgram(program)

        val job = launch(Dispatchers.Default) {
            vm.execute(debug = false)
        }

        delay(15)
        vm.stop()
        job.join()

        assertEquals(VmState.TERMINATED, vm.state)
    }
}
