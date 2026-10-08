package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.basic.ui.BasicStudioApp
import com.example.basic.ui.BasicViewModel
import com.example.ui.theme.BasicStudioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BasicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BasicStudioTheme {
                BasicStudioApp(viewModel = viewModel)
            }
        }
    }
}
