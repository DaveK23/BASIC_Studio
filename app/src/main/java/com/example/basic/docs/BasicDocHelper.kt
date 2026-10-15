package com.example.basic.docs

import java.util.Locale

/**
 * Metadata and lookup utilities for context-sensitive BASIC documentation.
 */
data class DocEntry(
    val anchor: String,
    val keyword: String,
    val category: DocCategory,
    val summary: String
)

enum class DocCategory(val displayName: String) {
    STATEMENT("Statements & Commands"),
    FUNCTION("Built-in Functions"),
    OPERATOR("Operators"),
    GRAPHICS("Graphics & Sprites"),
    AUDIO("Sound & SFX"),
    CONCEPT("Language Concepts")
}

object BasicDocHelper {

    val ENTRIES: List<DocEntry> = listOf(
        // Statements & Commands
        DocEntry("cmd_print", "PRINT", DocCategory.STATEMENT, "Outputs text, variables, and expressions to the screen."),
        DocEntry("cmd_input", "INPUT", DocCategory.STATEMENT, "Prompts the user for interactive keyboard input."),
        DocEntry("cmd_let", "LET", DocCategory.STATEMENT, "Assigns an expression value to a variable or array element."),
        DocEntry("cmd_if", "IF", DocCategory.STATEMENT, "Conditionally executes statements based on a boolean expression."),
        DocEntry("cmd_for", "FOR", DocCategory.STATEMENT, "Loops a section of code over a numeric range with an optional STEP."),
        DocEntry("cmd_while", "WHILE", DocCategory.STATEMENT, "Repeats a block of code as long as a condition evaluates to true."),
        DocEntry("cmd_goto", "GOTO", DocCategory.STATEMENT, "Transfers program execution directly to a specified line number."),
        DocEntry("cmd_gosub", "GOSUB", DocCategory.STATEMENT, "Calls a subroutine at a line number, returning via RETURN."),
        DocEntry("cmd_end", "END", DocCategory.STATEMENT, "Terminates program execution cleanly."),
        DocEntry("cmd_stop", "STOP", DocCategory.STATEMENT, "Pauses program execution and enters debug halt mode."),
        DocEntry("cmd_rem", "REM", DocCategory.STATEMENT, "Inserts non-executing comments and documentation remarks in code."),
        DocEntry("cmd_cls", "CLS", DocCategory.STATEMENT, "Clears the terminal output screen and resets cursor position."),
        DocEntry("cmd_color", "COLOR", DocCategory.STATEMENT, "Sets the active text/graphics foreground drawing color (0-15)."),
        DocEntry("cmd_beep", "BEEP", DocCategory.STATEMENT, "Emits a standard retro alert buzzer tone."),
        DocEntry("cmd_sleep", "SLEEP", DocCategory.STATEMENT, "Suspends program execution for a specified duration in milliseconds."),
        DocEntry("cmd_dim", "DIM", DocCategory.STATEMENT, "Declares and allocates memory for one-dimensional arrays."),

        // Graphics Commands
        DocEntry("cmd_screen", "SCREEN", DocCategory.GRAPHICS, "Selects display mode (0: Text terminal, 1: 320x200 graphics)."),
        DocEntry("cmd_pset", "PSET", DocCategory.GRAPHICS, "Plots an individual pixel at coordinates (X, Y) with optional color."),
        DocEntry("cmd_line", "LINE", DocCategory.GRAPHICS, "Draws a line segment between points (X1, Y1) and (X2, Y2)."),
        DocEntry("cmd_rect", "RECT", DocCategory.GRAPHICS, "Draws an outlined or filled rectangle between two diagonal points."),
        DocEntry("cmd_circle", "CIRCLE", DocCategory.GRAPHICS, "Draws an outlined or filled circle with given center and radius."),

        // Audio Commands
        DocEntry("cmd_sound", "SOUND", DocCategory.AUDIO, "Synthesizes custom audio frequencies with waveform selection."),
        DocEntry("cmd_sfx", "SFX", DocCategory.AUDIO, "Triggers arcade sound effect presets (Laser, Jump, Explosion, etc.)."),

        // Sprites
        DocEntry("cmd_spritedef", "SPRITEDEF", DocCategory.GRAPHICS, "Defines a pixel sprite pattern matrix (8x8 or 16x16)."),
        DocEntry("cmd_sprite", "SPRITE", DocCategory.GRAPHICS, "Places or updates a sprite instance at screen coordinates (X, Y)."),
        DocEntry("cmd_spritemove", "SPRITEMOVE", DocCategory.GRAPHICS, "Translates a sprite position by relative coordinate offsets (dX, dY)."),
        DocEntry("cmd_spriteshow", "SPRITESHOW", DocCategory.GRAPHICS, "Toggles sprite visibility state on or off."),
        DocEntry("fn_spritehit", "SPRITEHIT", DocCategory.GRAPHICS, "Tests bounding-box collision between two active sprites."),

        // Built-in Functions
        DocEntry("fn_int", "INT", DocCategory.FUNCTION, "Truncates/floors a floating-point number to the nearest integer."),
        DocEntry("fn_abs", "ABS", DocCategory.FUNCTION, "Computes the absolute (non-negative) magnitude of a numeric value."),
        DocEntry("fn_sqr", "SQR", DocCategory.FUNCTION, "Calculates the positive mathematical square root of a number."),
        DocEntry("fn_rnd", "RND", DocCategory.FUNCTION, "Generates pseudo-random numbers between 0.0 and a given upper bound."),
        DocEntry("fn_sin", "SIN", DocCategory.FUNCTION, "Calculates the trigonometric sine of an angle given in radians."),
        DocEntry("fn_cos", "COS", DocCategory.FUNCTION, "Calculates the trigonometric cosine of an angle given in radians."),
        DocEntry("fn_tan", "TAN", DocCategory.FUNCTION, "Calculates the trigonometric tangent of an angle given in radians."),
        DocEntry("fn_len", "LEN", DocCategory.FUNCTION, "Returns the number of characters in a string value."),
        DocEntry("fn_str", "STR$", DocCategory.FUNCTION, "Converts a numeric value to its string representation."),
        DocEntry("fn_val", "VAL", DocCategory.FUNCTION, "Parses and converts a numeric string into a floating-point number."),
        DocEntry("fn_chr", "CHR$", DocCategory.FUNCTION, "Returns a single-character string corresponding to an ASCII code."),
        DocEntry("fn_asc", "ASC", DocCategory.FUNCTION, "Returns the ASCII integer code of the first character in a string."),
        DocEntry("fn_tab", "TAB", DocCategory.FUNCTION, "Spaces the print cursor horizontally to a target column position."),

        // Operators
        DocEntry("op_add", "+", DocCategory.OPERATOR, "Arithmetic addition or string concatenation operator."),
        DocEntry("op_sub", "-", DocCategory.OPERATOR, "Arithmetic subtraction or unary negation operator."),
        DocEntry("op_mul", "*", DocCategory.OPERATOR, "Arithmetic multiplication operator."),
        DocEntry("op_div", "/", DocCategory.OPERATOR, "Arithmetic floating-point division operator."),
        DocEntry("op_pow", "^", DocCategory.OPERATOR, "Exponentiation (power) operator."),
        DocEntry("op_mod", "MOD", DocCategory.OPERATOR, "Modulo remainder division operator."),
        DocEntry("op_eq", "=", DocCategory.OPERATOR, "Equality comparison or assignment operator."),
        DocEntry("op_neq", "<>", DocCategory.OPERATOR, "Inequality comparison operator (not equal)."),
        DocEntry("op_lt", "<", DocCategory.OPERATOR, "Less-than relational comparison operator."),
        DocEntry("op_lte", "<=", DocCategory.OPERATOR, "Less-than or equal comparison operator."),
        DocEntry("op_gt", ">", DocCategory.OPERATOR, "Greater-than relational comparison operator."),
        DocEntry("op_gte", ">=", DocCategory.OPERATOR, "Greater-than or equal comparison operator."),
        DocEntry("op_and", "AND", DocCategory.OPERATOR, "Logical conjunction operator."),
        DocEntry("op_or", "OR", DocCategory.OPERATOR, "Logical disjunction operator."),
        DocEntry("op_not", "NOT", DocCategory.OPERATOR, "Logical negation operator.")
    )

