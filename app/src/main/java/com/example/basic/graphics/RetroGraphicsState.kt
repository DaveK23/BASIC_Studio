package com.example.basic.graphics

import androidx.compose.ui.graphics.Color

/**
 * Classic 16-color retro computing palette (C64 / CGA / MSX inspired).
 */
val RetroColorPalette: List<Color> = listOf(
    Color(0xFF000000), // 0: Black
    Color(0xFFFFFFFF), // 1: White
    Color(0xFF880000), // 2: Red
    Color(0xFFAAFEFF), // 3: Cyan
    Color(0xFFCC44CC), // 4: Purple / Magenta
    Color(0xFF00CC55), // 5: Green
    Color(0xFF0000AA), // 6: Blue
    Color(0xFFEEEE77), // 7: Yellow
    Color(0xFFDD8855), // 8: Orange
    Color(0xFF664400), // 9: Brown
    Color(0xFFFF7777), // 10: Light Red
    Color(0xFF333333), // 11: Dark Gray
    Color(0xFF777777), // 12: Gray
    Color(0xFFAAFF66), // 13: Light Green
    Color(0xFF0088FF), // 14: Light Blue
    Color(0xFFBBBBBB)  // 15: Light Gray
)

/**
 * Represents an individual sprite on the retro graphics canvas.
 */
data class Sprite(
    val id: Int,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 8,
    val height: Int = 8,
    val colorIndex: Int = 1,
    val visible: Boolean = true,
    // Pixel pattern: 2D array or flat row bits (true = colored pixel, false = transparent)
    val pattern: List<Boolean> = List(64) { false }
) {
    /**
     * Checks bounding box collision with another sprite.
     */
    fun hits(other: Sprite): Boolean {
        if (!this.visible || !other.visible) return false
        val left1 = x
        val right1 = x + width
        val top1 = y
        val bottom1 = y + height

        val left2 = other.x
        val right2 = other.x + other.width
        val top2 = other.y
        val bottom2 = other.y + other.height

        return !(left1 >= right2 || right1 <= left2 || top1 >= bottom2 || bottom1 <= top2)
    }
}

/**
 * Full retro graphics state containing virtual framebuffer (320x200),
 * current screen mode, drawing colors, and active sprites.
 */
data class RetroGraphicsState(
    val mode: Int = 0, // 0 = Text/Terminal, 1 = 320x200 16-color Graphics + Sprites
    val width: Int = 320,
    val height: Int = 200,
    val currentColor: Int = 1,
    val backgroundColor: Int = 0,
    // Framebuffer: 320x200 integers representing color index (0..15).
    // Using IntArray copy for fast immutable state snapshots in Compose.
    val pixels: IntArray = IntArray(320 * 200) { 0 },
    // 16 hardware sprite slots (0..15)
    val sprites: Map<Int, Sprite> = emptyMap(),
    // Monotonically increasing revision to trigger recomposition when pixels update
    val revision: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RetroGraphicsState) return false
        return mode == other.mode &&
                revision == other.revision &&
                sprites == other.sprites
    }

    override fun hashCode(): Int {
        var result = mode
        result = 31 * result + revision.hashCode()
        result = 31 * result + sprites.hashCode()
        return result
    }

    fun copyWithPixel(x: Int, y: Int, color: Int): RetroGraphicsState {
        if (x !in 0 until width || y !in 0 until height) return this
        val newPixels = pixels.copyOf()
        newPixels[y * width + x] = color.coerceIn(0, 15)
        return copy(pixels = newPixels, revision = revision + 1)
    }
}
