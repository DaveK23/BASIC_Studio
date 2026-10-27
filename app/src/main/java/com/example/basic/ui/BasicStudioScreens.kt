package com.example.basic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.TextRange
import com.example.basic.docs.BasicDocHelper
import com.example.basic.docs.DocEntry
import com.example.basic.file.BasicFileInfo
import com.example.basic.file.BasicFileManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.basic.apk.StandaloneApkBuilder
import com.example.basic.editor.BasicSample
import com.example.basic.editor.BasicSyntaxHighlighter
import com.example.basic.editor.SamplePrograms
import com.example.basic.vm.TerminalLine
import com.example.basic.vm.VmState

// CRT Retro Colors
val TerminalBg = Color(0xFF090D16)
val TerminalCyan = Color(0xFF38BDF8)
val TerminalGreen = Color(0xFF4ADE80)
val TerminalAmber = Color(0xFFFBBF24)
val DarkSurface = Color(0xFF131B2E)
val CardBorder = Color(0xFF1E293B)

val RetroPalette = listOf(
    Color(0xFF38BDF8), // 0: Cyan
    Color(0xFF60A5FA), // 1: Blue
    Color(0xFF4ADE80), // 2: Green
    Color(0xFFA3E635), // 3: Lime
    Color(0xFFF87171), // 4: Red
    Color(0xFFF472B6), // 5: Magenta
    Color(0xFFFBBF24), // 6: Brown/Amber
    Color(0xFFCBD5E1), // 7: Light gray
    Color(0xFF64748B), // 8: Dark gray
    Color(0xFF93C5FD), // 9: Bright blue
    Color(0xFF86EFAC), // 10: Bright green
    Color(0xFF67E8F9), // 11: Bright cyan
    Color(0xFFFCA5A5), // 12: Bright red
    Color(0xFFF0ABFC), // 13: Bright pink
    Color(0xFFFEF08A), // 14: Yellow
    Color(0xFFFFFFFF)  // 15: White
)

enum class AppTab {
    EDITOR, TERMINAL, BYTECODE, DEBUGGER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicStudioApp(viewModel: BasicViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val variables by viewModel.variables.collectAsState()
    val callStack by viewModel.callStack.collectAsState()
    val currentIp by viewModel.currentIp.collectAsState()
    val currentLine by viewModel.currentLine.collectAsState()
    val isApkBuilding by viewModel.isApkBuilding.collectAsState()
    val apkExportResult by viewModel.apkExportResult.collectAsState()

    var activeTab by remember { mutableStateOf(AppTab.EDITOR) }
    var showSamplesDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showFileManagerDialog by remember { mutableStateOf(false) }
    var showDocsDialog by remember { mutableStateOf(false) }
    var docInitialAnchor by remember { mutableStateOf<String?>(null) }

    val currentFileName by viewModel.currentFileName.collectAsState()
    val savedFiles by viewModel.savedFiles.collectAsState()
    val fileMessage by viewModel.fileMessage.collectAsState()

    val context = LocalContext.current
    val monoFont = remember {
        try {
            FontFamily(Font(R.font.jetbrains_mono))
        } catch (e: Exception) {
            FontFamily.Monospace
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.loadFromUri(context, it)
            activeTab = AppTab.EDITOR
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            viewModel.saveToUri(context, it)
        }
    }

    LaunchedEffect(fileMessage) {
        fileMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFileMessage()
        }
    }

