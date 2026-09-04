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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.arrowescape.audio.SoundManager
import com.example.arrowescape.model.LevelState
import com.example.arrowescape.ui.theme.GameColors
import kotlinx.coroutines.delay
import java.util.Random

@Composable
fun WinDialog(
    levelState: LevelState,
    reducedMotion: Boolean,
    soundManager: SoundManager? = null,
    onNextLevelClick: () -> Unit,
    onLevelsMapClick: () -> Unit
) {
    val livesLost = levelState.maxLives - levelState.livesRemaining
    val starCount = when {
        livesLost == 0 && levelState.hintsUsed == 0 -> 3
        livesLost <= 1 && levelState.hintsUsed <= 1 -> 2
        else -> 1
    }

    val isEagleEye = livesLost == 0 && levelState.hintsUsed == 0
    val isWowCombo = livesLost == 0 && levelState.hintsUsed > 0

    // Score calculation
    val baseScore = 1000
    val eagleEyeBonus = if (isEagleEye) 500 else if (isWowCombo) 250 else 100
    val arrowBonus = levelState.arrows.size * 30
    val totalScore = baseScore + eagleEyeBonus + arrowBonus

    val gemsEarned = if (levelState.isDaily) 50 else 25

    // Animated score counter
    val animatedScore = remember { Animatable(0f) }

    // Star pop-in scale animatables
    val star1Scale = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val star2Scale = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val star3Scale = remember { Animatable(if (reducedMotion) 1f else 0f) }

    // Button pulse transition
    val infiniteTransition = rememberInfiniteTransition(label = "win_pulse")
    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_pulse"
    )

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            // Star 1
            delay(180)
            soundManager?.playStarSound(1)
            star1Scale.animateTo(1.35f, tween(140, easing = FastOutSlowInEasing))
            star1Scale.animateTo(1.0f, tween(70))

            // Star 2
            if (starCount >= 2) {
                delay(130)
                soundManager?.playStarSound(2)
                star2Scale.animateTo(1.35f, tween(140, easing = FastOutSlowInEasing))
                star2Scale.animateTo(1.0f, tween(70))
            }

            // Star 3
            if (starCount >= 3) {
                delay(130)
                soundManager?.playStarSound(3)
                star3Scale.animateTo(1.35f, tween(140, easing = FastOutSlowInEasing))
                star3Scale.animateTo(1.0f, tween(70))
            }

            // Rolling score count-up
            animatedScore.animateTo(
                targetValue = totalScore.toFloat(),
                animationSpec = tween(750, easing = FastOutSlowInEasing)
            )
        } else {
            animatedScore.snapTo(totalScore.toFloat())
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dual-cannon confetti shower
            if (!reducedMotion) {
                ConfettiBurst(modifier = Modifier.fillMaxSize())
            }

            // Modal Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Header with Shape Theme subtitle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (levelState.isDaily) "Daily Solved! 🌟" else "Level ${levelState.levelNumber} Cleared!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = GameColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        levelState.shapeName?.let { theme ->
                            Text(
                                text = "थीम: $theme",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    // Dynamic Performance Badge: EAGLE EYE or WOW!
                    if (isEagleEye) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFFEF3C7))
                                    )
                                )
                                .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🦅", fontSize = 18.sp)
                                Column {
                                    Text(
                                        text = "EAGLE EYE! (चील जैसी पारखी नज़र)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "बिना किसी गलती के सटीक समाधान!",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    } else if (isWowCombo) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFEDE9FE))
                                .border(1.5.dp, Color(0xFF8B5CF6), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "⚡", fontSize = 18.sp)
                                Text(
                                    text = "WOW! UNSTOPPABLE SPEED!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
                    }

                    // 3-Star Rating Row with dynamic scales
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Star 1
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star 1",
                            tint = if (starCount >= 1) GameColors.StarGold else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .size(46.dp)
                                .scale(star1Scale.value)
                        )
                        // Star 2 (Center Hero Star)
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star 2",
                            tint = if (starCount >= 2) GameColors.StarGold else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .size(58.dp)
                                .scale(star2Scale.value)
                        )
                        // Star 3
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star 3",
                            tint = if (starCount >= 3) GameColors.StarGold else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .size(46.dp)
                                .scale(star3Scale.value)
                        )
                    }

                    // Rolling Score Board Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LEVEL SCORE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "+${animatedScore.value.toInt()}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 बेस: $baseScore",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "🦅 बोनस: +$eagleEyeBonus",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Gems reward pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(GameColors.AmberLight)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "💎", fontSize = 16.sp)
                        Text(
                            text = "+$gemsEarned Gems",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GameColors.AmberAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Next Level Primary Action Button with Pulse Effect
                    Button(
                        onClick = onNextLevelClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(if (!reducedMotion) buttonPulse else 1f)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .testTag("next_level_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.AmberAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (levelState.isDaily) "लेवल लिस्ट देखें" else "Next Level 🚀",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Secondary Levels Map Button
                    OutlinedButton(
                        onClick = onLevelsMapClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("win_levels_map_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = null,
                                tint = GameColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All Levels (1-100)",
                                fontSize = 13.sp,
                                color = GameColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val random = remember { Random(42) }
    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = random.nextFloat(),
                y = random.nextFloat() * -0.5f,
                speed = 0.8f + random.nextFloat() * 1.5f,
                angle = (-75f + random.nextFloat() * 150f),
                color = when (random.nextInt(6)) {
                    0 -> GameColors.AmberAccent
                    1 -> GameColors.PrimaryBlue
                    2 -> Color(0xFF10B981)
                    3 -> Color(0xFFEC4899)
                    4 -> Color(0xFF8B5CF6)
                    else -> Color(0xFFEF4444)
                },
                size = 6f + random.nextFloat() * 10f
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(1400, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val progress = anim.value
        for (p in particles) {
            val radians = Math.toRadians(p.angle.toDouble())
            val px = (p.x * size.width + Math.cos(radians) * progress * 250f * p.speed).toFloat()
            val py = (p.y * size.height + Math.sin(radians) * progress * 400f * p.speed + progress * progress * 200f).toFloat()

            if (px in 0f..size.width && py in 0f..size.height) {
                drawRect(
                    color = p.color.copy(alpha = (1f - progress * 0.7f).coerceIn(0f, 1f)),
                    topLeft = Offset(px, py),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val angle: Float,
    val color: Color,
    val size: Float
)
