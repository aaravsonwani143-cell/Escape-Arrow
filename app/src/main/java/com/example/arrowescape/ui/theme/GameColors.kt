package com.example.arrowescape.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GameColors {
    // Exact screen background: Pure crisp white
    val BackgroundStart = Color(0xFFFFFFFF)
    val BackgroundEnd = Color(0xFFFFFFFF)

    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(BackgroundStart, BackgroundEnd)
    )

    // Empty grid cell / dot matrix
    val GridDot = Color(0xFFCBD5E1)
    val GridLine = Color(0xFFF1F5F9)
    val GridCellBorder = Color(0xFFE2E8F0)

    // Exact vibrant tile colors
    val TileBlue = Color(0xFF2563EB)
    val TileOrange = Color(0xFFF97316)
    val TileGreen = Color(0xFF10B981)
    val TileYellow = Color(0xFFF59E0B)
    val TilePurple = Color(0xFF8B5CF6)
    val TileCoral = Color(0xFFEF4444)

    // Backward-compatible alias mappings
    val TileMint = TileGreen
    val TilePeach = TileOrange
    val TilePeriwinkle = TileBlue
    val TileRose = TileCoral
    val TileLavender = TilePurple

    // Arrow stroke colors matching screenshot:
    // Pure thin black arrow paths, vibrant electric blue when selected/hint
    val ArrowBlack = Color(0xFF000000)
    val ArrowActiveBlue = Color(0xFF2563EB)
    val ArrowErrorRed = Color(0xFFEF4444)

    // Arrow glyph color inside colored tile
    val ArrowWhite = Color(0xFFFFFFFF)
    val ArrowCharcoal = Color(0xFF0F172A)

    // Hearts (Lives)
    val HeartActive = Color(0xFFEF4444)
    val HeartLost = Color(0xFFE2E8F0)
    val HeartLostOutline = Color(0xFF94A3B8)
    val HeartCoral = HeartActive

    // Action buttons & accents
    val PrimaryBlue = Color(0xFF2563EB)
    val AmberAccent = Color(0xFFF59E0B)
    val AmberLight = Color(0xFFFEF3C7)

    // UI elements & typography
    val CardBackground = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)

    // Stars & Rewards
    val StarGold = Color(0xFFF59E0B)

    fun getColorForGroup(group: String): Color {
        return when (group.lowercase()) {
            "blue" -> TileBlue
            "orange" -> TileOrange
            "green" -> TileGreen
            "yellow" -> TileYellow
            "purple" -> TilePurple
            "coral" -> TileCoral
            "mint" -> TileGreen
            "peach" -> TileOrange
            "periwinkle" -> TileBlue
            "rose" -> TileCoral
            "lavender" -> TilePurple
            else -> TileBlue
        }
    }
}

