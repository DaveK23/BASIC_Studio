package com.example.basic.apk

/**
 * Diagnostic metrics and telemetry collected during standalone APK packaging.
 */
data class ApkBuildDiagnostics(
    val totalScanned: Int = 0,
    val writtenCount: Int = 0,
    val skippedDuplicates: List<String> = emptyList(),
    val filteredCount: Int = 0,
    val baseSource: String = "Unknown",
    val durationMs: Long = 0L,
    val errors: List<String> = emptyList()
) {
    fun summary(): String {
        val sb = StringBuilder()
        sb.appendLine("Base Source: $baseSource")
        sb.appendLine("Entries Scanned: $totalScanned | Written: $writtenCount | Filtered: $filteredCount")
        if (skippedDuplicates.isNotEmpty()) {
            sb.appendLine("Auto-Deduplicated (${skippedDuplicates.size} entries):")
            skippedDuplicates.take(5).forEach { sb.appendLine("  • Skipped duplicate: $it") }
            if (skippedDuplicates.size > 5) {
                sb.appendLine("  • ... and ${skippedDuplicates.size - 5} more")
            }
        } else {
            sb.appendLine("Deduplication: 0 collisions detected (clean)")
        }
        if (errors.isNotEmpty()) {
            sb.appendLine("Encountered Warnings/Errors:")
            errors.forEach { sb.appendLine("  ! $it") }
        }
        sb.appendLine("Packaging Time: ${durationMs}ms")
        return sb.toString().trim()
    }
}

/**
 * Mutable collector used while assembling the APK archive.
 */
class ApkBuildDiagnosticsCollector(
    var baseSource: String = "Unknown"
) {
    private val startTime = System.currentTimeMillis()
    var totalScanned: Int = 0
    var writtenCount: Int = 0
    var filteredCount: Int = 0
    private val _skippedDuplicates = mutableListOf<String>()
    private val _errors = mutableListOf<String>()

    fun recordScanned() {
        totalScanned++
    }

    fun recordWritten(entryName: String) {
        writtenCount++
    }

    fun recordFiltered() {
        filteredCount++
    }

    fun recordDuplicate(detail: String) {
        _skippedDuplicates.add(detail)
    }

    fun recordDuplicates(details: List<String>) {
        _skippedDuplicates.addAll(details)
    }

    fun recordError(errorMsg: String) {
        _errors.add(errorMsg)
    }

    fun build(): ApkBuildDiagnostics {
        return ApkBuildDiagnostics(
            totalScanned = totalScanned,
            writtenCount = writtenCount,
            skippedDuplicates = _skippedDuplicates.toList(),
            filteredCount = filteredCount,
            baseSource = baseSource,
            durationMs = System.currentTimeMillis() - startTime,
            errors = _errors.toList()
        )
    }
}
