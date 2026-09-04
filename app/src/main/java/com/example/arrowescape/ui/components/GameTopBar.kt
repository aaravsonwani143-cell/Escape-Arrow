package com.example.arrowescape.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.model.LevelState
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun GameTopBar(
    levelState: LevelState,
    gems: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUndoClick: () -> Unit = {},
    undoAvailable: Boolean = false,
    undoCount: Int = 12,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back/Home button + Level Title + Hearts row directly beneath
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Back/Home button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(1.5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onBackClick)
                        .testTag("topbar_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (levelState.isDaily) "Daily Challenge" else "Level ${levelState.levelNumber}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.testTag("level_title")
                        )
                        if (!levelState.shapeName.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = levelState.shapeName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }

                    // 4 Hearts row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..levelState.maxLives) {
                            val isAlive = i <= levelState.livesRemaining
                            AnimatedHeart(
                                isAlive = isAlive,
                                reducedMotion = false
                            )
                        }
                    }
                }
            }

            // Right: 3 Rounded Action Buttons (Undo/Restart, Sun, Settings)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Undo / Move counter button (🔄 12)
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .shadow(1.5.dp, RoundedCornerShape(19.dp))
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable(enabled = undoAvailable, onClick = onUndoClick)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Undo / Restart",
                        tint = if (undoAvailable) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$undoCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (undoAvailable) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                }

                // 2. Sun / Theme button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(1.5.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = { /* Light/Dark toggle */ }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Theme",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. Settings button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(1.5.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onSettingsClick)
                        .testTag("settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Clean thin divider line under top bar
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun LivesHeader(
    levelState: LevelState,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    // Left empty or minimal as hearts are integrated directly in GameTopBar
}

@Composable
private fun AnimatedHeart(
    isAlive: Boolean,
    reducedMotion: Boolean
) {
    val dropOffset = remember { Animatable(0f) }
    val rotateDeg = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(1f) }

    LaunchedEffect(isAlive) {
        if (!isAlive && !reducedMotion) {
            rotateDeg.animateTo(20f, tween(100, easing = LinearEasing))
            dropOffset.animateTo(16f, tween(180, easing = LinearEasing))
            heartAlpha.animateTo(0.35f, tween(120))
        } else if (isAlive) {
            dropOffset.snapTo(0f)
            rotateDeg.snapTo(0f)
            heartAlpha.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .offset(y = dropOffset.value.dp)
            .rotate(rotateDeg.value)
    ) {
        if (isAlive) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Life",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Lost Life",
                tint = Color(0xFFCBD5E1).copy(alpha = heartAlpha.value),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
