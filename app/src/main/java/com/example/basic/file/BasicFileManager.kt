package com.example.basic.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BasicFileInfo(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
) {
    val formattedSize: String
        get() {
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
                else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}

object BasicFileManager {

    private const val PROGRAMS_DIR_NAME = "programs"

    /**
     * Returns the dedicated directory on device where BASIC text files (.bas) are stored.
     * Uses app-specific external files dir when available, falling back to internal filesDir.
     */
    fun getProgramsDirectory(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        return getProgramsDirectory(baseDir)
    }

    fun getProgramsDirectory(baseDir: File): File {
        val programsDir = File(baseDir, PROGRAMS_DIR_NAME)
        if (!programsDir.exists()) {
            programsDir.mkdirs()
        }
        return programsDir
    }

    /**
     * Normalizes a filename to ensure it has a valid name and .bas extension.
     */
    fun sanitizeFileName(rawName: String): String {
        var clean = rawName.trim()
            .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
            .trim('_')
        if (clean.isBlank()) {
            clean = "program"
        }
        if (!clean.endsWith(".bas", ignoreCase = true) && !clean.endsWith(".txt", ignoreCase = true)) {
            clean = "$clean.bas"
        }
        return clean
    }

    /**
     * Lists all saved BASIC program text files on the device, sorted by most recently modified.
     */
    fun listSavedPrograms(context: Context): List<BasicFileInfo> {
        return listSavedPrograms(getProgramsDirectory(context))
    }

    fun listSavedPrograms(dir: File): List<BasicFileInfo> {
        val files = dir.listFiles { f ->
            f.isFile && (f.name.endsWith(".bas", ignoreCase = true) || f.name.endsWith(".txt", ignoreCase = true))
        } ?: emptyArray()

        return files
            .map { f ->
                BasicFileInfo(
                    file = f,
                    name = f.name,
                    sizeBytes = f.length(),
                    lastModified = f.lastModified()
                )
            }
            .sortedByDescending { it.lastModified }
    }

    /**
     * Saves BASIC source code as a text file in the device's programs directory.
     */
    fun saveProgram(context: Context, fileName: String, code: String): BasicFileInfo {
        return saveProgram(getProgramsDirectory(context), fileName, code)
    }

    fun saveProgram(dir: File, fileName: String, code: String): BasicFileInfo {
        val cleanName = sanitizeFileName(fileName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val targetFile = File(dir, cleanName)
        targetFile.writeText(code, Charsets.UTF_8)

        return BasicFileInfo(
            file = targetFile,
            name = targetFile.name,
            sizeBytes = targetFile.length(),
            lastModified = targetFile.lastModified()
        )
    }

    /**
     * Reads the source code text from a saved file.
     */
    fun loadProgram(file: File): String {
        return file.readText(Charsets.UTF_8)
    }

    /**
     * Deletes a program file from the device.
     */
    fun deleteProgram(file: File): Boolean {
        return file.exists() && file.delete()
    }

    /**
     * Reads text and resolves display name from an Android content Uri (Storage Access Framework).
     */
    fun readFromUri(context: Context, uri: Uri): Pair<String, String> {
        var displayName = "loaded_program.bas"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        displayName = name
                    }
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment?.let { displayName = it }
        }

        val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("Could not open input stream for $uri")

        return Pair(displayName, content)
    }

    /**
     * Writes source code text to an Android content Uri (Storage Access Framework).
     */
    fun writeToUri(context: Context, uri: Uri, code: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(code)
            }
        } ?: throw IllegalStateException("Could not open output stream for $uri")
    }

    /**
     * Shares the BASIC program as a text file using an ACTION_SEND Intent.
     */
    fun shareProgramFile(context: Context, file: File) {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, file.readText(Charsets.UTF_8))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share BASIC program (${file.name})")
        context.startActivity(chooser)
    }
}
