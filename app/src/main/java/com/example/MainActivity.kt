package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontFamily
import com.example.basic.ui.BasicStudioApp
import com.example.basic.ui.BasicViewModel
import com.example.basic.ui.TerminalScreen
import com.example.ui.theme.BasicStudioTheme
import org.json.JSONObject
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val viewModel: BasicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if running as a standalone exported program (assets/source.bas or assets/program.basic.bin)
        var bundledCode: String? = null
        try {
            assets.open("source.bas").use { inputStream ->
                bundledCode = InputStreamReader(inputStream).readText()
            }
        } catch (e: Exception) {
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
            // Standalone mode: immediately run the bundled user program in Terminal mode
            viewModel.loadCode(bundledCode!!)
            viewModel.runProgram()

            setContent {
                BasicStudioTheme {
                    val terminalOutput = viewModel.terminalOutput.collectAsState().value
                    val uiState = viewModel.uiState.collectAsState().value

                    TerminalScreen(
                        viewModel = viewModel,
                        terminalOutput = terminalOutput,
                        vmState = uiState.vmState,
                        monoFont = FontFamily.Monospace
                    )
                }
            }
        } else {
            // Studio IDE mode
            setContent {
                BasicStudioTheme {
                    BasicStudioApp(viewModel = viewModel)
                }
            }
        }
    }
}
