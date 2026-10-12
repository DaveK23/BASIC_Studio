package com.example.basic.apk

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.basic.model.BytecodeProgram
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ApkExportResult(
    val file: File,
    val publicPath: String,
    val fileSizeFormatted: String,
    val isMediaStored: Boolean = false,
    val diagnostics: ApkBuildDiagnostics? = null,
    val validationReport: ApkValidationReport? = null,
    val alignmentReport: ZipAligner.AlignmentResult? = null
)

object StandaloneApkBuilder {

    fun serializeBytecode(program: BytecodeProgram): ByteArray {
        val json = JSONObject()
        json.put("source", program.sourceCode)

        val consts = JSONArray()
        program.constants.forEach { c ->
            val co = JSONObject()
            co.put("type", c::class.java.simpleName)
            co.put("val", c.toString())
            consts.put(co)
        }
        json.put("constants", consts)

        val insts = JSONArray()
        program.instructions.forEach { inst ->
            val io = JSONObject()
            io.put("op", inst.op.name)
            io.put("argI", inst.argInt)
            io.put("argS", inst.argString)
            io.put("line", inst.sourceLine)
            insts.put(io)
        }
        json.put("instructions", insts)

        return json.toString(2).toByteArray(Charsets.UTF_8)
    }

    private fun getBaseApkFile(context: Context): File? {
        val sourceDir = context.applicationInfo.sourceDir
        if (!sourceDir.isNullOrEmpty()) {
            val installedApkFile = File(sourceDir)
            if (installedApkFile.exists() && installedApkFile.length() > 500_000L) {
                return installedApkFile
            }
        }
        return null
    }

