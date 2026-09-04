package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.arrowescape.ui.theme.GameColors

private val DarkColorScheme =
  darkColorScheme(
      primary = GameColors.AmberAccent,
      secondary = GameColors.TileMint,
      tertiary = GameColors.TilePeach,
      background = Color(0xFF1E1E1E),
      surface = Color(0xFF2B2A28)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GameColors.AmberAccent,
    secondary = GameColors.TileMint,
    tertiary = GameColors.TilePeach,
    background = GameColors.BackgroundStart,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = GameColors.TextPrimary,
    onBackground = GameColors.TextPrimary,
    onSurface = GameColors.TextPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For hypercasual game, keep the signature warm pastel palette consistent
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

