package com.example

import com.example.basic.file.BasicFileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BasicFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSanitizeFileName() {
        assertEquals("game.bas", BasicFileManager.sanitizeFileName("game"))
        assertEquals("game.bas", BasicFileManager.sanitizeFileName("game.bas"))
        assertEquals("script.txt", BasicFileManager.sanitizeFileName("script.txt"))
        assertEquals("my_app.bas", BasicFileManager.sanitizeFileName("my/app"))
        assertEquals("program.bas", BasicFileManager.sanitizeFileName("   "))
        assertEquals("cool_project.bas", BasicFileManager.sanitizeFileName("cool?project*:<>|"))
    }

    @Test
    fun testSaveAndLoadProgramFile() {
        val baseDir = tempFolder.newFolder("programs_test")
        val sampleCode = """
            10 REM Lunar Lander
            20 PRINT "THRUST: ";
            30 INPUT T
            40 END
        """.trimIndent()

        val savedInfo = BasicFileManager.saveProgram(baseDir, "lander.bas", sampleCode)
        assertNotNull(savedInfo)
        assertEquals("lander.bas", savedInfo.name)
        assertTrue("Saved file must exist", savedInfo.file.exists())
        assertTrue("Size must be positive", savedInfo.sizeBytes > 0)

        // Read back
        val loadedCode = BasicFileManager.loadProgram(savedInfo.file)
        assertEquals(sampleCode, loadedCode)
    }

    @Test
    fun testListSavedProgramsSorted() {
        val baseDir = tempFolder.newFolder("programs_sort_test")
        val code1 = "10 PRINT 1"
        val code2 = "20 PRINT 2"

        val f1 = BasicFileManager.saveProgram(baseDir, "first.bas", code1)
        Thread.sleep(50) // slight delay for timestamp ordering
        val f2 = BasicFileManager.saveProgram(baseDir, "second.bas", code2)

        val list = BasicFileManager.listSavedPrograms(baseDir)
        assertEquals(2, list.size)
        // Most recently modified first
        assertEquals("second.bas", list[0].name)
        assertEquals("first.bas", list[1].name)
    }

    @Test
    fun testDeleteProgramFile() {
        val baseDir = tempFolder.newFolder("programs_delete_test")
        val code = "10 PRINT \"DELETE ME\""
        val saved = BasicFileManager.saveProgram(baseDir, "temp.bas", code)
        assertTrue(saved.file.exists())

        val deleted = BasicFileManager.deleteProgram(saved.file)
        assertTrue("Delete must return true", deleted)
        assertFalse("File must no longer exist", saved.file.exists())

        val list = BasicFileManager.listSavedPrograms(baseDir)
        assertTrue(list.none { it.name == "temp.bas" })
    }

    @Test
    fun testFormats() {
        val baseDir = tempFolder.newFolder("formats_test")
        val code = "10 PRINT \"HELLO WORLD\""
        val saved = BasicFileManager.saveProgram(baseDir, "hello.bas", code)
        assertTrue(saved.formattedSize.isNotEmpty())
        assertTrue(saved.formattedDate.isNotEmpty())
    }
}
