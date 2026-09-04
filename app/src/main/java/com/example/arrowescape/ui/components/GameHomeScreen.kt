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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.model.PlayerProgress
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun GameHomeScreen(
    progress: PlayerProgress,
    onPlayClick: () -> Unit,
    onLevelSelectClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_anim")

    // Gentle floating pulse for the big play button
    val playScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_button_scale"
    )

    // Floating background arrows offset
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_float"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEFF6FF),
                        Color(0xFFE0E7FF)
                    )
                )
            )
            .testTag("game_home_screen")
    ) {
        // Decorative background geometric arrow paths
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            // Ambient arrow tracks in background
            drawLine(
                color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                start = Offset(size.width * 0.1f, size.height * 0.2f + floatOffset),
                end = Offset(size.width * 0.9f, size.height * 0.2f + floatOffset),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF6366F1).copy(alpha = 0.08f),
                start = Offset(size.width * 0.85f, size.height * 0.4f - floatOffset),
                end = Offset(size.width * 0.85f, size.height * 0.85f - floatOffset),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Gems, Streak & Sound toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gems pill
                Row(
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Gems",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${progress.gems}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                // Quick Action Icons (Sound & Settings)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Sound Toggle
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onToggleSound),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (progress.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Sound",
                            tint = if (progress.soundEnabled) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onSettingsClick)
                            .testTag("home_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Center Hero: Branding, Logo Card & App Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Interactive Mini-Logo Icon: Clean overlapping maze arrows with 3D gradient
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(12.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Stylized Maze Arrows Icon
                    Canvas(modifier = Modifier.size(68.dp)) {
                        val strokeW = 5.5.dp.toPx()
                        // Bent escaping arrow 1 (Cyan/White)
                        drawLine(
                            color = Color.White,
                            start = Offset(size.width * 0.25f, size.height * 0.75f),
                            end = Offset(size.width * 0.25f, size.height * 0.35f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(size.width * 0.25f, size.height * 0.35f),
                            end = Offset(size.width * 0.8f, size.height * 0.35f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        // Arrowhead
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(size.width * 0.65f, size.height * 0.2f),
                            end = Offset(size.width * 0.84f, size.height * 0.35f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(size.width * 0.65f, size.height * 0.5f),
                            end = Offset(size.width * 0.84f, size.height * 0.35f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )

                        // Bent escaping arrow 2 (Electric Orange/Gold)
                        drawLine(
                            color = Color(0xFFFBBF24),
                            start = Offset(size.width * 0.5f, size.height * 0.75f),
                            end = Offset(size.width * 0.75f, size.height * 0.75f),
                            strokeWidth = strokeW * 0.9f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Play Store SEO App Title: "Arrow Maze: Tap Out"
                Text(
                    text = "Arrow Escape",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "TAP OUT • BRAIN PUZZLE MAZE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    letterSpacing = 2.sp
                )

                // Current Level Badge
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Level ${progress.currentLevel}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "•",
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "${progress.starsPerLevel.values.sum()} ⭐",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Bottom Buttons: Large PLAY / CONTINUE & Secondary Level Select / Daily
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // BIG PLAY BUTTON (Viral Hyper-Casual Style)
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .scale(playScale)
                        .shadow(12.dp, RoundedCornerShape(22.dp))
                        .testTag("home_play_button"),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (progress.currentLevel > 1) "CONTINUE LEVEL ${progress.currentLevel}" else "PLAY GAME",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                    }
                }

                // Row with 2 secondary buttons: "All Levels" and "Daily Challenge"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // All Levels Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clickable(onClick = onLevelSelectClick)
                            .testTag("home_level_select_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Levels",
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All Levels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    // Daily Challenge Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clickable(onClick = onDailyChallengeClick)
                            .testTag("home_daily_challenge_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Daily",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Maze",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }
    }
}
