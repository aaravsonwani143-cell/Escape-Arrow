package com.example

import com.example.arrowescape.engine.GameSolver
import com.example.arrowescape.engine.LevelGenerator
import com.example.arrowescape.model.Arrow
import com.example.arrowescape.model.Direction
import com.example.arrowescape.model.GridPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testLevel1_isSolvable() {
    val level1 = LevelGenerator.generateLevel(1)
    val removable = GameSolver.findRemovableArrows(level1)
    assertTrue("Should have at least 1 valid opening move", removable.isNotEmpty())
    assertTrue(GameSolver.isSolvable(level1.gridRows, level1.gridCols, level1.arrows))
  }

  @Test
  fun testLevel6_isSolvable() {
    val level6 = LevelGenerator.generateLevel(6)
    assertTrue(GameSolver.isSolvable(level6.gridRows, level6.gridCols, level6.arrows))
  }

  @Test
  fun testLevel7_isSolvable() {
    val level7 = LevelGenerator.generateLevel(7)
    assertTrue(GameSolver.isSolvable(level7.gridRows, level7.gridCols, level7.arrows))
  }

  @Test
  fun testLevel20_isSolvable() {
    val level20 = LevelGenerator.generateLevel(20)
    val removable = GameSolver.findRemovableArrows(level20)
    assertTrue(removable.isNotEmpty())
    assertTrue(GameSolver.isSolvable(level20.gridRows, level20.gridCols, level20.arrows))
  }

  @Test
  fun testProceduralLevels_areGuaranteedSolvable() {
    for (lvl in listOf(1, 2, 3, 5, 10, 15, 20, 25, 35, 50, 75, 100)) {
      val state = LevelGenerator.generateLevel(lvl)
      val solvable = GameSolver.isSolvable(state.gridRows, state.gridCols, state.arrows)
      assertTrue("Level $lvl should be solvable", solvable)
    }
  }

  @Test
  fun testSnakeArrowExitRay() {
    // Arrow pointing UP at (1, 1). If cell (0, 1) is occupied -> blocked.
    val arrow = Arrow(
      id = "test_arrow",
      points = listOf(GridPoint(2, 1), GridPoint(1, 1)),
      headDirection = Direction.UP
    )

    val isBlocked = !GameSolver.isArrowClearToExit(arrow, 4, 4) { r, c ->
      r == 0 && c == 1
    }
    assertTrue("Arrow should be blocked by obstacle directly ahead", isBlocked)

    val isClear = GameSolver.isArrowClearToExit(arrow, 4, 4) { _, _ -> false }
    assertTrue("Arrow should be clear when exit ray has no obstacles", isClear)
  }

  @Test
  fun testAllLevels_haveZeroOverlappingCells() {
    // Verify levels 1 through 50 and high milestones up to 100 have zero overlapping cells
    val testLevels = (1..30).toList() + listOf(40, 50, 65, 80, 100)
    for (lvl in testLevels) {
      val state = LevelGenerator.generateLevel(lvl)
      val seenCells = mutableMapOf<Pair<Int, Int>, String>()

      for (arrow in state.arrows) {
        val expanded = LevelGenerator.expandContiguousPoints(arrow.points)

        // Check contiguity: every consecutive pair must have Manhattan distance of 1
        for (i in 0 until expanded.size - 1) {
          val p1 = expanded[i]
          val p2 = expanded[i + 1]
          val dist = kotlin.math.abs(p1.row - p2.row) + kotlin.math.abs(p1.col - p2.col)
          assertEquals("Arrow ${arrow.id} in level $lvl points must be contiguous (dist=1)", 1, dist)
        }

        // Check that no cell is touched by more than one arrow
        for (pt in expanded) {
          val cell = pt.row to pt.col
          assertFalse(
            "Overlap detected in level $lvl: cell $cell shared between '${seenCells[cell]}' and '${arrow.id}'",
            seenCells.containsKey(cell)
          )
          seenCells[cell] = arrow.id
        }
      }
    }
  }

  @Test
  fun testLevel100_extremeComplexity() {
    val level100 = LevelGenerator.generateLevel(100)
    assertTrue("Level 100 should have a large grid (actual: ${level100.gridRows})", level100.gridRows >= 13)
    assertTrue("Level 100 should have dense arrow count (actual: ${level100.arrows.size})", level100.arrows.size >= 30)
    assertTrue("Level 100 must be solvable", GameSolver.isSolvable(level100.gridRows, level100.gridCols, level100.arrows))
  }
}
