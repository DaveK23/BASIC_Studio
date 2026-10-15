package com.example

import com.example.basic.docs.BasicDocHelper
import com.example.basic.docs.DocCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BasicDocumentationTest {

    @Test
    fun testResolveEntryAtCursorForStatements() {
        val code = "10 PRINT \"HELLO WORLD\""
        // Cursor on 'PRINT'
        val entry1 = BasicDocHelper.resolveEntryAtCursor(code, 3)
        assertNotNull(entry1)
        assertEquals("cmd_print", entry1?.anchor)
        assertEquals("PRINT", entry1?.keyword)

        // Cursor at end of 'PRINT'
        val entry2 = BasicDocHelper.resolveEntryAtCursor(code, 8)
        assertNotNull(entry2)
        assertEquals("cmd_print", entry2?.anchor)

        // Cursor on string literal shouldn't resolve keyword
        val entry3 = BasicDocHelper.resolveEntryAtCursor(code, 15)
        assertNull(entry3)
    }

    @Test
    fun testResolveEntryAtCursorForGraphicsAndAudio() {
        val code = "50 CIRCLE (160, 100), 45, 12, 1"
        val entryCircle = BasicDocHelper.resolveEntryAtCursor(code, 6)
        assertNotNull(entryCircle)
        assertEquals("cmd_circle", entryCircle?.anchor)
        assertEquals(DocCategory.GRAPHICS, entryCircle?.category)

        val soundCode = "SOUND 440, 200, 3"
        val entrySound = BasicDocHelper.resolveEntryAtCursor(soundCode, 2)
        assertNotNull(entrySound)
        assertEquals("cmd_sound", entrySound?.anchor)
        assertEquals(DocCategory.AUDIO, entrySound?.category)

        val sfxCode = "SFX 2"
        val entrySfx = BasicDocHelper.resolveEntryAtCursor(sfxCode, 1)
        assertNotNull(entrySfx)
        assertEquals("cmd_sfx", entrySfx?.anchor)
    }

    @Test
    fun testResolveEntryAtCursorForFunctions() {
        val code = "VAL = SIN(A) * SQR(B) + RND(10)"
        val entrySin = BasicDocHelper.resolveEntryAtCursor(code, 7)
        assertNotNull(entrySin)
        assertEquals("fn_sin", entrySin?.anchor)
        assertEquals("SIN", entrySin?.keyword)

        val entrySqr = BasicDocHelper.resolveEntryAtCursor(code, 16)
        assertNotNull(entrySqr)
        assertEquals("fn_sqr", entrySqr?.anchor)

        val entryRnd = BasicDocHelper.resolveEntryAtCursor(code, 25)
        assertNotNull(entryRnd)
        assertEquals("fn_rnd", entryRnd?.anchor)

        val strCode = "T$ = STR$(COUNT) + CHR$(65)"
        val entryStr = BasicDocHelper.resolveEntryAtCursor(strCode, 6)
        assertNotNull(entryStr)
        assertEquals("fn_str", entryStr?.anchor)

        val entryChr = BasicDocHelper.resolveEntryAtCursor(strCode, 20)
        assertNotNull(entryChr)
        assertEquals("fn_chr", entryChr?.anchor)
    }

    @Test
    fun testResolveEntryAtCursorForOperators() {
        val code = "IF A <> B AND C <= D THEN GOTO 100"
        val entryNeq = BasicDocHelper.resolveEntryAtCursor(code, 6)
        assertNotNull(entryNeq)
        assertEquals("op_neq", entryNeq?.anchor)

        val entryAnd = BasicDocHelper.resolveEntryAtCursor(code, 11)
        assertNotNull(entryAnd)
        assertEquals("op_and", entryAnd?.anchor)

        val entryLte = BasicDocHelper.resolveEntryAtCursor(code, 17)
        assertNotNull(entryLte)
        assertEquals("op_lte", entryLte?.anchor)

        val entryGoto = BasicDocHelper.resolveEntryAtCursor(code, 27)
        assertNotNull(entryGoto)
        assertEquals("cmd_goto", entryGoto?.anchor)
    }

    @Test
    fun testAllDocEntriesHaveAnchorsInHtmlFile() {
        val candidate1 = File("src/main/assets/docs/basic_reference.html")
        val candidate2 = File("app/src/main/assets/docs/basic_reference.html")
        val htmlFile = if (candidate1.exists()) candidate1 else candidate2
        assertTrue("HTML reference manual must exist", htmlFile.exists())
        val content = htmlFile.readText()

        assertTrue("HTML manual should have Table of Contents", content.contains("id=\"toc\""))
        assertTrue("HTML manual should have JavaScript filterDocs", content.contains("function filterDocs"))

        // Verify each documented entry has a corresponding id in the HTML
        for (entry in BasicDocHelper.ENTRIES) {
            val expectedId = "id=\"${entry.anchor}\""
            assertTrue(
                "HTML manual must contain anchor '$expectedId' for keyword '${entry.keyword}'",
                content.contains(expectedId)
            )
        }
    }
}
