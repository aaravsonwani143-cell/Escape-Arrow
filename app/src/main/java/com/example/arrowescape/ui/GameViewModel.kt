package com.example.arrowescape.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrowescape.audio.SoundManager
import com.example.arrowescape.data.PreferencesManager
import com.example.arrowescape.engine.GameSolver
import com.example.arrowescape.engine.LevelGenerator
import com.example.arrowescape.model.Arrow
import com.example.arrowescape.model.CollisionImpact
import com.example.arrowescape.model.FlyingArrow
import com.example.arrowescape.model.GameStatus
import com.example.arrowescape.model.LevelState
import com.example.arrowescape.model.PlayerProgress
import com.example.arrowescape.model.RemovedArrowAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    val soundManager = SoundManager(application)

    private val _progress = MutableStateFlow(preferencesManager.loadProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private val _levelState = MutableStateFlow(
        LevelGenerator.generateLevel(levelNumber = 1)
    )
    val levelState: StateFlow<LevelState> = _levelState.asStateFlow()

    private val _isShowingHomeScreen = MutableStateFlow(false)
    val isShowingHomeScreen: StateFlow<Boolean> = _isShowingHomeScreen.asStateFlow()

    private val _isShowingLevelSelect = MutableStateFlow(false)
    val isShowingLevelSelect: StateFlow<Boolean> = _isShowingLevelSelect.asStateFlow()

    private val _isShowingSettings = MutableStateFlow(false)
    val isShowingSettings: StateFlow<Boolean> = _isShowingSettings.asStateFlow()

    private val _isShowingDailyChallenge = MutableStateFlow(false)
    val isShowingDailyChallenge: StateFlow<Boolean> = _isShowingDailyChallenge.asStateFlow()

    private val _isWatchingAd = MutableStateFlow(false)
    val isWatchingAd: StateFlow<Boolean> = _isWatchingAd.asStateFlow()

    private val _redVignetteFlash = MutableStateFlow(false)
    val redVignetteFlash: StateFlow<Boolean> = _redVignetteFlash.asStateFlow()

    private val _messageToast = MutableStateFlow<String?>(null)
    val messageToast: StateFlow<String?> = _messageToast.asStateFlow()

    private var comboCount = 0
    private var shakeJob: Job? = null
    private var vignetteJob: Job? = null
    private var collisionJob: Job? = null

    init {
        // Sync sound preferences with soundManager
        soundManager.soundEnabled = _progress.value.soundEnabled
        soundManager.hapticsEnabled = _progress.value.hapticsEnabled
        loadLevel(1)
    }

    fun onTileTapped(arrow: Arrow) {
        val current = _levelState.value
        if (current.status != GameStatus.PLAYING || !arrow.filled) return

        val rows = current.gridRows
        val cols = current.gridCols

        // Check if forward exit ray collides with another filled arrow
        var obstacleArrow: Arrow? = null
        var collisionRow = -1
        var collisionCol = -1

        var r = arrow.head.row + arrow.headDirection.dr
        var c = arrow.head.col + arrow.headDirection.dc
        while (r in 0 until rows && c in 0 until cols) {
            val hit = current.getArrowAt(r, c)
            if (hit != null && hit.filled && hit.id != arrow.id) {
                obstacleArrow = hit
                collisionRow = r
                collisionCol = c
                break
            }
            r += arrow.headDirection.dr
            c += arrow.headDirection.dc
        }

        if (obstacleArrow == null) {
            handleValidTap(arrow)
        } else {
            handleCollisionTap(
                arrow = arrow,
                obstacleArrow = obstacleArrow,
                collisionRow = collisionRow,
                collisionCol = collisionCol
            )
        }
    }

    private fun handleValidTap(arrow: Arrow) {
        comboCount++
        soundManager.playValidTap(comboCount)

        val current = _levelState.value
        val newArrows = current.arrows.map {
            if (it.id == arrow.id) it.copy(filled = false) else it
        }

        val flying = FlyingArrow(
            arrow = arrow,
            startRow = arrow.head.row,
            startCol = arrow.head.col,
            direction = arrow.headDirection
        )

        val updatedUndo = current.undoStack + RemovedArrowAction(arrow, current.movesUsed)
        val newMoves = current.movesUsed + 1
        val isCleared = newArrows.none { it.filled }

        _levelState.update {
            it.copy(
                arrows = newArrows,
                movesUsed = newMoves,
                undoStack = updatedUndo,
                flyingArrows = it.flyingArrows + flying,
                activeHintArrowId = if (it.activeHintArrowId == arrow.id) null else it.activeHintArrowId,
                status = if (isCleared) GameStatus.WON else GameStatus.PLAYING
            )
        }

        // Clean up flying arrow after rocket animation completes
        viewModelScope.launch {
            delay(460)
            _levelState.update { state ->
                state.copy(flyingArrows = state.flyingArrows.filter { it.arrow.id != arrow.id })
            }
        }

        if (isCleared) {
            handleWin()
        }
    }

    private fun handleCollisionTap(
        arrow: Arrow,
        obstacleArrow: Arrow,
        collisionRow: Int,
        collisionCol: Int
    ) {
        comboCount = 0
        // Play physical collision sound
        soundManager.playInvalidTap()

        // Create impact event
        val impact = CollisionImpact(
            id = "col_${System.currentTimeMillis()}",
            row = collisionRow,
            col = collisionCol,
            movingArrowId = arrow.id,
            hitArrowId = obstacleArrow.id
        )

        // Trigger simultaneous shake on the arrow and show collision visual
        shakeJob?.cancel()
        collisionJob?.cancel()
        _levelState.update {
            it.copy(
                shakeArrowId = arrow.id,
                activeCollision = impact
            )
        }

        collisionJob = viewModelScope.launch {
            delay(320)
            _levelState.update {
                it.copy(
                    shakeArrowId = null,
                    activeCollision = null
                )
            }
        }

        // Trigger red vignette
        vignetteJob?.cancel()
        _redVignetteFlash.value = true
        vignetteJob = viewModelScope.launch {
            delay(250)
            _redVignetteFlash.value = false
        }

        val newLives = _levelState.value.livesRemaining - 1
        val isLost = newLives <= 0

        _levelState.update {
            it.copy(
                livesRemaining = newLives.coerceAtLeast(0),
                status = if (isLost) GameStatus.LOST else it.status
            )
        }

        if (isLost) {
            soundManager.playHeartLoss()
        }
    }

    private fun handleInvalidTap(arrow: Arrow) {
        comboCount = 0
        soundManager.playInvalidTap()

        // Trigger arrow shake animation
        shakeJob?.cancel()
        _levelState.update { it.copy(shakeArrowId = arrow.id) }
        shakeJob = viewModelScope.launch {
            delay(260)
            _levelState.update { it.copy(shakeArrowId = null) }
        }

        // Trigger red vignette
        vignetteJob?.cancel()
        _redVignetteFlash.value = true
        vignetteJob = viewModelScope.launch {
            delay(250)
            _redVignetteFlash.value = false
        }

        val newLives = _levelState.value.livesRemaining - 1
        val isLost = newLives <= 0

        _levelState.update {
            it.copy(
                livesRemaining = newLives.coerceAtLeast(0),
                status = if (isLost) GameStatus.LOST else it.status
            )
        }

        if (isLost) {
            soundManager.playHeartLoss()
        }
    }

    private fun handleWin() {
        soundManager.playWinSound()
        val current = _levelState.value

        val livesLost = current.maxLives - current.livesRemaining
        val stars = when {
            livesLost == 0 && current.hintsUsed == 0 -> 3
            livesLost <= 1 && current.hintsUsed <= 1 -> 2
            else -> 1
        }

        val gemsEarned = if (current.isDaily) 50 else 25

        _progress.update { prog ->
            val updatedStars = prog.starsPerLevel.toMutableMap()
            val oldStars = updatedStars[current.levelNumber] ?: 0
            if (stars > oldStars) {
                updatedStars[current.levelNumber] = stars
            }

            val nextLvl = if (!current.isDaily && current.levelNumber == prog.currentLevel) {
                prog.currentLevel + 1
            } else {
                prog.currentLevel
            }

            var newStreak = prog.dailyChallengeStreak
            var lastDaily = prog.lastDailyChallengeDate
            if (current.isDaily) {
                val today = getTodayDateString()
                if (prog.lastDailyChallengeDate != today) {
                    newStreak++
                    lastDaily = today
                }
            }

            prog.copy(
                currentLevel = nextLvl,
                starsPerLevel = updatedStars,
                gems = prog.gems + gemsEarned,
                dailyChallengeStreak = newStreak,
                lastDailyChallengeDate = lastDaily
            )
        }

        preferencesManager.saveProgress(_progress.value)
    }

    fun onUndoTapped() {
        val current = _levelState.value
        if (current.undoStack.isEmpty() || current.status != GameStatus.PLAYING) return

        val lastAction = current.undoStack.last()
        val newUndoStack = current.undoStack.dropLast(1)

        val restoredArrows = current.arrows.map {
            if (it.id == lastAction.arrow.id) it.copy(filled = true) else it
        }

        soundManager.playValidTap()
        _levelState.update {
            it.copy(
                arrows = restoredArrows,
                undoStack = newUndoStack,
                activeHintArrowId = null
            )
        }
    }

    fun onHintTapped() {
        val currentProg = _progress.value
        val currentState = _levelState.value

        if (currentState.status != GameStatus.PLAYING) return

        if (currentProg.hintsRemaining <= 0) {
            if (currentProg.gems >= 50) {
                _progress.update {
                    it.copy(gems = it.gems - 50, hintsRemaining = it.hintsRemaining + 3)
                }
                preferencesManager.saveProgress(_progress.value)
                showToast("Used 50 Gems for 3 Hints! 💡")
            } else {
                showToast("Need 50 Gems for Hints. Watch Ad to earn gems!")
                return
            }
        }

        val bestHint = GameSolver.findBestHintArrow(currentState)
        if (bestHint != null) {
            _progress.update { it.copy(hintsRemaining = (it.hintsRemaining - 1).coerceAtLeast(0)) }
            preferencesManager.saveProgress(_progress.value)

            _levelState.update {
                it.copy(
                    activeHintArrowId = bestHint.id,
                    hintsUsed = it.hintsUsed + 1
                )
            }
            soundManager.playHint()
        } else {
            showToast("No clear arrow path right now — try Undo!")
        }
    }

    fun onShuffleTapped() {
        openLevelSelect()
    }

    fun refillLivesWithGems() {
        val prog = _progress.value
        if (prog.gems >= 50) {
            _progress.update {
                it.copy(
                    gems = it.gems - 50,
                    totalLivesRefilled = it.totalLivesRefilled + 1
                )
            }
            preferencesManager.saveProgress(_progress.value)

            _levelState.update {
                it.copy(
                    livesRemaining = it.maxLives,
                    status = GameStatus.PLAYING
                )
            }
            soundManager.playWinSound()
            showToast("Lives Refilled! ❤️❤️❤️❤️")
        } else {
            showToast("Not enough gems! Watch an ad instead.")
        }
    }

    fun showRewardedAd() {
        viewModelScope.launch {
            _isWatchingAd.value = true
            delay(1600)
            _isWatchingAd.value = false

            _levelState.update {
                it.copy(
                    livesRemaining = (it.livesRemaining + 1).coerceAtMost(it.maxLives),
                    status = GameStatus.PLAYING
                )
            }
            _progress.update {
                it.copy(totalLivesRefilled = it.totalLivesRefilled + 1, gems = it.gems + 15)
            }
            preferencesManager.saveProgress(_progress.value)
            soundManager.playWinSound()
            showToast("+1 Life & +15 Gems from Ad! 🎁")
        }
    }

    fun retryLevel() {
        val current = _levelState.value
        comboCount = 0
        if (current.isDaily) {
            loadDailyChallenge()
        } else {
            loadLevel(current.levelNumber)
        }
    }

    fun nextLevel() {
        val nextLvl = _levelState.value.levelNumber + 1
        loadLevel(nextLvl)
    }

    fun startGame() {
        _isShowingHomeScreen.value = false
        _isShowingLevelSelect.value = false
    }

    fun returnToHomeScreen() {
        _isShowingHomeScreen.value = true
        _isShowingLevelSelect.value = false
    }

    fun loadLevel(levelNumber: Int) {
        comboCount = 0
        _levelState.value = LevelGenerator.generateLevel(levelNumber = levelNumber, isDaily = false)
        _isShowingLevelSelect.value = false
        _isShowingHomeScreen.value = false
    }

    fun loadDailyChallenge() {
        comboCount = 0
        val today = getTodayDateString()
        val seed = today.hashCode().toLong()
        _levelState.value = LevelGenerator.generateLevel(
            levelNumber = 999,
            isDaily = true,
            dailySeed = seed
        )
        _isShowingDailyChallenge.value = false
        _isShowingLevelSelect.value = false
        _isShowingHomeScreen.value = false
    }

    fun openLevelSelect() {
        _isShowingLevelSelect.value = true
    }

    fun closeLevelSelect() {
        _isShowingLevelSelect.value = false
    }

    fun openSettings() {
        _isShowingSettings.value = true
    }

    fun closeSettings() {
        _isShowingSettings.value = false
    }

    fun openDailyChallenge() {
        _isShowingDailyChallenge.value = true
    }

    fun closeDailyChallenge() {
        _isShowingDailyChallenge.value = false
    }

    fun toggleSound() {
        _progress.update { it.copy(soundEnabled = !it.soundEnabled) }
        soundManager.soundEnabled = _progress.value.soundEnabled
        preferencesManager.saveProgress(_progress.value)
    }

    fun toggleHaptics() {
        _progress.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
        soundManager.hapticsEnabled = _progress.value.hapticsEnabled
        preferencesManager.saveProgress(_progress.value)
    }

    fun toggleReducedMotion() {
        _progress.update { it.copy(reducedMotion = !it.reducedMotion) }
        preferencesManager.saveProgress(_progress.value)
    }

    fun resetAllProgress() {
        val fresh = preferencesManager.resetProgress()
        _progress.value = fresh
        loadLevel(1)
        closeSettings()
        showToast("Progress Reset")
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun showToast(msg: String) {
        viewModelScope.launch {
            _messageToast.value = msg
            delay(2200)
            if (_messageToast.value == msg) {
                _messageToast.value = null
            }
        }
    }
}