    private val KEYWORD_MAP: Map<String, DocEntry> = buildMap {
        for (entry in ENTRIES) {
            put(entry.keyword.uppercase(Locale.ROOT), entry)
        }
        // Aliases and compound keywords
        get("IF")?.let { put("THEN", it); put("ELSE", it) }
        get("FOR")?.let { put("TO", it); put("STEP", it); put("NEXT", it) }
        get("WHILE")?.let { put("WEND", it) }
        get("GOSUB")?.let { put("RETURN", it) }
        get("END")?.let { put("STOP", it) }
        get("REM")?.let { put("'", it) }
        get("STR$")?.let { put("STR", it) }
        get("CHR$")?.let { put("CHR", it) }
    }

    /**
     * Resolves the token or keyword at or adjacent to [cursorIndex] in [code],
     * returning its matching documentation entry if recognized.
     */
    fun resolveEntryAtCursor(code: String, cursorIndex: Int): DocEntry? {
        if (code.isEmpty()) return null
        val safeIndex = cursorIndex.coerceIn(0, code.length)

        // 1. Check for two-character operators at or near cursor (e.g. <=, >=, <>)
        val checkIndices = listOf(safeIndex - 2, safeIndex - 1, safeIndex).filter { it in 0..(code.length - 2) }
        for (idx in checkIndices) {
            val twoChar = code.substring(idx, idx + 2)
            if (twoChar in listOf("<=", ">=", "<>")) {
                KEYWORD_MAP[twoChar]?.let { return it }
            }
        }

        // 2. Check for single-character operators
        val singleIndices = listOf(safeIndex - 1, safeIndex).filter { it in 0 until code.length }
        for (idx in singleIndices) {
            val charStr = code[idx].toString()
            if (charStr in listOf("+", "-", "*", "/", "^", "=", "<", ">", "'")) {
                KEYWORD_MAP[charStr]?.let { return it }
            }
        }

        // 3. Extract word/identifier around cursor
        var start = safeIndex
        if (start > 0 && (start == code.length || !isIdentifierChar(code[start]))) {
            // If cursor is right after a word, look back 1 character
            if (isIdentifierChar(code[start - 1])) {
                start--
            }
        }

        if (start in 0 until code.length && isIdentifierChar(code[start])) {
            var wordStart = start
            while (wordStart > 0 && isIdentifierChar(code[wordStart - 1])) {
                wordStart--
            }
            var wordEnd = start
            while (wordEnd < code.length && isIdentifierChar(code[wordEnd])) {
                wordEnd++
            }
            val word = code.substring(wordStart, wordEnd).uppercase(Locale.ROOT)
            KEYWORD_MAP[word]?.let { return it }
        }

        return null
    }

    private fun isIdentifierChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '$' || c == '_'
    }

    /**
     * Returns an entry by direct keyword name or anchor identifier.
     */
    fun findEntry(query: String): DocEntry? {
        val q = query.trim().uppercase(Locale.ROOT)
        KEYWORD_MAP[q]?.let { return it }
        return ENTRIES.firstOrNull { it.anchor.equals(q, ignoreCase = true) }
    }
}
