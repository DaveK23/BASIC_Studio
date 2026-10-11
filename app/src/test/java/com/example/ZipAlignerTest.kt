package com.example

import com.example.basic.apk.ZipAligner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipAlignerTest {

    @Test
    fun testZipAlignerAlignsStoredEntries() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "zipalign_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val unaligned = File(tempDir, "unaligned.apk")
        val aligned = File(tempDir, "aligned.apk")

        try {
            // Build an unaligned zip with an odd-sized first entry, then STORED resources.arsc
            ZipOutputStream(FileOutputStream(unaligned)).use { zos ->
                // Entry 1: 5 bytes deflated (creates non-aligned offset for next entry)
                val e1 = ZipEntry("small.txt").apply { time = 0L; method = ZipEntry.DEFLATED }
                zos.putNextEntry(e1)
                zos.write("hello".toByteArray())
                zos.closeEntry()

                // Entry 2: resources.arsc STORED (uncompressed)
                val arscData = "SAMPLE_RESOURCE_TABLE_DATA_FOR_UNIT_TEST".toByteArray()
                val crc = CRC32().apply { update(arscData) }.value
                val e2 = ZipEntry("resources.arsc").apply {
                    time = 0L
                    method = ZipEntry.STORED
                    size = arscData.size.toLong()
                    compressedSize = arscData.size.toLong()
                    setCrc(crc)
                }
                zos.putNextEntry(e2)
                zos.write(arscData)
                zos.closeEntry()
            }

            // Align using pure Kotlin ZipAligner
            ZipAligner.align(unaligned, aligned, defaultAlignment = 4)

            // Verify with ZipAligner
            val result = ZipAligner.verify(aligned)
            assertTrue("Archive should be aligned: ${result.details}", result.isAligned)
            assertTrue("resources.arsc must be stored uncompressed", result.resourcesArscStored)
            assertTrue("resources.arsc must be 4-byte aligned", result.resourcesArscAligned)
            assertEquals(2, result.totalEntries)
            assertEquals(2, result.alignedEntries)
        } finally {
            unaligned.delete()
            aligned.delete()
            tempDir.delete()
        }
    }
}
