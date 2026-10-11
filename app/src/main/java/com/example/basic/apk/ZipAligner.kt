package com.example.basic.apk

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin ZipAligner implementation matching official Android SDK zipalign behavior.
 * Ensures 4-byte alignment of uncompressed (STORED) entries (such as resources.arsc)
 * and 4096-byte alignment of uncompressed native libraries (.so).
 *
 * This satisfies Android 11+ (API 30+) requirements where unaligned or compressed resources.arsc
 * causes Android Package Manager to reject the APK with "App not installed".
 */
object ZipAligner {

    data class AlignmentResult(
        val isAligned: Boolean,
        val totalEntries: Int,
        val alignedEntries: Int,
        val resourcesArscAligned: Boolean,
        val resourcesArscStored: Boolean,
        val details: List<String>
    ) {
        fun summary(): String {
            val sb = StringBuilder()
            sb.appendLine(if (isAligned) "✓ Zip alignment: VERIFIED (4-byte aligned)" else "✗ Zip alignment: FAILED")
            sb.appendLine("  resources.arsc stored: $resourcesArscStored")
            sb.appendLine("  resources.arsc aligned: $resourcesArscAligned")
            sb.appendLine("  Entries aligned: $alignedEntries / $totalEntries")
            return sb.toString().trim()
        }
    }

