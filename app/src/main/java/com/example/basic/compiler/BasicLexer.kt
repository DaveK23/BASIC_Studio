package com.example.basic.compiler

import com.example.basic.model.BasicValue
import com.example.basic.model.BytecodeProgram
import com.example.basic.model.Instruction
import com.example.basic.model.OpCode
import java.util.Locale

/**
 * Token types recognized by the BASIC lexer.
 */
enum class TokenType {
    NUMBER, STRING, IDENTIFIER,
    // Keywords
    PRINT, INPUT, LET, IF, THEN, ELSE, FOR, TO, STEP, NEXT,
    GOTO, GOSUB, RETURN, WHILE, WEND, END, STOP, REM, CLS, COLOR, BEEP, SLEEP, DIM,
    // Operators
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, POWER,
    EQ, NEQ, LT, LTE, GT, GTE,
    AND, OR, NOT,
    // Punctuation
    LPAREN, RPAREN, COMMA, SEMICOLON, COLON, NEWLINE, EOF
}

data class Token(
    val type: TokenType,
    val text: String,
    val line: Int,
    val column: Int,
    val numberVal: Double = 0.0
)

/**
 * BASIC Lexer: parses statements with or without line numbers.
 */
class BasicLexer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var col = 1

    private val keywords = mapOf(
        "PRINT" to TokenType.PRINT,
        "INPUT" to TokenType.INPUT,
        "LET" to TokenType.LET,
        "IF" to TokenType.IF,
        "THEN" to TokenType.THEN,
        "ELSE" to TokenType.ELSE,
        "FOR" to TokenType.FOR,
        "TO" to TokenType.TO,
        "STEP" to TokenType.STEP,
        "NEXT" to TokenType.NEXT,
        "GOTO" to TokenType.GOTO,
        "GOSUB" to TokenType.GOSUB,
        "RETURN" to TokenType.RETURN,
        "WHILE" to TokenType.WHILE,
        "WEND" to TokenType.WEND,
        "END" to TokenType.END,
        "STOP" to TokenType.STOP,
        "REM" to TokenType.REM,
        "CLS" to TokenType.CLS,
        "COLOR" to TokenType.COLOR,
        "BEEP" to TokenType.BEEP,
        "SLEEP" to TokenType.SLEEP,
        "DIM" to TokenType.DIM,
        "AND" to TokenType.AND,
        "OR" to TokenType.OR,
        "NOT" to TokenType.NOT,
        "MOD" to TokenType.MODULO
    )

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < source.length) {
            val c = source[pos]
            when {
                c == '\r' -> {
                    advance()
                }
                c == '\n' -> {
                    tokens.add(Token(TokenType.NEWLINE, "\\n", line, col))
                    advance()
                    line++
                    col = 1
                }
                c == ':' -> {
                    // Colons can separate multiple statements on the same line
                    tokens.add(Token(TokenType.COLON, ":", line, col))
                    advance()
                }
                c.isWhitespace() -> {
                    advance()
                }
                c == '\'' -> {
                    // Comment until end of line
                    skipComment()
                }
                c == '"' -> {
                    tokens.add(readString())
                }
                c.isDigit() -> {
                    tokens.add(readNumber())
                }
                c.isLetter() || c == '_' -> {
                    val tok = readIdentifierOrKeyword()
                    if (tok.type == TokenType.REM) {
                        skipComment()
                    } else {
                        tokens.add(tok)
                    }
                }
                c == '+' -> { tokens.add(Token(TokenType.PLUS, "+", line, col)); advance() }
                c == '-' -> { tokens.add(Token(TokenType.MINUS, "-", line, col)); advance() }
                c == '*' -> { tokens.add(Token(TokenType.MULTIPLY, "*", line, col)); advance() }
                c == '/' -> { tokens.add(Token(TokenType.DIVIDE, "/", line, col)); advance() }
                c == '^' -> { tokens.add(Token(TokenType.POWER, "^", line, col)); advance() }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(", line, col)); advance() }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")", line, col)); advance() }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",", line, col)); advance() }
                c == ';' -> { tokens.add(Token(TokenType.SEMICOLON, ";", line, col)); advance() }
                c == '=' -> { tokens.add(Token(TokenType.EQ, "=", line, col)); advance() }
                c == '<' -> {
                    val startCol = col
                    advance()
                    if (pos < source.length && source[pos] == '>') {
                        tokens.add(Token(TokenType.NEQ, "<>", line, startCol))
                        advance()
                    } else if (pos < source.length && source[pos] == '=') {
                        tokens.add(Token(TokenType.LTE, "<=", line, startCol))
                        advance()
                    } else {
                        tokens.add(Token(TokenType.LT, "<", line, startCol))
                    }
                }
                c == '>' -> {
                    val startCol = col
                    advance()
                    if (pos < source.length && source[pos] == '=') {
                        tokens.add(Token(TokenType.GTE, ">=", line, startCol))
                        advance()
                    } else {
                        tokens.add(Token(TokenType.GT, ">", line, startCol))
                    }
                }
                c == '?' -> {
                    // In classic BASIC, '?' is shorthand for PRINT
                    tokens.add(Token(TokenType.PRINT, "?", line, col))
                    advance()
                }
                else -> {
                    // Unknown character, skip
                    advance()
                }
            }
        }
        tokens.add(Token(TokenType.EOF, "", line, col))
        return tokens
    }

    private fun advance(): Char {
        val c = source[pos]
        pos++
        col++
        return c
    }

    private fun skipComment() {
        while (pos < source.length && source[pos] != '\n' && source[pos] != '\r') {
            advance()
        }
    }

    private fun readString(): Token {
        val startCol = col
        advance() // skip open quote
        val sb = StringBuilder()
        while (pos < source.length && source[pos] != '"' && source[pos] != '\n') {
            sb.append(source[pos])
            advance()
        }
        if (pos < source.length && source[pos] == '"') {
            advance() // skip closing quote
        }
        return Token(TokenType.STRING, sb.toString(), line, startCol)
    }

    private fun readNumber(): Token {
        val startCol = col
        val sb = StringBuilder()
        var hasDot = false
        while (pos < source.length) {
            val c = source[pos]
            if (c.isDigit()) {
                sb.append(c)
                advance()
            } else if (c == '.' && !hasDot) {
                hasDot = true
                sb.append(c)
                advance()
            } else {
                break
            }
        }
        val text = sb.toString()
        val num = text.toDoubleOrNull() ?: 0.0
        return Token(TokenType.NUMBER, text, line, startCol, num)
    }

    private fun readIdentifierOrKeyword(): Token {
        val startCol = col
        val sb = StringBuilder()
        while (pos < source.length) {
            val c = source[pos]
            if (c.isLetterOrDigit() || c == '_' || c == '$' || c == '%') {
                sb.append(c)
                advance()
            } else {
                break
            }
        }
        val text = sb.toString()
        val upper = text.uppercase(Locale.ROOT)
        val keywordType = keywords[upper]
        return if (keywordType != null) {
            Token(keywordType, text, line, startCol)
        } else {
            Token(TokenType.IDENTIFIER, upper, line, startCol)
        }
    }
}
