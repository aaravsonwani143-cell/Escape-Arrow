package com.example.arrowescape.model

data class LevelState(
    val levelNumber: Int,
    val gridRows: Int,
    val gridCols: Int,
    val arrows: List<Arrow>,
    val livesRemaining: Int = 4,
    val maxLives: Int = 4,
    val movesUsed: Int = 0,
    val hintsUsed: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val isDaily: Boolean = false,
    val undoStack: List<RemovedArrowAction> = emptyList(),
    val activeHintArrowId: String? = null,
    val shakeArrowId: String? = null,
    val flyingArrows: List<FlyingArrow> = emptyList(),
    val activeCollision: CollisionImpact? = null,
    val shapeName: String? = null
) {
    // Backward compatibility aliases
    val tiles: List<Arrow> get() = arrows
    val activeHintTileId: String? get() = activeHintArrowId
    val shakeTileId: String? get() = shakeArrowId
    val flyingTiles: List<FlyingArrow> get() = flyingArrows

    fun getArrowAt(r: Int, c: Int): Arrow? {
        return arrows.firstOrNull { arrow ->
            arrow.filled && arrow.points.any { it.row == r && it.col == c }
        }
    }

    fun isCellOccupied(r: Int, c: Int): Boolean {
        return arrows.any { arrow ->
            arrow.filled && arrow.points.any { it.row == r && it.col == c }
        }
    }

    fun isFilled(r: Int, c: Int): Boolean = isCellOccupied(r, c)

    fun getTileAt(r: Int, c: Int): Arrow? = getArrowAt(r, c)

    fun remainingCount(): Int = arrows.count { it.filled }

    fun totalInitialTiles(): Int = arrows.size

    fun totalInitialArrows(): Int = arrows.size
}

data class PlayerProgress(
    val currentLevel: Int = 1,
    val starsPerLevel: Map<Int, Int> = emptyMap(),
    val gems: Int = 100,
    val hintsRemaining: Int = 3,
    val totalLivesRefilled: Int = 0,
    val dailyChallengeStreak: Int = 0,
    val lastDailyChallengeDate: String = "",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false
)
