package com.example.arrowescape.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.ui.theme.GameColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onDismiss: () -> Unit
) {
    val rocketProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Pulsing radar rings behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LaunchedEffect(Unit) {
        // 1. Rocket accelerates into the center
        rocketProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(650, easing = FastOutSlowInEasing)
        )
        // 2. Title and subtitle fade in
        titleAlpha.animateTo(1f, tween(300))
        subtitleAlpha.animateTo(1f, tween(300))

        // 3. Hold for visual branding impact, then auto dismiss
        delay(800)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B), // Indigo 950
                        Color(0xFF090D16)  // Midnight Obsidian
                    )
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Decorative background geometric grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 48.dp.toPx()
            val cols = (size.width / gridSpacing).toInt() + 1
            val rows = (size.height / gridSpacing).toInt() + 1

            for (c in 0..cols) {
                val x = c * gridSpacing
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            for (r in 0..rows) {
                val y = r * gridSpacing
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Rocket Arrow Logo Container
            Box(
                modifier = Modifier
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer shockwave aura
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GameColors.AmberAccent.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.48f
                    )
                }

                // Center Rocket Arrow Emblem
                Canvas(modifier = Modifier.size(110.dp)) {
                    val p = rocketProgress.value
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Rocket body offset while flying in from bottom-left
                    val offsetX = (1f - p) * -80.dp.toPx()
                    val offsetY = (1f - p) * 120.dp.toPx()

                    // Trailing fiery jet thrust
                    if (p > 0.05f) {
                        val flamePath = Path().apply {
                            moveTo(cx + offsetX - 16.dp.toPx(), cy + offsetY + 24.dp.toPx())
                            lineTo(cx + offsetX - 28.dp.toPx(), cy + offsetY + 54.dp.toPx())
                            lineTo(cx + offsetX - 8.dp.toPx(), cy + offsetY + 32.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = flamePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFBBF24), Color(0xFFEF4444), Color.Transparent)
                            )
                        )
                    }

                    // Modern Arrow Emblem (Stylized curved supersonic arrow)
                    val arrowPath = Path().apply {
                        val startX = cx + offsetX - 30.dp.toPx()
                        val startY = cy + offsetY + 30.dp.toPx()
                        val midX = cx + offsetX - 5.dp.toPx()
                        val midY = cy + offsetY - 5.dp.toPx()
                        val headX = cx + offsetX + 24.dp.toPx()
                        val headY = cy + offsetY - 24.dp.toPx()

                        moveTo(startX, startY)
                        quadraticTo(midX, startY, midX, midY)
                        lineTo(headX, headY)
                    }

                    // Neon trail stroke
                    drawPath(
                        path = arrowPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFFF43F5E))
                        ),
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Arrowhead points
                    val headPoint = Offset(cx + offsetX + 24.dp.toPx(), cy + offsetY - 24.dp.toPx())
                    val wing1 = Offset(headPoint.x - 16.dp.toPx(), headPoint.y + 4.dp.toPx())
                    val wing2 = Offset(headPoint.x - 4.dp.toPx(), headPoint.y + 16.dp.toPx())

                    val headPath = Path().apply {
                        moveTo(headPoint.x, headPoint.y)
                        lineTo(wing1.x, wing1.y)
                        lineTo(cx + offsetX + 10.dp.toPx(), cy + offsetY - 10.dp.toPx())
                        lineTo(wing2.x, wing2.y)
                        close()
                    }
                    drawPath(path = headPath, color = Color(0xFFF59E0B))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title
            Text(
                text = "ARROW ESCAPE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 3.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(0.8f + 0.2f * titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Tagline
            Text(
                text = "ROCKET MAZE PUZZLE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = Color(0xFF38BDF8), // Electric Sky Blue
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(0.8f + 0.2f * subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Loading / Tap to play prompt
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "टैप करें या लोड हो रहा है... 🚀",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
