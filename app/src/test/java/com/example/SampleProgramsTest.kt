package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.editor.SamplePrograms
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
}
