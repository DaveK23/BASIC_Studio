package com.example.basic.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * High-performance, rich syntax highlighter for BASIC code in the IDE editor.
 *
 * Categorizes language constructs with high-contrast, cohesive retro-modern colors:
 * - Control Flow (IF, THEN, ELSE, FOR, TO, STEP, NEXT, GOTO, GOSUB, RETURN, WHILE, WEND, END, STOP, ON): Bright Violet / Purple
 * - Commands & I/O (PRINT, INPUT, LET, DIM, CLS, COLOR, BEEP, SLEEP, REM, CLEAR, RANDOMIZE, DATA, READ, RESTORE, SWAP): Electric Cyan
 * - Graphics & Audio (SCREEN, PSET, LINE, RECT, CIRCLE, SOUND, SFX, SPRITEDEF, SPRITE, SPRITEMOVE, SPRITESHOW): Vibrant Emerald / Teal
 * - Built-in Functions (INT, ABS, SQR, RND, SIN, COS, TAN, ATN, LOG, EXP, LEN, STR$, VAL, CHR$, ASC, TAB, SPC, SPRITEHIT): Golden Amber
 * - Operators (+, -, *, /, ^, =, <>, <, <=, >, >=, AND, OR, NOT, MOD, XOR): Rose Pink
 * - Strings ("..."): Bright Lime Green
 * - Comments (REM ..., ' ...): Muted Slate Gray with Italic font style
 * - Line Numbers at start of lines & branch targets (e.g. 10 in GOTO 10): Soft Neon Lavender Bold
 * - Numeric Literals: Warm Orange
 * - String Variables (A$, NAME$): Soft Ice Blue
 */
object BasicSyntaxHighlighter {

    // Control Flow: IF, THEN, ELSE, FOR, TO, STEP, NEXT, GOTO, GOSUB, RETURN, WHILE, WEND, END, STOP, ON
    val CONTROL_FLOW_COLOR = Color(0xFFC084FC)    // Bright Violet / Purple

    // Core Statements & I/O: PRINT, INPUT, LET, DIM, CLS, COLOR, BEEP, SLEEP, CLEAR, RANDOMIZE, DATA, READ, RESTORE
    val COMMAND_COLOR = Color(0xFF38BDF8)         // Electric Sky Blue / Cyan

    // Graphics & Audio: SCREEN, PSET, LINE, RECT, CIRCLE, SOUND, SFX, SPRITEDEF, SPRITE, SPRITEMOVE, SPRITESHOW
    val GRAPHICS_SOUND_COLOR = Color(0xFF34D399)  // Vibrant Mint / Emerald Teal

    // Built-in Functions: INT, ABS, SQR, RND, SIN, COS, TAN, LEN, STR$, VAL, CHR$, ASC, TAB, SPRITEHIT, etc.
    val FUNCTION_COLOR = Color(0xFFFBBF24)        // Warm Golden Amber

    // Operators: +, -, *, /, ^, =, <>, <, <=, >, >=, AND, OR, NOT, MOD, XOR
    val OPERATOR_COLOR = Color(0xFFF472B6)        // Rose Pink

    // Literals, labels, and comments
    val STRING_COLOR = Color(0xFFA3E635)          // Bright Lime Green
    val NUMBER_COLOR = Color(0xFFFB923C)          // Warm Orange
    val LINE_LABEL_COLOR = Color(0xFFA78BFA)      // Soft Neon Lavender
    val COMMENT_COLOR = Color(0xFF94A3B8)         // Muted Slate Gray
    val STRING_VAR_COLOR = Color(0xFF93C5FD)      // Soft Ice Blue

    val CONTROL_FLOW_KEYWORDS = setOf(
        "IF", "THEN", "ELSE",
        "FOR", "TO", "STEP", "NEXT",
        "GOTO", "GOSUB", "RETURN",
        "WHILE", "WEND", "DO", "LOOP",
        "END", "STOP",
        "ON"
    )

    val COMMAND_KEYWORDS = setOf(
        "PRINT", "INPUT", "LET", "DIM",
        "CLS", "COLOR", "BEEP", "SLEEP",
        "CLEAR", "RANDOMIZE", "DATA", "READ", "RESTORE", "SWAP",
        "POKE", "OUT", "DEF", "FN"
    )

    val GRAPHICS_SOUND_KEYWORDS = setOf(
        "SCREEN", "PSET", "LINE", "RECT", "CIRCLE",
        "SOUND", "SFX",
        "SPRITEDEF", "SPRITE", "SPRITEMOVE", "SPRITESHOW"
    )

    val FUNCTION_KEYWORDS = setOf(
        "INT", "ABS", "SQR", "RND",
        "SIN", "COS", "TAN", "ATN", "LOG", "EXP",
        "LEN", "STR$", "VAL", "CHR$", "ASC",
        "TAB", "SPC", "LEFT$", "RIGHT$", "MID$",
        "SPRITEHIT", "PEEK", "INP", "POS"
    )

    val LOGICAL_OPERATORS = setOf(
        "AND", "OR", "NOT", "MOD", "XOR"
    )

    // Keywords that indicate the following number is a line target
    private val JUMP_KEYWORDS = setOf(
        "GOTO", "GOSUB", "THEN", "RESTORE"
    )

    fun highlight(code: String, font: FontFamily = FontFamily.Monospace): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            var i = 0
            val len = code.length
            var lastKeyword: String? = null

            while (i < len) {
                val c = code[i]

                // Line comment starting with '
                if (c == '\'') {
                    val start = i
                    while (i < len && code[i] != '\n') i++
                    addStyle(
                        SpanStyle(
                            color = COMMENT_COLOR,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal
                        ),
                        start, i
                    )
                    lastKeyword = null
                    continue
                }

                // String literal ("...")
                if (c == '"') {
                    val start = i
                    i++
                    while (i < len && code[i] != '"' && code[i] != '\n') i++
                    if (i < len && code[i] == '"') i++
                    addStyle(
                        SpanStyle(
                            color = STRING_COLOR,
                            fontWeight = FontWeight.Normal
                        ),
                        start, i
                    )
                    lastKeyword = null
                    continue
                }

                // Number (could be line number at start of line, branch target after GOTO/THEN, or numeric literal)
                if (c.isDigit()) {
                    val start = i
                    val isLineStart = isLeadingLineNumber(code, start)
                    while (i < len && (code[i].isDigit() || code[i] == '.')) i++
                    val isJumpTarget = lastKeyword != null && JUMP_KEYWORDS.contains(lastKeyword)

                    val (color, weight) = when {
                        isLineStart -> LINE_LABEL_COLOR to FontWeight.Bold
                        isJumpTarget -> LINE_LABEL_COLOR to FontWeight.Bold
                        else -> NUMBER_COLOR to FontWeight.Medium
                    }
                    addStyle(SpanStyle(color = color, fontWeight = weight), start, i)
                    lastKeyword = null
                    continue
                }

                // Words / Identifiers / Keywords
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < len && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '$' || code[i] == '%')) i++
                    val word = code.substring(start, i)
                    val upper = word.uppercase()

                    if (upper == "REM") {
                        // REM comment runs till end of line
                        while (i < len && code[i] != '\n') i++
                        addStyle(
                            SpanStyle(
                                color = COMMENT_COLOR,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Normal
                            ),
                            start, i
                        )
                        lastKeyword = null
                    } else when {
                        CONTROL_FLOW_KEYWORDS.contains(upper) -> {
                            addStyle(SpanStyle(color = CONTROL_FLOW_COLOR, fontWeight = FontWeight.Bold), start, i)
                            lastKeyword = upper
                        }
                        COMMAND_KEYWORDS.contains(upper) -> {
                            addStyle(SpanStyle(color = COMMAND_COLOR, fontWeight = FontWeight.Bold), start, i)
                            lastKeyword = upper
                        }
                        GRAPHICS_SOUND_KEYWORDS.contains(upper) -> {
                            addStyle(SpanStyle(color = GRAPHICS_SOUND_COLOR, fontWeight = FontWeight.Bold), start, i)
                            lastKeyword = upper
                        }
                        FUNCTION_KEYWORDS.contains(upper) -> {
                            addStyle(SpanStyle(color = FUNCTION_COLOR, fontWeight = FontWeight.Bold), start, i)
                            lastKeyword = upper
                        }
                        LOGICAL_OPERATORS.contains(upper) -> {
                            addStyle(SpanStyle(color = OPERATOR_COLOR, fontWeight = FontWeight.Bold), start, i)
                            lastKeyword = upper
                        }
                        upper == "TRUE" || upper == "FALSE" -> {
                            addStyle(SpanStyle(color = FUNCTION_COLOR, fontWeight = FontWeight.Bold), start, i)
                            lastKeyword = upper
                        }
                        word.endsWith("$") -> {
                            // String variable
                            addStyle(SpanStyle(color = STRING_VAR_COLOR, fontWeight = FontWeight.Medium), start, i)
                            lastKeyword = null
                        }
                        else -> {
                            // Regular identifier / variable
                            lastKeyword = null
                        }
                    }
                    continue
                }

                // Operator & punctuation characters
                if (c == '?') {
                    // Classic BASIC shortcut for PRINT
                    addStyle(SpanStyle(color = COMMAND_COLOR, fontWeight = FontWeight.Bold), i, i + 1)
                    lastKeyword = "PRINT"
                    i++
                    continue
                }

                if (c == ':') {
                    // Statement separator
                    addStyle(SpanStyle(color = Color(0xFF818CF8), fontWeight = FontWeight.Bold), i, i + 1)
                    lastKeyword = null
                    i++
                    continue
                }

                // Check two-character operators: <=, >=, <>
                if (i + 1 < len) {
                    val twoChar = code.substring(i, i + 2)
                    if (twoChar == "<=" || twoChar == ">=" || twoChar == "<>") {
                        addStyle(SpanStyle(color = OPERATOR_COLOR, fontWeight = FontWeight.Bold), i, i + 2)
                        lastKeyword = null
                        i += 2
                        continue
                    }
                }

                // Single character operators
                if ("+-*/^=<>".contains(c)) {
                    addStyle(SpanStyle(color = OPERATOR_COLOR, fontWeight = FontWeight.Bold), i, i + 1)
                    lastKeyword = null
                    i++
                    continue
                }

                if (c == '\n') {
                    lastKeyword = null
                }

                i++
            }
        }
    }

    private fun isLeadingLineNumber(code: String, index: Int): Boolean {
        var p = index - 1
        while (p >= 0 && code[p] != '\n') {
            if (!code[p].isWhitespace()) return false
            p--
        }
        return true
    }
}
