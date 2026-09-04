package com.example.arrowescape.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.model.PlayerProgress
import com.example.arrowescape.ui.theme.GameColors
import kotlin.math.sin

@Composable
fun LevelSelectScreen(
    progress: PlayerProgress,
    onLevelSelected: (Int) -> Unit,
    onDailyChallengeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLevelsToShow = 100
    val listState = rememberLazyListState()

    // Scroll to near current level on first launch
    LaunchedEffect(progress.currentLevel) {
        val targetIndex = (progress.currentLevel - 2).coerceAtLeast(0)
        listState.scrollToItem(targetIndex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameColors.BackgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBackClick)
                        .testTag("level_select_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to game",
                        tint = GameColors.TextPrimary
                    )
                }

                Text(
                    text = "Select Level",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameColors.TextPrimary
                )

                // Gems badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "💎", fontSize = 14.sp)
                    Text(
                        text = "${progress.gems}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameColors.TextPrimary
                    )
                }
            }

            // Daily Challenge Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable(onClick = onDailyChallengeClick)
                    .testTag("daily_challenge_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GameColors.AmberLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = GameColors.AmberAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Daily Challenge",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GameColors.TextPrimary
                            )
                            Text(
                                text = "Streak: ${progress.dailyChallengeStreak} days  •  +50 💎",
                                fontSize = 13.sp,
                                color = GameColors.TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onDailyChallengeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.AmberAccent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Play", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Winding Path Map of Levels
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                items(totalLevelsToShow) { index ->
                    val lvl = index + 1
                    val isUnlocked = lvl <= progress.currentLevel
                    val isCurrent = lvl == progress.currentLevel
                    val stars = progress.starsPerLevel[lvl] ?: 0

                    // Gentle sine-wave offset to create a pleasant winding map path
                    val horizontalShiftFraction = sin(index * 0.75).toFloat() * 0.36f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LevelNodeItem(
                            level = lvl,
                            isUnlocked = isUnlocked,
                            isCurrent = isCurrent,
                            stars = stars,
                            horizontalShiftFraction = horizontalShiftFraction,
                            onClick = {
                                if (isUnlocked) onLevelSelected(lvl)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelNodeItem(
    level: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    horizontalShiftFraction: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    Column(
        modifier = Modifier
            .offset(x = (horizontalShiftFraction * 140).dp)
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_node_$level"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing highlight ring for current level
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size((64 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(GameColors.AmberAccent.copy(alpha = 0.35f))
                )
            }

            // Node Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = if (isCurrent) 8.dp else if (isUnlocked) 4.dp else 1.dp,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> GameColors.AmberAccent
                            isUnlocked -> Color.White
                            else -> Color(0xFFE8E5DF)
                        }
                    )
                    .then(
                        if (isUnlocked && !isCurrent) {
                            Modifier.border(2.dp, GameColors.GridCellBorder, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = "$level",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White else GameColors.TextPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = GameColors.HeartLost,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Stars beneath node
        if (isUnlocked && stars > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (s in 1..3) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (s <= stars) GameColors.StarGold else Color(0xFFDEDBD4),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
