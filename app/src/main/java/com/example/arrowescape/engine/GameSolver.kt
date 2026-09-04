package com.example.arrowescape.engine

import com.example.arrowescape.model.Arrow
import com.example.arrowescape.model.Direction
import com.example.arrowescape.model.LevelState

object GameSolver {

    /**
     * Checks if an arrow has an unobstructed ray from its head in headDirection
     * to the boundary of the board.
     * Path is clear if no other filled arrow occupies any cell along the forward exit ray.
     */
    fun isArrowClearToExit(
        arrow: Arrow,
        rows: Int,
        cols: Int,
        isCellOccupiedByOther: (row: Int, col: Int) -> Boolean
    ): Boolean {
        var currR = arrow.head.row + arrow.headDirection.dr
        var currC = arrow.head.col + arrow.headDirection.dc

        while (currR in 0 until rows && currC in 0 until cols) {
            if (isCellOccupiedByOther(currR, currC)) {
                return false // Obstacle in exit path!
            }
            currR += arrow.headDirection.dr
            currC += arrow.headDirection.dc
        }
        return true
    }

    /**
     * Backward-compatible cell-based check.
     */
    fun isPathClear(
        r: Int,
        c: Int,
        direction: Direction,
        rows: Int,
        cols: Int,
        isCellFilled: (row: Int, col: Int) -> Boolean
    ): Boolean {
        var currR = r + direction.dr
        var currC = c + direction.dc

        while (currR in 0 until rows && currC in 0 until cols) {
            if (isCellFilled(currR, currC)) {
                return false
            }
            currR += direction.dr
            currC += direction.dc
        }
        return true
    }

    /**
     * Finds all currently removable arrows from the given state.
     */
    fun findRemovableArrows(state: LevelState): List<Arrow> {
        val occupiedGrid = Array(state.gridRows) { BooleanArray(state.gridCols) }
        for (arrow in state.arrows) {
            if (arrow.filled) {
                for (pt in arrow.points) {
                    if (pt.row in 0 until state.gridRows && pt.col in 0 until state.gridCols) {
                        occupiedGrid[pt.row][pt.col] = true
                    }
                }
            }
        }

        return state.arrows.filter { arrow ->
            if (!arrow.filled) return@filter false
            // Check if ray is clear of other arrows
            isArrowClearToExit(arrow, state.gridRows, state.gridCols) { r, c ->
                // Cell is occupied and not part of this arrow's own body
                occupiedGrid[r][c] && arrow.points.none { it.row == r && it.col == c }
            }
        }
    }

    fun findRemovableTiles(state: LevelState): List<Arrow> = findRemovableArrows(state)

    /**
     * Greedily picks the best hint arrow that unblocks the most downstream arrows.
     */
    fun findBestHintArrow(state: LevelState): Arrow? {
        val candidates = findRemovableArrows(state)
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        val occupiedGrid = Array(state.gridRows) { BooleanArray(state.gridCols) }
        for (arrow in state.arrows) {
            if (arrow.filled) {
                for (pt in arrow.points) {
                    if (pt.row in 0 until state.gridRows && pt.col in 0 until state.gridCols) {
                        occupiedGrid[pt.row][pt.col] = true
                    }
                }
            }
        }

        var bestArrow: Arrow = candidates.first()
        var maxUnblocked = -1

        for (candidate in candidates) {
            // Temporarily un-occupy candidate points
            for (pt in candidate.points) {
                if (pt.row in 0 until state.gridRows && pt.col in 0 until state.gridCols) {
                    occupiedGrid[pt.row][pt.col] = false
                }
            }

            var unblockedCount = 0
            for (other in state.arrows) {
                if (other.filled && other.id != candidate.id) {
                    val clear = isArrowClearToExit(other, state.gridRows, state.gridCols) { r, c ->
                        occupiedGrid[r][c] && other.points.none { it.row == r && it.col == c }
                    }
                    if (clear) unblockedCount++
                }
            }

            // Restore candidate points
            for (pt in candidate.points) {
                if (pt.row in 0 until state.gridRows && pt.col in 0 until state.gridCols) {
                    occupiedGrid[pt.row][pt.col] = true
                }
            }

            if (unblockedCount > maxUnblocked) {
                maxUnblocked = unblockedCount
                bestArrow = candidate
            }
        }

        return bestArrow
    }

    fun findBestHintTile(state: LevelState): Arrow? = findBestHintArrow(state)

    /**
     * Verifies if an arrow configuration is 100% solvable via forward simulation.
     */
    fun isSolvable(
        rows: Int,
        cols: Int,
        arrows: List<Arrow>
    ): Boolean {
        val occupiedGrid = Array(rows) { BooleanArray(cols) }
        val remainingArrows = arrows.filter { it.filled }.toMutableList()

        for (arrow in remainingArrows) {
            for (pt in arrow.points) {
                if (pt.row in 0 until rows && pt.col in 0 until cols) {
                    occupiedGrid[pt.row][pt.col] = true
                }
            }
        }

        while (remainingArrows.isNotEmpty()) {
            val removableIndex = remainingArrows.indexOfFirst { arrow ->
                isArrowClearToExit(arrow, rows, cols) { r, c ->
                    occupiedGrid[r][c] && arrow.points.none { it.row == r && it.col == c }
                }
            }
            if (removableIndex == -1) {
                return false // Deadlock!
            }
            val removed = remainingArrows.removeAt(removableIndex)
            for (pt in removed.points) {
                if (pt.row in 0 until rows && pt.col in 0 until cols) {
                    occupiedGrid[pt.row][pt.col] = false
                }
            }
        }

        return true
    }

    /**
     * Counts how many initial moves are immediately available on this board.
     */
    fun countInitialMoves(rows: Int, cols: Int, arrows: List<Arrow>): Int {
        val occupiedGrid = Array(rows) { BooleanArray(cols) }
        for (arrow in arrows) {
            if (arrow.filled) {
                for (pt in arrow.points) {
                    if (pt.row in 0 until rows && pt.col in 0 until cols) {
                        occupiedGrid[pt.row][pt.col] = true
                    }
                }
            }
        }
        return arrows.count { arrow ->
            arrow.filled && isArrowClearToExit(arrow, rows, cols) { r, c ->
                occupiedGrid[r][c] && arrow.points.none { it.row == r && it.col == c }
            }
        }
    }
}
