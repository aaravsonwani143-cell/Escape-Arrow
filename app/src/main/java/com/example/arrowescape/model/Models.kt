package com.example.arrowescape.model

enum class Direction(val dr: Int, val dc: Int, val angleDegrees: Float) {
    UP(-1, 0, 270f),
    DOWN(1, 0, 90f),
    LEFT(0, -1, 180f),
    RIGHT(0, 1, 0f);

    companion object {
        fun fromDelta(dr: Int, dc: Int): Direction? {
            return entries.firstOrNull { it.dr == dr && it.dc == dc }
        }
    }
}

enum class GameStatus {
    PLAYING,
    WON,
    LOST
}

data class GridPoint(
    val row: Int,
    val col: Int
)

/**
 * An Arrow is a continuous snake-like path on the grid from tail (points.first())
 * to head (points.last()), with an arrowhead pointing in headDirection.
 */
data class Arrow(
    val id: String,
    val points: List<GridPoint>,
    val headDirection: Direction,
    val filled: Boolean = true,
    val colorHex: Long = 0xFF000000 // default solid black
) {
    val head: GridPoint get() = points.last()
    val tail: GridPoint get() = points.first()

    // Backward compatibility helpers
    val row: Int get() = head.row
    val col: Int get() = head.col
    val direction: Direction get() = headDirection
    val colorGroup: String get() = "black"
}

// For compatibility alias
typealias Tile = Arrow

data class RemovedArrowAction(
    val arrow: Arrow,
    val moveIndex: Int
)

typealias RemovedTileAction = RemovedArrowAction

data class CollisionImpact(
    val id: String,
    val row: Int,
    val col: Int,
    val movingArrowId: String,
    val hitArrowId: String? = null
)

data class FlyingArrow(
    val arrow: Arrow,
    val direction: Direction,
    val progress: Float = 0f,
    val startRow: Int = arrow.head.row,
    val startCol: Int = arrow.head.col,
    val isColliding: Boolean = false,
    val collisionTargetRow: Int = -1,
    val collisionTargetCol: Int = -1
) {
    val tile: Arrow get() = arrow
}

typealias FlyingTile = FlyingArrow