    LaunchedEffect(uiState.lastCompilationError) {
        uiState.lastCompilationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Auto-switch to Terminal when running or waiting for input
    LaunchedEffect(uiState.vmState) {
        if (uiState.vmState == VmState.RUNNING || uiState.vmState == VmState.WAITING_FOR_INPUT) {
            if (activeTab != AppTab.DEBUGGER) {
                activeTab = AppTab.TERMINAL
            }
        }
    }

    // Auto-switch to Debugger when a breakpoint is hit
    LaunchedEffect(Unit) {
        viewModel.breakpointHitEvent.collect {
            activeTab = AppTab.DEBUGGER
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = TerminalBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (uiState.vmState) {
                                        VmState.RUNNING -> TerminalGreen
                                        VmState.PAUSED_DEBUG -> TerminalAmber
                                        VmState.WAITING_FOR_INPUT -> Color(0xFFF472B6)
                                        VmState.ERROR -> Color(0xFFEF4444)
                                        else -> Color(0xFF64748B)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontFamily = monoFont,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${uiState.vmState.name}]",
                            fontFamily = monoFont,
                            fontSize = 11.sp,
                            color = TerminalCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                ),
                actions = {
                    // File Manager button
                    IconButton(
                        onClick = {
                            viewModel.refreshSavedFiles(context)
                            showFileManagerDialog = true
                        },
                        modifier = Modifier.testTag("file_manager_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.file_manager),
                            tint = TerminalCyan
                        )
                    }

                    // Samples button
                    IconButton(
                        onClick = { showSamplesDialog = true },
                        modifier = Modifier.testTag("samples_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = stringResource(R.string.samples),
                            tint = Color.White
                        )
                    }

                    // Run Button
                    IconButton(
                        onClick = {
                            viewModel.runProgram()
                            activeTab = AppTab.TERMINAL
                        },
                        modifier = Modifier.testTag("run_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.run_action),
                            tint = TerminalGreen
                        )
                    }

                    // Debug Button
                    IconButton(
                        onClick = {
                            viewModel.startDebugging()
                            activeTab = AppTab.DEBUGGER
                        },
                        modifier = Modifier.testTag("debug_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = stringResource(R.string.debug_action),
                            tint = TerminalAmber
                        )
                    }

                    // Step Button
                    if (uiState.vmState == VmState.PAUSED_DEBUG) {
                        IconButton(
                            onClick = { viewModel.stepLine() },
                            modifier = Modifier.testTag("step_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.step_action),
                                tint = TerminalCyan
                            )
                        }
                    }

                    // Stop Button
                    if (uiState.vmState == VmState.RUNNING || uiState.vmState == VmState.PAUSED_DEBUG || uiState.vmState == VmState.WAITING_FOR_INPUT) {
                        IconButton(
                            onClick = { viewModel.stopProgram() },
                            modifier = Modifier.testTag("stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop_action),
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }

                    // Export APK Button
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_apk_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = stringResource(R.string.export_apk_action),
                            tint = TerminalCyan
                        )
                    }

                    // Help Documentation Button
                    IconButton(
                        onClick = {
                            docInitialAnchor = null
                            showDocsDialog = true
                        },
                        modifier = Modifier.testTag("help_docs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = stringResource(R.string.help_action),
                            tint = TerminalCyan
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExecutionToolbar(
                    vmState = uiState.vmState,
                    currentLine = currentLine,
                    monoFont = monoFont,
                    onStep = {
                        viewModel.stepLine()
                    },
                    onPause = {
                        viewModel.pauseProgram()
                        activeTab = AppTab.DEBUGGER
                    },
                    onResume = {
                        viewModel.resumeExecution()
                    },
                    onStop = {
                        viewModel.stopProgram()
                    }
                )
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 6.dp
                ) {
                NavigationBarItem(
                    selected = activeTab == AppTab.EDITOR,
                    onClick = { activeTab = AppTab.EDITOR },
                    icon = { Icon(Icons.Default.Code, contentDescription = stringResource(R.string.editor_tab)) },
                    label = { Text(stringResource(R.string.editor_tab), fontFamily = monoFont) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerminalCyan,
                        selectedTextColor = TerminalCyan,
                        indicatorColor = DarkSurface
                    ),
                    modifier = Modifier.testTag("tab_editor")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.TERMINAL,
                    onClick = { activeTab = AppTab.TERMINAL },
                    icon = {
                        if (uiState.vmState == VmState.WAITING_FOR_INPUT) {
                            BadgedBox(badge = { Badge { Text("?") } }) {
                                Icon(Icons.Default.Terminal, contentDescription = stringResource(R.string.terminal_tab))
                            }
                        } else {
                            Icon(Icons.Default.Terminal, contentDescription = stringResource(R.string.terminal_tab))
                        }
                    },
                    label = { Text(stringResource(R.string.terminal_tab), fontFamily = monoFont) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerminalGreen,
                        selectedTextColor = TerminalGreen,
                        indicatorColor = DarkSurface
                    ),
                    modifier = Modifier.testTag("tab_terminal")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.BYTECODE,
                    onClick = {
                        viewModel.ensureBytecodeCompiled(resetVm = false)
                        activeTab = AppTab.BYTECODE
                    },
                    icon = { Icon(Icons.Default.Memory, contentDescription = stringResource(R.string.bytecode_tab)) },
                    label = { Text(stringResource(R.string.bytecode_tab), fontFamily = monoFont) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerminalAmber,
                        selectedTextColor = TerminalAmber,
                        indicatorColor = DarkSurface
                    ),
                    modifier = Modifier.testTag("tab_bytecode")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.DEBUGGER,
                    onClick = { activeTab = AppTab.DEBUGGER },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = stringResource(R.string.debugger_tab)) },
                    label = { Text(stringResource(R.string.debugger_tab), fontFamily = monoFont) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF472B6),
                        selectedTextColor = Color(0xFFF472B6),
                        indicatorColor = DarkSurface
                    ),
                    modifier = Modifier.testTag("tab_debugger")
                )
            }
        }
    }
) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                AppTab.EDITOR -> EditorScreen(
                    viewModel = viewModel,
                    monoFont = monoFont,
                    currentDebugLine = if (uiState.vmState == VmState.PAUSED_DEBUG) currentLine else null,
                    onOpenFilePicker = {
                        openDocumentLauncher.launch(arrayOf("text/*", "*/*"))
                    },
                    onOpenFileManager = {
                        viewModel.refreshSavedFiles(context)
                        showFileManagerDialog = true
                    },
                    onOpenDocs = { anchor ->
                        docInitialAnchor = anchor
                        showDocsDialog = true
                    }
                )
                AppTab.TERMINAL -> TerminalScreen(
                    viewModel = viewModel,
                    terminalOutput = terminalOutput,
                    vmState = uiState.vmState,
                    monoFont = monoFont
                )
                AppTab.BYTECODE -> BytecodeScreen(
                    viewModel = viewModel,
                    currentIp = currentIp,
                    monoFont = monoFont
                )
                AppTab.DEBUGGER -> DebuggerScreen(
                    viewModel = viewModel,
                    variables = variables,
                    callStack = callStack,
                    currentIp = currentIp,
                    currentLine = currentLine,
                    vmState = uiState.vmState,
                    monoFont = monoFont
                )
            }
        }
    }

    val lastExportedApk by viewModel.lastExportedApk.collectAsState()

    // Dialog for Sample Programs
    if (showSamplesDialog) {
        SamplesDialog(
            samples = SamplePrograms.SAMPLES,
            onSelect = { sample ->
                viewModel.loadCode(sample.code)
                showSamplesDialog = false
            },
            onDismiss = { showSamplesDialog = false },
            monoFont = monoFont
        )
    }

    // Dialog for APK Export
    if (showExportDialog) {
        val selectedIconIndex by viewModel.selectedIconIndex.collectAsState()
        ExportApkDialog(
            isBuilding = isApkBuilding,
            exportResult = apkExportResult,
            apkFile = lastExportedApk,
            selectedIconIndex = selectedIconIndex,
            onSelectIcon = { viewModel.setSelectedIconIndex(it) },
            onExport = { appName, pkgName ->
                viewModel.exportApk(context, appName, pkgName)
            },
            onInstall = { file ->
                StandaloneApkBuilder.openApkInstaller(context, file)
            },
            onShare = { file ->
                StandaloneApkBuilder.shareApk(context, file)
            },
            onDismiss = {
                showExportDialog = false
                viewModel.clearExportResult()
            },
            monoFont = monoFont
        )
    }

    // Dialog for File Management
    if (showFileManagerDialog) {
        FileManagerDialog(
            currentFileName = currentFileName,
            savedFiles = savedFiles,
            onSaveToDevice = { name ->
                viewModel.saveCurrentProgram(context, name)
            },
            onSaveAsSaf = { name ->
                val safeName = if (name.endsWith(".bas", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true)) name else "$name.bas"
                createDocumentLauncher.launch(safeName)
            },
            onOpenFromSaf = {
                openDocumentLauncher.launch(arrayOf("text/*", "*/*"))
            },
            onLoadFile = { fileInfo ->
                viewModel.loadProgramFile(fileInfo.file)
                activeTab = AppTab.EDITOR
                showFileManagerDialog = false
            },
            onShareFile = { fileInfo ->
                BasicFileManager.shareProgramFile(context, fileInfo.file)
            },
            onDeleteFile = { fileInfo ->
                viewModel.deleteProgramFile(context, fileInfo.file)
            },
            onNewProgram = {
                viewModel.newProgram()
                activeTab = AppTab.EDITOR
                showFileManagerDialog = false
            },
            onDismiss = { showFileManagerDialog = false },
            monoFont = monoFont
        )
    }

    // Dialog for HTML Documentation Viewer
    if (showDocsDialog) {
        BasicDocViewerDialog(
            initialAnchor = docInitialAnchor,
            onDismiss = { showDocsDialog = false },
            monoFont = monoFont
        )
    }
}

/**
 * Utility to calculate cursor line positions and target scroll offsets to keep the
 * cursor centered vertically in the editor viewport when the soft keyboard appears.
 */
object EditorScrollHelper {
    fun calculateCursorLineIndex(text: String, cursorPosition: Int): Int {
        val safePos = cursorPosition.coerceIn(0, text.length)
        return text.substring(0, safePos).count { it == '\n' }
    }

