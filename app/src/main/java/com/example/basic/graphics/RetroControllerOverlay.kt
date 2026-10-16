package com.example.basic.graphics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * On-screen overlay providing an 8-way virtual directional joystick and
 * tactile arcade fire button for bitmap games (SCREEN 1).
 */
@Composable
fun RetroControllerOverlay(
    onJoystickChange: (direction: Int, axisX: Int, axisY: Int) -> Unit,
    onFireChange: (pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .testTag("retro_controller_overlay")
    ) {
        // Bottom row containing Joystick (left) and Fire Button (right)
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // 8-Way Virtual Directional Joystick (Bottom-Left)
            VirtualJoystick8Way(
                onDirectionChanged = onJoystickChange,
                modifier = Modifier
                    .padding(start = 4.dp, bottom = 4.dp)
                    .testTag("virtual_joystick_dpad")
            )

            // Tactile Arcade Fire Button (Bottom-Right)
            VirtualFireButton(
                onFireChanged = onFireChange,
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .testTag("virtual_fire_button")
            )
        }
    }
}

/**
 * 8-Way virtual thumbstick / D-pad.
 * Returns direction code 0..8:
 * 0: Center, 1: Up, 2: Up-Right, 3: Right, 4: Down-Right,
 * 5: Down, 6: Down-Left, 7: Left, 8: Up-Left.
 * Also returns digital axisX (-1, 0, 1) and axisY (-1, 0, 1).
 */
@Composable
fun VirtualJoystick8Way(
    onDirectionChanged: (direction: Int, axisX: Int, axisY: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseSizeDp = 100.dp
    val knobSizeDp = 40.dp
    val baseRadiusPx = with(density) { (baseSizeDp / 2).toPx() }
    val maxDragRadiusPx = baseRadiusPx * 0.75f
    val deadZonePx = with(density) { 10.dp.toPx() }

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    var currentDirection by remember { mutableIntStateOf(0) }

    fun updateTouch(touchX: Float, touchY: Float) {
        val dx = touchX - baseRadiusPx
        val dy = touchY - baseRadiusPx
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < deadZonePx) {
            knobOffsetX = 0f
            knobOffsetY = 0f
            if (currentDirection != 0) {
                currentDirection = 0
                onDirectionChanged(0, 0, 0)
            }
            return
        }

        // Clamp knob displacement
        val clampedDist = dist.coerceAtMost(maxDragRadiusPx)
        val angleRad = atan2(dy, dx)
        knobOffsetX = cos(angleRad) * clampedDist
        knobOffsetY = sin(angleRad) * clampedDist

        // Convert angle [-PI, PI] to 8 sectors:
        // Sector angles:
        // Right: around 0
        // Down-Right: around PI/4
        // Down: around PI/2
        // Down-Left: around 3PI/4
        // Left: around PI or -PI
        // Up-Left: around -3PI/4
        // Up: around -PI/2
        // Up-Right: around -PI/4
        var deg = Math.toDegrees(angleRad.toDouble())
        if (deg < 0) deg += 360.0

        val (dir, ax, ay) = when {
            deg in 337.5..360.0 || deg in 0.0..22.5 -> Triple(3, 1, 0)      // Right
            deg in 22.5..67.5 -> Triple(4, 1, 1)                             // Down-Right
            deg in 67.5..112.5 -> Triple(5, 0, 1)                            // Down
            deg in 112.5..157.5 -> Triple(6, -1, 1)                          // Down-Left
            deg in 157.5..202.5 -> Triple(7, -1, 0)                          // Left
            deg in 202.5..247.5 -> Triple(8, -1, -1)                         // Up-Left
            deg in 247.5..292.5 -> Triple(1, 0, -1)                          // Up
            else -> Triple(2, 1, -1)                                         // Up-Right
        }

        if (currentDirection != dir) {
            currentDirection = dir
            onDirectionChanged(dir, ax, ay)
        }
    }

    fun resetTouch() {
        knobOffsetX = 0f
        knobOffsetY = 0f
        if (currentDirection != 0) {
            currentDirection = 0
            onDirectionChanged(0, 0, 0)
        }
    }

    Box(
        modifier = modifier
            .size(baseSizeDp)
            .clip(CircleShape)
            .background(Color(0xB30B132B))
            .border(2.dp, Color(0x9938BDF8), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    updateTouch(down.position.x, down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            updateTouch(change.position.x, change.position.y)
                            change.consume()
                        } else {
                            resetTouch()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Direction indicators (Up, Down, Left, Right)
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            tint = if (currentDirection in listOf(1, 2, 8)) Color(0xFF38BDF8) else Color(0x5564748B),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            tint = if (currentDirection in listOf(4, 5, 6)) Color(0xFF38BDF8) else Color(0x5564748B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Left",
            tint = if (currentDirection in listOf(6, 7, 8)) Color(0xFF38BDF8) else Color(0x5564748B),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Right",
            tint = if (currentDirection in listOf(2, 3, 4)) Color(0xFF38BDF8) else Color(0x5564748B),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(20.dp)
        )

        // Movable central thumb knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt()) }
                .size(knobSizeDp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (currentDirection != 0) Color(0xFF0284C7) else Color(0xFF334155),
                            if (currentDirection != 0) Color(0xFF0369A1) else Color(0xFF1E293B)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (currentDirection != 0) Color(0xFF38BDF8) else Color(0xFF475569),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Arcade-style tactile FIRE push-button.
 */
@Composable
fun VirtualFireButton(
    onFireChanged: (pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.90f else 1.0f, label = "button_scale")

    Box(
        modifier = modifier
            .size(68.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xB31E1B4B))
            .border(2.dp, if (isPressed) Color(0xFFF87171) else Color(0x99EF4444), CircleShape)
            .padding(4.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    onFireChanged(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onFireChanged(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D))
                        } else {
                            listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                        }
                    )
                )
                .border(1.5.dp, if (isPressed) Color(0xFFFCA5A5) else Color(0xFFF87171), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "FIRE",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
