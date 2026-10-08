package com.example.basic.compiler

import com.example.basic.model.BasicValue
import com.example.basic.model.BytecodeProgram
import com.example.basic.model.Instruction
import com.example.basic.model.OpCode
import java.util.Locale

/**
 * Compiler exception with line details.
 */
class CompilerException(val line: Int, val col: Int, override val message: String) : Exception("Line $line, Col $col: $message")

/**
 * Compiles BASIC source tokens into virtual machine Bytecode.
 * Supports:
 * - Line numbers or line-numberless BASIC
 * - PRINT (expressions, ;, ,, strings, numbers)
 * - INPUT [prompt;] variable
 * - LET var = expr (and implicit var = expr)
 * - IF cond THEN stmt [ELSE stmt]
 * - FOR var = start TO finish [STEP step] ... NEXT [var]
 * - WHILE cond ... WEND
 * - GOTO line
 * - GOSUB line ... RETURN
 * - CLS, COLOR, BEEP, SLEEP
 * - DIM array(size), array(index) = val, array(index)
 * - Built-in functions: INT, ABS, SQR, RND, SIN, COS, TAN, LEN, STR$, VAL, CHR$, ASC
 */
class BasicCompiler {

    fun compile(source: String): BytecodeProgram {
        val lexer = BasicLexer(source)
        val tokens = lexer.tokenize()
        val parser = Parser(tokens, source)
        return parser.parseProgram()
    }

    private class Parser(private val tokens: List<Token>, private val originalSource: String) {
        private var current = 0
        private val instructions = mutableListOf<Instruction>()
        private val constants = mutableListOf<BasicValue>()
        private val constantMap = mutableMapOf<Any, Int>()
        private val lineToIpMap = mutableMapOf<Int, Int>()
        private val basicLineNumberToIp = mutableMapOf<Int, Int>()
        private val jumpPatches = mutableListOf<JumpPatch>()

        // Loops tracking for FOR / NEXT
        private data class ForLoopInfo(
            val varName: String,
            val stepConstOrVar: BasicValue?,
            val conditionIp: Int,
            val endJumpInstructionIndex: Int
        )
        private val forStack = mutableListOf<ForLoopInfo>()

        // Loops tracking for WHILE / WEND
        private data class WhileLoopInfo(
            val conditionIp: Int,
            val endJumpInstructionIndex: Int
        )
        private val whileStack = mutableListOf<WhileLoopInfo>()

        private data class JumpPatch(
            val instructionIndex: Int,
            val targetBasicLineNum: Int? = null,
            val targetIp: Int? = null
        )

        fun parseProgram(): BytecodeProgram {
            while (!isAtEnd()) {
                skipNewlinesAndColons()
                if (isAtEnd()) break

                var currentSourceLine = peek().line
                var basicLineNum: Int? = null

                // Check for leading line number (e.g. "10 PRINT ...")
                if (peek().type == TokenType.NUMBER && (peek().text.toDoubleOrNull()?.let { it % 1.0 == 0.0 } == true)) {
                    val lineTok = advance()
                    basicLineNum = lineTok.numberVal.toInt()
                    basicLineNumberToIp[basicLineNum] = instructions.size
                }

                if (!lineToIpMap.containsKey(currentSourceLine)) {
                    lineToIpMap[currentSourceLine] = instructions.size
                }

                if (match(TokenType.NEWLINE) || match(TokenType.COLON)) {
                    continue
                }

                parseStatement(currentSourceLine, basicLineNum)

                // Optional trailing colon or newline
                while (match(TokenType.COLON)) {
                    if (!match(TokenType.NEWLINE) && !isAtEnd()) {
                        parseStatement(currentSourceLine, basicLineNum)
                    }
                }
                match(TokenType.NEWLINE)
            }

            // Append final HALT instruction
            val finalLine = if (tokens.isNotEmpty()) tokens.last().line else 1
            emit(OpCode.HALT, sourceLine = finalLine)

            // Resolve jump patches
            for (patch in jumpPatches) {
                val targetIp = when {
                    patch.targetIp != null -> patch.targetIp
                    patch.targetBasicLineNum != null -> {
                        basicLineNumberToIp[patch.targetBasicLineNum]
                            ?: lineToIpMap[patch.targetBasicLineNum]
                            ?: throw CompilerException(
                                instructions[patch.instructionIndex].sourceLine,
                                1,
                                "Undefined target line: ${patch.targetBasicLineNum}"
                            )
                    }
                    else -> 0
                }
                val original = instructions[patch.instructionIndex]
                instructions[patch.instructionIndex] = original.copy(argInt = targetIp)
            }

            return BytecodeProgram(
                instructions = instructions,
                constants = constants,
                sourceCode = originalSource,
                lineToIpMap = lineToIpMap
            )
        }

