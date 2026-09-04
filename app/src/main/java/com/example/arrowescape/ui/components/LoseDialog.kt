package com.example.arrowescape.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.DialogProperties
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun LoseDialog(
    gemsAvailable: Int,
    isWatchingAd: Boolean,
    onWatchAdClick: () -> Unit,
    onRefillGemsClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Heart Broken mascot / icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDE8E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💔", fontSize = 34.sp)
                }

                Text(
                    text = "Out of Lives!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameColors.TextPrimary
                )

                Text(
                    text = "Keep going to clear the remaining arrows without losing your puzzle streak.",
                    fontSize = 14.sp,
                    color = GameColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Button 1: Watch Ad for +1 Life (primary, amber fill)
                Button(
                    onClick = onWatchAdClick,
                    enabled = !isWatchingAd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .testTag("watch_ad_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.AmberAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isWatchingAd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Playing Sponsor Video...", fontSize = 15.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Watch Ad for +1 Life",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Button 2: Use 50 Gems to Refill (secondary, outline)
                OutlinedButton(
                    onClick = onRefillGemsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("use_gems_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💎", fontSize = 16.sp)
                        Text(
                            text = "Use 50 Gems to Refill (You: $gemsAvailable)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GameColors.TextPrimary
                        )
                    }
                }

                // Button 3: Retry Level (ghost text button)
                TextButton(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("retry_level_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = GameColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Retry Level",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GameColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
