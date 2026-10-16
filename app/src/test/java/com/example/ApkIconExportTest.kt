package com.example

import com.example.basic.apk.RetroIconData
import com.example.basic.apk.RetroIconGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ApkIconExportTest {

    @Test
    fun testAdaptiveIconVectorsExistForAllEightChoices() {
        assertEquals(8, RetroIconGenerator.ICONS.size)

        val foregrounds = mutableListOf<ByteArray>()
        val backgrounds = mutableListOf<ByteArray>()

        for (i in 0 until 8) {
            val fg = RetroIconGenerator.getAdaptiveForegroundBytes(null, i)
            val bg = RetroIconGenerator.getAdaptiveBackgroundBytes(null, i)

            assertTrue("Foreground for icon $i must not be empty", fg.isNotEmpty())
            assertTrue("Background for icon $i must not be empty", bg.isNotEmpty())

            // Verify RES_XML_TYPE header (0x0003 in little endian)
            val fgBuf = ByteBuffer.wrap(fg).order(ByteOrder.LITTLE_ENDIAN)
            val fgType = fgBuf.short.toInt() and 0xFFFF
            assertEquals("Foreground $i must be compiled Android binary XML (0x0003)", 0x0003, fgType)

            val bgBuf = ByteBuffer.wrap(bg).order(ByteOrder.LITTLE_ENDIAN)
            val bgType = bgBuf.short.toInt() and 0xFFFF
            assertEquals("Background $i must be compiled Android binary XML (0x0003)", 0x0003, bgType)

            foregrounds.add(fg)
            backgrounds.add(bg)
        }

        // Verify that different icon choices produce distinct binary vectors
        assertNotEquals("Icon 0 and 1 must have distinct foregrounds", foregrounds[0].size, foregrounds[1].size)
        assertNotEquals("Icon 1 and 4 must have distinct foregrounds", foregrounds[1].size, foregrounds[4].size)
        assertNotEquals("Icon 2 and 3 must have distinct foregrounds", foregrounds[2].size, foregrounds[3].size)
    }

    @Test
    fun testRetroIconDataDirectAccess() {
        for (i in 0 until 8) {
            val fg = RetroIconData.getForegroundBytes(i)
            val bg = RetroIconData.getBackgroundBytes(i)
            assertTrue("FG $i must be > 500 bytes", fg.size > 500)
            assertTrue("BG $i must be > 500 bytes", bg.size > 500)
        }
    }

    @Test
    fun testRunnerBaseApkBundlesUpdatedTerminalAndGraphics() {
        val candidateFiles = listOf(
            java.io.File("src/main/assets/runner-base.apk"),
            java.io.File("app/src/main/assets/runner-base.apk")
        )
        val assetApk = candidateFiles.firstOrNull { it.exists() }
        assertTrue("runner-base.apk must exist in assets", assetApk != null && assetApk.exists())

        var foundAppendChunk = false
        var foundRetroGraphics = false
        var foundRetroController = false

        java.util.zip.ZipFile(assetApk!!).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".dex")) {
                    val bytes = zip.getInputStream(entry).readBytes()
                    val text = String(bytes, Charsets.ISO_8859_1)
                    if (text.contains("appendChunk")) foundAppendChunk = true
                    if (text.contains("RetroGraphicsCanvas")) foundRetroGraphics = true
                    if (text.contains("RetroControllerOverlay")) foundRetroController = true
                }
            }
        }

        assertTrue("runner-base.apk must contain appendChunk for PRINT_SEMI inline concatenation", foundAppendChunk)
        assertTrue("runner-base.apk must contain RetroGraphicsCanvas for graphics screen mode", foundRetroGraphics)
        assertTrue("runner-base.apk must contain RetroControllerOverlay for on-screen joystick and fire button", foundRetroController)
    }
}
