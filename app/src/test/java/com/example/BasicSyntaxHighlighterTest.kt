package com.example

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.basic.editor.BasicSyntaxHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicSyntaxHighlighterTest {

    @Test
    fun testCommonKeywordsAreCategorized() {
        // Control flow keywords
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("IF"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("THEN"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("ELSE"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("GOTO"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("GOSUB"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("RETURN"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("FOR"))
        assertTrue(BasicSyntaxHighlighter.CONTROL_FLOW_KEYWORDS.contains("NEXT"))

        // Common commands
        assertTrue(BasicSyntaxHighlighter.COMMAND_KEYWORDS.contains("PRINT"))
        assertTrue(BasicSyntaxHighlighter.COMMAND_KEYWORDS.contains("INPUT"))
        assertTrue(BasicSyntaxHighlighter.COMMAND_KEYWORDS.contains("LET"))
        assertTrue(BasicSyntaxHighlighter.COMMAND_KEYWORDS.contains("DIM"))
        assertTrue(BasicSyntaxHighlighter.COMMAND_KEYWORDS.contains("CLS"))
        assertTrue(BasicSyntaxHighlighter.COMMAND_KEYWORDS.contains("COLOR"))
    }

    @Test
    fun testHighlightPrintAndInputStatements() {
        val code = "10 PRINT \"HELLO WORLD\"\n20 INPUT \"NAME: \", N$"
        val annotated = BasicSyntaxHighlighter.highlight(code)

        // Find style for PRINT
        val printStart = code.indexOf("PRINT")
        val printStyles = annotated.spanStyles.filter { it.start <= printStart && it.end >= printStart + 5 }
        assertTrue("PRINT must have a SpanStyle", printStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.COMMAND_COLOR, printStyles.first().item.color)
        assertEquals(FontWeight.Bold, printStyles.first().item.fontWeight)

        // Find style for INPUT
        val inputStart = code.indexOf("INPUT")
        val inputStyles = annotated.spanStyles.filter { it.start <= inputStart && it.end >= inputStart + 5 }
        assertTrue("INPUT must have a SpanStyle", inputStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.COMMAND_COLOR, inputStyles.first().item.color)
        assertEquals(FontWeight.Bold, inputStyles.first().item.fontWeight)

        // Find style for String literal
        val strStart = code.indexOf("\"HELLO WORLD\"")
        val strStyles = annotated.spanStyles.filter { it.start <= strStart && it.end >= strStart + 13 }
        assertTrue("String literal must be styled", strStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.STRING_COLOR, strStyles.first().item.color)

        // Find style for String variable N$
        val varStart = code.indexOf("N$")
        val varStyles = annotated.spanStyles.filter { it.start <= varStart && it.end >= varStart + 2 }
        assertTrue("String variable N$ must be styled", varStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.STRING_VAR_COLOR, varStyles.first().item.color)
    }

    @Test
    fun testHighlightIfThenGotoConditionals() {
        val code = "140 IF GUESS = SECRET THEN GOTO 200"
        val annotated = BasicSyntaxHighlighter.highlight(code)

        // IF should be CONTROL_FLOW_COLOR
        val ifStart = code.indexOf("IF")
        val ifStyles = annotated.spanStyles.filter { it.start <= ifStart && it.end >= ifStart + 2 }
        assertEquals(BasicSyntaxHighlighter.CONTROL_FLOW_COLOR, ifStyles.first().item.color)
        assertEquals(FontWeight.Bold, ifStyles.first().item.fontWeight)

        // THEN should be CONTROL_FLOW_COLOR
        val thenStart = code.indexOf("THEN")
        val thenStyles = annotated.spanStyles.filter { it.start <= thenStart && it.end >= thenStart + 4 }
        assertEquals(BasicSyntaxHighlighter.CONTROL_FLOW_COLOR, thenStyles.first().item.color)
        assertEquals(FontWeight.Bold, thenStyles.first().item.fontWeight)

        // GOTO should be CONTROL_FLOW_COLOR
        val gotoStart = code.indexOf("GOTO")
        val gotoStyles = annotated.spanStyles.filter { it.start <= gotoStart && it.end >= gotoStart + 4 }
        assertEquals(BasicSyntaxHighlighter.CONTROL_FLOW_COLOR, gotoStyles.first().item.color)
        assertEquals(FontWeight.Bold, gotoStyles.first().item.fontWeight)

        // 200 after GOTO is a jump target line number, styled with LINE_LABEL_COLOR
        val targetStart = code.indexOf("200")
        val targetStyles = annotated.spanStyles.filter { it.start <= targetStart && it.end >= targetStart + 3 }
        assertEquals(BasicSyntaxHighlighter.LINE_LABEL_COLOR, targetStyles.first().item.color)
        assertEquals(FontWeight.Bold, targetStyles.first().item.fontWeight)

        // Leading line number 140
        val line140Styles = annotated.spanStyles.filter { it.start == 0 && it.end == 3 }
        assertEquals(BasicSyntaxHighlighter.LINE_LABEL_COLOR, line140Styles.first().item.color)
        assertEquals(FontWeight.Bold, line140Styles.first().item.fontWeight)
    }

    @Test
    fun testHighlightComments() {
        val code = "10 REM This is a comment\n20 ' Another comment"
        val annotated = BasicSyntaxHighlighter.highlight(code)

        val remStart = code.indexOf("REM")
        val remStyles = annotated.spanStyles.filter { it.start == remStart }
        assertTrue("REM must be styled as comment", remStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.COMMENT_COLOR, remStyles.first().item.color)
        assertEquals(FontStyle.Italic, remStyles.first().item.fontStyle)

        val tickStart = code.indexOf("'")
        val tickStyles = annotated.spanStyles.filter { it.start == tickStart }
        assertTrue("Apostrophe comment must be styled as comment", tickStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.COMMENT_COLOR, tickStyles.first().item.color)
        assertEquals(FontStyle.Italic, tickStyles.first().item.fontStyle)
    }

    @Test
    fun testHighlightBuiltInFunctionsAndOperators() {
        val code = "50 VAL = SIN(A) * SQR(B) + RND(10)"
        val annotated = BasicSyntaxHighlighter.highlight(code)

        // SIN
        val sinStart = code.indexOf("SIN")
        val sinStyles = annotated.spanStyles.filter { it.start <= sinStart && it.end >= sinStart + 3 }
        assertEquals(BasicSyntaxHighlighter.FUNCTION_COLOR, sinStyles.first().item.color)

        // SQR
        val sqrStart = code.indexOf("SQR")
        val sqrStyles = annotated.spanStyles.filter { it.start <= sqrStart && it.end >= sqrStart + 3 }
        assertEquals(BasicSyntaxHighlighter.FUNCTION_COLOR, sqrStyles.first().item.color)

        // Operators * and +
        val mulStart = code.indexOf("*")
        val mulStyles = annotated.spanStyles.filter { it.start == mulStart && it.end == mulStart + 1 }
        assertEquals(BasicSyntaxHighlighter.OPERATOR_COLOR, mulStyles.first().item.color)

        val plusStart = code.indexOf("+")
        val plusStyles = annotated.spanStyles.filter { it.start == plusStart && it.end == plusStart + 1 }
        assertEquals(BasicSyntaxHighlighter.OPERATOR_COLOR, plusStyles.first().item.color)
    }

    @Test
    fun testDisablesLigaturesOnOperatorsAndTokens() {
        assertEquals("'calt' 0, 'liga' 0", BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES)

        val code = "10 IF X <= 5 AND Y >= 10 AND Z <> 0 THEN GOTO 100"
        val annotated = BasicSyntaxHighlighter.highlight(code)

        // <= operator
        val lteStart = code.indexOf("<=")
        val lteStyles = annotated.spanStyles.filter { it.start == lteStart && it.end == lteStart + 2 }
        assertTrue("<= operator should have a SpanStyle", lteStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.OPERATOR_COLOR, lteStyles.first().item.color)
        assertEquals(BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES, lteStyles.first().item.fontFeatureSettings)

        // >= operator
        val gteStart = code.indexOf(">=")
        val gteStyles = annotated.spanStyles.filter { it.start == gteStart && it.end == gteStart + 2 }
        assertTrue(">= operator should have a SpanStyle", gteStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.OPERATOR_COLOR, gteStyles.first().item.color)
        assertEquals(BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES, gteStyles.first().item.fontFeatureSettings)

        // <> operator
        val neqStart = code.indexOf("<>")
        val neqStyles = annotated.spanStyles.filter { it.start == neqStart && it.end == neqStart + 2 }
        assertTrue("<> operator should have a SpanStyle", neqStyles.isNotEmpty())
        assertEquals(BasicSyntaxHighlighter.OPERATOR_COLOR, neqStyles.first().item.color)
        assertEquals(BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES, neqStyles.first().item.fontFeatureSettings)

        // All spans should have fontFeatureSettings configured to disable ligatures
        for (span in annotated.spanStyles) {
            assertEquals(
                "Span style must disable font ligatures",
                BasicSyntaxHighlighter.FONT_FEATURE_SETTINGS_NO_LIGATURES,
                span.item.fontFeatureSettings
            )
        }
    }
}
