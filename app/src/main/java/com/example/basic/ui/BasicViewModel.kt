package com.example.basic.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basic.apk.StandaloneApkBuilder
import com.example.basic.compiler.BasicCompiler
import com.example.basic.compiler.CompilerException
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

data class IdeUiState(
    val vmState: VmState = VmState.IDLE,
    val lastCompilationError: String? = null
)

class BasicViewModel : ViewModel() {

    private val compiler = BasicCompiler()

    private val _sourceCode = MutableStateFlow(SamplePrograms.SAMPLES.first().code)
    val sourceCode: StateFlow<String> = _sourceCode.asStateFlow()

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private val _compiledProgram = MutableStateFlow<BytecodeProgram?>(null)
    val compiledProgram: StateFlow<BytecodeProgram?> = _compiledProgram.asStateFlow()

    private val _lastCompilationError = MutableStateFlow<String?>(null)
    val lastCompilationError: StateFlow<String?> = _lastCompilationError.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalOutput: StateFlow<List<TerminalLine>> = _terminalOutput.asStateFlow()

    private val _variables = MutableStateFlow<Map<String, BasicValue>>(emptyMap())
    val variables: StateFlow<Map<String, BasicValue>> = _variables.asStateFlow()

    private val _callStack = MutableStateFlow<List<CallFrame>>(emptyList())
    val callStack: StateFlow<List<CallFrame>> = _callStack.asStateFlow()

    private val _currentIp = MutableStateFlow(0)
    val currentIp: StateFlow<Int> = _currentIp.asStateFlow()

    private val _currentLine = MutableStateFlow(1)
    val currentLine: StateFlow<Int> = _currentLine.asStateFlow()

    private val _breakpoints = MutableStateFlow<Set<Int>>(emptySet())
    val breakpoints: StateFlow<Set<Int>> = _breakpoints.asStateFlow()

    private val _isApkBuilding = MutableStateFlow(false)
    val isApkBuilding: StateFlow<Boolean> = _isApkBuilding.asStateFlow()

    private val _apkExportResult = MutableStateFlow<String?>(null)
    val apkExportResult: StateFlow<String?> = _apkExportResult.asStateFlow()

    private val _lastExportedApk = MutableStateFlow<java.io.File?>(null)
    val lastExportedApk: StateFlow<java.io.File?> = _lastExportedApk.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            // Audio cue optional
        }
    }

    private val vm = BasicVirtualMachine(
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
        },
        onClearScreen = {
            _terminalOutput.value = emptyList()
        },
        onBeep = {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) {
                // Tone generator fallback
            }
        }
    )

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

    fun toggleBreakpoint(line: Int) {
        val current = _breakpoints.value.toMutableSet()
        if (current.contains(line)) {
            current.remove(line)
        } else {
            current.add(line)
        }
        _breakpoints.value = current
        vm.toggleBreakpoint(line)
    }

    fun clearTerminal() {
        _terminalOutput.value = emptyList()
    }

    fun provideTerminalInput(text: String) {
        vm.provideInput(text)
    }

    fun compileCurrentCode(): Boolean {
        return try {
            val prog = compiler.compile(_sourceCode.value)
            _compiledProgram.value = prog
            _lastCompilationError.value = null
            _uiState.value = _uiState.value.copy(lastCompilationError = null)
            vm.loadProgram(prog)
            true
        } catch (e: CompilerException) {
            _lastCompilationError.value = e.message
            _uiState.value = _uiState.value.copy(lastCompilationError = e.message)
            _terminalOutput.value = _terminalOutput.value + TerminalLine("[COMPILATION ERROR]: ${e.message}", isError = true)
            false
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown compiler error"
            _lastCompilationError.value = msg
            _uiState.value = _uiState.value.copy(lastCompilationError = msg)
            _terminalOutput.value = _terminalOutput.value + TerminalLine("[COMPILATION ERROR]: $msg", isError = true)
            false
        }
    }

    fun runProgram() {
        if (!compileCurrentCode()) return

        _terminalOutput.value = listOf(
            TerminalLine("READY.", colorIndex = 0, isSystemNotice = true),
            TerminalLine("RUN\n", colorIndex = 14, isInputEcho = true)
        )

        viewModelScope.launch(Dispatchers.Default) {
            vm.execute(debug = false)
        }
    }

    fun startDebugging() {
        if (!compileCurrentCode()) return

        _terminalOutput.value = listOf(
            TerminalLine("*** DEBUGGER ATTACHED ***", colorIndex = 11, isSystemNotice = true),
            TerminalLine("Step execution and live watch enabled.\n", colorIndex = 8)
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
            vm.execute(debug = true)
        }
    }

    fun stopProgram() {
        vm.stop()
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
                    _apkExportResult.value = "Success! Standalone APK (${exportRes.fileSizeFormatted}) saved to:\n${exportRes.publicPath}\n\nExact file path:\n${exportRes.file.absolutePath}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isApkBuilding.value = false
                    _apkExportResult.value = "Build error: ${e.message}"
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
}
