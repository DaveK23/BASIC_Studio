package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.OpCode
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.TerminalLine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicPrintConcatenationTest {

    private val compiler = BasicCompiler()

    @Test
    fun testCompilerSuppressesNewlineForSemicolon() {
        val code = """
            10 PRINT "A";
            20 PRINT "B";
            30 PRINT "C"
        """.trimIndent()
        val program = compiler.compile(code)

        val nonHaltOpcodes = program.instructions.filter { it.op != OpCode.HALT }.map { it.op }
        // Line 10 should emit PRINT_SEMI, not PRINT_NEWLINE
        assertTrue("Expected PRINT_SEMI in bytecode", nonHaltOpcodes.contains(OpCode.PRINT_SEMI))
        // Line 30 should end with PRINT (standard print with newline)
        assertEquals(OpCode.PRINT, nonHaltOpcodes.last())
    }

    @Test
    fun testCompilerHandlesLoneSemicolonPrint() {
        val code = """
            10 PRINT ;
            20 PRINT "HELLO"
        """.trimIndent()
        val program = compiler.compile(code)
        // Should compile successfully without throwing CompilerException
        assertTrue(program.instructions.isNotEmpty())
    }

    @Test
    fun testMultiplePrintStatementsConcatenateOnSingleLine() = runBlocking {
        val code = """
            10 PRINT "A";
            20 PRINT "B";
            30 PRINT "C"
        """.trimIndent()
        val program = compiler.compile(code)

        val lines = mutableListOf<TerminalLine>()
        val vm = BasicVirtualMachine(
            onOutput = { chunk ->
                val updated = TerminalLine.appendChunk(lines, chunk)
                lines.clear()
                lines.addAll(updated)
            }
        )
        vm.loadProgram(program)
        vm.execute(debug = false)

        // Find the line containing the output
        val outputLines = lines.filter { !it.isSystemNotice }
        assertEquals(1, outputLines.size)
        assertEquals("ABC", outputLines[0].text)
    }

    @Test
    fun testPrintLoopConcatenation() = runBlocking {
        val code = """
            10 FOR I = 1 TO 5
            20   PRINT I;
            30 NEXT I
        """.trimIndent()
        val program = compiler.compile(code)

        val lines = mutableListOf<TerminalLine>()
        val vm = BasicVirtualMachine(
            onOutput = { chunk ->
                val updated = TerminalLine.appendChunk(lines, chunk)
                lines.clear()
                lines.addAll(updated)
            }
        )
        vm.loadProgram(program)
        vm.execute(debug = false)

        val outputLines = lines.filter { !it.isSystemNotice }
        assertEquals(1, outputLines.size)
        assertEquals("12345", outputLines[0].text)
    }

    @Test
    fun testPrintWithoutSemicolonCreatesNewLines() = runBlocking {
        val code = """
            10 PRINT "LINE 1"
            20 PRINT "LINE 2"
        """.trimIndent()
        val program = compiler.compile(code)

        val lines = mutableListOf<TerminalLine>()
        val vm = BasicVirtualMachine(
            onOutput = { chunk ->
                val updated = TerminalLine.appendChunk(lines, chunk)
                lines.clear()
                lines.addAll(updated)
            }
        )
        vm.loadProgram(program)
        vm.execute(debug = false)

        val outputLines = lines.filter { !it.isSystemNotice }
        assertEquals(2, outputLines.size)
        assertEquals("LINE 1", outputLines[0].text)
        assertEquals("LINE 2", outputLines[1].text)
    }

    @Test
    fun testSemicolonFollowedByEmptyPrintNewline() = runBlocking {
        val code = """
            10 PRINT "PART 1";
            20 PRINT " PART 2";
            30 PRINT
            40 PRINT "PART 3"
        """.trimIndent()
        val program = compiler.compile(code)

        val lines = mutableListOf<TerminalLine>()
        val vm = BasicVirtualMachine(
            onOutput = { chunk ->
                val updated = TerminalLine.appendChunk(lines, chunk)
                lines.clear()
                lines.addAll(updated)
            }
        )
        vm.loadProgram(program)
        vm.execute(debug = false)

        val outputLines = lines.filter { !it.isSystemNotice }
        assertEquals(2, outputLines.size)
        assertEquals("PART 1 PART 2", outputLines[0].text)
        assertEquals("PART 3", outputLines[1].text)
    }
}