    /**
     * Aligns an unaligned APK archive and writes the aligned output file.
     * All STORED entries are padded in the Local File Header extra field to align with 4 bytes (or 4096 for .so).
     */
    fun align(inputFile: File, outputFile: File, defaultAlignment: Int = 4) {
        if (outputFile.exists()) outputFile.delete()

        RandomAccessFile(inputFile, "r").use { inRaf ->
            RandomAccessFile(outputFile, "rw").use { outRaf ->
                val entries = readZipEntries(inRaf)
                val centralDirRecords = mutableListOf<ByteArray>()

                for (entry in entries) {
                    val lfhOffset = outRaf.filePointer

                    // Determine alignment
                    val alignment = if (entry.compressionMethod == 0) { // STORED
                        if (entry.name.endsWith(".so", ignoreCase = true)) 4096 else defaultAlignment
                    } else {
                        0 // DEFLATED does not require alignment
                    }

                    val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
                    val padding = if (alignment > 0) {
                        val dataOffset = lfhOffset + 30 + nameBytes.size
                        ((alignment - (dataOffset % alignment)) % alignment).toInt()
                    } else {
                        0
                    }

                    val extraBytes = ByteArray(padding)

                    // Write Local File Header (30 bytes + name + extra)
                    val lfhBuf = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN)
                    lfhBuf.putInt(0x04034b50) // signature
                    lfhBuf.putShort(entry.versionNeeded.toShort())
                    lfhBuf.putShort((entry.generalPurposeFlag and 0xFFF7.toInt()).toShort()) // clear bit 3
                    lfhBuf.putShort(entry.compressionMethod.toShort())
                    lfhBuf.putShort(entry.lastModTime.toShort())
                    lfhBuf.putShort(entry.lastModDate.toShort())
                    lfhBuf.putInt(entry.crc32.toInt())
                    lfhBuf.putInt(entry.compressedSize.toInt())
                    lfhBuf.putInt(entry.uncompressedSize.toInt())
                    lfhBuf.putShort(nameBytes.size.toShort())
                    lfhBuf.putShort(extraBytes.size.toShort())

                    outRaf.write(lfhBuf.array())
                    outRaf.write(nameBytes)
                    if (extraBytes.isNotEmpty()) {
                        outRaf.write(extraBytes)
                    }

                    // Copy entry raw data
                    inRaf.seek(entry.dataOffset)
                    copyBytes(inRaf, outRaf, entry.compressedSize)

                    // Build Central Directory Header (46 bytes + name)
                    val cdBuf = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN)
                    cdBuf.putInt(0x02014b50) // signature
                    cdBuf.putShort(entry.versionMadeBy.toShort())
                    cdBuf.putShort(entry.versionNeeded.toShort())
                    cdBuf.putShort((entry.generalPurposeFlag and 0xFFF7.toInt()).toShort())
                    cdBuf.putShort(entry.compressionMethod.toShort())
                    cdBuf.putShort(entry.lastModTime.toShort())
                    cdBuf.putShort(entry.lastModDate.toShort())
                    cdBuf.putInt(entry.crc32.toInt())
                    cdBuf.putInt(entry.compressedSize.toInt())
                    cdBuf.putInt(entry.uncompressedSize.toInt())
                    cdBuf.putShort(nameBytes.size.toShort())
                    cdBuf.putShort(0.toShort()) // cd extra len
                    cdBuf.putShort(0.toShort()) // comment len
                    cdBuf.putShort(0.toShort()) // disk number
                    cdBuf.putShort(entry.internalAttr.toShort())
                    cdBuf.putInt(entry.externalAttr.toInt())
                    cdBuf.putInt(lfhOffset.toInt()) // relative offset of local header

                    val cdRecord = ByteArray(46 + nameBytes.size)
                    System.arraycopy(cdBuf.array(), 0, cdRecord, 0, 46)
                    System.arraycopy(nameBytes, 0, cdRecord, 46, nameBytes.size)
                    centralDirRecords.add(cdRecord)
                }

                // Write Central Directory
                val cdOffset = outRaf.filePointer
                for (rec in centralDirRecords) {
                    outRaf.write(rec)
                }
                val cdSize = outRaf.filePointer - cdOffset

                // Write End of Central Directory Record (22 bytes)
                val eocdBuf = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
                eocdBuf.putInt(0x06054b50) // signature
                eocdBuf.putShort(0.toShort()) // disk number
                eocdBuf.putShort(0.toShort()) // disk with cd
                eocdBuf.putShort(centralDirRecords.size.toShort()) // entries on this disk
                eocdBuf.putShort(centralDirRecords.size.toShort()) // total entries
                eocdBuf.putInt(cdSize.toInt()) // size of cd
                eocdBuf.putInt(cdOffset.toInt()) // offset of cd
                eocdBuf.putShort(0.toShort()) // comment len

                outRaf.write(eocdBuf.array())
            }
        }
    }

    /**
     * Verifies 4-byte alignment of an APK file and checks resources.arsc requirements.
     */
    fun verify(apkFile: File): AlignmentResult {
        if (!apkFile.exists() || apkFile.length() < 22) {
            return AlignmentResult(false, 0, 0, false, false, listOf("File missing or too small"))
        }

        val details = mutableListOf<String>()
        var isAllAligned = true
        var totalEntries = 0
        var alignedCount = 0
        var arscAligned = false
        var arscStored = false

        RandomAccessFile(apkFile, "r").use { raf ->
            val entries = try {
                readZipEntries(raf)
            } catch (e: Exception) {
                return AlignmentResult(false, 0, 0, false, false, listOf("Failed to parse zip entries: ${e.message}"))
            }

            totalEntries = entries.size
            for (entry in entries) {
                val isArsc = entry.name.equals("resources.arsc", ignoreCase = true)
                if (isArsc) {
                    arscStored = (entry.compressionMethod == 0)
                }

                if (entry.compressionMethod == 0) { // STORED
                    val alignment = if (entry.name.endsWith(".so", ignoreCase = true)) 4096 else 4
                    val isEntryAligned = (entry.dataOffset % alignment == 0L)
                    if (isEntryAligned) {
                        alignedCount++
                        if (isArsc) arscAligned = true
                    } else {
                        isAllAligned = false
                        details.add("Unaligned STORED entry: '${entry.name}' at offset ${entry.dataOffset} (mod $alignment = ${entry.dataOffset % alignment})")
                    }
                } else {
                    alignedCount++
                }
            }
        }

        if (!arscStored) {
            details.add("resources.arsc is NOT stored uncompressed (violates Android 11+ requirements)")
            isAllAligned = false
        }
        if (arscStored && !arscAligned) {
            details.add("resources.arsc is not 4-byte aligned (violates Android 11+ requirements)")
            isAllAligned = false
        }

        return AlignmentResult(
            isAligned = isAllAligned,
            totalEntries = totalEntries,
            alignedEntries = alignedCount,
            resourcesArscAligned = arscAligned,
            resourcesArscStored = arscStored,
            details = details
        )
    }

    private data class ParsedEntry(
        val name: String,
        val compressionMethod: Int,
        val crc32: Long,
        val compressedSize: Long,
        val uncompressedSize: Long,
        val versionNeeded: Int,
        val versionMadeBy: Int,
        val generalPurposeFlag: Int,
        val lastModTime: Int,
        val lastModDate: Int,
        val internalAttr: Int,
        val externalAttr: Long,
        val lfhOffset: Long,
        val dataOffset: Long
    )

    private fun readZipEntries(raf: RandomAccessFile): List<ParsedEntry> {
        val fileLen = raf.length()
        // Locate EOCD signature 0x06054b50 in the last 65KB
        val maxSearch = minOf(fileLen, 65557L).toInt()
        val searchBuf = ByteArray(maxSearch)
        val searchStart = fileLen - maxSearch
        raf.seek(searchStart)
        raf.readFully(searchBuf)

        var eocdPos = -1
        for (i in maxSearch - 22 downTo 0) {
            if (searchBuf[i] == 0x50.toByte() &&
                searchBuf[i + 1] == 0x4b.toByte() &&
                searchBuf[i + 2] == 0x05.toByte() &&
                searchBuf[i + 3] == 0x06.toByte()
            ) {
                eocdPos = i
                break
            }
        }
        if (eocdPos == -1) throw IllegalStateException("End of Central Directory (EOCD) not found")

        val eocdOffset = searchStart + eocdPos
        raf.seek(eocdOffset)
        val eocdHeader = ByteArray(22)
        raf.readFully(eocdHeader)
        val eocdBuf = ByteBuffer.wrap(eocdHeader).order(ByteOrder.LITTLE_ENDIAN)
        eocdBuf.position(10)
        val totalEntries = eocdBuf.short.toInt() and 0xFFFF
        val cdSize = eocdBuf.int.toLong() and 0xFFFFFFFFL
        val cdOffset = eocdBuf.int.toLong() and 0xFFFFFFFFL

        raf.seek(cdOffset)
        val cdBytes = ByteArray(cdSize.toInt())
        raf.readFully(cdBytes)
        val cdBuf = ByteBuffer.wrap(cdBytes).order(ByteOrder.LITTLE_ENDIAN)

        val result = mutableListOf<ParsedEntry>()
        var pos = 0
        for (i in 0 until totalEntries) {
            if (pos + 46 > cdBytes.size) break
            val sig = cdBuf.getInt(pos)
            if (sig != 0x02014b50) break

            val verMade = cdBuf.getShort(pos + 4).toInt() and 0xFFFF
            val verNeed = cdBuf.getShort(pos + 6).toInt() and 0xFFFF
            val flags = cdBuf.getShort(pos + 8).toInt() and 0xFFFF
            val method = cdBuf.getShort(pos + 10).toInt() and 0xFFFF
            val modTime = cdBuf.getShort(pos + 12).toInt() and 0xFFFF
            val modDate = cdBuf.getShort(pos + 14).toInt() and 0xFFFF
            val crc = cdBuf.getInt(pos + 16).toLong() and 0xFFFFFFFFL
            val cSize = cdBuf.getInt(pos + 20).toLong() and 0xFFFFFFFFL
            val uSize = cdBuf.getInt(pos + 24).toLong() and 0xFFFFFFFFL
            val nLen = cdBuf.getShort(pos + 28).toInt() and 0xFFFF
            val eLen = cdBuf.getShort(pos + 30).toInt() and 0xFFFF
            val cLen = cdBuf.getShort(pos + 32).toInt() and 0xFFFF
            val iAttr = cdBuf.getShort(pos + 36).toInt() and 0xFFFF
            val eAttr = cdBuf.getInt(pos + 38).toLong() and 0xFFFFFFFFL
            val lfhOff = cdBuf.getInt(pos + 42).toLong() and 0xFFFFFFFFL

            val nameBytes = ByteArray(nLen)
            System.arraycopy(cdBytes, pos + 46, nameBytes, 0, nLen)
            val name = String(nameBytes, Charsets.UTF_8)

            // Read Local File Header to determine exact data offset
            raf.seek(lfhOff)
            val lfhHeader = ByteArray(30)
            raf.readFully(lfhHeader)
            val lfhBuf = ByteBuffer.wrap(lfhHeader).order(ByteOrder.LITTLE_ENDIAN)
            val localNameLen = lfhBuf.getShort(26).toInt() and 0xFFFF
            val localExtraLen = lfhBuf.getShort(28).toInt() and 0xFFFF
            val dataOffset = lfhOff + 30 + localNameLen + localExtraLen

            result.add(
                ParsedEntry(
                    name = name,
                    compressionMethod = method,
                    crc32 = crc,
                    compressedSize = cSize,
                    uncompressedSize = uSize,
                    versionNeeded = verNeed,
                    versionMadeBy = verMade,
                    generalPurposeFlag = flags,
                    lastModTime = modTime,
                    lastModDate = modDate,
                    internalAttr = iAttr,
                    externalAttr = eAttr,
                    lfhOffset = lfhOff,
                    dataOffset = dataOffset
                )
            )

            pos += 46 + nLen + eLen + cLen
        }

        return result
    }

    private fun copyBytes(inRaf: RandomAccessFile, outRaf: RandomAccessFile, count: Long) {
        val buf = ByteArray(65536)
        var remaining = count
        while (remaining > 0) {
            val toRead = minOf(remaining, buf.size.toLong()).toInt()
            inRaf.readFully(buf, 0, toRead)
            outRaf.write(buf, 0, toRead)
            remaining -= toRead
        }
    }
}
