package com.example.arrowescape.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.model.GameStatus
import com.example.arrowescape.ui.components.BottomActionBar
import com.example.arrowescape.ui.components.DailyChallengeDialog
import com.example.arrowescape.ui.components.GameBoard
import com.example.arrowescape.ui.components.GameHomeScreen
import com.example.arrowescape.ui.components.GameTopBar
import com.example.arrowescape.ui.components.LevelSelectScreen
import com.example.arrowescape.ui.components.LivesHeader
import com.example.arrowescape.ui.components.LoseDialog
import com.example.arrowescape.ui.components.SettingsDialog
import com.example.arrowescape.ui.components.WinDialog
import com.example.arrowescape.ui.theme.GameColors

@Composable
fun ArrowEscapeGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progress.collectAsState()
    val levelState by viewModel.levelState.collectAsState()
    val isShowingHomeScreen by viewModel.isShowingHomeScreen.collectAsState()
    val isShowingLevelSelect by viewModel.isShowingLevelSelect.collectAsState()
    val isShowingSettings by viewModel.isShowingSettings.collectAsState()
    val isShowingDailyChallenge by viewModel.isShowingDailyChallenge.collectAsState()
    val isWatchingAd by viewModel.isWatchingAd.collectAsState()
    val redVignetteFlash by viewModel.redVignetteFlash.collectAsState()
    val messageToast by viewModel.messageToast.collectAsState()

    if (isShowingHomeScreen) {
        GameHomeScreen(
            progress = progress,
            onPlayClick = { viewModel.startGame() },
            onLevelSelectClick = { viewModel.openLevelSelect() },
            onDailyChallengeClick = { viewModel.openDailyChallenge() },
            onSettingsClick = { viewModel.openSettings() },
            onToggleSound = { viewModel.toggleSound() },
            modifier = modifier
        )
        if (isShowingSettings) {
            SettingsDialog(
                progress = progress,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onToggleReducedMotion = { viewModel.toggleReducedMotion() },
                onResetProgress = { viewModel.resetAllProgress() },
                onDismiss = { viewModel.closeSettings() }
            )
        }
        if (isShowingDailyChallenge) {
            DailyChallengeDialog(
                progress = progress,
                todayDate = viewModel.getTodayDateString(),
                onStartDaily = { viewModel.loadDailyChallenge() },
                onDismiss = { viewModel.closeDailyChallenge() }
            )
        }
        return
    }

    if (isShowingLevelSelect) {
        LevelSelectScreen(
            progress = progress,
            onLevelSelected = { lvl -> viewModel.loadLevel(lvl) },
            onDailyChallengeClick = { viewModel.openDailyChallenge() },
            onBackClick = { viewModel.returnToHomeScreen() },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("arrow_escape_root"),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GameColors.BackgroundGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar
                GameTopBar(
                    levelState = levelState,
                    gems = progress.gems,
                    onBackClick = { viewModel.returnToHomeScreen() },
                    onSettingsClick = { viewModel.openSettings() },
                    onUndoClick = { viewModel.onUndoTapped() },
                    undoAvailable = levelState.undoStack.isNotEmpty() && levelState.status == GameStatus.PLAYING,
                    undoCount = (12 - levelState.movesUsed).coerceAtLeast(0)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Square Grid Board with snake-like black arrows
                GameBoard(
                    levelState = levelState,
                    reducedMotion = progress.reducedMotion,
                    onTileClick = { arrow -> viewModel.onTileTapped(arrow) },
                    modifier = Modifier.weight(7f, fill = false)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Action Bar: Hint (bulb), Level select (#), and "Tap Away Arrows"
                BottomActionBar(
                    hintsCount = progress.hintsRemaining,
                    undoAvailable = levelState.undoStack.isNotEmpty() && levelState.status == GameStatus.PLAYING,
                    onShuffleClick = { viewModel.openLevelSelect() },
                    onHintClick = { viewModel.onHintTapped() },
                    onUndoClick = { viewModel.onUndoTapped() }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Soft Red Vignette on Life Lost
            if (redVignetteFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE85D5D).copy(alpha = 0.22f))
                )
            }

            // Message Toast Floating Notification
            AnimatedVisibility(
                visible = messageToast != null,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            ) {
                messageToast?.let { msg ->
                    Box(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2C3E50))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Dialogs
            if (levelState.status == GameStatus.WON) {
                WinDialog(
                    levelState = levelState,
                    reducedMotion = progress.reducedMotion,
                    onNextLevelClick = {
                        if (levelState.isDaily) {
                            viewModel.openLevelSelect()
                        } else {
                            viewModel.nextLevel()
                        }
                    },
                    onLevelsMapClick = { viewModel.openLevelSelect() }
                )
            }

            if (levelState.status == GameStatus.LOST) {
                LoseDialog(
                    gemsAvailable = progress.gems,
                    isWatchingAd = isWatchingAd,
                    onWatchAdClick = { viewModel.showRewardedAd() },
                    onRefillGemsClick = { viewModel.refillLivesWithGems() },
                    onRetryClick = { viewModel.retryLevel() }
                )
            }

            if (isShowingDailyChallenge) {
                DailyChallengeDialog(
                    progress = progress,
                    todayDate = viewModel.getTodayDateString(),
                    onStartDaily = { viewModel.loadDailyChallenge() },
                    onDismiss = { viewModel.closeDailyChallenge() }
                )
            }

            if (isShowingSettings) {
                SettingsDialog(
                    progress = progress,
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleHaptics = { viewModel.toggleHaptics() },
                    onToggleReducedMotion = { viewModel.toggleReducedMotion() },
                    onResetProgress = { viewModel.resetAllProgress() },
                    onDismiss = { viewModel.closeSettings() }
                )
            }
        }
    }
}
