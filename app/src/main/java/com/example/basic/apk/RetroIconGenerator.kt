package com.example.basic.apk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Metadata for one of the 8 selectable launcher icons.
 */
data class LauncherIconOption(
    val id: Int,
    val name: String,
    val description: String,
    val primaryColorHex: String
)

/**
 * Generates custom 8-bit retro styled app launcher icon Bitmaps and binary adaptive drawables.
 */
object RetroIconGenerator {

    val ICONS: List<LauncherIconOption> = listOf(
        LauncherIconOption(0, "Retro Microcomputer", "Classic 80s beige desktop with phosphor monitor", "#3B82F6"),
        LauncherIconOption(1, "BASIC 8-Bit Prompt", "CRT glowing green command line prompt", "#10B981"),
        LauncherIconOption(2, "3.5\" Floppy Disk", "Retro blue data diskette with white label", "#6366F1"),
        LauncherIconOption(3, "Silicon Microchip", "Black DIP IC chip with golden connection pins", "#F59E0B"),
        LauncherIconOption(4, "Space Invader", "Classic pixel art alien arcade creature", "#EC4899"),
        LauncherIconOption(5, "Arcade Joystick", "Red ball arcade stick with dual pushbuttons", "#EF4444"),
        LauncherIconOption(6, "Pixel Diamond", "8-bit cyan gemstone item powerup", "#06B6D4"),
        LauncherIconOption(7, "Cassette Tape", "Retro data cassette tape for loading BASIC", "#8B5CF6")
    )

