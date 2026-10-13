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
import androidx.compose.material.icons.outlined.Circle
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
                        viewModel.compileCurrentCode()
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
}

@Composable
fun EditorScreen(
    viewModel: BasicViewModel,
    monoFont: FontFamily,
    currentDebugLine: Int?,
    onOpenFilePicker: () -> Unit = {},
    onOpenFileManager: () -> Unit = {}
) {
    val code by viewModel.sourceCode.collectAsState()
    val breakpoints by viewModel.breakpoints.collectAsState()
    val lines = remember(code) { code.lines() }
    val currentFileName by viewModel.currentFileName.collectAsState()
    val context = LocalContext.current

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

        // Quick syntax helper toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val helpers = listOf("PRINT", "INPUT", "LET", "IF", "THEN", "FOR", "TO", "NEXT", "GOTO", "CLS")
            helpers.forEach { keyword ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable {
                        viewModel.insertSnippet("$keyword ")
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
        Row(modifier = Modifier.fillMaxSize()) {
            // Line numbers / Gutter column
            LazyColumn(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0F172A))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(lines.size) { index ->
                    val lineNum = index + 1
                    val hasBreakpoint = breakpoints.contains(lineNum)
                    val isCurrent = currentDebugLine == lineNum

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clickable { viewModel.toggleBreakpoint(lineNum) }
                            .padding(horizontal = 2.dp),
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
                            fontFamily = monoFont
                        )
                    }
                }
            }

            // Code input field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { viewModel.updateCode(it) },
                    textStyle = TextStyle(
                        fontFamily = monoFont,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(TerminalCyan),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_field")
                )
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

        val graphicsState by viewModel.graphicsState.collectAsState()

        // If SCREEN mode > 0, show Retro 320x200 Graphics Canvas with Sprites
        if (graphicsState.mode > 0) {
            com.example.basic.graphics.RetroGraphicsCanvas(
                graphicsState = graphicsState,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Terminal output screen
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(if (graphicsState.mode > 0) 0.8f else 1f)
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
                    fontSize = 13.sp
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
                        textStyle = TextStyle(color = Color.White, fontFamily = monoFont),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(12.dp)
    ) {
        // Debug Control Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.stepLine() },
                        enabled = vmState == VmState.PAUSED_DEBUG,
                        modifier = Modifier.testTag("debugger_step_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = TerminalCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Step", fontFamily = monoFont, color = TerminalCyan)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resumeExecution() },
                        enabled = vmState == VmState.PAUSED_DEBUG,
                        modifier = Modifier.testTag("debugger_resume_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TerminalGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Continue", fontFamily = monoFont, color = TerminalGreen)
                    }
                }
            }
        }

        // Live Watch & Variables Inspector
        Text(
            text = stringResource(R.string.variables),
            color = Color.White,
            fontFamily = monoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            if (variables.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No variables initialized yet", color = Color(0xFF64748B), fontFamily = monoFont)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp).testTag("variables_watch_list")) {
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
            }
        }

        // GOSUB Call Stack
        Text(
            text = stringResource(R.string.call_stack),
            color = Color.White,
            fontFamily = monoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
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
                            fontSize = 12.sp
                        )
                    }
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
