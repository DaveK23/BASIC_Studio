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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

/**
 * 320x200 Retro CRT Canvas Composable with hardware sprite overlays,
 * integer coordinate mapping, CRT bezel framing, and debug hitbox overlays.
 */
@Composable
fun RetroGraphicsCanvas(
    graphicsState: RetroGraphicsState,
    showDebugBoundingBoxes: Boolean = false,
    selectedSpriteId: Int? = null,
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

            // 3. Debug Bounding Boxes Overlay for active sprites
            if (showDebugBoundingBoxes && graphicsState.sprites.isNotEmpty()) {
                drawSpriteDebugOverlay(
                    sprites = graphicsState.sprites,
                    selectedSpriteId = selectedSpriteId,
                    pixelW = pixelW,
                    pixelH = pixelH,
                    canvasW = canvasW,
                    canvasH = canvasH
                )
            }
        }
    }
}

private val DebugBoxPalette = listOf(
    Color(0xFF00FF66), // 0: Neon Green
    Color(0xFFFF007F), // 1: Neon Magenta
    Color(0xFF00F0FF), // 2: Neon Cyan
    Color(0xFFFFEA00), // 3: Neon Yellow
    Color(0xFFFF6600), // 4: Neon Orange
    Color(0xFFA855F7), // 5: Neon Purple
    Color(0xFFFF3366), // 6: Coral Pink
    Color(0xFF38BDF8)  // 7: Sky Blue
)

private fun DrawScope.drawSpriteDebugOverlay(
    sprites: Map<Int, Sprite>,
    selectedSpriteId: Int?,
    pixelW: Float,
    pixelH: Float,
    canvasW: Float,
    canvasH: Float
) {
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 9.dp.toPx()
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
        isFakeBoldText = true
    }

    // 3a. Draw bounding box and label for each active sprite
    for ((id, sprite) in sprites) {
        val left = sprite.x * pixelW
        val top = sprite.y * pixelH
        val boxW = sprite.width * pixelW
        val boxH = sprite.height * pixelH
        val boxColor = DebugBoxPalette[id % DebugBoxPalette.size]
        val isSelected = (id == selectedSpriteId)

        if (sprite.visible) {
            // Translucent fill
            val fillAlpha = if (isSelected) 0.28f else 0.16f
            drawRect(
                color = boxColor.copy(alpha = fillAlpha),
                topLeft = Offset(left, top),
                size = Size(boxW, boxH)
            )

            // Outline stroke
            val strokeW = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx()
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(width = strokeW)
            )
        } else {
            // Hidden sprite (does not collide in SPRITEHIT)
            drawRect(
                color = boxColor.copy(alpha = 0.45f),
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                )
            )
        }

        // Label tag: e.g. "S1: [148,30] 8x8"
        val label = if (sprite.visible) {
            "S$id: [${sprite.x},${sprite.y}]"
        } else {
            "S$id: [${sprite.x},${sprite.y}] (HIDDEN)"
        }
        val textWidth = textPaint.measureText(label)
        val textHeight = 11.dp.toPx()
        val badgeX = left.coerceIn(2.dp.toPx(), (canvasW - textWidth - 6.dp.toPx()).coerceAtLeast(2.dp.toPx()))
        val badgeY = if (top >= 14.dp.toPx()) top - 3.dp.toPx() else top + boxH + 11.dp.toPx()

        drawRoundRect(
            color = Color(0xDD0F172A),
            topLeft = Offset(badgeX - 2.dp.toPx(), badgeY - 9.dp.toPx()),
            size = Size(textWidth + 4.dp.toPx(), textHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawContext.canvas.nativeCanvas.drawText(label, badgeX, badgeY, textPaint)
    }

    // 3b. Highlight any colliding/intersecting pairs matching SPRITEHIT
    val spriteList = sprites.values.toList()
    for (i in spriteList.indices) {
        val s1 = spriteList[i]
        for (j in i + 1 until spriteList.size) {
            val s2 = spriteList[j]
            if (s1.hits(s2)) {
                val overlapLeft = maxOf(s1.x, s2.x) * pixelW
                val overlapRight = minOf(s1.x + s1.width, s2.x + s2.width) * pixelW
                val overlapTop = maxOf(s1.y, s2.y) * pixelH
                val overlapBottom = minOf(s1.y + s1.height, s2.y + s2.height) * pixelH
                val overlapW = overlapRight - overlapLeft
                val overlapH = overlapBottom - overlapTop

                if (overlapW > 0 && overlapH > 0) {
                    drawRect(
                        color = Color(0xFFFF2222).copy(alpha = 0.5f),
                        topLeft = Offset(overlapLeft, overlapTop),
                        size = Size(overlapW, overlapH)
                    )
                    drawRect(
                        color = Color(0xFFFF2222),
                        topLeft = Offset(overlapLeft, overlapTop),
                        size = Size(overlapW, overlapH),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val hitLabel = "HIT S${s1.id}*S${s2.id}"
                    val hitPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.YELLOW
                        textSize = 9.dp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    val hitW = hitPaint.measureText(hitLabel)
                    val hitX = overlapLeft.coerceIn(2.dp.toPx(), (canvasW - hitW - 6.dp.toPx()).coerceAtLeast(2.dp.toPx()))
                    val hitY = (overlapTop + overlapH / 2f + 4.dp.toPx()).coerceIn(12.dp.toPx(), canvasH - 4.dp.toPx())

                    drawRoundRect(
                        color = Color(0xEE880000),
                        topLeft = Offset(hitX - 2.dp.toPx(), hitY - 9.dp.toPx()),
                        size = Size(hitW + 4.dp.toPx(), 11.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(hitLabel, hitX, hitY, hitPaint)
                }
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
