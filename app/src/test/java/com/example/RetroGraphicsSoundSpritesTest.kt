package com.example

import com.example.basic.apk.RetroIconGenerator
import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.BasicValue
import com.example.basic.model.OpCode
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.VmState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            15 SPRITEDEFMULTI 1, 8, "01234567", 0
            20 SPRITE 0, 100, 80, 0, 4
            30 SPRITEMOVE 0, 5, -2
            35 SPRITEPOS 0, 120, 140
            40 SPRITESHOW 0, 1
            50 HIT = SPRITEHIT(0, 1)
        """.trimIndent()
        val program = compiler.compile(code)
        val ops = program.instructions.map { it.op }

        assertTrue(ops.contains(OpCode.SPRITEDEF))
        assertTrue(ops.contains(OpCode.SPRITEDEFMULTI))
        assertTrue(ops.contains(OpCode.SPRITE))
        assertTrue(ops.contains(OpCode.SPRITEMOVE))
        assertTrue(ops.contains(OpCode.SPRITEPOS))
        assertTrue(ops.contains(OpCode.SPRITESHOW))
        assertTrue(ops.contains(OpCode.SPRITEHIT))
    }

    @Test
    fun testVmExecutesSpritePos() = runBlocking {
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "11111111"
            30 SPRITE 0, 10, 20, 0, 4
            40 SPRITEPOS 0, 150, 75
        """.trimIndent()
        val program = compiler.compile(code)
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null

        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        vm.loadProgram(program)
        vm.execute(debug = false)

        val sprite = lastGState?.sprites?.get(0)
        assertNotNull(sprite)
        assertEquals(150, sprite?.x)
        assertEquals(75, sprite?.y)
        assertEquals(4, sprite?.colorIndex)
        assertEquals(true, sprite?.visible)
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

    @Test
    fun testAllThirteenSfxPresetsConfigured() {
        assertEquals(13, com.example.basic.audio.RetroSoundSynth.PRESETS.size)
        val ids = com.example.basic.audio.RetroSoundSynth.PRESETS.map { it.id }
        for (i in 0..12) {
            assertTrue("Preset $i must be registered", ids.contains(i))
        }
    }

    @Test
    fun testVmExecutesExpandedSfxPresets() = runBlocking {
        val sfxPlayed = mutableListOf<Int>()
        val vm = BasicVirtualMachine(
            onSfx = { sfxPlayed.add(it) }
        )
        val code = """
            10 SFX 0
            20 SFX 5
            30 SFX 7
            40 SFX 9
            50 SFX 12
        """.trimIndent()
        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(listOf(0, 5, 7, 9, 12), sfxPlayed)
    }

    @Test
    fun testCompilerGeneratesJoystickOpCodeAndFunctions() {
        val code = """
            10 SCREEN 1
            20 JOYSTICK 1
            30 D = JOY()
            40 X = JOY(1)
            50 Y = JOY(2)
            60 F = FIRE()
            70 JOYSTICK 0
        """.trimIndent()
        val program = compiler.compile(code)
        val ops = program.instructions.map { it.op }
        assertTrue(ops.contains(OpCode.JOYSTICK))
    }

    @Test
    fun testVmExecutesJoystickAndFireFunctions() = runBlocking {
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null
        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        val code = """
            10 SCREEN 1
            20 JOYSTICK 1
            30 D = JOY()
            40 X = JOY(1)
            50 Y = JOY(2)
            60 F = FIRE()
        """.trimIndent()
        val program = compiler.compile(code)
        vm.loadProgram(program)

        // Set joystick state in VM before executing
        vm.updateJoystick(direction = 3, axisX = 1, axisY = 0)
        vm.updateFireButton(pressed = true)

        vm.execute(debug = false)

        assertNotNull(lastGState)
        assertEquals(true, lastGState?.showJoystick)
        assertEquals(3.0, vm.variables["D"]?.asNumber() ?: 0.0, 0.001)
        assertEquals(1.0, vm.variables["X"]?.asNumber() ?: 0.0, 0.001)
        assertEquals(0.0, vm.variables["Y"]?.asNumber() ?: 0.0, 0.001)
        assertEquals(1.0, vm.variables["F"]?.asNumber() ?: 0.0, 0.001)
    }

    @Test
    fun testSpriteShapesDecodedCorrectlyForVariousFormats() = runBlocking {
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null
        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        val code = """
            10 SCREEN 1
            20 REM Pattern 0: 64-char binary string
            30 SPRITEDEF 0, 8, "0010010001011010011111101101101111111111010000101010010101000010"
            40 REM Pattern 1: ASCII visual pattern with # and .
            50 SPRITEDEF 1, 8, "..####....####..##....####....####....####....##"
            60 REM Pattern 2: Hexadecimal string (16 hex digits = 64 bits)
            70 SPRITEDEF 2, 8, "3C7EFFFFDBFF24A5"
            80 REM Pattern 3: Comma-separated decimal row bytes
            90 SPRITEDEF 3, 8, "60, 126, 255, 219, 255, 36, 165, 0"
            100 REM Spawn sprites using the defined patterns
            110 SPRITE 0, 10, 10, 0, 1
            120 SPRITE 1, 30, 10, 1, 2
            130 SPRITE 2, 50, 10, 2, 3
            140 SPRITE 3, 70, 10, 3, 4
            150 REM Sprite 4 reuses pattern 0 with different color and position
            160 SPRITE 4, 90, 10, 0, 5
        """.trimIndent()
        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertNotNull(lastGState)
        assertEquals(5, lastGState?.sprites?.size)

        // Verify Sprite 0 (64-char binary)
        val s0 = lastGState?.sprites?.get(0)
        assertNotNull(s0)
        assertEquals(64, s0?.pattern?.size)
        // Row 0 is "00100100" -> bits 2 and 5 are true
        assertEquals(false, s0?.pattern?.get(0))
        assertEquals(false, s0?.pattern?.get(1))
        assertEquals(true, s0?.pattern?.get(2))
        assertEquals(false, s0?.pattern?.get(3))
        assertEquals(false, s0?.pattern?.get(4))
        assertEquals(true, s0?.pattern?.get(5))
        assertEquals(false, s0?.pattern?.get(6))
        assertEquals(false, s0?.pattern?.get(7))

        // Verify Sprite 1 (ASCII with # and .)
        val s1 = lastGState?.sprites?.get(1)
        assertNotNull(s1)
        assertEquals(64, s1?.pattern?.size)
        // "..####.." -> bits 0,1 are false, 2,3,4,5 are true, 6,7 are false
        assertEquals(false, s1?.pattern?.get(0))
        assertEquals(false, s1?.pattern?.get(1))
        assertEquals(true, s1?.pattern?.get(2))
        assertEquals(true, s1?.pattern?.get(3))
        assertEquals(true, s1?.pattern?.get(4))
        assertEquals(true, s1?.pattern?.get(5))
        assertEquals(false, s1?.pattern?.get(6))
        assertEquals(false, s1?.pattern?.get(7))

        // Verify Sprite 2 (Hex: 3C = 00111100, 7E = 01111110)
        val s2 = lastGState?.sprites?.get(2)
        assertNotNull(s2)
        assertEquals(64, s2?.pattern?.size)
        // 0x3C = 00111100
        assertEquals(false, s2?.pattern?.get(0))
        assertEquals(false, s2?.pattern?.get(1))
        assertEquals(true, s2?.pattern?.get(2))
        assertEquals(true, s2?.pattern?.get(3))
        assertEquals(true, s2?.pattern?.get(4))
        assertEquals(true, s2?.pattern?.get(5))
        assertEquals(false, s2?.pattern?.get(6))
        assertEquals(false, s2?.pattern?.get(7))

        // Verify Sprite 3 (Comma bytes: 60 = 0x3C = 00111100)
        val s3 = lastGState?.sprites?.get(3)
        assertNotNull(s3)
        assertEquals(64, s3?.pattern?.size)
        assertEquals(false, s3?.pattern?.get(0))
        assertEquals(false, s3?.pattern?.get(1))
        assertEquals(true, s3?.pattern?.get(2))
        assertEquals(true, s3?.pattern?.get(3))
        assertEquals(true, s3?.pattern?.get(4))
        assertEquals(true, s3?.pattern?.get(5))
        assertEquals(false, s3?.pattern?.get(6))
        assertEquals(false, s3?.pattern?.get(7))

        // Verify Sprite 4 correctly reused Pattern 0
        val s4 = lastGState?.sprites?.get(4)
        assertNotNull(s4)
        assertEquals(s0?.pattern, s4?.pattern)
        assertEquals(5, s4?.colorIndex)
    }

    @Test
    fun testVmExecutesSpriteDefMultiWithMask() = runBlocking {
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null
        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        // 8x8 mask: alternate 4 solid and 4 transparent in each row
        // 8x8 hex: 64 hex characters (0..F repeated 4 times)
        val maskPattern = "11110000".repeat(8)
        val hexPattern = "0123456789ABCDEF".repeat(4)

        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "$maskPattern"
            30 SPRITEDEFMULTI 1, 8, "$hexPattern", 0
            40 SPRITE 1, 40, 50, 1, 15
            50 SPRITEDEFMULTI 2, "$hexPattern", 0
            60 SPRITE 2, 70, 80, 2, 1
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(VmState.TERMINATED, vm.state)
        val s1 = lastGState?.sprites?.get(1)
        assertNotNull(s1)
        assertEquals(40, s1?.x)
        assertEquals(50, s1?.y)
        assertEquals(64, s1?.pattern?.size)
        // Check mask applied correctly
        assertEquals(true, s1?.pattern?.get(0))
        assertEquals(false, s1?.pattern?.get(4))
        // Check colorPattern attached correctly
        assertNotNull(s1?.colorPattern)
        assertEquals(64, s1?.colorPattern?.size)
        assertEquals(0, s1?.colorPattern?.get(0))
        assertEquals(1, s1?.colorPattern?.get(1))
        assertEquals(15, s1?.colorPattern?.get(15))

        // Check 3-argument version (Sprite 2)
        val s2 = lastGState?.sprites?.get(2)
        assertNotNull(s2)
        assertEquals(70, s2?.x)
        assertEquals(80, s2?.y)
        assertEquals(s1?.pattern, s2?.pattern)
        assertEquals(s1?.colorPattern, s2?.colorPattern)
    }

    @Test
    fun testSpriteDefMultiDimensionMismatchCausesError() = runBlocking {
        val vm = BasicVirtualMachine()
        // Mask is 8x8 (64 pixels), but hexPattern only has 16 characters
        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "1111000011110000111100001111000000001111000011110000111100001111"
            30 SPRITEDEFMULTI 1, 8, "0123456789ABCDEF", 0
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(VmState.ERROR, vm.state)
        assertNotNull(vm.lastError)
        assertTrue(
            "Error message should mention dimension mismatch: ${vm.lastError}",
            vm.lastError!!.contains("dimension", ignoreCase = true)
        )
    }

    @Test
    fun testSpriteDefMultiMissingMaskCausesError() = runBlocking {
        val vm = BasicVirtualMachine()
        val hexPattern = "0123456789ABCDEF".repeat(4)
        val code = """
            10 SCREEN 1
            20 SPRITEDEFMULTI 1, 8, "$hexPattern", 9
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertEquals(VmState.ERROR, vm.state)
        assertNotNull(vm.lastError)
        assertTrue(
            "Error message should mention undefined mask: ${vm.lastError}",
            vm.lastError!!.contains("mask", ignoreCase = true)
        )
    }

    @Test
    fun testSpriteDefMultiSharesIdWithMonochromeSprites() = runBlocking {
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null
        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        val maskPattern = "11111111".repeat(8)
        val hexPattern = "0123456789ABCDEF".repeat(4)

        val code = """
            10 SCREEN 1
            20 SPRITEDEF 0, 8, "$maskPattern"
            30 SPRITE 0, 10, 10, 0, 4
            40 SPRITEDEFMULTI 0, 8, "$hexPattern", 0
            50 SPRITE 0, 10, 10, 0, 4
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        val s0 = lastGState?.sprites?.get(0)
        assertNotNull(s0)
        assertNotNull(s0?.colorPattern)
        assertEquals(64, s0?.colorPattern?.size)

        // Now redefine back to monochrome with SPRITEDEF 0
        val code2 = """
            10 SPRITEDEF 0, 8, "$maskPattern"
            20 SPRITE 0, 10, 10, 0, 4
        """.trimIndent()
        val program2 = compiler.compile(code2)
        vm.loadProgram(program2)
        vm.execute(debug = false)

        val s0Mono = lastGState?.sprites?.get(0)
        assertNotNull(s0Mono)
        assertNull(s0Mono?.colorPattern)
    }

    @Test
    fun testBitmapDefinitionsDecoupledFromActiveSpriteIds() = runBlocking {
        var lastGState: com.example.basic.graphics.RetroGraphicsState? = null
        val vm = BasicVirtualMachine(
            onGraphicsStateChanged = { lastGState = it }
        )
        val maskPattern = "11110000".repeat(8)
        val hexPattern1 = "1111222233334444".repeat(4)
        val hexPattern2 = "5555666677778888".repeat(4)

        val code = """
            10 SCREEN 1
            20 REM Define arbitrary high bitmap IDs (50 and 100)
            30 SPRITEDEF 50, 8, "$maskPattern"
            40 SPRITEDEFMULTI 100, 8, "$hexPattern1", 50
            50 REM No active sprites placed yet
            60 REM Now assign bitmaps to active sprite slots 0, 1, and 2
            70 SPRITE 0, 10, 20, 50, 2
            80 SPRITE 1, 30, 40, 100, 15
            90 SPRITE 2, 50, 60, 100, 14
            100 REM Dynamically redefine bitmap 100
            110 SPRITEDEFMULTI 100, 8, "$hexPattern2", 50
        """.trimIndent()

        val program = compiler.compile(code)
        vm.loadProgram(program)
        vm.execute(debug = false)

        assertNotNull(lastGState)
        assertEquals(3, lastGState?.sprites?.size)

        // Active sprite 0 uses bitmap 50
        val s0 = lastGState?.sprites?.get(0)
        assertNotNull(s0)
        assertEquals(50, s0?.patternDefId)
        assertEquals(2, s0?.colorIndex)
        assertNull(s0?.colorPattern)

        // Active sprite 1 and sprite 2 both share bitmap 100, and received the updated pattern from line 110
        val s1 = lastGState?.sprites?.get(1)
        val s2 = lastGState?.sprites?.get(2)
        assertNotNull(s1)
        assertNotNull(s2)
        assertEquals(100, s1?.patternDefId)
        assertEquals(100, s2?.patternDefId)
        assertNotNull(s1?.colorPattern)
        assertNotNull(s2?.colorPattern)
        assertEquals(5, s1?.colorPattern?.get(0))
        assertEquals(5, s2?.colorPattern?.get(0))
    }
}

