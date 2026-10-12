package com.example

import com.example.basic.apk.RetroIconGenerator
import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.BasicValue
import com.example.basic.model.OpCode
import com.example.basic.vm.BasicVirtualMachine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroGraphicsSoundSpritesTest {

    private val compiler = BasicCompiler()

    @Test
    fun testCompilerGeneratesGraphicsAndSoundOpCodes() {
        val code = """
            10 SCREEN 1
            20 CLS
            30 COLOR 14
            40 PSET (10, 20), 4
            50 LINE (0, 0)-(100, 100), 2
            60 RECT (10, 10)-(50, 50), 3, 1
            70 CIRCLE (60, 60), 15, 5, 0
            80 SOUND 440, 200, 0
            90 SFX 3
        """.trimIndent()
        val program = compiler.compile(code)
        val ops = program.instructions.map { it.op }

        assertTrue(ops.contains(OpCode.SCREEN))
        assertTrue(ops.contains(OpCode.CLS))
        assertTrue(ops.contains(OpCode.COLOR))
        assertTrue(ops.contains(OpCode.PSET))
        assertTrue(ops.contains(OpCode.DRAW_LINE))
        assertTrue(ops.contains(OpCode.DRAW_RECT))
        assertTrue(ops.contains(OpCode.DRAW_CIRCLE))
        assertTrue(ops.contains(OpCode.SOUND))
        assertTrue(ops.contains(OpCode.SFX))
    }

    @Test
    fun testCompilerGeneratesSpriteOpCodes() {
        val code = """
            10 SPRITEDEF 0, 8, "01010101"
            20 SPRITE 0, 100, 80, 0, 4
            30 SPRITEMOVE 0, 5, -2
            40 SPRITESHOW 0, 1
            50 HIT = SPRITEHIT(0, 1)
        """.trimIndent()
        val program = compiler.compile(code)
        val ops = program.instructions.map { it.op }

        assertTrue(ops.contains(OpCode.SPRITEDEF))
        assertTrue(ops.contains(OpCode.SPRITE))
        assertTrue(ops.contains(OpCode.SPRITEMOVE))
        assertTrue(ops.contains(OpCode.SPRITESHOW))
        assertTrue(ops.contains(OpCode.SPRITEHIT))
    }

    @Test
    fun testVmExecutesGraphicsAndSpriteCollisions() = runBlocking {
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "11111111"
            30 SPRITEDEF 1, 8, "11111111"
            40 SPRITE 0, 10, 10, 0, 1
            50 SPRITE 1, 14, 14, 0, 2
            60 HIT1 = SPRITEHIT(0, 1)
            70 SPRITEMOVE 1, 50, 50
            80 HIT2 = SPRITEHIT(0, 1)
        """.trimIndent()
        val program = compiler.compile(code)
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null

        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertNotNull(lastGState)
        assertEquals(1, lastGState?.mode)
        assertEquals(2, lastGState?.sprites?.size)

        // Overlapping at (10,10) and (14,14) with 8x8 size -> Collision True
        val hit1 = vm.variables["HIT1"]
        val hit2 = vm.variables["HIT2"]

        assertEquals(true, hit1?.toBoolean())
        assertEquals(false, hit2?.toBoolean())
    }

    @Test
    fun testRetroIconMetadataHasEightOptions() {
        assertEquals(8, RetroIconGenerator.ICONS.size)
        val names = RetroIconGenerator.ICONS.map { it.name }
        assertTrue(names.contains("Retro Microcomputer"))
        assertTrue(names.contains("BASIC 8-Bit Prompt"))
        assertTrue(names.contains("3.5\" Floppy Disk"))
        assertTrue(names.contains("Silicon Microchip"))
        assertTrue(names.contains("Space Invader"))
        assertTrue(names.contains("Arcade Joystick"))
        assertTrue(names.contains("Pixel Diamond"))
        assertTrue(names.contains("Cassette Tape"))
    }
}
