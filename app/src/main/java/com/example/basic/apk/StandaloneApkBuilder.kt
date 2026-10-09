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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ApkExportResult(
    val file: File,
    val publicPath: String,
    val fileSizeFormatted: String,
    val isMediaStored: Boolean = false,
    val validationReport: ApkValidationReport? = null
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
     * 2. Injects the compiled BASIC bytecode into assets/program.basic.bin.
     * 3. Signs using official Android SDK ApkSigner with v1, v2, and v3 schemes.
     * 4. Runs diagnostic verification to report signature status.
     */
    fun buildApk(
        context: Context,
        appName: String,
        packageName: String,
        program: BytecodeProgram
    ): ApkExportResult {
        val safeName = appName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
        val apkFileName = "$safeName.apk"
        val currentPackageName = context.packageName

        val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val apkFile = File(appDownloadsDir, apkFileName)

        val bytecodeBytes = serializeBytecode(program)
        val sourceBytes = program.sourceCode.toByteArray(Charsets.UTF_8)

        val tempApkFile = File(appDownloadsDir, "$safeName.tmp.apk")
        if (tempApkFile.exists()) tempApkFile.delete()

        val seenEntries = mutableSetOf<String>()
        val installedFile = getBaseApkFile(context)

        BufferedOutputStream(FileOutputStream(tempApkFile)).use { bos ->
            ZipOutputStream(bos).use { zos ->
                val buffer = ByteArray(65536)

                fun safePutEntry(entryName: String, data: ByteArray) {
                    val clean = entryName.trim().trimStart('/')
                    if (clean.isEmpty() || seenEntries.contains(clean)) return
                    seenEntries.add(clean)
                    try {
                        val newEntry = ZipEntry(clean)
                        zos.putNextEntry(newEntry)
                        zos.write(data)
                        zos.closeEntry()
                    } catch (ignored: Exception) {
                    }
                }

                fun safeCopyStream(entryName: String, inputStream: InputStream) {
                    val clean = entryName.trim().trimStart('/')
                    if (clean.isEmpty() || seenEntries.contains(clean)) return
                    seenEntries.add(clean)
                    try {
                        val newEntry = ZipEntry(clean)
                        zos.putNextEntry(newEntry)
                        var count: Int
                        while (inputStream.read(buffer).also { count = it } != -1) {
                            zos.write(buffer, 0, count)
                        }
                        zos.closeEntry()
                    } catch (ignored: Exception) {
                    }
                }

                if (installedFile != null) {
                    ZipFile(installedFile).use { zip ->
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val name = entry.name.trim().trimStart('/')

                            if (name.isEmpty() ||
                                name == "assets/program.basic.bin" ||
                                name == "assets/source.bas" ||
                                name == "assets/runner-base.apk" ||
                                name.startsWith("META-INF/", ignoreCase = true) ||
                                seenEntries.contains(name) ||
                                entry.isDirectory
                            ) {
                                continue
                            }

                            if (name == "AndroidManifest.xml") {
                                val rawManifest = ByteArrayOutputStream().use { baos ->
                                    zip.getInputStream(entry).use { `is` -> `is`.copyTo(baos) }
                                    baos.toByteArray()
                                }
                                val modifiedManifest = BinaryXmlModifier.updatePackageAndLabel(
                                    manifestBytes = rawManifest,
                                    currentPackageName = currentPackageName,
                                    newPackageName = packageName,
                                    newLabel = appName
                                )
                                safePutEntry("AndroidManifest.xml", modifiedManifest)
                                continue
                            }

                            zip.getInputStream(entry).use { `is` ->
                                safeCopyStream(name, `is`)
                            }
                        }
                    }
                } else {
                    context.assets.open("runner-base.apk").use { rawStream ->
                        ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val name = entry.name.trim().trimStart('/')
                                if (name.isNotEmpty() &&
                                    name != "assets/program.basic.bin" &&
                                    name != "assets/source.bas" &&
                                    name != "assets/runner-base.apk" &&
                                    !name.startsWith("META-INF/", ignoreCase = true) &&
                                    !seenEntries.contains(name) &&
                                    !entry.isDirectory
                                ) {
                                    if (name == "AndroidManifest.xml") {
                                        val rawManifest = ByteArrayOutputStream().use { baos ->
                                            zis.copyTo(baos)
                                            baos.toByteArray()
                                        }
                                        val modifiedManifest = BinaryXmlModifier.updatePackageAndLabel(
                                            manifestBytes = rawManifest,
                                            currentPackageName = currentPackageName,
                                            newPackageName = packageName,
                                            newLabel = appName
                                        )
                                        safePutEntry("AndroidManifest.xml", modifiedManifest)
                                    } else {
                                        safeCopyStream(name, zis)
                                    }
                                }
                                entry = zis.nextEntry
                            }
                        }
                    }
                }

                // Inject compiled BASIC payload
                safePutEntry("assets/program.basic.bin", bytecodeBytes)
                safePutEntry("assets/source.bas", sourceBytes)
            }
        }

        // Sign with official Android SDK ApkSigner (v1, v2, v3)
        val signedApkFile = File(appDownloadsDir, "$safeName.signed.apk")
        if (signedApkFile.exists()) signedApkFile.delete()

        try {
            val keystoreStream = context.resources.openRawResource(com.example.R.raw.debug_keystore)
            ApkSignerHelper.signApk(
                unsignedApk = tempApkFile,
                signedApk = signedApkFile,
                keystoreStream = keystoreStream,
                minSdkVersion = 21
            )

            tempApkFile.delete()
            if (apkFile.exists()) apkFile.delete()
            signedApkFile.renameTo(apkFile)
        } catch (e: Exception) {
            tempApkFile.delete()
            signedApkFile.delete()
            val causeMsg = e.cause?.message?.let { " (Cause: $it)" } ?: ""
            throw Exception("Signing stage failed: ${e.message}$causeMsg", e)
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

        val validationReport = ApkSignerHelper.validateApk(apkFile, minTargetSdk = 21)

        return ApkExportResult(
            file = apkFile,
            publicPath = displayLocation,
            fileSizeFormatted = sizeFormatted,
            isMediaStored = publicExported,
            validationReport = validationReport
        )
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