    fun calculateTargetScrollY(
        cursorLineIndex: Int,
        lineHeightPx: Float,
        topPaddingPx: Float,
        viewportHeightPx: Int
    ): Int {
        if (viewportHeightPx <= 0) return 0
        val lineCenterY = topPaddingPx + (cursorLineIndex * lineHeightPx) + (lineHeightPx / 2f)
        return (lineCenterY - (viewportHeightPx / 2f)).roundToInt().coerceAtLeast(0)
    }
}

@Composable
fun EditorScreen(
    viewModel: BasicViewModel,
    monoFont: FontFamily,
    currentDebugLine: Int?,
    onOpenFilePicker: () -> Unit = {},
    onOpenFileManager: () -> Unit = {},
    onOpenDocs: (anchor: String?) -> Unit = {}
) {
    val code by viewModel.sourceCode.collectAsState()
    val breakpoints by viewModel.breakpoints.collectAsState()
    val lines = remember(code) { code.lines() }
    val currentFileName by viewModel.currentFileName.collectAsState()
    val context = LocalContext.current

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = code, selection = TextRange(code.length)))
    }

    LaunchedEffect(code) {
        if (code != textFieldValue.text) {
            val safePos = textFieldValue.selection.start.coerceIn(0, code.length)
            textFieldValue = textFieldValue.copy(
                text = code,
                selection = TextRange(safePos)
            )
        }
    }

    val currentDocEntry = remember(textFieldValue.text, textFieldValue.selection) {
        val cursor = textFieldValue.selection.start
        BasicDocHelper.resolveEntryAtCursor(textFieldValue.text, cursor)
    }

    fun insertAtCursor(snippet: String) {
        val selStart = textFieldValue.selection.start
        val selEnd = textFieldValue.selection.end
        val currentText = textFieldValue.text
        val newText = currentText.replaceRange(selStart, selEnd, snippet)
        val newPos = selStart + snippet.length
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newPos))
        viewModel.updateCode(newText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // File bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("editor_file_bar"),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenFileManager() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TerminalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentFileName,
                        color = Color.White,
                        fontFamily = monoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Context-Sensitive Help Button
                    if (currentDocEntry != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0369A1).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, TerminalCyan),
                            modifier = Modifier
                                .clickable { onOpenDocs(currentDocEntry.anchor) }
                                .testTag("editor_context_help_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = stringResource(R.string.quick_help),
                                    tint = TerminalCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentDocEntry.keyword,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { onOpenDocs(null) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("editor_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = stringResource(R.string.help_action),
                                tint = TerminalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // New Button
                    IconButton(
                        onClick = { viewModel.newProgram() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_new_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_program),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save Button
                    IconButton(
                        onClick = { viewModel.saveCurrentProgram(context) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.save_action),
                            tint = TerminalGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Files Button
                    IconButton(
                        onClick = onOpenFileManager,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.file_manager),
                            tint = TerminalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Quick syntax helper toolbar (horizontally scrollable with docs shortcut)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Reference Manual Button
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0369A1),
                border = BorderStroke(1.dp, TerminalCyan),
                modifier = Modifier
                    .clickable { onOpenDocs(null) }
                    .testTag("quick_doc_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DOCS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = monoFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val helpers = listOf(
                "PRINT", "INPUT", "LET", "IF", "THEN", "ELSE",
                "FOR", "TO", "STEP", "NEXT", "WHILE", "WEND",
                "GOTO", "GOSUB", "RETURN", "DIM", "CLS", "COLOR",
                "SCREEN", "PSET", "LINE", "RECT", "CIRCLE",
                "SOUND", "SFX", "SPRITEDEF", "SPRITEDEFMULTI", "SPRITE", "SPRITEMOVE", "SPRITEPOS",
                "JOYSTICK", "JOY", "FIRE",
                "RND", "INT", "SIN", "COS", "SLEEP", "END"
            )
            helpers.forEach { keyword ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable {
                        insertAtCursor("$keyword ")
                    }
                ) {
                    Text(
                        text = keyword,
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontFamily = monoFont,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Code editor area with line numbers and breakpoint toggling
        val lineHeight = 24.sp
        val density = LocalDensity.current
        val lineHeightDp = with(density) { lineHeight.toDp() }
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        var editorViewportHeightPx by remember { mutableIntStateOf(0) }

        @OptIn(ExperimentalLayoutApi::class)
        val isImeVisible = WindowInsets.isImeVisible
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val isKeyboardOpen = isImeVisible || imeBottomPx > 0

        // Find current 0-indexed line of cursor in text
        val cursorLineIndex = remember(textFieldValue.text, textFieldValue.selection.start) {
            EditorScrollHelper.calculateCursorLineIndex(textFieldValue.text, textFieldValue.selection.start)
        }

        // When on-screen keyboard pops up or cursor moves to a different line while keyboard is visible,
        // scroll the text view to keep the cursor position centred vertically in the visible editor viewport.
        LaunchedEffect(isKeyboardOpen, editorViewportHeightPx, cursorLineIndex) {
            if (isKeyboardOpen && editorViewportHeightPx > 0) {
                delay(60) // Allow layout pass and soft keyboard transition to stabilize
                val lineHeightPx = with(density) { lineHeightDp.toPx() }
                val topPaddingPx = with(density) { 8.dp.toPx() }
                val targetScrollY = EditorScrollHelper.calculateTargetScrollY(
                    cursorLineIndex = cursorLineIndex,
                    lineHeightPx = lineHeightPx,
                    topPaddingPx = topPaddingPx,
                    viewportHeightPx = editorViewportHeightPx
                )
                verticalScrollState.animateScrollTo(targetScrollY)
            }
        }

        // Auto-scroll to current debug line during debugging
        LaunchedEffect(currentDebugLine) {
            currentDebugLine?.let { line ->
                if (line > 0 && lines.isNotEmpty()) {
                    val lineHeightPx = with(density) { lineHeightDp.toPx() }
                    val targetY = ((line - 1) * lineHeightPx - 160).toInt().coerceAtLeast(0)
                    verticalScrollState.animateScrollTo(targetY)
                }
            }
        }

        val extraBottomPaddingDp = with(density) {
            if (editorViewportHeightPx > 0) {
                maxOf(32.dp, (editorViewportHeightPx / 2).toDp())
            } else {
                32.dp
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBg)
                .onSizeChanged { size ->
                    if (size.height > 0) {
                        editorViewportHeightPx = size.height
                    }
                }
        ) {
            // Persistent background gutter panel so left 48.dp is always dark slate full height
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(Color(0xFF0F172A))
            )

            // Gutter vertical separator line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 48.dp)
                    .width(1.dp)
                    .background(CardBorder)
            )

            // Unified vertically-scrollable Row: line numbers gutter + code editor ALWAYS scroll in unison
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Line numbers / Breakpoints gutter column
                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .padding(top = 8.dp, bottom = extraBottomPaddingDp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    lines.forEachIndexed { index, _ ->
                        val lineNum = index + 1
                        val hasBreakpoint = breakpoints.contains(lineNum)
                        val isCurrent = currentDebugLine == lineNum

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(lineHeightDp)
                                .clickable { viewModel.toggleBreakpoint(lineNum) }
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (hasBreakpoint) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            } else if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Current debug line",
                                    tint = TerminalAmber,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(8.dp))
                            }

                            Text(
                                text = "$lineNum",
                                color = if (isCurrent) TerminalAmber else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontFamily = monoFont,
                                style = TextStyle(
                                    fontFamily = monoFont,
                                    fontSize = 11.sp,
                                    fontFeatureSettings = BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES
                                )
                            )
                        }
                    }
                }

                // Code input field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                        .padding(top = 8.dp, bottom = extraBottomPaddingDp, start = 8.dp, end = 16.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != code) {
                                viewModel.updateCode(newValue.text)
                            }
                        },
                        visualTransformation = { text ->
                            TransformedText(
                                BasicSyntaxHighlighter.highlight(text.text, monoFont),
                                OffsetMapping.Identity
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = monoFont,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = lineHeight,
                            fontFeatureSettings = BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        cursorBrush = SolidColor(TerminalCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_editor_field")
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalScreen(
    viewModel: BasicViewModel,
    terminalOutput: List<TerminalLine>,
    vmState: VmState,
    monoFont: FontFamily
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(terminalOutput.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // CRT Terminal header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.terminal_title),
                    color = Color.White,
                    fontFamily = monoFont,
                    fontSize = 12.sp
                )
            }
            val showSpriteDebugOverlay by viewModel.showSpriteDebugOverlay.collectAsState()
            val selectedDebugSpriteId by viewModel.selectedDebugSpriteId.collectAsState()

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Debug Hitboxes Toggle
                FilterChip(
                    selected = showSpriteDebugOverlay,
                    onClick = { viewModel.toggleSpriteDebugOverlay() },
                    label = {
                        Text(
                            text = if (showSpriteDebugOverlay) stringResource(R.string.sprite_hitboxes_on) else stringResource(R.string.sprite_hitboxes_off),
                            fontSize = 11.sp,
                            fontFamily = monoFont
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = stringResource(R.string.sprite_hitboxes),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TerminalAmber.copy(alpha = 0.2f),
                        selectedLabelColor = TerminalAmber,
                        selectedLeadingIconColor = TerminalAmber,
                        containerColor = DarkSurface,
                        labelColor = Color(0xFF94A3B8),
                        iconColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showSpriteDebugOverlay,
                        borderColor = Color(0xFF475569),
                        selectedBorderColor = TerminalAmber
                    ),
                    modifier = Modifier
                        .testTag("toggle_sprite_hitbox_overlay")
                        .padding(end = 6.dp)
                )

                TextButton(
                    onClick = { viewModel.clearTerminal() },
                    modifier = Modifier.testTag("clear_terminal_button")
                ) {
                    Text(
                        text = stringResource(R.string.clear_action),
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontFamily = monoFont
                    )
                }
            }
        }

        val graphicsState by viewModel.graphicsState.collectAsState()
        val showSpriteDebugOverlay by viewModel.showSpriteDebugOverlay.collectAsState()
        val selectedDebugSpriteId by viewModel.selectedDebugSpriteId.collectAsState()

        // If SCREEN mode > 0 or active sprites exist, show Retro 320x200 Graphics Canvas with Sprites & Hitboxes
        if (graphicsState.mode > 0 || graphicsState.sprites.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                com.example.basic.graphics.RetroGraphicsCanvas(
                    graphicsState = graphicsState,
                    showDebugBoundingBoxes = showSpriteDebugOverlay,
                    selectedSpriteId = selectedDebugSpriteId,
                    modifier = Modifier.fillMaxSize()
                )

                // 8-Way Virtual Joystick and Fire Button overlay on bitmap display (SCREEN 1)
                if (graphicsState.showJoystick) {
                    com.example.basic.graphics.RetroControllerOverlay(
                        onJoystickChange = { dir, x, y -> viewModel.updateJoystick(dir, x, y) },
                        onFireChange = { pressed -> viewModel.updateFireButton(pressed) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Terminal output screen
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(if (graphicsState.mode > 0 || graphicsState.sprites.isNotEmpty()) 0.8f else 1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("terminal_output_list")
        ) {
            items(terminalOutput) { line ->
                val annotatedString = buildAnnotatedString {
                    if (line.spans.isNotEmpty()) {
                        for (span in line.spans) {
                            val spanColor = when {
                                span.isError -> Color(0xFFEF4444)
                                span.isSystemNotice -> TerminalCyan
                                span.isInputEcho -> Color(0xFFFBBF24)
                                span.colorIndex in RetroPalette.indices -> RetroPalette[span.colorIndex]
                                else -> TerminalGreen
                            }
                            withStyle(SpanStyle(color = spanColor)) {
                                append(span.text.trimEnd('\r', '\n'))
                            }
                        }
                    } else {
                        val color = when {
                            line.isError -> Color(0xFFEF4444)
                            line.isSystemNotice -> TerminalCyan
                            line.isInputEcho -> Color(0xFFFBBF24)
                            line.colorIndex in RetroPalette.indices -> RetroPalette[line.colorIndex]
                            else -> TerminalGreen
                        }
                        withStyle(SpanStyle(color = color)) {
                            append(line.text.trimEnd('\r', '\n'))
                        }
                    }
                }
                val displayText = if (annotatedString.isEmpty()) AnnotatedString(" ") else annotatedString
                Text(
                    text = displayText,
                    fontFamily = monoFont,
                    fontSize = 13.sp,
                    style = TextStyle(
                        fontFamily = monoFont,
                        fontSize = 13.sp,
                        fontFeatureSettings = BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES
                    )
                )
            }
        }

        // Terminal command/input prompt
        if (vmState == VmState.WAITING_FOR_INPUT) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(TerminalAmber))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "?> ",
                        color = TerminalAmber,
                        fontFamily = monoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Enter input for BASIC program…", color = Color(0xFF64748B), fontFamily = monoFont) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_input_field"),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = monoFont,
                            fontFeatureSettings = BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotEmpty()) {
                                viewModel.provideTerminalInput(inputText)
                                inputText = ""
                            }
                        }),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedIndicatorColor = TerminalAmber
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            viewModel.provideTerminalInput(inputText)
                            inputText = ""
                        },
                        modifier = Modifier.testTag("terminal_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send), tint = TerminalAmber)
                    }
                }
            }
        }
    }
}