    /**
     * Generates a square or circular PNG bitmap of size [size] for icon option [iconIndex].
     */
    fun generateIconBitmap(iconIndex: Int, size: Int = 192, isRound: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background color
        val bgColors = listOf(
            0xFF1E293B.toInt(), // 0: Slate Dark
            0xFF064E3B.toInt(), // 1: Emerald Dark
            0xFF1E1B4B.toInt(), // 2: Indigo Dark
            0xFF451A03.toInt(), // 3: Amber Dark
            0xFF500724.toInt(), // 4: Pink Dark
            0xFF450A0A.toInt(), // 5: Red Dark
            0xFF083344.toInt(), // 6: Cyan Dark
            0xFF2E1065.toInt()  // 7: Violet Dark
        )
        val bgColor = bgColors[iconIndex.coerceIn(0, bgColors.lastIndex)]
        paint.color = bgColor

        if (isRound) {
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        } else {
            val corner = size * 0.22f
            canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), corner, corner, paint)
        }

        // Pixel scale factor
        val p = size / 24f

        when (iconIndex % 8) {
            0 -> drawMicrocomputer(canvas, paint, p, size)
            1 -> drawBasicPrompt(canvas, paint, p, size)
            2 -> drawFloppyDisk(canvas, paint, p, size)
            3 -> drawMicrochip(canvas, paint, p, size)
            4 -> drawSpaceInvader(canvas, paint, p, size)
            5 -> drawArcadeJoystick(canvas, paint, p, size)
            6 -> drawPixelDiamond(canvas, paint, p, size)
            7 -> drawCassetteTape(canvas, paint, p, size)
        }

        return bitmap
    }

    fun generateIconBytes(iconIndex: Int, size: Int = 192, isRound: Boolean = false, isWebp: Boolean = false): ByteArray {
        val bmp = generateIconBitmap(iconIndex, size, isRound)
        val stream = ByteArrayOutputStream()
        if (isWebp) {
            @Suppress("DEPRECATION")
            bmp.compress(Bitmap.CompressFormat.WEBP, 100, stream)
        } else {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return stream.toByteArray()
    }

    fun generateIconPngBytes(iconIndex: Int, size: Int = 192, isRound: Boolean = false): ByteArray {
        return generateIconBytes(iconIndex, size, isRound, isWebp = false)
    }

    /**
     * Retrieves the compiled binary XML VectorDrawable for the adaptive icon foreground.
     */
    fun getAdaptiveForegroundBytes(context: Context?, iconIndex: Int): ByteArray {
        val embedded = RetroIconData.getForegroundBytes(iconIndex)
        if (embedded.isNotEmpty()) return embedded

        val idx = iconIndex.coerceIn(0, 7)
        val assetPath = "icons/fg_$idx.bin"
        try {
            if (context != null) {
                context.assets.open(assetPath).use { return it.readBytes() }
            }
        } catch (_: Throwable) {}
        try {
            val stream = RetroIconGenerator::class.java.classLoader?.getResourceAsStream("assets/$assetPath")
                ?: RetroIconGenerator::class.java.getResourceAsStream("/$assetPath")
            if (stream != null) {
                stream.use { return it.readBytes() }
            }
        } catch (_: Throwable) {}
        try {
            val file = File("app/src/main/assets/$assetPath").takeIf { it.exists() }
                ?: File("src/main/assets/$assetPath").takeIf { it.exists() }
            if (file != null) return file.readBytes()
        } catch (_: Throwable) {}
        return ByteArray(0)
    }

    /**
     * Retrieves the compiled binary XML VectorDrawable for the adaptive icon background.
     */
    fun getAdaptiveBackgroundBytes(context: Context?, iconIndex: Int): ByteArray {
        val embedded = RetroIconData.getBackgroundBytes(iconIndex)
        if (embedded.isNotEmpty()) return embedded

        val idx = iconIndex.coerceIn(0, 7)
        val assetPath = "icons/bg_$idx.bin"
        try {
            if (context != null) {
                context.assets.open(assetPath).use { return it.readBytes() }
            }
        } catch (_: Throwable) {}
        try {
            val stream = RetroIconGenerator::class.java.classLoader?.getResourceAsStream("assets/$assetPath")
                ?: RetroIconGenerator::class.java.getResourceAsStream("/$assetPath")
            if (stream != null) {
                stream.use { return it.readBytes() }
            }
        } catch (_: Throwable) {}
        try {
            val file = File("app/src/main/assets/$assetPath").takeIf { it.exists() }
                ?: File("src/main/assets/$assetPath").takeIf { it.exists() }
            if (file != null) return file.readBytes()
        } catch (_: Throwable) {}
        return ByteArray(0)
    }

    private fun drawMicrocomputer(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        // Monitor bezel
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(RectF(4 * p, 4 * p, 20 * p, 16 * p), paint)
        // CRT screen
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(RectF(6 * p, 6 * p, 18 * p, 14 * p), paint)
        // Screen prompt
        paint.color = Color.parseColor("#10B981")
        canvas.drawRect(RectF(8 * p, 8 * p, 13 * p, 10 * p), paint)
        canvas.drawRect(RectF(8 * p, 11 * p, 10 * p, 12 * p), paint)
        // Keyboard base
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRect(RectF(3 * p, 17 * p, 21 * p, 20 * p), paint)
        // Spacebar
        paint.color = Color.parseColor("#475569")
        canvas.drawRect(RectF(8 * p, 18 * p, 16 * p, 19.5f * p), paint)
    }

    private fun drawBasicPrompt(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        // Dark CRT Glass
        paint.color = Color.parseColor("#052E16")
        canvas.drawRect(RectF(3 * p, 3 * p, 21 * p, 21 * p), paint)
        // Glowing text: "READY."
        paint.color = Color.parseColor("#10B981")
        // Draw prompt symbol "> _"
        paint.strokeWidth = 2 * p
        paint.style = Paint.Style.STROKE
        canvas.drawLine(6 * p, 8 * p, 11 * p, 12 * p, paint)
        canvas.drawLine(11 * p, 12 * p, 6 * p, 16 * p, paint)
        paint.style = Paint.Style.FILL
        // Blinking cursor block
        canvas.drawRect(RectF(14 * p, 10 * p, 18 * p, 15 * p), paint)
    }

    private fun drawFloppyDisk(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        // Diskette Body
        paint.color = Color.parseColor("#3B82F6")
        canvas.drawRect(RectF(4 * p, 4 * p, 20 * p, 20 * p), paint)
        // Metal shutter
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(RectF(7 * p, 4 * p, 17 * p, 11 * p), paint)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(RectF(12 * p, 6 * p, 15 * p, 10 * p), paint)
        // Paper label
        paint.color = Color.parseColor("#FEF08A")
        canvas.drawRect(RectF(6 * p, 13 * p, 18 * p, 19 * p), paint)
        paint.color = Color.parseColor("#DC2626")
        canvas.drawRect(RectF(8 * p, 14.5f * p, 16 * p, 15.5f * p), paint)
    }

    private fun drawMicrochip(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        // Pins on left and right
        paint.color = Color.parseColor("#F59E0B")
        val pinY = listOf(6f, 9f, 12f, 15f, 18f)
        for (y in pinY) {
            canvas.drawRect(RectF(2 * p, y * p, 6 * p, (y + 1.5f) * p), paint)
            canvas.drawRect(RectF(18 * p, y * p, 22 * p, (y + 1.5f) * p), paint)
        }
        // Black IC package
        paint.color = Color.parseColor("#18181B")
        canvas.drawRect(RectF(5 * p, 4 * p, 19 * p, 20 * p), paint)
        // Chip notch & label
        paint.color = Color.parseColor("#71717A")
        canvas.drawCircle(12 * p, 5.5f * p, 1.5f * p, paint)
        paint.color = Color.parseColor("#E4E4E7")
        paint.textSize = 3 * p
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("BASIC", 12 * p, 13 * p, paint)
        canvas.drawText("6502", 12 * p, 16.5f * p, paint)
    }

    private fun drawSpaceInvader(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        paint.color = Color.parseColor("#F43F5E")
        // 11x8 classic invader sprite grid
        val invaderBits = listOf(
            "00100000100",
            "00010001000",
            "00111111100",
            "01101110110",
            "11111111111",
            "10111111101",
            "10100000101",
            "00011011000"
        )
        val offsetX = 6.5f * p
        val offsetY = 7.5f * p
        val pixelSize = 1f * p

        for (row in invaderBits.indices) {
            val line = invaderBits[row]
            for (col in line.indices) {
                if (line[col] == '1') {
                    canvas.drawRect(
                        offsetX + col * pixelSize,
                        offsetY + row * pixelSize,
                        offsetX + (col + 1) * pixelSize,
                        offsetY + (row + 1) * pixelSize,
                        paint
                    )
                }
            }
        }
    }

    private fun drawArcadeJoystick(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        // Metal base
        paint.color = Color.parseColor("#334155")
        canvas.drawRect(RectF(4 * p, 14 * p, 20 * p, 19 * p), paint)
        // Joystick shaft
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 2.5f * p
        canvas.drawLine(10 * p, 14 * p, 9 * p, 7 * p, paint)
        // Red ball knob
        paint.color = Color.parseColor("#EF4444")
        canvas.drawCircle(8.5f * p, 6.5f * p, 3.5f * p, paint)
        // Action Buttons
        paint.color = Color.parseColor("#3B82F6")
        canvas.drawCircle(15 * p, 16.5f * p, 1.8f * p, paint)
        paint.color = Color.parseColor("#10B981")
        canvas.drawCircle(18 * p, 15.5f * p, 1.8f * p, paint)
    }

    private fun drawPixelDiamond(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        paint.color = Color.parseColor("#22D3EE")
        // Faceted gem
        val path = android.graphics.Path()
        path.moveTo(12 * p, 4 * p)
        path.lineTo(19 * p, 10 * p)
        path.lineTo(12 * p, 20 * p)
        path.lineTo(5 * p, 10 * p)
        path.close()
        canvas.drawPath(path, paint)

        // Highlight facet
        paint.color = Color.parseColor("#E0F2FE")
        val highlight = android.graphics.Path()
        highlight.moveTo(12 * p, 4 * p)
        highlight.lineTo(12 * p, 10 * p)
        highlight.lineTo(5 * p, 10 * p)
        highlight.close()
        canvas.drawPath(highlight, paint)

        // Center line
        paint.color = Color.parseColor("#0891B2")
        canvas.drawLine(12 * p, 10 * p, 12 * p, 20 * p, paint)
    }

    private fun drawCassetteTape(canvas: Canvas, paint: Paint, p: Float, size: Int) {
        // Cassette body
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(RectF(4 * p, 6 * p, 20 * p, 18 * p), paint)
        // Label
        paint.color = Color.parseColor("#A855F7")
        canvas.drawRect(RectF(6 * p, 8 * p, 18 * p, 15 * p), paint)
        // Tape reels window
        paint.color = Color.parseColor("#F3E8FF")
        canvas.drawRect(RectF(8 * p, 10 * p, 16 * p, 14 * p), paint)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawCircle(10 * p, 12 * p, 1.5f * p, paint)
        canvas.drawCircle(14 * p, 12 * p, 1.5f * p, paint)
    }
}
