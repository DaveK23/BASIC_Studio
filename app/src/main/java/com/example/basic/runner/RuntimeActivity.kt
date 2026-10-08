package com.example.basic.runner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.example.basic.compiler.BasicCompiler
import com.example.basic.model.BytecodeProgram
import com.example.basic.ui.BasicViewModel
import com.example.basic.ui.TerminalScreen
import com.example.ui.theme.BasicStudioTheme
import org.json.JSONObject
import java.io.InputStreamReader

/**
 * Dedicated standalone activity that runs a compiled BASIC payload
 * in full terminal mode directly when launched from the home screen.
 */
class RuntimeActivity : ComponentActivity() {

    private val viewModel: BasicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Attempt to read bundled standalone BASIC payload from assets/source.bas or assets/program.basic.bin
        var bundledCode: String? = null
        try {
            assets.open("source.bas").use { inputStream ->
                bundledCode = InputStreamReader(inputStream).readText()
            }
        } catch (e: Exception) {
            // Check program.basic.bin JSON
            try {
                assets.open("program.basic.bin").use { inputStream ->
                    val jsonStr = InputStreamReader(inputStream).readText()
                    val obj = JSONObject(jsonStr)
                    bundledCode = obj.optString("source")
                }
            } catch (e2: Exception) {
                bundledCode = null
            }
        }

        if (!bundledCode.isNullOrBlank()) {
            viewModel.loadCode(bundledCode!!)
            viewModel.runProgram()
        }

        setContent {
            BasicStudioTheme {
                val terminalOutput = viewModel.terminalOutput.collectAsState().value
                val uiState = viewModel.uiState.collectAsState().value

                TerminalScreen(
                    viewModel = viewModel,
                    terminalOutput = terminalOutput,
                    vmState = uiState.vmState,
                    monoFont = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}
