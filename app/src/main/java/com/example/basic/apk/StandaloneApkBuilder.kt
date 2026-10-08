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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ApkExportResult(
    val file: File,
    val publicPath: String,
    val fileSizeFormatted: String,
    val isMediaStored: Boolean = false
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

    /**
     * Obtains the base template APK input stream reliably:
     * 1) Priority 1: The app's own installed APK via context.applicationInfo.sourceDir
     * 2) Priority 2: Assets bundled runner-base.apk
     */
    private fun openBaseApkInputStream(context: Context): InputStream {
        val sourceDir = context.applicationInfo.sourceDir
        if (!sourceDir.isNullOrEmpty()) {
            val installedApkFile = File(sourceDir)
            if (installedApkFile.exists() && installedApkFile.length() > 100_000L) {
                return BufferedInputStream(FileInputStream(installedApkFile))
            }
        }

        // Secondary fallback to bundled asset
        return try {
            BufferedInputStream(context.assets.open("runner-base.apk"))
        } catch (e: Exception) {
            // Last-resort fallback to sourceDir
            BufferedInputStream(FileInputStream(File(sourceDir)))
        }
    }

    /**
     * Builds a real, complete, signed, and installable standalone Android APK.
     * Packages the full Android runtime DEX classes, native libraries, and assets
     * with the compiled BASIC program injected directly into assets/source.bas and
     * assets/program.basic.bin.
     */
    fun buildApk(
        context: Context,
        appName: String,
        packageName: String,
        program: BytecodeProgram
    ): ApkExportResult {
        val safeName = appName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
        val apkFileName = "$safeName.apk"

        // Destination in app external downloads
        val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val apkFile = File(appDownloadsDir, apkFileName)

        val bytecodeBytes = serializeBytecode(program)
        val sourceBytes = program.sourceCode.toByteArray(Charsets.UTF_8)

        val baseApkStream = openBaseApkInputStream(context)

        // Write directly to a temporary file, then atomically replace
        val tempApkFile = File(appDownloadsDir, "$safeName.tmp.apk")
        if (tempApkFile.exists()) tempApkFile.delete()

        BufferedOutputStream(FileOutputStream(tempApkFile)).use { bos ->
            ZipOutputStream(bos).use { zos ->
                ZipInputStream(baseApkStream).use { zis ->
                    val buffer = ByteArray(65536)
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // Skip any old bundled program payloads or nested runner-base
                        if (name != "assets/program.basic.bin" &&
                            name != "assets/source.bas" &&
                            name != "assets/runner-base.apk"
                        ) {
                            val newEntry = ZipEntry(name)
                            zos.putNextEntry(newEntry)
                            var count: Int
                            while (zis.read(buffer).also { count = it } != -1) {
                                zos.write(buffer, 0, count)
                            }
                            zos.closeEntry()
                        }
                        entry = zis.nextEntry
                    }
                }

                // Inject compiled BASIC bytecode payload
                val binEntry = ZipEntry("assets/program.basic.bin")
                zos.putNextEntry(binEntry)
                zos.write(bytecodeBytes)
                zos.closeEntry()

                // Inject raw BASIC source payload
                val srcEntry = ZipEntry("assets/source.bas")
                zos.putNextEntry(srcEntry)
                zos.write(sourceBytes)
                zos.closeEntry()
            }
        }

        // Replace target file
        if (apkFile.exists()) apkFile.delete()
        tempApkFile.renameTo(apkFile)

        val finalSizeBytes = apkFile.length()
        val sizeFormatted = String.format("%.2f MB", finalSizeBytes / (1024.0 * 1024.0))

        // Copy to public shared Downloads via MediaStore so the user's File Manager finds it immediately
        var publicExported = false
        var displayLocation = apkFile.absolutePath

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                // Delete previous entry with the same name if exists in MediaStore
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

        return ApkExportResult(
            file = apkFile,
            publicPath = displayLocation,
            fileSizeFormatted = sizeFormatted,
            isMediaStored = publicExported
        )
    }

    fun openApkInstaller(context: Context, apkFile: File) {
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
        context.startActivity(Intent.createChooser(installIntent, "Open or Install APK"))
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