@Composable
fun BytecodeScreen(
    viewModel: BasicViewModel,
    currentIp: Int,
    monoFont: FontFamily
) {
    val program by viewModel.compiledProgram.collectAsState()
    val compilationError by viewModel.lastCompilationError.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(currentIp, program) {
        val p = program
        if (p != null && currentIp in 0 until p.instructions.size) {
            val targetIndex = maxOf(0, currentIp - 2)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(12.dp)
    ) {
        if (compilationError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1219)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = compilationError ?: "",
                        color = Color(0xFFFCA5A5),
                        fontFamily = monoFont,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (program != null) {
            val p = program!!
            Text(
                text = "Compiled Instructions: ${p.instructions.size} | Constants: ${p.constants.size}",
                color = TerminalCyan,
                fontFamily = monoFont,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bytecode_list")
            ) {
                items(p.instructions.size) { index ->
                    val inst = p.instructions[index]
                    val isCurrent = index == currentIp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrent) Color(0xFF1E3A8A) else Color.Transparent)
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCurrent) "▶" else " ",
                            color = TerminalAmber,
                            fontFamily = monoFont,
                            fontSize = 12.sp,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            text = inst.toDisassembly(index, p.constants),
                            color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                            fontFamily = monoFont,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Click RUN or COMPILE to generate bytecode", color = Color(0xFF64748B), fontFamily = monoFont)
            }
        }
    }
}

