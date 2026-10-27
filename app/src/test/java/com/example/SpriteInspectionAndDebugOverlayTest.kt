package com.example

import com.example.basic.compiler.BasicCompiler
import com.example.basic.graphics.Sprite
import com.example.basic.model.BasicValue
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.VmState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpriteInspectionAndDebugOverlayTest {

    private val compiler = BasicCompiler()

    @Test
    fun testGalaxyInvadersSpriteCoordinatesAndHitboxRanges() = runBlocking {
        val vm = BasicVirtualMachine()
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 1, 8, "0010010001011010011111101101101111111111010000101010010101000010"
            30 SPRITEDEF 2, 8, "0001100000111100000110000001100000011000000110000011110000011000"
            40 SPRITE 1, 148, 30, 1, 14
            50 SPRITE 2, 145, 35, 2, 12
            60 HIT = SPRITEHIT(1, 2)
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        val alien = vm.graphicsState.sprites[1]
        val laser = vm.graphicsState.sprites[2]

        assertNotNull(alien)
        assertNotNull(laser)

        // Alien (Sprite 1) coordinates & bounds
        assertEquals(148, alien?.x)
        assertEquals(30, alien?.y)
        assertEquals(8, alien?.width)
        assertEquals(8, alien?.height)
        assertEquals(1, alien?.patternDefId)
        assertTrue(alien?.visible == true)

        // Laser (Sprite 2) coordinates & bounds
        assertEquals(145, laser?.x)
        assertEquals(35, laser?.y)
        assertEquals(8, laser?.width)
        assertEquals(8, laser?.height)
        assertEquals(2, laser?.patternDefId)
        assertTrue(laser?.visible == true)

        // Verify bounding box collision ranges tested by SPRITEHIT
        // Alien: X in [148, 156), Y in [30, 38)
        // Laser: X in [145, 153), Y in [35, 43)
        // Overlap X: [148, 153) (span = 5 pixels)
        // Overlap Y: [35, 38) (span = 3 pixels)
        assertTrue(alien!!.hits(laser!!))
        assertTrue(laser.hits(alien))
        assertEquals(BasicValue.NumberVal(1.0), vm.variables["HIT"])
    }

    @Test
    fun testSpriteCollisionMaskSolidPixelsCount() = runBlocking {
        val vm = BasicVirtualMachine()
        // 8x8 pattern with 16 ones and 48 zeroes
        val pattern = "1100000000000000110000000000000011000000000000001100000000000000"
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 3, 8, "$pattern"
            30 SPRITE 3, 50, 60, 3, 5
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        val sprite = vm.graphicsState.sprites[3]
        assertNotNull(sprite)
        assertEquals(8, sprite?.width)
        assertEquals(8, sprite?.height)
        assertEquals(64, sprite?.pattern?.size)

        val solidPixels = sprite?.pattern?.count { it } ?: 0
        assertEquals(8, solidPixels)
        assertEquals(3, sprite?.patternDefId)
    }

    @Test
    fun testSpriteInspectorStateAvailableWhenPausedAtBreakpoint() = runBlocking {
        val vm = BasicVirtualMachine()
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "11111111"
            30 SPRITE 0, 100, 120, 0, 4
            40 LET FLAG = 1
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        // Set breakpoint on line 40 (editor line 4)
        vm.toggleBreakpoint(40)

        vm.execute(debug = true)

        // Paused at breakpoint before line 40
        assertEquals(VmState.PAUSED_DEBUG, vm.state)

        // Sprite 0 must already be registered and inspectable in graphicsState
        val sprite = vm.graphicsState.sprites[0]
        assertNotNull(sprite)
        assertEquals(100, sprite?.x)
        assertEquals(120, sprite?.y)
        assertEquals(8, sprite?.width)
        assertEquals(8, sprite?.height)
        assertEquals(true, sprite?.visible)
    }

    @Test
    fun testHiddenSpriteIgnoredBySpriteHit() = runBlocking {
        val vm = BasicVirtualMachine()
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "11111111"
            30 SPRITEDEF 1, 8, "11111111"
            40 SPRITE 0, 50, 50, 0, 1
            50 SPRITE 1, 52, 52, 1, 2
            60 HIT_BEFORE = SPRITEHIT(0, 1)
            70 SPRITESHOW 1, 0
            80 HIT_AFTER = SPRITEHIT(0, 1)
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(BasicValue.NumberVal(1.0), vm.variables["HIT_BEFORE"])
        assertEquals(BasicValue.NumberVal(0.0), vm.variables["HIT_AFTER"])

        val s1 = vm.graphicsState.sprites[1]
        assertNotNull(s1)
        assertFalse(s1!!.visible)
    }

    @Test
    fun testSpriteDefinitionsMapExposedOnVm() = runBlocking {
        val vm = BasicVirtualMachine()
        val code = """
            10 SPRITEDEF 5, 8, "10101010"
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        val def = vm.spriteDefinitionsMap[5]
        assertNotNull(def)
        assertEquals(8, def?.width)
        assertEquals(8, def?.height)
    }
}
