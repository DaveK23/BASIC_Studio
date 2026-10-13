package com.example.basic.apk

import java.util.Locale

/**
 * Normalizes ZIP entry paths and guarantees case-insensitive uniqueness.
 * Prevents java.util.zip.ZipException ("duplicate entry: ...") during APK packaging.
 */
class CaseInsensitiveEntryRegistry {
    // Map canonical lowercase normalized path -> original entry path
    private val canonicalEntries = mutableMapOf<String, String>()
    private val _skippedDuplicates = mutableListOf<String>()

    val skippedDuplicates: List<String> get() = _skippedDuplicates.toList()
    val totalRegistered: Int get() = canonicalEntries.size

    companion object {
        /**
         * Normalizes entry path:
         * - Converts backslashes to forward slashes
         * - Trims leading/trailing whitespace
         * - Removes leading slashes and relative path indicators ("./")
         * - Collapses redundant consecutive slashes ("//")
         */
        fun normalize(entryName: String): String {
            var cleaned = entryName.replace('\\', '/').trim()
            while (cleaned.startsWith("./")) {
                cleaned = cleaned.substring(2).trim()
            }
            cleaned = cleaned.trimStart('/')
            while (cleaned.contains("//")) {
                cleaned = cleaned.replace("//", "/")
            }
            return cleaned
        }
    }

    /**
     * Checks if the entry should be skipped (e.g. empty, directory, blacklisted).
     */
    fun shouldFilterOut(entryName: String, isDirectory: Boolean): Boolean {
        val normalized = normalize(entryName)
        if (normalized.isEmpty() || isDirectory || normalized.endsWith("/")) {
            return true
        }
        val lower = normalized.lowercase(Locale.ROOT)
        // Skip existing runner payloads, icon generator templates, and signature/manifest meta files from template
        if (lower == "assets/program.basic.bin" ||
            lower == "assets/source.bas" ||
            lower == "assets/runner-base.apk" ||
            lower.startsWith("assets/icons/") ||
            lower.startsWith("meta-inf/")
        ) {
            return true
        }
        return false
    }

    /**
     * Attempts to register an entry name.
     * Returns true if registered successfully (unique).
     * Returns false if this entry (or a case-insensitive variant) has already been registered.
     */
    fun tryRegister(entryName: String): Boolean {
        val normalized = normalize(entryName)
        if (normalized.isEmpty() || normalized.endsWith("/")) {
            return false
        }
        val canonicalKey = normalized.lowercase(Locale.ROOT)
        val existing = canonicalEntries[canonicalKey]
        if (existing != null) {
            _skippedDuplicates.add("$normalized (matches existing $existing)")
            return false
        }
        canonicalEntries[canonicalKey] = normalized
        return true
    }

    fun hasEntry(entryName: String): Boolean {
        val canonicalKey = normalize(entryName).lowercase(Locale.ROOT)
        return canonicalEntries.containsKey(canonicalKey)
    }

    fun clear() {
        canonicalEntries.clear()
        _skippedDuplicates.clear()
    }
}
