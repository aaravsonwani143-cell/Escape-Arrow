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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arrowescape.model.PlayerProgress
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun SettingsDialog(
    progress: PlayerProgress,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleReducedMotion: () -> Unit,
    onResetProgress: () -> Unit,
    onDismiss: () -> Unit
) {
    var showConfirmReset by remember { mutableStateOf(false) }

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
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
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

                HorizontalDivider(color = GameColors.GridCellBorder)

                // Sound Setting
                SettingToggleRow(
                    icon = Icons.Default.Hearing,
                    title = "Sound Effects",
                    subtitle = "Relaxing tap & win chimes",
                    checked = progress.soundEnabled,
                    onCheckedChange = { onToggleSound() },
                    testTag = "sound_toggle"
                )

                // Haptics Setting
                SettingToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Gentle vibrations on tap & buzz",
                    checked = progress.hapticsEnabled,
                    onCheckedChange = { onToggleHaptics() },
                    testTag = "haptics_toggle"
                )

                // Reduced Motion Setting
                SettingToggleRow(
                    icon = Icons.Default.MotionPhotosOff,
                    title = "Reduced Motion",
                    subtitle = "Disable tile sliding & confetti",
                    checked = progress.reducedMotion,
                    onCheckedChange = { onToggleReducedMotion() },
                    testTag = "reduced_motion_toggle"
                )

                HorizontalDivider(color = GameColors.GridCellBorder)

                Spacer(modifier = Modifier.height(4.dp))

                // Reset Progress
                OutlinedButton(
                    onClick = { showConfirmReset = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_progress_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Reset Game Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Reset All Progress?", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset your levels, stars, and gems back to level 1. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmReset = false
                        onResetProgress()
                    }
                ) {
                    Text("Reset", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("Cancel", color = GameColors.TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GameColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GameColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = GameColors.TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GameColors.AmberAccent
            )
        )
    }
}
