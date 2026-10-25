package com.example.basic.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * 320x200 Retro CRT Canvas Composable with hardware sprite overlays,
 * integer coordinate mapping, and CRT bezel framing.
 */
@Composable
fun RetroGraphicsCanvas(
    graphicsState: RetroGraphicsState,
    modifier: Modifier = Modifier
) {
    // 320x200 has an aspect ratio of 16:10 (1.6)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(2.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(4.dp)
            .testTag("retro_graphics_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(4.dp))
                .background(RetroColorPalette[graphicsState.backgroundColor.coerceIn(0, 15)])
                .testTag("retro_screen_canvas")
        ) {
            val canvasW = size.width
            val canvasH = size.height

            val pixelW = canvasW / 320f
            val pixelH = canvasH / 200f

            // 1. Draw virtual framebuffer pixels
            val pixels = graphicsState.pixels
            val bgCol = graphicsState.backgroundColor
            for (y in 0 until 200) {
                val rowOffset = y * 320
                for (x in 0 until 320) {
                    val colorIdx = pixels[rowOffset + x]
                    if (colorIdx != bgCol && colorIdx in RetroColorPalette.indices) {
                        drawRect(
                            color = RetroColorPalette[colorIdx],
                            topLeft = Offset(x * pixelW, y * pixelH),
                            size = Size(pixelW + 0.5f, pixelH + 0.5f)
                        )
                    }
                }
            }

            // 2. Draw hardware sprites
            for ((_, sprite) in graphicsState.sprites) {
                if (!sprite.visible) continue
                drawSprite(sprite, pixelW, pixelH)
            }
        }
    }
}

private fun DrawScope.drawSprite(sprite: Sprite, pixelW: Float, pixelH: Float) {
    val defaultColor = RetroColorPalette[sprite.colorIndex.coerceIn(0, 15)]
    val width = sprite.width
    val height = sprite.height
    val pattern = sprite.pattern
    val colorPattern = sprite.colorPattern

    for (sy in 0 until height) {
        val rowOffset = sy * width
        for (sx in 0 until width) {
            val bitIdx = rowOffset + sx
            if (bitIdx in pattern.indices && pattern[bitIdx]) {
                val color = if (colorPattern != null && bitIdx in colorPattern.indices) {
                    RetroColorPalette[colorPattern[bitIdx].coerceIn(0, 15)]
                } else {
                    defaultColor
                }
                val px = (sprite.x + sx) * pixelW
                val py = (sprite.y + sy) * pixelH
                drawRect(
                    color = color,
                    topLeft = Offset(px, py),
                    size = Size(pixelW + 0.5f, pixelH + 0.5f)
                )
            }
        }
    }
}
