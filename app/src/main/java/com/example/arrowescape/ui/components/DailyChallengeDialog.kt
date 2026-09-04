package com.example.arrowescape.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arrowescape.model.PlayerProgress
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun DailyChallengeDialog(
    progress: PlayerProgress,
    todayDate: String,
    onStartDaily: () -> Unit,
    onDismiss: () -> Unit
) {
    val isCompletedToday = progress.lastDailyChallengeDate == todayDate

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Challenge",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameColors.TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GameColors.TextSecondary
                        )
                    }
                }

                // Trophy or Streak Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GameColors.AmberLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompletedToday) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (isCompletedToday) Color(0xFF4CAF50) else GameColors.AmberAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Date and description
                Text(
                    text = "Puzzle of the Day ($todayDate)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GameColors.TextPrimary
                )

                Text(
                    text = if (isCompletedToday)
                        "You cleared today's special maze! Check back tomorrow for a brand-new board."
                    else
                        "Complete today's handcrafted maze puzzle to build your streak and earn bonus gems!",
                    fontSize = 13.sp,
                    color = GameColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                // Streak Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF9F7F3))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(text = "Streak", fontSize = 11.sp, color = GameColors.TextSecondary)
                            Text(
                                text = "${progress.dailyChallengeStreak} Days",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GameColors.TextPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "💎", fontSize = 18.sp)
                        Column {
                            Text(text = "Reward", fontSize = 11.sp, color = GameColors.TextSecondary)
                            Text(
                                text = "+50 Gems",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GameColors.AmberAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action button
                if (isCompletedToday) {
                    TextButton(onClick = onStartDaily, modifier = Modifier.fillMaxWidth()) {
                        Text("Replay Today's Challenge", fontSize = 14.sp, color = GameColors.TextSecondary)
                    }
                } else {
                    Button(
                        onClick = onStartDaily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .testTag("start_daily_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.AmberAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Text("Start Challenge", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
