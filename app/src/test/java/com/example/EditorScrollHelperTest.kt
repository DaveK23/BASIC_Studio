package com.example

import com.example.basic.ui.EditorScrollHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorScrollHelperTest {

    @Test
    fun testCursorLineIndexCalculation() {
        val text = "10 PRINT \"HELLO\"\n20 LET X = 5\n30 IF X <= 5 THEN PRINT X\n40 END"

        // Cursor at position 0 (start of line 0)
        assertEquals(0, EditorScrollHelper.calculateCursorLineIndex(text, 0))

        // Cursor within first line
        assertEquals(0, EditorScrollHelper.calculateCursorLineIndex(text, 5))

        // Cursor at index of first newline (still line 0 before newline)
        val firstNewline = text.indexOf('\n')
        assertEquals(0, EditorScrollHelper.calculateCursorLineIndex(text, firstNewline))

        // Cursor immediately after first newline (start of line 1)
        assertEquals(1, EditorScrollHelper.calculateCursorLineIndex(text, firstNewline + 1))

        // Cursor on line 2 (line with <= operator)
        val line2Start = text.indexOf("30 IF")
        assertEquals(2, EditorScrollHelper.calculateCursorLineIndex(text, line2Start))
        assertEquals(2, EditorScrollHelper.calculateCursorLineIndex(text, line2Start + 10))

        // Cursor on line 3 (40 END)
        val line3Start = text.indexOf("40 END")
        assertEquals(3, EditorScrollHelper.calculateCursorLineIndex(text, line3Start))
        assertEquals(3, EditorScrollHelper.calculateCursorLineIndex(text, text.length))

        // Out of bounds positions clamped safely
        assertEquals(0, EditorScrollHelper.calculateCursorLineIndex(text, -10))
        assertEquals(3, EditorScrollHelper.calculateCursorLineIndex(text, 9999))
    }

    @Test
    fun testTargetScrollYKeepsCursorCenteredVertically() {
        val lineHeightPx = 48f
        val topPaddingPx = 16f
        val viewportHeightPx = 400

        // For line 0: lineCenterY = 16 + 0*48 + 24 = 40.
        // 40 - (400/2) = -160, coerced to 0 (top of viewport)
        val scrollYLine0 = EditorScrollHelper.calculateTargetScrollY(
            cursorLineIndex = 0,
            lineHeightPx = lineHeightPx,
            topPaddingPx = topPaddingPx,
            viewportHeightPx = viewportHeightPx
        )
        assertEquals(0, scrollYLine0)

        // For line 10:
        // lineTop = 16 + 10 * 48 = 496
        // lineCenterY = 496 + 24 = 520
        // targetScrollY = 520 - (400 / 2) = 520 - 200 = 320
        val scrollYLine10 = EditorScrollHelper.calculateTargetScrollY(
            cursorLineIndex = 10,
            lineHeightPx = lineHeightPx,
            topPaddingPx = topPaddingPx,
            viewportHeightPx = viewportHeightPx
        )
        assertEquals(320, scrollYLine10)

        // Verify that with this scroll offset, line 10 center is exactly at viewport center (200px)
        val line10CenterY = topPaddingPx + 10 * lineHeightPx + (lineHeightPx / 2f)
        val positionInViewport = line10CenterY - scrollYLine10
        assertEquals(viewportHeightPx / 2f, positionInViewport, 0.001f)
    }

    @Test
    fun testTargetScrollYWithZeroOrNegativeViewport() {
        assertEquals(0, EditorScrollHelper.calculateTargetScrollY(5, 48f, 16f, 0))
        assertEquals(0, EditorScrollHelper.calculateTargetScrollY(5, 48f, 16f, -100))
    }
}
