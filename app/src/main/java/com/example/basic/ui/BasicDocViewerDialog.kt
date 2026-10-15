package com.example.basic.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.basic.docs.BasicDocHelper

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BasicDocViewerDialog(
    initialAnchor: String? = null,
    onDismiss: () -> Unit,
    monoFont: FontFamily
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    val resolvedEntry = remember(initialAnchor) {
        initialAnchor?.let { BasicDocHelper.findEntry(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("basic_doc_viewer_dialog"),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF090D16),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header / Toolbar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left section: icon and title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = TerminalCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.doc_viewer_title),
                                        color = Color.White,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (resolvedEntry != null) {
                                        Text(
                                            text = "${resolvedEntry.category.displayName}: ${resolvedEntry.keyword}",
                                            color = TerminalCyan,
                                            fontFamily = monoFont,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Right section: Navigation and control actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Search toggle button
                                IconButton(
                                    onClick = { showSearchBar = !showSearchBar },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("doc_viewer_search_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search_action),
                                        tint = if (showSearchBar || searchQuery.isNotEmpty()) TerminalCyan else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Table of Contents button
                                IconButton(
                                    onClick = {
                                        webViewInstance?.loadUrl("file:///android_asset/docs/basic_reference.html#toc")
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("doc_viewer_toc_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = stringResource(R.string.toc_action),
                                        tint = TerminalAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // WebView Back
                                IconButton(
                                    onClick = {
                                        if (webViewInstance?.canGoBack() == true) {
                                            webViewInstance?.goBack()
                                        }
                                    },
                                    enabled = canGoBack,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("doc_viewer_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back_action),
                                        tint = if (canGoBack) Color.White else Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // WebView Forward
                                IconButton(
                                    onClick = {
                                        if (webViewInstance?.canGoForward() == true) {
                                            webViewInstance?.goForward()
                                        }
                                    },
                                    enabled = canGoForward,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("doc_viewer_forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.forward_action),
                                        tint = if (canGoForward) Color.White else Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Close Button
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("doc_viewer_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.close_action),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Search Bar when expanded
                        if (showSearchBar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B1120))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TerminalCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { query ->
                                        searchQuery = query
                                        val escaped = query.replace("\"", "\\\"").replace("'", "\\'")
                                        webViewInstance?.evaluateJavascript("filterDocs('$escaped');", null)
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = monoFont,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    ),
                                    cursorBrush = SolidColor(TerminalCyan),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        val escaped = searchQuery.replace("\"", "\\\"").replace("'", "\\'")
                                        webViewInstance?.evaluateJavascript("filterDocs('$escaped');", null)
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("doc_viewer_search_input"),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search_docs_hint),
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp,
                                                fontFamily = monoFont
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            webViewInstance?.evaluateJavascript("filterDocs('');", null)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.clear_action),
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading progress indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = TerminalCyan,
                        trackColor = Color(0xFF1E293B)
                    )
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF090D16))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.parseColor("#090D16"))
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        canGoBack = canGoBack()
                                        canGoForward = canGoForward()

                                        // If initial anchor was requested, jump to it smoothly
                                        if (!initialAnchor.isNullOrBlank()) {
                                            val anchorClean = initialAnchor.removePrefix("#")
                                            view?.evaluateJavascript(
                                                """
                                                (function() {
                                                    var target = document.getElementById('$anchorClean');
                                                    if (target) {
                                                        target.scrollIntoView({ behavior: 'smooth' });
                                                    }
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        return false
                                    }
                                }

                                val baseUrl = "file:///android_asset/docs/basic_reference.html"
                                val targetUrl = if (!initialAnchor.isNullOrBlank()) {
                                    val cleanAnchor = initialAnchor.removePrefix("#")
                                    "$baseUrl#$cleanAnchor"
                                } else {
                                    baseUrl
                                }
                                loadUrl(targetUrl)
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("basic_doc_webview")
                    )
                }
            }
        }
    }
}
