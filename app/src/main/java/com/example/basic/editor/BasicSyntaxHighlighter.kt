package com.example.basic.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object BasicSyntaxHighlighter {

    private val KEYWORD_COLOR = Color(0xFF38BDF8)   // Light sky blue
    private val STRING_COLOR = Color(0xFFA3E635)    // Bright lime green
    private val NUMBER_COLOR = Color(0xFFFB923C)    // Warm orange
    private val COMMENT_COLOR = Color(0xFF94A3B8)   // Muted slate gray
    private val LINE_NUM_COLOR = Color(0xFFC084FC)  // Soft purple
    private val OPERATOR_COLOR = Color(0xFFF472B6)  // Pink

    private val KEYWORDS = setOf(
        "PRINT", "INPUT", "LET", "IF", "THEN", "ELSE", "FOR", "TO", "STEP", "NEXT",
        "GOTO", "GOSUB", "RETURN", "WHILE", "WEND", "END", "STOP", "REM", "CLS",
        "COLOR", "BEEP", "SLEEP", "DIM", "AND", "OR", "NOT", "MOD",
        "INT", "ABS", "SQR", "RND", "SIN", "COS", "TAN", "LEN", "STR$", "VAL", "CHR$", "ASC"
    )

    fun highlight(code: String, font: FontFamily = FontFamily.Monospace): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            var i = 0
            val len = code.length

            while (i < len) {
                val c = code[i]

                // Line comment starting with ' or REM
                if (c == '\'') {
                    val start = i
                    while (i < len && code[i] != '\n') i++
                    addStyle(SpanStyle(color = COMMENT_COLOR, fontWeight = FontWeight.Normal), start, i)
                    continue
                }

                // String literal
                if (c == '"') {
                    val start = i
                    i++
                    while (i < len && code[i] != '"' && code[i] != '\n') i++
                    if (i < len && code[i] == '"') i++
                    addStyle(SpanStyle(color = STRING_COLOR, fontWeight = FontWeight.Medium), start, i)
                    continue
                }

                // Number
                if (c.isDigit()) {
                    val start = i
                    val isLineStart = (start == 0 || code[start - 1] == '\n')
                    while (i < len && (code[i].isDigit() || code[i] == '.')) i++
                    val color = if (isLineStart) LINE_NUM_COLOR else NUMBER_COLOR
                    val weight = if (isLineStart) FontWeight.Bold else FontWeight.Normal
                    addStyle(SpanStyle(color = color, fontWeight = weight), start, i)
                    continue
                }

                // Word (Keywords, REM, Identifiers)
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < len && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '$' || code[i] == '%')) i++
                    val word = code.substring(start, i)
                    val upper = word.uppercase()
                    if (upper == "REM") {
                        // REM comment till end of line
                        while (i < len && code[i] != '\n') i++
                        addStyle(SpanStyle(color = COMMENT_COLOR, fontWeight = FontWeight.Normal), start, i)
                    } else if (KEYWORDS.contains(upper)) {
                        addStyle(SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold), start, i)
                    }
                    continue
                }

                // Operators
                if ("+-*/^=<>:;?".contains(c)) {
                    addStyle(SpanStyle(color = OPERATOR_COLOR, fontWeight = FontWeight.Bold), i, i + 1)
                }

                i++
            }
        }
    }
}