@Composable
fun DebuggerScreen(
    viewModel: BasicViewModel,
    variables: Map<String, Any>,
    callStack: List<Any>,
    currentIp: Int,
    currentLine: Int,
    vmState: VmState,
    monoFont: FontFamily
) {
    val graphicsState by viewModel.graphicsState.collectAsState()
    val showSpriteDebugOverlay by viewModel.showSpriteDebugOverlay.collectAsState()
    val selectedDebugSpriteId by viewModel.selectedDebugSpriteId.collectAsState()

    var watchTab by remember { mutableIntStateOf(if (graphicsState.sprites.isNotEmpty()) 1 else 0) }

    LaunchedEffect(graphicsState.sprites.size) {
        if (graphicsState.sprites.isNotEmpty() && watchTab == 0 && variables.isEmpty()) {
            watchTab = 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(12.dp)
    ) {
        // Debug Control Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Instruction Pointer (IP): #$currentIp",
                            color = TerminalCyan,
                            fontFamily = monoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Source Code Line: $currentLine",
                            color = TerminalAmber,
                            fontFamily = monoFont,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { viewModel.stepLine() },
                            enabled = vmState == VmState.PAUSED_DEBUG,
                            modifier = Modifier
                                .testTag("debugger_step_line_button")
                                .padding(end = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = TerminalAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(stringResource(R.string.step_line), fontFamily = monoFont, color = TerminalAmber, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.stepInstruction() },
                            enabled = vmState == VmState.PAUSED_DEBUG,
                            modifier = Modifier
                                .testTag("debugger_step_inst_button")
                                .padding(end = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = TerminalCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(stringResource(R.string.step_instruction), fontFamily = monoFont, color = TerminalCyan, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.resumeExecution() },
                            enabled = vmState == VmState.PAUSED_DEBUG,
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, TerminalGreen),
                            modifier = Modifier.testTag("debugger_resume_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Resume", fontFamily = monoFont, color = TerminalGreen, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary status bar with Hitbox overlay toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showSpriteDebugOverlay,
                        onClick = { viewModel.toggleSpriteDebugOverlay() },
                        label = {
                            Text(
                                text = if (showSpriteDebugOverlay) stringResource(R.string.sprite_hitboxes_on) else stringResource(R.string.sprite_hitboxes_off),
                                fontSize = 10.sp,
                                fontFamily = monoFont
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TerminalAmber.copy(alpha = 0.2f),
                            selectedLabelColor = TerminalAmber,
                            selectedLeadingIconColor = TerminalAmber
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = showSpriteDebugOverlay,
                            borderColor = Color(0xFF475569),
                            selectedBorderColor = TerminalAmber
                        ),
                        modifier = Modifier.testTag("debugger_toggle_hitbox_overlay")
                    )

                    Text(
                        text = "${graphicsState.sprites.size} sprites active in VM",
                        color = if (graphicsState.sprites.isNotEmpty()) TerminalCyan else Color.Gray,
                        fontFamily = monoFont,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Watch Window Navigation Tabs
        TabRow(
            selectedTabIndex = watchTab,
            containerColor = DarkSurface,
            contentColor = TerminalCyan,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            Tab(
                selected = watchTab == 0,
                onClick = { watchTab = 0 },
                text = { Text("Variables (${variables.size})", fontFamily = monoFont, fontSize = 11.sp) },
                modifier = Modifier.testTag("watch_tab_variables")
            )
            Tab(
                selected = watchTab == 1,
                onClick = { watchTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tab_inspect_sprite), fontFamily = monoFont, fontSize = 11.sp)
                        if (graphicsState.sprites.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TerminalGreen)
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("watch_tab_inspect_sprite")
            )
            Tab(
                selected = watchTab == 2,
                onClick = { watchTab = 2 },
                text = { Text("Call Stack (${callStack.size})", fontFamily = monoFont, fontSize = 11.sp) },
                modifier = Modifier.testTag("watch_tab_call_stack")
            )
            Tab(
                selected = watchTab == 3,
                onClick = { watchTab = 3 },
                text = { Text(stringResource(R.string.tab_canvas_preview), fontFamily = monoFont, fontSize = 11.sp) },
                modifier = Modifier.testTag("watch_tab_canvas_preview")
            )
        }

        // Watch Window Content
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            when (watchTab) {
                0 -> {
                    // TAB 0: VARIABLES WATCH
                    if (variables.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No variables initialized yet", color = Color(0xFF64748B), fontFamily = monoFont)
                                if (graphicsState.sprites.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { watchTab = 1 },
                                        modifier = Modifier.testTag("jump_to_sprite_inspector_button")
                                    ) {
                                        Icon(Icons.Default.Widgets, contentDescription = null, tint = TerminalCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Inspect ${graphicsState.sprites.size} Active Sprites ->", fontFamily = monoFont, color = TerminalCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            LazyColumn(modifier = Modifier.weight(1f).testTag("variables_watch_list")) {
                                items(variables.toList()) { (name, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = name,
                                            color = TerminalCyan,
                                            fontFamily = monoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = value.toString(),
                                            color = TerminalGreen,
                                            fontFamily = monoFont,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            if (graphicsState.sprites.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { watchTab = 1 },
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("inspect_sprites_quick_button")
                                ) {
                                    Icon(Icons.Default.Widgets, contentDescription = null, tint = TerminalAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inspect Active Sprites (${graphicsState.sprites.size}) ->", fontFamily = monoFont, color = TerminalAmber, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: INSPECT SPRITE (Coordinates, Bounding Box, Collision Mask, SPRITEHIT Status)
                    if (graphicsState.sprites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.no_sprites_defined),
                                    color = Color.White,
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Define sprites using SPRITEDEF and SPRITE to inspect their coordinates, dimensions, bounding boxes, and collision masks.",
                                    color = Color(0xFF94A3B8),
                                    fontFamily = monoFont,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        val activeSprites = graphicsState.sprites
                        val selectedSprite = activeSprites[selectedDebugSpriteId]
                            ?: activeSprites.values.firstOrNull()
                            ?: com.example.basic.graphics.Sprite(id = 0)

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            // Sprite Selector Horizontal Chips
                            Text(
                                text = "Select Sprite Slot to Inspect:",
                                color = Color.White,
                                fontFamily = monoFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("sprite_selector_row"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for ((id, s) in activeSprites) {
                                    val isSelected = (s.id == selectedSprite.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectDebugSprite(id) },
                                        label = {
                                            Text(
                                                text = "Sprite #$id: (${s.x},${s.y})",
                                                fontFamily = monoFont,
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (s.visible) TerminalGreen else Color.Gray)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TerminalCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = TerminalCyan,
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFCBD5E1)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = Color(0xFF334155),
                                            selectedBorderColor = TerminalCyan
                                        ),
                                        modifier = Modifier.testTag("sprite_chip_$id")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Main Sprite Inspector Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sprite_inspector_card")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    // Inspector Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(TerminalGreen)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "SPRITE #${selectedSprite.id}",
                                                color = TerminalCyan,
                                                fontFamily = monoFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${selectedSprite.width}x${selectedSprite.height}",
                                                    color = TerminalAmber,
                                                    fontFamily = monoFont,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = if (selectedSprite.visible) Color(0x2210B981) else Color(0x22EF4444),
                                            border = BorderStroke(1.dp, if (selectedSprite.visible) TerminalGreen else Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (selectedSprite.visible) "VISIBLE (HITBOX ON)" else "HIDDEN (NO HIT)",
                                                color = if (selectedSprite.visible) TerminalGreen else Color(0xFFEF4444),
                                                fontFamily = monoFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Metric Rows (X, Y, Width, Height, Ranges)
                                    Text(
                                        text = "COORDINATES & SPRITEHIT RANGES",
                                        color = Color(0xFF94A3B8),
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E293B))
                                            .padding(8.dp)
                                    ) {
                                        SpriteMetricRow(label = "X Position", value = "${selectedSprite.x}", monoFont = monoFont)
                                        SpriteMetricRow(label = "Y Position", value = "${selectedSprite.y}", monoFont = monoFont)
                                        SpriteMetricRow(label = "Width", value = "${selectedSprite.width} px", monoFont = monoFont)
                                        SpriteMetricRow(label = "Height", value = "${selectedSprite.height} px", monoFont = monoFont)
                                        SpriteMetricRow(
                                            label = "SPRITEHIT X Range",
                                            value = "[${selectedSprite.x} .. ${selectedSprite.x + selectedSprite.width - 1}] (${selectedSprite.width} px)",
                                            highlight = true,
                                            monoFont = monoFont
                                        )
                                        SpriteMetricRow(
                                            label = "SPRITEHIT Y Range",
                                            value = "[${selectedSprite.y} .. ${selectedSprite.y + selectedSprite.height - 1}] (${selectedSprite.height} px)",
                                            highlight = true,
                                            monoFont = monoFont
                                        )
                                        SpriteMetricRow(
                                            label = "Bounding Box [L,T,R,B]",
                                            value = "[${selectedSprite.x}, ${selectedSprite.y}, ${selectedSprite.x + selectedSprite.width}, ${selectedSprite.y + selectedSprite.height}]",
                                            monoFont = monoFont
                                        )
                                        SpriteMetricRow(
                                            label = "Pattern Def ID",
                                            value = "#${selectedSprite.patternDefId ?: "Default"}",
                                            monoFont = monoFont
                                        )
                                        SpriteMetricRow(
                                            label = "Palette Color Index",
                                            value = "${selectedSprite.colorIndex}",
                                            monoFont = monoFont
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Collision Mask Section
                                    val solidPixels = selectedSprite.pattern.count { it }
                                    val totalPixels = selectedSprite.pattern.size.coerceAtLeast(1)
                                    val solidPercent = (solidPixels * 100) / totalPixels

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.collision_mask),
                                            color = Color(0xFF94A3B8),
                                            fontFamily = monoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "$solidPixels/$totalPixels solid pixels ($solidPercent%)",
                                            color = TerminalCyan,
                                            fontFamily = monoFont,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Visual Collision Mask Grid
                                        SpriteCollisionMaskGrid(
                                            sprite = selectedSprite,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Binary mask row preview
                                        Column(
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1E293B))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = "Pixel Hitmask Rows:",
                                                color = Color.White,
                                                fontFamily = monoFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val w = selectedSprite.width
                                            val h = selectedSprite.height
                                            val pat = selectedSprite.pattern
                                            for (row in 0 until minOf(h, 8)) {
                                                val sb = StringBuilder()
                                                for (col in 0 until w) {
                                                    val bit = (row * w + col)
                                                    sb.append(if (bit in pat.indices && pat[bit]) "1" else "0")
                                                }
                                                Text(
                                                    text = "R$row: $sb",
                                                    fontFamily = monoFont,
                                                    fontSize = 10.sp,
                                                    color = TerminalGreen
                                                )
                                            }
                                            if (h > 8) {
                                                Text(text = "... (${h - 8} more rows)", color = Color.Gray, fontFamily = monoFont, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Active Collision Detection Checks vs Other Sprites
                                    Text(
                                        text = "LIVE SPRITEHIT STATUS VS OTHER SPRITES",
                                        color = Color(0xFF94A3B8),
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val otherSprites = activeSprites.values.filter { it.id != selectedSprite.id }
                                    if (otherSprites.isEmpty()) {
                                        Text(
                                            text = "No other active sprites currently loaded in VM.",
                                            color = Color.Gray,
                                            fontFamily = monoFont,
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            for (other in otherSprites) {
                                                val isHit = selectedSprite.hits(other)
                                                val bgColor = if (isHit) Color(0x33EF4444) else Color(0xFF1E293B)
                                                val borderColor = if (isHit) Color(0xFFEF4444) else Color(0xFF334155)

                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                                    border = BorderStroke(1.dp, borderColor),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Sprite #${other.id} at (${other.x}, ${other.y})",
                                                                color = Color.White,
                                                                fontFamily = monoFont,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                            Surface(
                                                                color = if (isHit) Color(0xFFEF4444) else Color(0xFF334155),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isHit) "SPRITEHIT = 1 (COLLIDING!)" else "SPRITEHIT = 0 (CLEAR)",
                                                                    color = if (isHit) Color.White else Color(0xFF94A3B8),
                                                                    fontFamily = monoFont,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        if (isHit) {
                                                            val ovLeft = maxOf(selectedSprite.x, other.x)
                                                            val ovRight = minOf(selectedSprite.x + selectedSprite.width, other.x + other.width)
                                                            val ovTop = maxOf(selectedSprite.y, other.y)
                                                            val ovBottom = minOf(selectedSprite.y + selectedSprite.height, other.y + other.height)
                                                            Text(
                                                                text = "Overlap Bounds: X: [$ovLeft .. $ovRight], Y: [$ovTop .. $ovBottom] (${ovRight - ovLeft}x${ovBottom - ovTop} px)",
                                                                color = Color(0xFFFCA5A5),
                                                                fontFamily = monoFont,
                                                                fontSize = 11.sp,
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            )
                                                        } else {
                                                            val dx = other.x - selectedSprite.x
                                                            val dy = other.y - selectedSprite.y
                                                            Text(
                                                                text = "Offset: ΔX = $dx px, ΔY = $dy px",
                                                                color = Color(0xFF94A3B8),
                                                                fontFamily = monoFont,
                                                                fontSize = 11.sp,
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Mini Canvas Preview with Bounding Box Overlay
                                    Text(
                                        text = "VISUAL BOUNDING BOX OVERLAY PREVIEW",
                                        color = Color(0xFF94A3B8),
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    com.example.basic.graphics.RetroGraphicsCanvas(
                                        graphicsState = graphicsState,
                                        showDebugBoundingBoxes = true,
                                        selectedSpriteId = selectedSprite.id,
                                        modifier = Modifier.fillMaxWidth().height(160.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: GOSUB CALL STACK
                    if (callStack.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Call stack is empty (main routine)", color = Color(0xFF64748B), fontFamily = monoFont, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            items(callStack) { frame ->
                                Text(
                                    text = frame.toString(),
                                    color = Color(0xFFF472B6),
                                    fontFamily = monoFont,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: FULL CRT SCREEN & HITBOXES PREVIEW
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.basic.graphics.RetroGraphicsCanvas(
                            graphicsState = graphicsState,
                            showDebugBoundingBoxes = showSpriteDebugOverlay,
                            selectedSpriteId = selectedDebugSpriteId,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpriteMetricRow(
    label: String,
    value: String,
    monoFont: FontFamily,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (highlight) TerminalAmber else Color(0xFF94A3B8),
            fontFamily = monoFont,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = if (highlight) TerminalGreen else Color.White,
            fontFamily = monoFont,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}

/**
 * Pixel-by-pixel collision mask grid renderer.
 * Visualizes solid (collidable) vs transparent bits.
 */
@Composable
fun SpriteCollisionMaskGrid(
    sprite: com.example.basic.graphics.Sprite,
    modifier: Modifier = Modifier
) {
    val width = sprite.width
    val height = sprite.height
    val pattern = sprite.pattern

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF020617))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .padding(6.dp)
            .testTag("collision_mask_grid_${sprite.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hitmask [${width}x${height}]",
            color = TerminalCyan,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        for (y in 0 until minOf(height, 16)) {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                for (x in 0 until minOf(width, 16)) {
                    val idx = y * width + x
                    val isSolid = idx in pattern.indices && pattern[idx]
                    Box(
                        modifier = Modifier
                            .size(if (width <= 8) 12.dp else 7.dp)
                            .padding(0.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (isSolid) Color(0xFF22C55E) else Color(0xFF1E293B)
                            )
                            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun FileManagerDialog(
    currentFileName: String,
    savedFiles: List<BasicFileInfo>,
    onSaveToDevice: (String) -> Unit,
    onSaveAsSaf: (String) -> Unit,
    onOpenFromSaf: () -> Unit,
    onLoadFile: (BasicFileInfo) -> Unit,
    onShareFile: (BasicFileInfo) -> Unit,
    onDeleteFile: (BasicFileInfo) -> Unit,
    onNewProgram: () -> Unit,
    onDismiss: () -> Unit,
    monoFont: FontFamily
) {
    var fileNameInput by remember(currentFileName) { mutableStateOf(currentFileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = TerminalCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.file_manager),
                    fontFamily = monoFont,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section: Save Program
                Text(
                    text = "SAVE PROGRAM AS TEXT FILE",
                    fontFamily = monoFont,
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text(stringResource(R.string.filename_label), fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = monoFont, fontSize = 13.sp, color = Color.White),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedIndicatorColor = TerminalCyan,
                        unfocusedIndicatorColor = CardBorder,
                        focusedLabelColor = TerminalCyan,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveToDevice(fileNameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_file_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.save_action),
                            color = Color.Black,
                            fontFamily = monoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { onSaveAsSaf(fileNameInput) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_as_saf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = TerminalCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.save_as_action),
                            color = TerminalCyan,
                            fontFamily = monoFont,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Device / SAF Open & New
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenFromSaf,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_saf_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = TerminalAmber
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.open_file_action),
                            fontFamily = monoFont,
                            fontSize = 11.sp
                        )
                    }

                    FilledTonalButton(
                        onClick = onNewProgram,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_program_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.new_program),
                            fontFamily = monoFont,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Saved on Device
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.saved_programs).uppercase(),
                        fontFamily = monoFont,
                        color = TerminalAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${savedFiles.size} files",
                        fontFamily = monoFont,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (savedFiles.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_saved_programs),
                            color = Color(0xFF94A3B8),
                            fontFamily = monoFont,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    savedFiles.forEach { fileInfo ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("file_item_${fileInfo.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileInfo.name,
                                        color = TerminalCyan,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${fileInfo.formattedSize}  •  ${fileInfo.formattedDate}",
                                        color = Color(0xFF94A3B8),
                                        fontFamily = monoFont,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    // Load Button
                                    IconButton(
                                        onClick = { onLoadFile(fileInfo) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("load_file_button_${fileInfo.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Load ${fileInfo.name}",
                                            tint = TerminalGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Share Button
                                    IconButton(
                                        onClick = { onShareFile(fileInfo) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("share_file_button_${fileInfo.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share ${fileInfo.name}",
                                            tint = TerminalCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { onDeleteFile(fileInfo) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_file_button_${fileInfo.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${fileInfo.name}",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_file_dialog_button")
            ) {
                Text(
                    text = "Close",
                    fontFamily = monoFont,
                    color = TerminalCyan
                )
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun SamplesDialog(
    samples: List<BasicSample>,
    onSelect: (BasicSample) -> Unit,
    onDismiss: () -> Unit,
    monoFont: FontFamily
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Load Sample Program", fontFamily = monoFont, color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(samples) { sample ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(sample) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(sample.title, color = TerminalCyan, fontFamily = monoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(sample.description, color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF090D16),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val previewCode = remember(sample.code) {
                                    val previewText = sample.code.lines().filter { it.isNotBlank() }.take(3).joinToString("\n")
                                    BasicSyntaxHighlighter.highlight(previewText, monoFont)
                                }
                                Text(
                                    text = previewCode,
                                    fontFamily = monoFont,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = monoFont, color = TerminalCyan)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun ExportApkDialog(
    isBuilding: Boolean,
    exportResult: String?,
    apkFile: java.io.File?,
    selectedIconIndex: Int,
    onSelectIcon: (Int) -> Unit,
    onExport: (appName: String, packageName: String) -> Unit,
    onInstall: (java.io.File) -> Unit,
    onShare: (java.io.File) -> Unit,
    onDismiss: () -> Unit,
    monoFont: FontFamily
) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf("MyBasicApp") }
    var packageName by remember { mutableStateOf("com.basic.mybasicapp") }
    val isConflictingPackage = packageName.trim().equals(context.packageName, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Android, contentDescription = null, tint = TerminalGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Standalone APK", fontFamily = monoFont, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Compiles the active BASIC code into bytecode and packages an installable Android APK with binary manifest, 4-byte zip alignment, and official v1/v2/v3 signatures.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Application Name") },
                    textStyle = TextStyle(color = Color.White, fontFamily = monoFont),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("apk_name_field")
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package ID (e.g. com.app.basic)") },
                        textStyle = TextStyle(color = Color.White, fontFamily = monoFont),
                        isError = isConflictingPackage,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("apk_package_field")
                    )
                    if (isConflictingPackage) {
                        Text(
                            text = "Cannot use host app's ID (${context.packageName}). Android rejects conflicting signatures. Use e.g. com.basic.myapp.",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontFamily = monoFont,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Launcher Icon Selector Header
                Text(
                    text = "Select Launcher Icon (${selectedIconIndex + 1}/8):",
                    color = TerminalCyan,
                    fontSize = 12.sp,
                    fontFamily = monoFont
                )

                // 8 Default Launcher Icon Options Grid (4 columns x 2 rows)
                val iconOptions = com.example.basic.apk.RetroIconGenerator.ICONS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0..3) {
                                val idx = row * 4 + col
                                val iconOpt = iconOptions[idx]
                                val isSelected = idx == selectedIconIndex

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectIcon(idx) }
                                        .testTag("launcher_icon_option_$idx"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TerminalAmber else Color(0xFF334155)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Visual thumbnail bitmap
                                        val iconBitmap = remember(idx) {
                                            com.example.basic.apk.RetroIconGenerator.generateIconBitmap(idx, 64)
                                        }
                                        androidx.compose.foundation.Image(
                                            bitmap = iconBitmap.asImageBitmap(),
                                            contentDescription = iconOpt.name,
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = iconOpt.name,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            fontFamily = monoFont,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isBuilding) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = TerminalGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.exporting_apk), color = TerminalGreen, fontFamily = monoFont, fontSize = 12.sp)
                    }
                }

                if (exportResult != null) {
                    val isError = exportResult.startsWith("Build error") ||
                            exportResult.startsWith("Compilation failed") ||
                            exportResult.contains("failed", ignoreCase = true)
                    val cardBg = if (isError) Color(0xFF450A0A) else Color(0xFF064E3B)
                    val textCol = if (isError) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                    val borderCol = if (isError) Color(0xFFEF4444) else Color(0xFF10B981)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = exportResult,
                                color = textCol,
                                fontFamily = monoFont,
                                fontSize = 11.sp
                            )

                            if (apkFile != null && apkFile.exists() && !isError) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onInstall(apkFile) },
                                        modifier = Modifier.weight(1f).testTag("install_apk_button")
                                    ) {
                                        Icon(Icons.Default.Android, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Install", color = TerminalGreen, fontSize = 11.sp, fontFamily = monoFont)
                                    }

                                    OutlinedButton(
                                        onClick = { onShare(apkFile) },
                                        modifier = Modifier.weight(1f).testTag("share_apk_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = TerminalCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share / Save", color = TerminalCyan, fontSize = 11.sp, fontFamily = monoFont)
                                    }
                                }
                                Text(
                                    text = "Tip: If a prior version of this app is installed with a conflicting signature, uninstall it first.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = monoFont,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onExport(appName, packageName) },
                enabled = !isBuilding && !isConflictingPackage && packageName.isNotBlank() && appName.isNotBlank(),
                modifier = Modifier.testTag("build_apk_confirm_button")
            ) {
                Text("Compile & Build APK", fontFamily = monoFont, color = TerminalGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontFamily = monoFont, color = TerminalCyan)
            }
        },
        containerColor = DarkSurface
    )
}

/**
 * Dedicated execution control toolbar docked above the bottom navigation tabs.
 * Provides Step, Pause/Resume, and Stop buttons with live interpreter state indication.
 */
@Composable
fun ExecutionToolbar(
    vmState: VmState,
    currentLine: Int,
    monoFont: FontFamily,
    onStep: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("execution_toolbar"),
        color = Color(0xFF0C1222),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF182238))
                    .border(1.dp, Color(0xFF243354), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("toolbar_status_badge")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (vmState) {
                                VmState.RUNNING -> TerminalGreen
                                VmState.PAUSED_DEBUG -> TerminalAmber
                                VmState.WAITING_FOR_INPUT -> Color(0xFFF472B6)
                                VmState.ERROR -> Color(0xFFEF4444)
                                else -> Color(0xFF64748B)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (vmState) {
                        VmState.RUNNING -> stringResource(R.string.status_running)
                        VmState.PAUSED_DEBUG -> if (currentLine > 0) "${stringResource(R.string.status_paused)} L:$currentLine" else stringResource(R.string.status_paused)
                        VmState.WAITING_FOR_INPUT -> stringResource(R.string.status_input)
                        VmState.TERMINATED -> stringResource(R.string.status_stopped)
                        else -> stringResource(R.string.status_idle)
                    },
                    fontFamily = monoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (vmState) {
                        VmState.RUNNING -> TerminalGreen
                        VmState.PAUSED_DEBUG -> TerminalAmber
                        VmState.WAITING_FOR_INPUT -> Color(0xFFF472B6)
                        VmState.ERROR -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }
                )
            }

            // Controls: Step, Pause/Resume, Stop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Step Button
                FilledTonalButton(
                    onClick = onStep,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("toolbar_step"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = TerminalCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.step_action),
                        modifier = Modifier.size(18.dp),
                        tint = TerminalCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.step_action),
                        fontFamily = monoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pause or Resume Button
                if (vmState == VmState.PAUSED_DEBUG) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("toolbar_resume"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalGreen,
                            contentColor = Color(0xFF090D16)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.resume_action),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.resume_action),
                            fontFamily = monoFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val isRunning = vmState == VmState.RUNNING
                    FilledTonalButton(
                        onClick = onPause,
                        enabled = isRunning,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("toolbar_pause"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isRunning) Color(0xFF45220A) else Color(0xFF1E293B).copy(alpha = 0.5f),
                            contentColor = if (isRunning) TerminalAmber else Color(0xFF64748B),
                            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.3f),
                            disabledContentColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = stringResource(R.string.pause_action),
                            modifier = Modifier.size(18.dp),
                            tint = if (isRunning) TerminalAmber else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.pause_action),
                            fontFamily = monoFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stop Button
                val isStopActive = vmState == VmState.RUNNING || vmState == VmState.PAUSED_DEBUG || vmState == VmState.WAITING_FOR_INPUT
                FilledTonalButton(
                    onClick = onStop,
                    enabled = isStopActive,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("toolbar_stop"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isStopActive) Color(0xFF450A0A) else Color(0xFF1E293B).copy(alpha = 0.5f),
                        contentColor = if (isStopActive) Color(0xFFEF4444) else Color(0xFF64748B),
                        disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.3f),
                        disabledContentColor = Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stop_action),
                        modifier = Modifier.size(18.dp),
                        tint = if (isStopActive) Color(0xFFEF4444) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.stop_action),
                        fontFamily = monoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
