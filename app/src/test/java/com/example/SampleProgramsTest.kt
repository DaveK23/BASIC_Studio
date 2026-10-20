package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.editor.SamplePrograms
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleProgramsTest {

    private val compiler = BasicCompiler()

    @Test
    fun testAllSampleProgramsCompileSuccessfully() {
        assertTrue("Sample programs list should not be empty", SamplePrograms.SAMPLES.isNotEmpty())

        for (sample in SamplePrograms.SAMPLES) {
            val program = try {
                compiler.compile(sample.code)
            } catch (e: Exception) {
                throw AssertionError("Failed to compile sample '${sample.title}': ${e.message}", e)
            }
            assertNotNull("Compiled bytecode must not be null for '${sample.title}'", program)
            assertTrue(
                "Compiled instructions should not be empty for '${sample.title}'",
                program.instructions.isNotEmpty()
            )
        }
    }

    @Test
    fun testNoSampleProgramContainsIfThenFollowedByColon() {
        // In this BASIC compiler, an IF ... THEN statement only governs a single statement.
        // Any statements separated by colons ':' on the same line execute unconditionally.
        // Therefore, sample programs must NOT place colon-separated statements after THEN.
        val regex = Regex("""(?i)\bIF\b.*\bTHEN\b.*:""")
        for (sample in SamplePrograms.SAMPLES) {
            val lines = sample.code.lines()
            for ((index, line) in lines.withIndex()) {
                val hasIfThenColon = regex.containsMatchIn(line)
                assertTrue(
                    "Sample '${sample.title}' line ${index + 1} contains 'IF ... THEN ... :' which executes trailing statements unconditionally: $line",
                    !hasIfThenColon
                )
            }
        }
    }

    @Test
    fun testGalaxyInvadersTorpedoLifecycle() = runBlocking {
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null
        val vm = com.example.basic.vm.BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        val testProgram = """
            10 SCREEN 1
            20 PX = 150 : PY = 175
            30 GOSUB 500
            40 REM Initial launch check
            50 L1 = LACTIVE
            60 REM Advance torpedo while on screen (LY was 165, becomes 155)
            70 GOSUB 540
            80 L2 = LACTIVE
            90 REM Simulate torpedo near top
            100 LY = 15
            110 GOSUB 540
            120 L3 = LACTIVE
            130 END
            500 REM --- LAUNCH TORPEDO ---
            510 LACTIVE = 1
            520 LX = PX : LY = PY - 10
            530 SPRITE 2, LX, LY, 2, 12
            535 SPRITESHOW 2, 1
            538 SFX 0
            539 RETURN
            540 REM --- ADVANCE TORPEDO ---
            550 LY = LY - 10
            560 SPRITEMOVE 2, 0, -10
            570 IF LY >= 10 THEN RETURN
            572 LACTIVE = 0
            574 SPRITESHOW 2, 0
            575 RETURN
        """.trimIndent()

        val bytecode = compiler.compile(testProgram)
        vm.loadProgram(bytecode)
        vm.execute(debug = false)

        // After launch (line 50), LACTIVE should be 1
        assertEquals(1.0, vm.variables["L1"]?.asNumber() ?: 0.0, 0.001)
        // After advancing once (line 70), LY is 155 >= 10, so LACTIVE should remain 1
        assertEquals(1.0, vm.variables["L2"]?.asNumber() ?: 0.0, 0.001)
        // After advancing from LY = 15 (line 110), LY becomes 5 < 10, so LACTIVE should be 0
        assertEquals(0.0, vm.variables["L3"]?.asNumber() ?: 1.0, 0.001)
        // Torpedo sprite (id 2) should now be invisible
        val sprite2 = lastGState?.sprites?.get(2)
        assertNotNull(sprite2)
        assertEquals(false, sprite2?.visible)
    }

    @Test
    fun testLunarLanderHorizontalVelocityPreservedWithinBounds() = runBlocking {
        val vm = com.example.basic.vm.BasicVirtualMachine()
        val testProgram = """
            330 LX = 150 : VX = 2.5
            345 IF LX >= 5 THEN GOTO 355
            350 LX = 5 : VX = 0
            355 IF LX <= 305 THEN GOTO 365
            360 LX = 305 : VX = 0
            365 FINAL_VX = VX
            370 END
        """.trimIndent()

        val bytecode = compiler.compile(testProgram)
        vm.loadProgram(bytecode)
        vm.execute(debug = false)

        assertEquals(2.5, vm.variables["FINAL_VX"]?.asNumber() ?: 0.0, 0.001)
    }
}
