package com.example.arrowescape.ui.components

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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun BottomActionBar(
    hintsCount: Int,
    undoAvailable: Boolean,
    onShuffleClick: () -> Unit,
    onHintClick: () -> Unit,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row of action buttons (Bulb with green badge, and Hash # button)
        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Hint Button (Light bulb with green count badge)
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(2.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFEFF6FF)) // soft light blue background
                        .clickable(onClick = onHintClick)
                        .testTag("hint_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Hint",
                        tint = Color(0xFF6366F1), // lavender-purple / indigo icon tint
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Green badge for hints remaining
                if (hintsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(22.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)), // emerald green badge
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$hintsCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Hash # Button (Level selection / grid)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFEFF6FF))
                    .clickable(onClick = onShuffleClick)
                    .testTag("levels_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = "Levels",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Title text: "Tap Away Arrows" in bold dark navy
        Text(
            text = "Tap Away Arrows",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E3A8A), // deep navy
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