    /**
     * Builds a standalone installable Android APK:
     * 1. Extracts binary AndroidManifest.xml and rewrites package and label.
     * 2. Injects compiled BASIC bytecode into assets/program.basic.bin.
     * 3. Stores resources.arsc uncompressed (ZipEntry.STORED) as required by Android 11+ (API 30+).
     * 4. Aligns all STORED entries on 4-byte boundaries with pure-Kotlin ZipAligner.
     * 5. Signs using official Android SDK ApkSigner with authentic v1, v2, and v3 signatures.
     * 6. Validates alignment and cryptographic signatures to guarantee seamless installation.
     */
    fun buildApk(
        context: Context,
        appName: String,
        packageName: String,
        program: BytecodeProgram,
        iconIndex: Int = 0
    ): ApkExportResult {
        val safeName = appName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase().ifBlank { "basic_app" }
        val apkFileName = "$safeName.apk"
        val currentPackageName = context.packageName
        val cleanPkg = packageName.trim()

        if (cleanPkg.equals(currentPackageName, ignoreCase = true)) {
            throw IllegalArgumentException(
                "Package ID '$cleanPkg' cannot match BASIC Studio's own package name ($currentPackageName). " +
                "Android prohibits installing conflicting signing certificates over the existing application. " +
                "Please choose a distinct Package ID such as 'com.basic.$safeName'."
            )
        }

        val targetPackageName = if (cleanPkg.contains(".") && cleanPkg.split(".").all { it.matches(Regex("[a-zA-Z0-9_]+")) }) {
            cleanPkg
        } else {
            "com.basic.$safeName"
        }

        val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val apkFile = File(appDownloadsDir, apkFileName)

        val bytecodeBytes = serializeBytecode(program)
        val sourceBytes = program.sourceCode.toByteArray(Charsets.UTF_8)

        val unalignedTempApk = File(appDownloadsDir, "$safeName.unaligned.tmp.apk")
        val alignedTempApk = File(appDownloadsDir, "$safeName.aligned.tmp.apk")
        val signedTempApk = File(appDownloadsDir, "$safeName.signed.tmp.apk")

        if (unalignedTempApk.exists()) unalignedTempApk.delete()
        if (alignedTempApk.exists()) alignedTempApk.delete()
        if (signedTempApk.exists()) signedTempApk.delete()

        val diagnostics: ApkBuildDiagnostics
        try {
            // Stage 1: Assemble unsigned archive with uncompressed resources.arsc
            diagnostics = assembleArchiveWithFallback(
                context = context,
                targetZip = unalignedTempApk,
                appName = appName,
                currentPackageName = currentPackageName,
                targetPackageName = targetPackageName,
                bytecodeBytes = bytecodeBytes,
                sourceBytes = sourceBytes,
                iconIndex = iconIndex
            )

            // Stage 2: ZipAlign 4-byte boundaries on uncompressed entries (Android 11+ requirement)
            ZipAligner.align(
                inputFile = unalignedTempApk,
                outputFile = alignedTempApk,
                defaultAlignment = 4
            )

            // Stage 3: Sign using official Android SDK ApkSigner (v1, v2, v3)
            val keystoreStream = context.resources.openRawResource(com.example.R.raw.debug_keystore)
            ApkSignerHelper.signApk(
                unsignedApk = alignedTempApk,
                signedApk = signedTempApk,
                keystoreStream = keystoreStream,
                minSdkVersion = 21
            )

            // Finalize target APK
            if (apkFile.exists()) apkFile.delete()
            val renamed = signedTempApk.renameTo(apkFile)
            if (!renamed) {
                signedTempApk.copyTo(apkFile, overwrite = true)
                signedTempApk.delete()
            }
        } finally {
            if (unalignedTempApk.exists()) unalignedTempApk.delete()
            if (alignedTempApk.exists()) alignedTempApk.delete()
            if (signedTempApk.exists()) signedTempApk.delete()
        }

        val finalSizeBytes = apkFile.length()
        val sizeFormatted = String.format("%.2f MB", finalSizeBytes / (1024.0 * 1024.0))

        var publicExported = false
        var displayLocation = apkFile.absolutePath

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(apkFileName)
                resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, apkFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        FileInputStream(apkFile).use { `is` ->
                            val buf = ByteArray(65536)
                            var read: Int
                            while (`is`.read(buf).also { read = it } != -1) {
                                os.write(buf, 0, read)
                            }
                        }
                    }
                    publicExported = true
                    displayLocation = "Internal Storage > Download > $apkFileName"
                }
            } catch (e: Exception) {
                // Ignore MediaStore fallback
            }
        } else {
            try {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDir.exists() || publicDir.mkdirs()) {
                    val publicFile = File(publicDir, apkFileName)
                    apkFile.copyTo(publicFile, overwrite = true)
                    publicExported = true
                    displayLocation = publicFile.absolutePath
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        val alignmentReport = ZipAligner.verify(apkFile)
        val validationReport = ApkSignerHelper.validateApk(apkFile, minTargetSdk = 21)

        return ApkExportResult(
            file = apkFile,
            publicPath = displayLocation,
            fileSizeFormatted = sizeFormatted,
            isMediaStored = publicExported,
            diagnostics = diagnostics,
            validationReport = validationReport,
            alignmentReport = alignmentReport
        )
    }

    private fun assembleArchiveWithFallback(
        context: Context,
        targetZip: File,
        appName: String,
        currentPackageName: String,
        targetPackageName: String,
        bytecodeBytes: ByteArray,
        sourceBytes: ByteArray,
        iconIndex: Int
    ): ApkBuildDiagnostics {
        var assetError: Throwable? = null

        // Primary Attempt: Asset template (runner-base.apk)
        try {
            val collector = ApkBuildDiagnosticsCollector(baseSource = "Asset (runner-base.apk)")
            assembleFromAssetStream(
                context = context,
                targetZip = targetZip,
                appName = appName,
                currentPackageName = currentPackageName,
                targetPackageName = targetPackageName,
                bytecodeBytes = bytecodeBytes,
                sourceBytes = sourceBytes,
                iconIndex = iconIndex,
                diagnostics = collector
            )
            return collector.build()
        } catch (e: Throwable) {
            assetError = e
            if (targetZip.exists()) {
                targetZip.delete()
            }
        }

        // Secondary Attempt: Installed host APK fallback
        val installedFile = getBaseApkFile(context)
        if (installedFile != null) {
            try {
                val collector = ApkBuildDiagnosticsCollector(baseSource = "Installed APK (${installedFile.name})")
                assembleFromZipFile(
                    sourceFile = installedFile,
                    targetZip = targetZip,
                    appName = appName,
                    currentPackageName = currentPackageName,
                    targetPackageName = targetPackageName,
                    bytecodeBytes = bytecodeBytes,
                    sourceBytes = sourceBytes,
                    iconIndex = iconIndex,
                    diagnostics = collector
                )
                return collector.build()
            } catch (installedError: Throwable) {
                if (targetZip.exists()) {
                    targetZip.delete()
                }
                val assetMsg = assetError?.message ?: "unknown"
                val instMsg = installedError.message ?: "unknown"
                throw Exception("Archive packaging failed on both asset ($assetMsg) and installed APK ($instMsg)", installedError)
            }
        }

        throw Exception("Archive packaging from asset failed: ${assetError?.message ?: "unknown asset error"}", assetError)
    }

    private fun assembleFromAssetStream(
        context: Context,
        targetZip: File,
        appName: String,
        currentPackageName: String,
        targetPackageName: String,
        bytecodeBytes: ByteArray,
        sourceBytes: ByteArray,
        iconIndex: Int,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        val registry = CaseInsensitiveEntryRegistry()

        BufferedOutputStream(FileOutputStream(targetZip)).use { bos ->
            ZipOutputStream(bos).use { zos ->
                context.assets.open("runner-base.apk").use { rawStream ->
                    ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            diagnostics.recordScanned()
                            val rawName = entry.name
                            val isDir = entry.isDirectory

                            if (registry.shouldFilterOut(rawName, isDir)) {
                                diagnostics.recordFiltered()
                                zis.closeEntry()
                                entry = zis.nextEntry
                                continue
                            }

                            val normalized = CaseInsensitiveEntryRegistry.normalize(rawName)
                            // If this entry is an existing ic_launcher png or webp, replace it with generated custom icon
                            if (isLauncherIconEntry(normalized)) {
                                val customIconBytes = RetroIconGenerator.generateIconPngBytes(iconIndex, getIconSizeForPath(normalized))
                                if (registry.tryRegister(normalized)) {
                                    writeEntry(zos, normalized, customIconBytes, isStored = false, diagnostics = diagnostics)
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                                continue
                            }

                            if (!registry.tryRegister(normalized)) {
                                diagnostics.recordDuplicate(normalized)
                                zis.closeEntry()
                                entry = zis.nextEntry
                                continue
                            }

                            val shouldStoreUncompressed = normalized.equals("resources.arsc", ignoreCase = true) ||
                                    (entry.method == ZipEntry.STORED)

                            if (normalized.equals("AndroidManifest.xml", ignoreCase = true)) {
                                val rawManifest = zis.readBytes()
                                val modifiedManifest = BinaryXmlModifier.updatePackageAndLabel(
                                    manifestBytes = rawManifest,
                                    currentPackageName = currentPackageName,
                                    newPackageName = targetPackageName,
                                    newLabel = appName
                                )
                                writeEntry(zos, "AndroidManifest.xml", modifiedManifest, isStored = false, diagnostics = diagnostics)
                            } else if (shouldStoreUncompressed) {
                                val rawBytes = zis.readBytes()
                                writeEntry(zos, normalized, rawBytes, isStored = true, diagnostics = diagnostics)
                            } else {
                                writeDeflatedFromZis(zos, normalized, zis, diagnostics)
                            }

                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }

                // Inject compiled BASIC payload (compressed)
                injectPayload(zos, registry, bytecodeBytes, sourceBytes, diagnostics)
                // Inject custom launcher icons into mipmap densities
                injectCustomIcons(zos, registry, iconIndex, diagnostics)
            }
        }
    }

    private fun assembleFromZipFile(
        sourceFile: File,
        targetZip: File,
        appName: String,
        currentPackageName: String,
        targetPackageName: String,
        bytecodeBytes: ByteArray,
        sourceBytes: ByteArray,
        iconIndex: Int,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        val registry = CaseInsensitiveEntryRegistry()

        BufferedOutputStream(FileOutputStream(targetZip)).use { bos ->
            ZipOutputStream(bos).use { zos ->
                ZipFile(sourceFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        diagnostics.recordScanned()
                        val rawName = entry.name
                        val isDir = entry.isDirectory

                        if (registry.shouldFilterOut(rawName, isDir)) {
                            diagnostics.recordFiltered()
                            continue
                        }

                        val normalized = CaseInsensitiveEntryRegistry.normalize(rawName)
                        if (isLauncherIconEntry(normalized)) {
                            val customIconBytes = RetroIconGenerator.generateIconPngBytes(iconIndex, getIconSizeForPath(normalized))
                            if (registry.tryRegister(normalized)) {
                                writeEntry(zos, normalized, customIconBytes, isStored = false, diagnostics = diagnostics)
                            }
                            continue
                        }

                        if (!registry.tryRegister(normalized)) {
                            diagnostics.recordDuplicate(normalized)
                            continue
                        }

                        val shouldStoreUncompressed = normalized.equals("resources.arsc", ignoreCase = true) ||
                                (entry.method == ZipEntry.STORED)

                        if (normalized.equals("AndroidManifest.xml", ignoreCase = true)) {
                            val rawManifest = zip.getInputStream(entry).use { it.readBytes() }
                            val modifiedManifest = BinaryXmlModifier.updatePackageAndLabel(
                                manifestBytes = rawManifest,
                                currentPackageName = currentPackageName,
                                newPackageName = targetPackageName,
                                newLabel = appName
                            )
                            writeEntry(zos, "AndroidManifest.xml", modifiedManifest, isStored = false, diagnostics = diagnostics)
                        } else if (shouldStoreUncompressed) {
                            val rawBytes = zip.getInputStream(entry).use { it.readBytes() }
                            writeEntry(zos, normalized, rawBytes, isStored = true, diagnostics = diagnostics)
                        } else {
                            zip.getInputStream(entry).use { inputStream ->
                                writeDeflatedFromStream(zos, normalized, inputStream, diagnostics)
                            }
                        }
                    }
                }

                // Inject compiled BASIC payload (compressed)
                injectPayload(zos, registry, bytecodeBytes, sourceBytes, diagnostics)
                // Inject custom launcher icons into mipmap densities
                injectCustomIcons(zos, registry, iconIndex, diagnostics)
            }
        }
    }

    private fun isLauncherIconEntry(entryName: String): Boolean {
        val lower = entryName.lowercase()
        return (lower.startsWith("res/mipmap") || lower.startsWith("res/drawable")) &&
                (lower.contains("ic_launcher.png") || lower.contains("ic_launcher.webp") ||
                 lower.contains("ic_launcher_round.png") || lower.contains("ic_launcher_round.webp"))
    }

    private fun getIconSizeForPath(path: String): Int {
        val lower = path.lowercase()
        return when {
            lower.contains("xxxhdpi") -> 192
            lower.contains("xxhdpi") -> 144
            lower.contains("xhdpi") -> 96
            lower.contains("hdpi") -> 72
            lower.contains("mdpi") -> 48
            else -> 192
        }
    }

    private fun injectCustomIcons(
        zos: ZipOutputStream,
        registry: CaseInsensitiveEntryRegistry,
        iconIndex: Int,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        val iconDensities = listOf(
            "res/mipmap-mdpi-v4/ic_launcher.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher.png" to 192,
            "res/mipmap-mdpi-v4/ic_launcher_round.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher_round.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher_round.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher_round.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher_round.png" to 192
        )
        for ((path, size) in iconDensities) {
            if (registry.tryRegister(path)) {
                val pngBytes = RetroIconGenerator.generateIconPngBytes(iconIndex, size)
                writeEntry(zos, path, pngBytes, isStored = false, diagnostics = diagnostics)
            }
        }
    }

    private fun injectPayload(
        zos: ZipOutputStream,
        registry: CaseInsensitiveEntryRegistry,
        bytecodeBytes: ByteArray,
        sourceBytes: ByteArray,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        if (registry.tryRegister("assets/program.basic.bin")) {
            writeEntry(zos, "assets/program.basic.bin", bytecodeBytes, isStored = false, diagnostics = diagnostics)
        }
        if (registry.tryRegister("assets/source.bas")) {
            writeEntry(zos, "assets/source.bas", sourceBytes, isStored = false, diagnostics = diagnostics)
        }
    }

    private fun writeEntry(
        zos: ZipOutputStream,
        entryName: String,
        data: ByteArray,
        isStored: Boolean,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        val cleanName = CaseInsensitiveEntryRegistry.normalize(entryName)
        if (cleanName.isEmpty()) {
            diagnostics.recordError("Rejected empty entry name")
            return
        }

        try {
            val entry = ZipEntry(cleanName).apply {
                time = 0L
                if (isStored) {
                    method = ZipEntry.STORED
                    size = data.size.toLong()
                    compressedSize = data.size.toLong()
                    val crc = CRC32().apply { update(data) }.value
                    setCrc(crc)
                } else {
                    method = ZipEntry.DEFLATED
                }
            }
            zos.putNextEntry(entry)
            try {
                zos.write(data)
            } finally {
                zos.closeEntry()
            }
            diagnostics.recordWritten(cleanName)
        } catch (e: Exception) {
            val errorMsg = "Archive write error on entry '$cleanName' (${data.size} bytes, stored=$isStored): ${e.message}"
            diagnostics.recordError(errorMsg)
            throw ZipException(errorMsg).apply { initCause(e) }
        }
    }

    private fun writeDeflatedFromZis(
        zos: ZipOutputStream,
        entryName: String,
        zis: ZipInputStream,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        val cleanName = CaseInsensitiveEntryRegistry.normalize(entryName)
        if (cleanName.isEmpty()) {
            diagnostics.recordError("Rejected empty entry name from ZipInputStream")
            return
        }

        try {
            val entry = ZipEntry(cleanName).apply {
                time = 0L
                method = ZipEntry.DEFLATED
            }
            zos.putNextEntry(entry)
            val buffer = ByteArray(65536)
            var count: Int
            try {
                while (zis.read(buffer).also { count = it } != -1) {
                    zos.write(buffer, 0, count)
                }
            } finally {
                zos.closeEntry()
            }
            diagnostics.recordWritten(cleanName)
        } catch (e: Exception) {
            val errorMsg = "Archive copy error on entry '$cleanName': ${e.message}"
            diagnostics.recordError(errorMsg)
            throw ZipException(errorMsg).apply { initCause(e) }
        }
    }

    private fun writeDeflatedFromStream(
        zos: ZipOutputStream,
        entryName: String,
        inputStream: InputStream,
        diagnostics: ApkBuildDiagnosticsCollector
    ) {
        val cleanName = CaseInsensitiveEntryRegistry.normalize(entryName)
        if (cleanName.isEmpty()) {
            diagnostics.recordError("Rejected empty entry name from InputStream")
            return
        }

        try {
            val entry = ZipEntry(cleanName).apply {
                time = 0L
                method = ZipEntry.DEFLATED
            }
            zos.putNextEntry(entry)
            val buffer = ByteArray(65536)
            var count: Int
            try {
                while (inputStream.read(buffer).also { count = it } != -1) {
                    zos.write(buffer, 0, count)
                }
            } finally {
                zos.closeEntry()
            }
            diagnostics.recordWritten(cleanName)
        } catch (e: Exception) {
            val errorMsg = "Archive stream error on entry '$cleanName': ${e.message}"
            diagnostics.recordError(errorMsg)
            throw ZipException(errorMsg).apply { initCause(e) }
        }
    }

    fun openApkInstaller(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val settingsIntent = Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                } catch (e: Exception) {
                    // Fall through to standard installer
                }
            }
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            context.startActivity(Intent.createChooser(installIntent, "Open or Install APK"))
        }
    }

    fun shareApk(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share or Save APK to..."))
    }
}
