package com.example.basic.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses and rewrites the StringPool table inside Android's binary XML (AndroidManifest.xml).
 *
 * 1. Renames the base package identifier and all content provider authorities.
 * 2. Rewrites the application label attribute to directly reference the user's custom app name
 *    (replacing the @string/app_name resource reference 0x7f0b0002).
 * 3. Patches targetSdkVersion and compileSdkVersion from 36 down to 34 (Android 14 standard)
 *    to ensure full compatibility with Android 13/14 device package installers.
 */
object BinaryXmlModifier {

    fun updatePackageAndLabel(
        manifestBytes: ByteArray,
        currentPackageName: String,
        newPackageName: String,
        newLabel: String?
    ): ByteArray {
        val buf = ByteBuffer.wrap(manifestBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Root ResXMLTree_header: type (2), header_size (2), chunk_size (4)
        val rootType = buf.short.toInt() and 0xFFFF
        if (rootType != 0x0003) return manifestBytes

        // StringPool chunk starts at offset 8
        val spOffset = 8
        buf.position(spOffset)
        val spType = buf.short.toInt() and 0xFFFF
        if (spType != 0x0001) return manifestBytes

        val spHeaderSize = buf.short.toInt() and 0xFFFF
        val spChunkSize = buf.int
        val stringCount = buf.int
        val styleCount = buf.int
        val flags = buf.int
        val stringsStart = buf.int
        val stylesStart = buf.int

        val isUtf8 = (flags and (1 shl 8)) != 0

        // Read string offsets
        val offsetsStart = spOffset + spHeaderSize
        buf.position(offsetsStart)
        val offsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            offsets[i] = buf.int
        }

        // Read strings
        val strDataStart = spOffset + stringsStart
        val strings = ArrayList<String>(stringCount)

        for (i in 0 until stringCount) {
            val curOffset = strDataStart + offsets[i]
            buf.position(curOffset)
            if (!isUtf8) {
                // UTF-16
                val lenHeader = buf.short.toInt() and 0xFFFF
                val u16Len = if ((lenHeader and 0x8000) != 0) {
                    val high = lenHeader and 0x7FFF
                    val low = buf.short.toInt() and 0xFFFF
                    (high shl 16) or low
                } else {
                    lenHeader
                }
                val chars = CharArray(u16Len)
                for (c in 0 until u16Len) {
                    chars[c] = buf.char
                }
                strings.add(String(chars))
            } else {
                // UTF-8
                var len = buf.get().toInt() and 0xFF
                if ((len and 0x80) != 0) {
                    len = buf.get().toInt() and 0xFF
                }
                var byteLen = buf.get().toInt() and 0xFF
                if ((byteLen and 0x80) != 0) {
                    byteLen = buf.get().toInt() and 0xFF
                }
                val bytes = ByteArray(byteLen)
                buf.get(bytes)
                strings.add(String(bytes, Charsets.UTF_8))
            }
        }

        // Replace package names, label strings
        var labelStringIndex = -1
        val replacedStrings = ArrayList<String>(stringCount)
        val targetAppTitle = if (!newLabel.isNullOrBlank()) newLabel else "BASIC App"

        for (i in 0 until stringCount) {
            var s = strings[i]
            if (s.startsWith(currentPackageName)) {
                s = newPackageName + s.substring(currentPackageName.length)
            } else if (s.startsWith("com.aistudio.basicstudio.")) {
                val dotIdx = s.indexOf('.', "com.aistudio.basicstudio.".length)
                val suffix = if (dotIdx != -1) s.substring(dotIdx) else ""
                s = newPackageName + suffix
            }

            if (s == "BASIC Runner" || s == "BASIC Studio") {
                s = targetAppTitle
                labelStringIndex = i
            }

            // Remove preview platform codename '16' so standard released Android devices accept the APK
            if (s == "16") {
                s = ""
            }

            replacedStrings.add(s)
        }

        if (labelStringIndex == -1 && stringCount > 31) {
            labelStringIndex = 31
            replacedStrings[31] = targetAppTitle
        }

        // Rebuild StringPool
        val newStrData = java.io.ByteArrayOutputStream()
        val newOffsets = IntArray(stringCount)

        for (i in 0 until stringCount) {
            newOffsets[i] = newStrData.size()
            val s = replacedStrings[i]
            if (!isUtf8) {
                val len = s.length
                val lenBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(len.toShort()).array()
                newStrData.write(lenBytes)
                val strBytes = s.toByteArray(Charsets.UTF_16LE)
                newStrData.write(strBytes)
                newStrData.write(0)
                newStrData.write(0) // 2-byte null terminator
            } else {
                val strBytes = s.toByteArray(Charsets.UTF_8)
                val len = strBytes.size
                newStrData.write(len)
                newStrData.write(len)
                newStrData.write(strBytes)
                newStrData.write(0)
            }
        }

        // Pad to 4 bytes
        while (newStrData.size() % 4 != 0) {
            newStrData.write(0)
        }

        val newStrDataBytes = newStrData.toByteArray()
        val newOffsetsSize = stringCount * 4
        val newStringsStart = spHeaderSize + newOffsetsSize
        val newSpChunkSize = spHeaderSize + newOffsetsSize + newStrDataBytes.size

        // Build new StringPool chunk
        val newSpHeaderBuf = ByteBuffer.allocate(spHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
        newSpHeaderBuf.putShort(spType.toShort())
        newSpHeaderBuf.putShort(spHeaderSize.toShort())
        newSpHeaderBuf.putInt(newSpChunkSize)
        newSpHeaderBuf.putInt(stringCount)
        newSpHeaderBuf.putInt(styleCount)
        newSpHeaderBuf.putInt(flags)
        newSpHeaderBuf.putInt(newStringsStart)
        newSpHeaderBuf.putInt(stylesStart)

        val newOffsetsBuf = ByteBuffer.allocate(newOffsetsSize).order(ByteOrder.LITTLE_ENDIAN)
        for (off in newOffsets) {
            newOffsetsBuf.putInt(off)
        }

        val remainderOffset = spOffset + spChunkSize
        val remainderBytes = manifestBytes.copyOfRange(remainderOffset, manifestBytes.size)

        // 1. Mutate label attribute values to point directly to labelStringIndex
        // 2. Patch targetSdkVersion / compileSdkVersion from 36 down to 34 (Android 14)
        val rBuf = ByteBuffer.wrap(remainderBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (pos in 0 until remainderBytes.size - 20 step 4) {
            val aName = rBuf.getInt(pos + 4)
            val valData = rBuf.getInt(pos + 16)

            if (valData == 0x7f0b0002 && labelStringIndex != -1) { // Old @string/app_name reference
                rBuf.putInt(pos + 8, labelStringIndex) // rawValue
                remainderBytes[pos + 15] = 0x03 // dataType = TYPE_STRING
                rBuf.putInt(pos + 16, labelStringIndex) // data
            }

            if (aName in 0 until stringCount) {
                val nameStr = strings[aName]
                if (nameStr == "targetSdkVersion" || nameStr == "compileSdkVersion" || nameStr == "platformBuildVersionCode") {
                    rBuf.putInt(pos + 16, 34) // Android 14 stable target
                } else if (nameStr == "extractNativeLibs") {
                    // Ensure native libraries can be extracted by package manager
                    remainderBytes[pos + 15] = 0x12 // TYPE_INT_BOOLEAN
                    rBuf.putInt(pos + 16, -1) // 0xFFFFFFFF = true
                } else if (nameStr == "testOnly") {
                    remainderBytes[pos + 15] = 0x12
                    rBuf.putInt(pos + 16, 0) // false
                }
            }
        }

        val newTotalSize = 8 + newSpChunkSize + remainderBytes.size
        val newRootHeader = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        newRootHeader.putShort(0x0003.toShort())
        newRootHeader.putShort(8.toShort())
        newRootHeader.putInt(newTotalSize)

        val out = java.io.ByteArrayOutputStream(newTotalSize)
        out.write(newRootHeader.array())
        out.write(newSpHeaderBuf.array())
        out.write(newOffsetsBuf.array())
        out.write(newStrDataBytes)
        out.write(remainderBytes)

        return out.toByteArray()
    }
}
