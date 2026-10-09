package com.example.basic.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basic.apk.StandaloneApkBuilder
import com.example.basic.compiler.BasicCompiler
import com.example.basic.editor.SamplePrograms
import com.example.basic.model.BasicValue
import com.example.basic.model.BytecodeProgram
import com.example.basic.vm.BasicVirtualMachine
import com.example.basic.vm.CallFrame
import com.example.basic.vm.TerminalLine
import com.example.basic.vm.VmState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class IdeUiState(
    val vmState: VmState = VmState.IDLE,
    val lastCompilationError: String? = null
)

class BasicViewModel : ViewModel() {

    private val compiler = BasicCompiler()

    private val _sourceCode = MutableStateFlow(SamplePrograms.SAMPLES.first().code)
    val sourceCode: StateFlow<String> = _sourceCode.asStateFlow()

    private val _compiledProgram = MutableStateFlow<BytecodeProgram?>(null)
    val compiledProgram: StateFlow<BytecodeProgram?> = _compiledProgram.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalOutput: StateFlow<List<TerminalLine>> = _terminalOutput.asStateFlow()

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private val _variables = MutableStateFlow<Map<String, BasicValue>>(emptyMap())
    val variables: StateFlow<Map<String, BasicValue>> = _variables.asStateFlow()

    private val _callStack = MutableStateFlow<List<CallFrame>>(emptyList())
    val callStack: StateFlow<List<CallFrame>> = _callStack.asStateFlow()

    private val _currentIp = MutableStateFlow(0)
    val currentIp: StateFlow<Int> = _currentIp.asStateFlow()

    private val _currentLine = MutableStateFlow(0)
    val currentLine: StateFlow<Int> = _currentLine.asStateFlow()

    private val _breakpoints = MutableStateFlow<Set<Int>>(emptySet())
    val breakpoints: StateFlow<Set<Int>> = _breakpoints.asStateFlow()

    private val _lastCompilationError = MutableStateFlow<String?>(null)
    val lastCompilationError: StateFlow<String?> = _lastCompilationError.asStateFlow()

    private val _isApkBuilding = MutableStateFlow(false)
    val isApkBuilding: StateFlow<Boolean> = _isApkBuilding.asStateFlow()

    private val _apkExportResult = MutableStateFlow<String?>(null)
    val apkExportResult: StateFlow<String?> = _apkExportResult.asStateFlow()

    private val _lastExportedApk = MutableStateFlow<File?>(null)
    val lastExportedApk: StateFlow<File?> = _lastExportedApk.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    val vm: BasicVirtualMachine = BasicVirtualMachine(
        onOutput = { line ->
            _terminalOutput.value = _terminalOutput.value + line
        },
        onStateChange = { state ->
            _uiState.value = _uiState.value.copy(vmState = state)
        },
        onVariablesChanged = { vars ->
            _variables.value = vars
        },
        onIpChanged = { ip, line ->
            _currentIp.value = ip
            _currentLine.value = line
            _callStack.value = vm.callStack.toList()
        },
        onClearScreen = {
            _terminalOutput.value = emptyList()
        },
        onBeep = {
            playTone(800, 150)
        }
    )

    fun compileCurrentCode(): Boolean {
        return try {
            val program = compiler.compile(_sourceCode.value)
            _compiledProgram.value = program
            _lastCompilationError.value = null
            _uiState.value = _uiState.value.copy(lastCompilationError = null)
            vm.loadProgram(program)
            true
        } catch (e: Exception) {
            _compiledProgram.value = null
            val err = "[COMPILATION ERROR]: ${e.message}"
            _lastCompilationError.value = err
            _uiState.value = _uiState.value.copy(lastCompilationError = err)
            false
        }
    }

    fun runProgram() {
        if (!compileCurrentCode()) return
        _terminalOutput.value = listOf(
            TerminalLine("READY.", colorIndex = 0),
            TerminalLine("RUN\n", colorIndex = 0)
        )
        viewModelScope.launch(Dispatchers.Default) {
            vm.execute(debug = false)
        }
    }

    fun startDebugging() {
        if (!compileCurrentCode()) return
        _terminalOutput.value = listOf(
            TerminalLine("*** DEBUGGER ATTACHED ***", colorIndex = 11, isSystemNotice = true),
            TerminalLine("Step execution and live watch enabled.\n", colorIndex = 8, isSystemNotice = true)
        )
        viewModelScope.launch(Dispatchers.Default) {
            vm.execute(debug = true)
        }
    }

    fun stepInstruction() {
        viewModelScope.launch(Dispatchers.Default) {
            vm.step()
            _callStack.value = vm.callStack.toList()
        }
    }

    fun resumeExecution() {
        viewModelScope.launch(Dispatchers.Default) {
            vm.execute(debug = vm.isDebugMode)
        }
    }

    fun stopProgram() {
        vm.stop()
    }

    fun toggleBreakpoint(sourceLine: Int) {
        val set = _breakpoints.value.toMutableSet()
        if (set.contains(sourceLine)) {
            set.remove(sourceLine)
        } else {
            set.add(sourceLine)
        }
        _breakpoints.value = set
        vm.toggleBreakpoint(sourceLine)
    }

    fun clearTerminal() {
        _terminalOutput.value = emptyList()
    }

    fun provideTerminalInput(text: String) {
        vm.provideInput(text)
    }

    fun updateCode(newCode: String) {
        _sourceCode.value = newCode
    }

    fun insertSnippet(snippet: String) {
        _sourceCode.value = _sourceCode.value + snippet
    }

    fun loadCode(code: String) {
        _sourceCode.value = code
        compileCurrentCode()
    }

    fun exportApk(context: Context, appName: String, packageName: String) {
        if (!compileCurrentCode()) {
            _apkExportResult.value = "Compilation failed: Please fix errors before exporting."
            return
        }
        val prog = _compiledProgram.value ?: return
        _isApkBuilding.value = true
        _apkExportResult.value = null
        _lastExportedApk.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportRes = StandaloneApkBuilder.buildApk(
                    context = context,
                    appName = appName,
                    packageName = packageName,
                    program = prog
                )
                withContext(Dispatchers.Main) {
                    _isApkBuilding.value = false
                    _lastExportedApk.value = exportRes.file
                    val reportStr = exportRes.validationReport?.summary() ?: "Validation not available"
                    _apkExportResult.value = "Success! Standalone APK (${exportRes.fileSizeFormatted}) saved to:\n${exportRes.publicPath}\n\nExact file path:\n${exportRes.file.absolutePath}\n\n--- Diagnostic Verification Report ---\n$reportStr"
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _isApkBuilding.value = false
                    val trace = e.stackTrace.take(3).joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
                    _apkExportResult.value = "Build error: ${e.javaClass.simpleName}: ${e.message}\n$trace"
                }
            }
        }
    }

    fun clearExportResult() {
        _apkExportResult.value = null
        _lastExportedApk.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun playTone(freq: Int, durationMs: Int) {
        try {
            val toneType = when {
                freq < 400 -> ToneGenerator.TONE_PROP_BEEP2
                freq < 800 -> ToneGenerator.TONE_PROP_BEEP
                else -> ToneGenerator.TONE_PROP_ACK
            }
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            // Ignore tone failure
        }
    }
}
