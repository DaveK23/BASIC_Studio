# Standalone APK Builder Duplicate Entry Diagnosis & Resolution Plan

1-2 concise sentences summarizing the overarching goal:
Diagnose and eliminate the `"build error: duplicate entry:"` failure during in-app standalone APK generation by introducing case-insensitive archive entry normalization, preventing dirty-stream fallbacks, and adding transparent real-time diagnostic reporting to the export interface.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed through interactive clarification and define the core repair strategy:

- **Confirmed Error Context**: The error occurs directly within the running BASIC Studio Android application when tapping "Compile & Build APK" in the Export Standalone APK modal.
- **Confirmed Packaging Strategy**: Automatically deduplicate archive entries using normalized, case-insensitive comparison while capturing and presenting detailed diagnostic telemetry (entry count, skipped duplicates, and signature validation summary).

---

## 1. Overview & Core Concept

### What It Does
Provides a bulletproof, deterministic packaging engine (`StandaloneApkBuilder`) that extracts Dalvik runtime binaries and resources from the base template, updates the binary `AndroidManifest.xml` (renaming package and application labels), injects compiled BASIC bytecode, deduplicates conflicting archive entries, signs using official Android `ApkSigner` (v1, v2, v3 schemes), and displays an actionable build diagnostic card.

### Target Audience & Persona
Students, retro-computing hobbyists, and developers creating standalone Android applications using BASIC Studio who need reliable, self-contained APK exports without packaging crashes.

### Key Value
Transforms a cryptic `"build error: duplicate entry:"` failure into a zero-crash, 100% reliable build pipeline with complete visibility into archive assembly and signing health.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Initiate Export**: User taps the **Export APK** toolbar action from the BASIC Editor or Runner screen.
2. **Configure App Metadata**: The modal dialog displays the App Name and Package ID fields with validation.
3. **Build Execution & Progress**: When user taps **Compile & Build APK**, an animated progress indicator with active status text displays (`Compiling BASIC bytecode...`, `Assembling APK package...`, `Signing APK (v1/v2/v3)...`).
4. **Diagnostic Feedback**:
   - **On Success**: Shows a green-accented summary card with APK size, file location, signature verification chips (`v1 ✓`, `v2 ✓`, `v3 ✓`), and action buttons (`Install`, `Share / Save`).
   - **On Packaging Conflict / Error**: Shows an amber/red diagnostic breakdown detailing the conflicting entry name, total entries processed, deduplication log, and exact stack trace.

### Visual Identity & Theme
- **Theme**: Dark Terminal / Retro-Modern IDE (`0xFF0F172A` Slate background, `0xFF00FF66` Terminal Green, `0xFF38BDF8` Terminal Cyan, `0xFF1E293B` Card Surface).
- **Typography**: Monospace typography (`Jetpack Compose` monospace family) for all paths, package names, logs, and diagnostic badges.
- **Components**: Material 3 `AlertDialog`, `OutlinedTextField`, `ElevatedCard`, and compact badge indicators for signing schemes.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Case-Insensitive Canonical Entry Registry
- **Chosen Approach**: Maintain a dedicated `CaseInsensitiveEntryRegistry` that strips leading slashes, converts backslashes to forward slashes, trims whitespace, and compares paths in canonical lowercase while preserving original casing on first write.
- **Why**: ZIP specifications and Android asset packaging tools occasionally introduce entries with differing path casing or redundant directory markers (e.g., `res/drawable/` vs `res/drawable`). A case-insensitive check guarantees that `ZipOutputStream.putNextEntry()` will never trigger a `ZipException: duplicate entry`.
- **Alternatives Considered**: Raw `Set<String>` with exact case matching (failed previously due to case mismatch between runtime templates).

### Decision 2: Isolated Fallback Execution (No Dirty Streams)
- **Chosen Approach**: If extraction from the installed base APK (`context.applicationInfo.sourceDir`) encounters any exception, completely discard the in-progress temporary ZIP file, reset entry tracking, and perform a pristine run using `assets/runner-base.apk`.
- **Why**: Previously, if reading the installed APK failed midway, the builder continued writing to the same `ZipOutputStream` using `runner-base.apk`, attempting to write duplicate `AndroidManifest.xml` and resource entries that were already written during the first half.
- **Alternatives Considered**: Attempting to resume iteration midway (fragile and prone to missing byte chunks).

### Decision 3: Explicit Safe Stream Wrapper with Detailed Exception Attribution
- **Chosen Approach**: Replace blind `catch (ignored: Exception)` with explicit exception logging that records the entry name, byte size, and error details. If `ZipOutputStream.putNextEntry` fails, attribute the exact entry name in the thrown exception so user error reports are never blank.
- **Why**: The user saw `"build error: duplicate entry:"` with no filename because the exception lacked an entry attribute or originated from an empty entry name.

---

## 4. Technical Architecture & Data Strategy

### System Component Architecture

```
┌────────────────────────────────────────────────────────┐
│                   BasicStudio UI                       │
│    ExportApkDialog ───> BasicViewModel.exportApk()     │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                StandaloneApkBuilder                    │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │         CaseInsensitiveEntryRegistry             │  │
│  │   • Normalizes separators & casing               │  │
│  │   • Filters directory entries & duplicates       │  │
│  │   • Records diagnostic telemetry                 │  │
│  └──────────────────────────────────────────────────┘  │
│                            │                           │
│  ┌─────────────────────────┴────────────────────────┐  │
│  │                Archive Assembly                  │  │
│  │   Pass 1: Installed base.apk (Isolated)          │  │
│  │     └─ Failure Fallback ──> Clean Temp Reset     │  │
│  │   Pass 2: assets/runner-base.apk (Isolated)      │  │
│  │   Pass 3: AndroidManifest.xml Binary Patch       │  │
│  │   Pass 4: Inject bytecode & source payload       │  │
│  └─────────────────────────┬────────────────────────┘  │
│                            │                           │
│                            ▼                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │         ApkSignerHelper (Android SDK)            │  │
│  │   • v1 Scheme (JAR signing)                      │  │
│  │   • v2 Scheme (APK signing block)                │  │
│  │   • v3 Scheme (Android 9+ rotation block)        │  │
│  │   • ApkVerifier diagnostic report generation     │  │
│  └──────────────────────────────────────────────────┘  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Diagnostic Report & Export Result          │
│   • APK File Path (Downloads directory)                │
│   • Total Entries Processed & Deduplicated Count       │
│   • Signature Compliance Verification Summary          │
└────────────────────────────────────────────────────────┘
```

### Key Data Entities
- **`ApkBuildDiagnostics`**: Tracks `totalEntries`, `addedEntries`, `skippedDuplicates: List<String>`, `modifiedEntries: List<String>`, and `timingMs`.
- **`ApkExportResult`**: Contains generated `file: File`, `fileSizeFormatted: String`, `publicPath: String`, `diagnostics: ApkBuildDiagnostics`, and `validationReport: ApkValidationReport?`.
- **`CaseInsensitiveEntryRegistry`**: Internal helper encapsulating path sanitation, directory filtering, and duplicate suppression.

### Planned Verification & Testing Steps
1. Verify `StandaloneApkBuilder` unit test with sample BASIC program in local test suite (`gradle :app:testDebugUnitTest`).
2. Verify package compilation and absence of duplicate archive conflicts using `compile_applet`.
3. Test edge cases:
   - Multiple entries differing only in case or trailing slashes.
   - Clean reset when primary base APK stream throws an error.
   - Proper formatting of error message when an invalid entry is encountered.