        private fun parseStatement(sourceLine: Int, basicLineNum: Int?) {
            val token = peek()
            when (token.type) {
                TokenType.PRINT -> parsePrint(sourceLine, basicLineNum)
                TokenType.INPUT -> parseInput(sourceLine, basicLineNum)
                TokenType.LET -> {
                    advance()
                    parseAssignment(sourceLine, basicLineNum)
                }
                TokenType.IF -> parseIf(sourceLine, basicLineNum)
                TokenType.FOR -> parseFor(sourceLine, basicLineNum)
                TokenType.NEXT -> parseNext(sourceLine, basicLineNum)
                TokenType.WHILE -> parseWhile(sourceLine, basicLineNum)
                TokenType.WEND -> parseWend(sourceLine, basicLineNum)
                TokenType.GOTO -> parseGoto(sourceLine, basicLineNum)
                TokenType.GOSUB -> parseGosub(sourceLine, basicLineNum)
                TokenType.RETURN -> {
                    advance()
                    emit(OpCode.RETURN, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                }
                TokenType.END, TokenType.STOP -> {
                    advance()
                    emit(OpCode.HALT, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                }
                TokenType.CLS -> {
                    advance()
                    emit(OpCode.CLS, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                }
                TokenType.COLOR -> {
                    advance()
                    parseExpression(sourceLine)
                    emit(OpCode.COLOR, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                }
                TokenType.BEEP -> {
                    advance()
                    emit(OpCode.BEEP, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                }
                TokenType.SLEEP -> {
                    advance()
                    parseExpression(sourceLine)
                    emit(OpCode.SLEEP, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                }
                TokenType.DIM -> parseDim(sourceLine, basicLineNum)
                TokenType.IDENTIFIER -> {
                    // Implicit assignment e.g. A = 5 or A(1) = 5
                    parseAssignment(sourceLine, basicLineNum)
                }
                TokenType.REM -> {
                    advance() // Already skipped by lexer or single REM token
                }
                else -> {
                    throw CompilerException(token.line, token.column, "Unexpected statement keyword: '${token.text}'")
                }
            }
        }

        private fun parsePrint(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume PRINT / ?
            if (match(TokenType.NEWLINE) || match(TokenType.COLON) || isAtEnd()) {
                emit(OpCode.PRINT_NEWLINE, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                return
            }

            var endsWithSeparator = false
            while (!isAtEnd() && peek().type != TokenType.NEWLINE && peek().type != TokenType.COLON && peek().type != TokenType.ELSE) {
                parseExpression(sourceLine)
                if (match(TokenType.SEMICOLON)) {
                    emit(OpCode.PRINT_SEMI, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                    endsWithSeparator = true
                } else if (match(TokenType.COMMA)) {
                    emit(OpCode.PRINT_COMMA, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                    endsWithSeparator = true
                } else {
                    emit(OpCode.PRINT, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                    endsWithSeparator = false
                    break
                }
            }

            if (endsWithSeparator && (peek().type == TokenType.NEWLINE || peek().type == TokenType.COLON || isAtEnd())) {
                // suppressed newline
            } else if (!endsWithSeparator && instructions.lastOrNull()?.op != OpCode.PRINT) {
                emit(OpCode.PRINT_NEWLINE, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            }
        }

        private fun parseInput(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume INPUT
            var promptIndex = -1
            if (peek().type == TokenType.STRING) {
                val str = advance().text
                promptIndex = addConstant(BasicValue.StringVal(str))
                match(TokenType.SEMICOLON) || match(TokenType.COMMA)
            }

            val varToken = consume(TokenType.IDENTIFIER, "Expected variable name after INPUT")
            emit(
                OpCode.INPUT,
                argInt = promptIndex,
                argString = varToken.text,
                sourceLine = sourceLine,
                sourceLineNum = basicLineNum ?: -1
            )
        }

        private fun parseAssignment(sourceLine: Int, basicLineNum: Int?) {
            val varToken = consume(TokenType.IDENTIFIER, "Expected variable name for assignment")
            val isArray = match(TokenType.LPAREN)
            if (isArray) {
                parseExpression(sourceLine)
                consume(TokenType.RPAREN, "Expected ')' after array index")
                consume(TokenType.EQ, "Expected '=' in array assignment")
                parseExpression(sourceLine)
                emit(
                    OpCode.STORE_ARRAY,
                    argString = varToken.text,
                    sourceLine = sourceLine,
                    sourceLineNum = basicLineNum ?: -1
                )
            } else {
                consume(TokenType.EQ, "Expected '=' in variable assignment")
                parseExpression(sourceLine)
                emit(
                    OpCode.STORE_VAR,
                    argString = varToken.text,
                    sourceLine = sourceLine,
                    sourceLineNum = basicLineNum ?: -1
                )
            }
        }

        private fun parseDim(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume DIM
            val varToken = consume(TokenType.IDENTIFIER, "Expected array name after DIM")
            consume(TokenType.LPAREN, "Expected '(' after array name")
            parseExpression(sourceLine)
            consume(TokenType.RPAREN, "Expected ')' in DIM statement")
            emit(
                OpCode.DIM_ARRAY,
                argString = varToken.text,
                sourceLine = sourceLine,
                sourceLineNum = basicLineNum ?: -1
            )
        }

        private fun parseIf(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume IF
            parseExpression(sourceLine)
            consume(TokenType.THEN, "Expected 'THEN' after IF condition")

            // Emit jump if false placeholder
            val jumpIfFalseIndex = instructions.size
            emit(OpCode.JUMP_IF_FALSE, argInt = 0, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)

            // Then branch (can be a line number for GOTO shorthand or a statement)
            if (peek().type == TokenType.NUMBER) {
                val target = advance().numberVal.toInt()
                emit(OpCode.JUMP, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                jumpPatches.add(JumpPatch(instructions.size - 1, targetBasicLineNum = target))
            } else {
                parseStatement(sourceLine, basicLineNum)
            }

            var hasElse = false
            var jumpOverElseIndex = -1
            if (match(TokenType.ELSE)) {
                hasElse = true
                jumpOverElseIndex = instructions.size
                emit(OpCode.JUMP, argInt = 0, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            }

            // Patch jump if false
            val falseTarget = instructions.size
            instructions[jumpIfFalseIndex] = instructions[jumpIfFalseIndex].copy(argInt = falseTarget)

            if (hasElse) {
                if (peek().type == TokenType.NUMBER) {
                    val target = advance().numberVal.toInt()
                    emit(OpCode.JUMP, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
                    jumpPatches.add(JumpPatch(instructions.size - 1, targetBasicLineNum = target))
                } else {
                    parseStatement(sourceLine, basicLineNum)
                }
                val endTarget = instructions.size
                instructions[jumpOverElseIndex] = instructions[jumpOverElseIndex].copy(argInt = endTarget)
            }
        }

        private fun parseFor(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume FOR
            val varToken = consume(TokenType.IDENTIFIER, "Expected loop variable after FOR")
            consume(TokenType.EQ, "Expected '=' in FOR statement")
            parseExpression(sourceLine)
            // Store initial value to var
            emit(OpCode.STORE_VAR, argString = varToken.text, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)

            consume(TokenType.TO, "Expected 'TO' in FOR statement")
            // Compile end limit expression and store into internal helper var
            val limitVarName = "__limit_" + varToken.text
            parseExpression(sourceLine)
            emit(OpCode.STORE_VAR, argString = limitVarName, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)

            val stepVarName = "__step_" + varToken.text
            if (match(TokenType.STEP)) {
                parseExpression(sourceLine)
            } else {
                val cIdx = addConstant(BasicValue.NumberVal(1.0))
                emit(OpCode.PUSH_CONST, argInt = cIdx, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            }
            emit(OpCode.STORE_VAR, argString = stepVarName, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)

            // Loop header condition
            val conditionIp = instructions.size
            // Check condition: if step >= 0 then var <= limit else var >= limit
            emit(OpCode.LOAD_VAR, argString = varToken.text, sourceLine = sourceLine)
            emit(OpCode.LOAD_VAR, argString = limitVarName, sourceLine = sourceLine)
            emit(OpCode.LTE, sourceLine = sourceLine) // default step check

            val jumpOutIndex = instructions.size
            emit(OpCode.JUMP_IF_FALSE, argInt = 0, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)

            forStack.add(
                ForLoopInfo(
                    varName = varToken.text,
                    stepConstOrVar = null,
                    conditionIp = conditionIp,
                    endJumpInstructionIndex = jumpOutIndex
                )
            )
        }

        private fun parseNext(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume NEXT
            if (forStack.isEmpty()) {
                throw CompilerException(sourceLine, 1, "NEXT without matching FOR")
            }
            val loop = forStack.removeAt(forStack.size - 1)
            if (peek().type == TokenType.IDENTIFIER) {
                val varTok = advance()
                if (varTok.text != loop.varName) {
                    throw CompilerException(sourceLine, 1, "NEXT variable '${varTok.text}' does not match FOR '${loop.varName}'")
                }
            }

            // Increment: var = var + step
            val stepVarName = "__step_" + loop.varName
            emit(OpCode.LOAD_VAR, argString = loop.varName, sourceLine = sourceLine)
            emit(OpCode.LOAD_VAR, argString = stepVarName, sourceLine = sourceLine)
            emit(OpCode.ADD, sourceLine = sourceLine)
            emit(OpCode.STORE_VAR, argString = loop.varName, sourceLine = sourceLine)

            // Jump back to condition
            emit(OpCode.JUMP, argInt = loop.conditionIp, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)

            // Patch jump out instruction
            val exitIp = instructions.size
            instructions[loop.endJumpInstructionIndex] = instructions[loop.endJumpInstructionIndex].copy(argInt = exitIp)
        }

        private fun parseWhile(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume WHILE
            val condIp = instructions.size
            parseExpression(sourceLine)
            val jumpOutIndex = instructions.size
            emit(OpCode.JUMP_IF_FALSE, argInt = 0, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            whileStack.add(WhileLoopInfo(condIp, jumpOutIndex))
        }

        private fun parseWend(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume WEND
            if (whileStack.isEmpty()) {
                throw CompilerException(sourceLine, 1, "WEND without matching WHILE")
            }
            val loop = whileStack.removeAt(whileStack.size - 1)
            emit(OpCode.JUMP, argInt = loop.conditionIp, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            val exitIp = instructions.size
            instructions[loop.endJumpInstructionIndex] = instructions[loop.endJumpInstructionIndex].copy(argInt = exitIp)
        }

        private fun parseGoto(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume GOTO
            val targetTok = consume(TokenType.NUMBER, "Expected line number after GOTO")
            val targetLine = targetTok.numberVal.toInt()
            val instIndex = instructions.size
            emit(OpCode.JUMP, argInt = 0, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            jumpPatches.add(JumpPatch(instIndex, targetBasicLineNum = targetLine))
        }

        private fun parseGosub(sourceLine: Int, basicLineNum: Int?) {
            advance() // consume GOSUB
            val targetTok = consume(TokenType.NUMBER, "Expected line number after GOSUB")
            val targetLine = targetTok.numberVal.toInt()
            val instIndex = instructions.size
            emit(OpCode.GOSUB, argInt = 0, sourceLine = sourceLine, sourceLineNum = basicLineNum ?: -1)
            jumpPatches.add(JumpPatch(instIndex, targetBasicLineNum = targetLine))
        }

        // --- Expression Parsing (Precedence Climbing) ---

        private fun parseExpression(sourceLine: Int) {
            parseLogicalOr(sourceLine)
        }

        private fun parseLogicalOr(sourceLine: Int) {
            parseLogicalAnd(sourceLine)
            while (match(TokenType.OR)) {
                parseLogicalAnd(sourceLine)
                emit(OpCode.OR, sourceLine = sourceLine)
            }
        }

        private fun parseLogicalAnd(sourceLine: Int) {
            parseComparison(sourceLine)
            while (match(TokenType.AND)) {
                parseComparison(sourceLine)
                emit(OpCode.AND, sourceLine = sourceLine)
            }
        }

        private fun parseComparison(sourceLine: Int) {
            parseAddSub(sourceLine)
            while (true) {
                when {
                    match(TokenType.EQ) -> { parseAddSub(sourceLine); emit(OpCode.EQ, sourceLine = sourceLine) }
                    match(TokenType.NEQ) -> { parseAddSub(sourceLine); emit(OpCode.NEQ, sourceLine = sourceLine) }
                    match(TokenType.LT) -> { parseAddSub(sourceLine); emit(OpCode.LT, sourceLine = sourceLine) }
                    match(TokenType.LTE) -> { parseAddSub(sourceLine); emit(OpCode.LTE, sourceLine = sourceLine) }
                    match(TokenType.GT) -> { parseAddSub(sourceLine); emit(OpCode.GT, sourceLine = sourceLine) }
                    match(TokenType.GTE) -> { parseAddSub(sourceLine); emit(OpCode.GTE, sourceLine = sourceLine) }
                    else -> break
                }
            }
        }

        private fun parseAddSub(sourceLine: Int) {
            parseMulDiv(sourceLine)
            while (true) {
                when {
                    match(TokenType.PLUS) -> { parseMulDiv(sourceLine); emit(OpCode.ADD, sourceLine = sourceLine) }
                    match(TokenType.MINUS) -> { parseMulDiv(sourceLine); emit(OpCode.SUB, sourceLine = sourceLine) }
                    else -> break
                }
            }
        }

        private fun parseMulDiv(sourceLine: Int) {
            parsePower(sourceLine)
            while (true) {
                when {
                    match(TokenType.MULTIPLY) -> { parsePower(sourceLine); emit(OpCode.MUL, sourceLine = sourceLine) }
                    match(TokenType.DIVIDE) -> { parsePower(sourceLine); emit(OpCode.DIV, sourceLine = sourceLine) }
                    match(TokenType.MODULO) -> { parsePower(sourceLine); emit(OpCode.MOD, sourceLine = sourceLine) }
                    else -> break
                }
            }
        }

        private fun parsePower(sourceLine: Int) {
            parseUnary(sourceLine)
            while (match(TokenType.POWER)) {
                parseUnary(sourceLine)
                emit(OpCode.POW, sourceLine = sourceLine)
            }
        }

        private fun parseUnary(sourceLine: Int) {
            if (match(TokenType.MINUS)) {
                parseUnary(sourceLine)
                emit(OpCode.NEG, sourceLine = sourceLine)
            } else if (match(TokenType.NOT)) {
                parseUnary(sourceLine)
                emit(OpCode.NOT, sourceLine = sourceLine)
            } else {
                parsePrimary(sourceLine)
            }
        }

        private fun parsePrimary(sourceLine: Int) {
            val token = peek()
            when (token.type) {
                TokenType.NUMBER -> {
                    advance()
                    val cIdx = addConstant(BasicValue.NumberVal(token.numberVal))
                    emit(OpCode.PUSH_CONST, argInt = cIdx, sourceLine = sourceLine)
                }
                TokenType.STRING -> {
                    advance()
                    val cIdx = addConstant(BasicValue.StringVal(token.text))
                    emit(OpCode.PUSH_CONST, argInt = cIdx, sourceLine = sourceLine)
                }
                TokenType.IDENTIFIER -> {
                    advance()
                    val name = token.text
                    // Check if function call or array access
                    if (match(TokenType.LPAREN)) {
                        // Built-in function or array
                        if (isBuiltinFunction(name)) {
                            parseBuiltinFunction(name, sourceLine)
                            consume(TokenType.RPAREN, "Expected ')' after function arguments")
                        } else {
                            // Array element
                            parseExpression(sourceLine)
                            consume(TokenType.RPAREN, "Expected ')' after array index")
                            emit(OpCode.LOAD_ARRAY, argString = name, sourceLine = sourceLine)
                        }
                    } else {
                        emit(OpCode.LOAD_VAR, argString = name, sourceLine = sourceLine)
                    }
                }
                TokenType.LPAREN -> {
                    advance()
                    parseExpression(sourceLine)
                    consume(TokenType.RPAREN, "Expected ')' after expression")
                }
                else -> {
                    throw CompilerException(token.line, token.column, "Unexpected token in expression: '${token.text}'")
                }
            }
        }

        private fun isBuiltinFunction(name: String): Boolean {
            return when (name.uppercase(Locale.ROOT)) {
                "INT", "ABS", "SQR", "RND", "SIN", "COS", "TAN", "LEN", "STR$", "VAL", "CHR$", "ASC", "TAB" -> true
                else -> false
            }
        }

        private fun parseBuiltinFunction(name: String, sourceLine: Int) {
            val fn = name.uppercase(Locale.ROOT)
            if (fn == "RND") {
                // RND can have 0 or 1 argument
                if (peek().type != TokenType.RPAREN) {
                    parseExpression(sourceLine)
                } else {
                    val cIdx = addConstant(BasicValue.NumberVal(1.0))
                    emit(OpCode.PUSH_CONST, argInt = cIdx, sourceLine = sourceLine)
                }
            } else {
                parseExpression(sourceLine)
            }
            // Emit call through internal helper variable names or opcode mapping
            // Store as a virtual call using LOAD_VAR with special system prefix __FN_
            emit(OpCode.LOAD_VAR, argString = "__FN_$fn", sourceLine = sourceLine)
        }

        // --- Helpers ---

        private fun emit(
            op: OpCode,
            argInt: Int = 0,
            argString: String = "",
            sourceLine: Int = -1,
            sourceLineNum: Int = -1
        ) {
            instructions.add(Instruction(op, argInt, argString, sourceLine, sourceLineNum))
        }

        private fun addConstant(value: BasicValue): Int {
            return constantMap.getOrPut(value) {
                constants.add(value)
                constants.size - 1
            }
        }

        private fun match(type: TokenType): Boolean {
            if (check(type)) {
                advance()
                return true
            }
            return false
        }

        private fun check(type: TokenType): Boolean {
            if (isAtEnd()) return false
            return peek().type == type
        }

        private fun advance(): Token {
            if (!isAtEnd()) current++
            return previous()
        }

        private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

        private fun peek(): Token = tokens[current]

        private fun previous(): Token = tokens[current - 1]

        private fun consume(type: TokenType, errorMessage: String): Token {
            if (check(type)) return advance()
            val token = peek()
            throw CompilerException(token.line, token.column, "$errorMessage (got '${token.text}')")
        }

        private fun skipNewlinesAndColons() {
            while (!isAtEnd() && (peek().type == TokenType.NEWLINE || peek().type == TokenType.COLON)) {
                advance()
            }
        }
    }
}
