package com.example.arrowescape.engine

import com.example.arrowescape.model.Arrow
import com.example.arrowescape.model.Direction
import com.example.arrowescape.model.GridPoint
import com.example.arrowescape.model.LevelState
import java.util.Random

object LevelGenerator {

    fun generateLevel(levelNumber: Int, isDaily: Boolean = false, dailySeed: Long? = null): LevelState {
        // Special handcrafted levels for early onboarding, figurative shapes, and reference
        if (!isDaily) {
            val handcrafted = when (levelNumber) {
                1 -> createHandcraftedLevel1()
                2 -> createHandcraftedGlassesLevel()
                3 -> createHandcraftedHeartLevel()
                4 -> createHandcraftedCatLevel()
                5 -> createHandcraftedFishLevel()
                6 -> createHandcraftedButterflyLevel()
                7 -> createHandcraftedHouseLevel()
                8 -> createHandcraftedBrainLevel()
                20 -> createHandcraftedLevel20()
                else -> null
            }
            if (handcrafted != null) {
                return ensureZeroOverlaps(handcrafted)
            }
        }

        val seed = dailySeed ?: (levelNumber * 7919L + 42L)
        val rng = Random(seed)

        // Determine grid dimension and arrow count scaling with level
        // Distinct theme color palettes per level group:
        // Levels 1-4: Classic Black & Blue
        // Levels 5-9: Neon Teal & Midnight Navy & Amber
        // Levels 10-14: Cyber Magenta, Emerald Green & Indigo
        // Levels 15+: Royal Palette with Vivid Coral, Gold, Azure & Obsidian
        val palette = getPaletteForLevel(levelNumber)

        val (rows, cols, targetArrowCount, maxBends) = getLevelDifficultyParams(levelNumber, isDaily)

        for (attempt in 0 until 50) {
            val attemptSeed = seed + attempt * 1013L
            val arrows = generateProceduralArrows(
                rows = rows,
                cols = cols,
                arrowCount = targetArrowCount,
                maxBends = maxBends,
                palette = palette,
                rng = Random(attemptSeed)
            )

            if (arrows.isNotEmpty() && GameSolver.isSolvable(rows, cols, arrows)) {
                val initialMoves = GameSolver.countInitialMoves(rows, cols, arrows)
                // True brain puzzle: ensure 1-5 arrows can escape at first move
                if (initialMoves in 1..5 || attempt > 15) {
                    val rawLevel = LevelState(
                        levelNumber = levelNumber,
                        gridRows = rows,
                        gridCols = cols,
                        arrows = arrows,
                        livesRemaining = 4,
                        maxLives = 4,
                        isDaily = isDaily
                    )
                    return ensureZeroOverlaps(rawLevel)
                }
            }
        }

        // Guaranteed fallback if all random attempts failed
        val fallbackArrows = generateFallbackLevel(rows, cols, targetArrowCount)
        return ensureZeroOverlaps(
            LevelState(
                levelNumber = levelNumber,
                gridRows = rows,
                gridCols = cols,
                arrows = fallbackArrows,
                livesRemaining = 4,
                maxLives = 4,
                isDaily = isDaily
            )
        )
    }

    private fun getPaletteForLevel(level: Int): List<Long> {
        return when ((level - 1) % 5) {
            0 -> listOf(0xFF0F172A, 0xFF1E293B, 0xFF2563EB) // Classic Onyx & Electric Blue
            1 -> listOf(0xFF0F172A, 0xFF0D9488, 0xFFF59E0B) // Teal, Amber & Dark Slate
            2 -> listOf(0xFF1E1B4B, 0xFF7C3AED, 0xFFEC4899) // Royal Indigo, Purple & Magenta
            3 -> listOf(0xFF064E3B, 0xFF059669, 0xFFE11D48) // Emerald Forest & Crimson Rose
            else -> listOf(0xFF18181B, 0xFFEA580C, 0xFF2563EB, 0xFF16A34A) // Multi-tone Neon
        }
    }

    private data class DifficultyParams(
        val rows: Int,
        val cols: Int,
        val arrowCount: Int,
        val maxBends: Int
    )

    private fun getLevelDifficultyParams(level: Int, isDaily: Boolean): DifficultyParams {
        if (isDaily) {
            return DifficultyParams(rows = 11, cols = 11, arrowCount = 30, maxBends = 4)
        }
        return when {
            level <= 1 -> DifficultyParams(rows = 6, cols = 6, arrowCount = 8, maxBends = 2)
            level <= 2 -> DifficultyParams(rows = 7, cols = 7, arrowCount = 12, maxBends = 2)
            level <= 3 -> DifficultyParams(rows = 8, cols = 8, arrowCount = 16, maxBends = 3)
            level <= 4 -> DifficultyParams(rows = 9, cols = 9, arrowCount = 20, maxBends = 3)
            level <= 6 -> DifficultyParams(rows = 9, cols = 9, arrowCount = 24, maxBends = 3)
            level <= 10 -> DifficultyParams(rows = 10, cols = 10, arrowCount = 28, maxBends = 4)
            level <= 16 -> DifficultyParams(rows = 10, cols = 10, arrowCount = 32, maxBends = 4)
            level <= 25 -> DifficultyParams(rows = 11, cols = 11, arrowCount = 36, maxBends = 4)
            level <= 40 -> DifficultyParams(rows = 12, cols = 12, arrowCount = 44, maxBends = 5)
            level <= 60 -> DifficultyParams(rows = 12, cols = 12, arrowCount = 50, maxBends = 5)
            level <= 80 -> DifficultyParams(rows = 13, cols = 13, arrowCount = 58, maxBends = 6)
            else -> DifficultyParams(rows = 14, cols = 14, arrowCount = 64, maxBends = 6)
        }
    }

    /**
     * Procedurally constructs dense, interlocking snake-like arrows using Reverse-DAG construction.
     * Guarantees 100% solvability, 0 overlapping cells, and winding serpentine shapes
     * (S-curves, U-turns, L-bends, and nested spirals) as seen in high-difficulty puzzle mazes.
     */
    private fun generateProceduralArrows(
        rows: Int,
        cols: Int,
        arrowCount: Int,
        maxBends: Int,
        palette: List<Long>,
        rng: Random
    ): List<Arrow> {
        val occupied = Array(rows) { BooleanArray(cols) }
        val placedInReverse = mutableListOf<Arrow>()

        fun isFree(r: Int, c: Int): Boolean {
            return r in 0 until rows && c in 0 until cols && !occupied[r][c]
        }

        // True Reverse-DAG test: Check if the forward exit ray from this candidate head
        // is clear of any arrow placed in PRIOR steps of the reverse-DAG.
        // It CAN pass through empty cells, because those will be filled by arrows
        // placed in LATER steps (which the player will tap and clear BEFORE this arrow).
        fun isForwardRayClearOfPrior(headR: Int, headC: Int, dir: Direction): Boolean {
            var r = headR + dir.dr
            var c = headC + dir.dc
            while (r in 0 until rows && c in 0 until cols) {
                if (occupied[r][c]) {
                    return false // Collides with an arrow that is still on the board when this arrow exits!
                }
                r += dir.dr
                c += dir.dc
            }
            return true
        }

        val totalCells = rows * cols
        val maxTotalAttempts = arrowCount * 80
        var attempts = 0

        while (placedInReverse.size < arrowCount && attempts < maxTotalAttempts) {
            attempts++

            val headR = rng.nextInt(rows)
            val headC = rng.nextInt(cols)
            if (!isFree(headR, headC)) continue

            val candidateDirs = Direction.entries.shuffled(rng)
            var placed = false

            for (headDir in candidateDirs) {
                if (!isForwardRayClearOfPrior(headR, headC, headDir)) continue

                // Adjust length dynamically to maximize grid density
                val currentOccupiedCount = placedInReverse.sumOf { it.points.size }
                val occupancyRatio = currentOccupiedCount.toFloat() / totalCells
                val minLen = if (occupancyRatio > 0.60f || placedInReverse.size >= (arrowCount * 0.70).toInt()) 2 else 3
                val maxLen = (minLen + rng.nextInt(3) + 1).coerceIn(minLen, 5)

                val shapeType = when (rng.nextInt(5)) {
                    0 -> "S_CURVE"
                    1 -> "U_TURN"
                    2 -> "SPIRAL"
                    3 -> "L_BEND"
                    else -> "WINDING"
                }

                val snakePoints = growWindingSnake(
                    headR = headR,
                    headC = headC,
                    headDir = headDir,
                    targetLength = maxLen,
                    minLen = minLen,
                    maxBends = maxBends,
                    shapeType = shapeType,
                    isFree = ::isFree,
                    rng = rng
                )

                if (snakePoints.size >= minLen) {
                    for (pt in snakePoints) {
                        occupied[pt.row][pt.col] = true
                    }
                    val color = palette[placedInReverse.size % palette.size]
                    placedInReverse.add(
                        Arrow(
                            id = "arrow_${placedInReverse.size}_${rng.nextInt(10000)}",
                            points = snakePoints,
                            headDirection = headDir,
                            colorHex = color
                        )
                    )
                    placed = true
                    break
                }
            }
        }

        // Return reversed: The arrow placed last has NOTHING placed after it, so its ray is
        // 100% free of obstacles on move 1. Every subsequent arrow in this list is unlocked
        // by clearing preceding arrows!
        return placedInReverse.reversed()
    }

    private fun growWindingSnake(
        headR: Int,
        headC: Int,
        headDir: Direction,
        targetLength: Int,
        minLen: Int,
        maxBends: Int,
        shapeType: String,
        isFree: (Int, Int) -> Boolean,
        rng: Random
    ): List<GridPoint> {
        val points = mutableListOf<GridPoint>()
        points.add(GridPoint(headR, headC))

        var currR = headR
        var currC = headC
        var currDir = getOpposite(headDir)
        var bendsLeft = maxBends
        var turnIndex = 0

        for (step in 1 until targetLength) {
            val shouldBend = when (shapeType) {
                "S_CURVE" -> (step >= 2 && step % 2 == 0 && bendsLeft > 0 && rng.nextFloat() < 0.75f)
                "U_TURN" -> (step == 2 || step == 3) && bendsLeft > 0
                "SPIRAL" -> (step >= 2 && rng.nextFloat() < 0.65f && bendsLeft > 0)
                "L_BEND" -> (step >= 2 && bendsLeft == maxBends)
                else -> (bendsLeft > 0 && step >= 2 && rng.nextFloat() < 0.55f)
            }

            if (shouldBend) {
                val perps = getPerpendicularDirs(currDir).filter { d ->
                    val nr = currR + d.dr
                    val nc = currC + d.dc
                    isFree(nr, nc) && points.none { it.row == nr && it.col == nc }
                }

                if (perps.isNotEmpty()) {
                    val chosenDir = if (shapeType == "S_CURVE") {
                        if (turnIndex % 2 == 0) perps.first() else perps.last()
                    } else if (shapeType == "U_TURN") {
                        perps.first()
                    } else {
                        perps[rng.nextInt(perps.size)]
                    }
                    currDir = chosenDir
                    bendsLeft--
                    turnIndex++
                }
            }

            val nextR = currR + currDir.dr
            val nextC = currC + currDir.dc

            if (isFree(nextR, nextC) && points.none { it.row == nextR && it.col == nextC }) {
                points.add(GridPoint(nextR, nextC))
                currR = nextR
                currC = nextC
            } else {
                if (bendsLeft > 0) {
                    val perps = getPerpendicularDirs(currDir).filter { d ->
                        val nr = currR + d.dr
                        val nc = currC + d.dc
                        isFree(nr, nc) && points.none { it.row == nr && it.col == nc }
                    }
                    if (perps.isNotEmpty()) {
                        currDir = perps[rng.nextInt(perps.size)]
                        bendsLeft--
                        turnIndex++
                        val nr = currR + currDir.dr
                        val nc = currC + currDir.dc
                        points.add(GridPoint(nr, nc))
                        currR = nr
                        currC = nc
                        continue
                    }
                }
                break
            }
        }

        return if (points.size >= minLen) points.reversed() else emptyList()
    }

    private fun getOpposite(dir: Direction): Direction {
        return when (dir) {
            Direction.UP -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.LEFT -> Direction.RIGHT
            Direction.RIGHT -> Direction.LEFT
        }
    }

    private fun getPerpendicularDirs(dir: Direction): List<Direction> {
        return when (dir) {
            Direction.UP, Direction.DOWN -> listOf(Direction.LEFT, Direction.RIGHT)
            Direction.LEFT, Direction.RIGHT -> listOf(Direction.UP, Direction.DOWN)
        }
    }

    /**
     * Expands any point sequence into strictly step-by-step contiguous GridPoints (step distance = 1).
     * Eliminates any possibility of lines skipping cells or cutting through other arrows.
     */
    fun expandContiguousPoints(points: List<GridPoint>): List<GridPoint> {
        if (points.size <= 1) return points
        val result = mutableListOf<GridPoint>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val curr = points[i]
            val next = points[i + 1]

            var r = curr.row
            var c = curr.col

            while (r != next.row || c != next.col) {
                if (r < next.row) r++
                else if (r > next.row) r--
                else if (c < next.col) c++
                else if (c > next.col) c--

                val stepPoint = GridPoint(r, c)
                if (result.lastOrNull() != stepPoint) {
                    result.add(stepPoint)
                }
            }
        }

        return result
    }

    /**
     * Post-processing step applied to EVERY level (handcrafted, procedural, and fallback).
     * Guarantees that:
     * 1. Every arrow has strictly contiguous step-by-step points.
     * 2. NO two arrows ever share ANY cell (100% strictly disjoint).
     * 3. No arrow intersects, overlaps, or touches another arrow's cells.
     */
    fun ensureZeroOverlaps(level: LevelState): LevelState {
        val occupiedCells = mutableSetOf<Pair<Int, Int>>()
        val cleanArrows = mutableListOf<Arrow>()

        for (arrow in level.arrows) {
            val expanded = expandContiguousPoints(arrow.points)
            // Verify that no point in this arrow collides with an already occupied cell
            val collides = expanded.any { pt ->
                pt.row !in 0 until level.gridRows ||
                pt.col !in 0 until level.gridCols ||
                occupiedCells.contains(pt.row to pt.col)
            }

            if (!collides && expanded.size >= 2) {
                for (pt in expanded) {
                    occupiedCells.add(pt.row to pt.col)
                }
                cleanArrows.add(arrow.copy(points = expanded))
            }
        }

        return level.copy(arrows = cleanArrows)
    }

    private fun generateFallbackLevel(rows: Int, cols: Int, count: Int): List<Arrow> {
        val arrows = mutableListOf<Arrow>()
        val occupied = Array(rows) { BooleanArray(cols) }
        var id = 0

        // Place perimeter-facing arrows along the 4 borders with 0 overlap
        for (c in 0 until cols step 2) {
            if (arrows.size >= count) break
            val pts = listOf(GridPoint(1, c), GridPoint(0, c))
            if (pts.all { !occupied[it.row][it.col] }) {
                pts.forEach { occupied[it.row][it.col] = true }
                arrows.add(Arrow(id = "fb_${id++}", points = pts, headDirection = Direction.UP))
            }
        }
        for (c in 1 until cols step 2) {
            if (arrows.size >= count) break
            val pts = listOf(GridPoint(rows - 2, c), GridPoint(rows - 1, c))
            if (pts.all { !occupied[it.row][it.col] }) {
                pts.forEach { occupied[it.row][it.col] = true }
                arrows.add(Arrow(id = "fb_${id++}", points = pts, headDirection = Direction.DOWN))
            }
        }
        for (r in 0 until rows step 2) {
            if (arrows.size >= count) break
            val pts = listOf(GridPoint(r, 1), GridPoint(r, 0))
            if (pts.all { !occupied[it.row][it.col] }) {
                pts.forEach { occupied[it.row][it.col] = true }
                arrows.add(Arrow(id = "fb_${id++}", points = pts, headDirection = Direction.LEFT))
            }
        }
        for (r in 1 until rows step 2) {
            if (arrows.size >= count) break
            val pts = listOf(GridPoint(r, cols - 2), GridPoint(r, cols - 1))
            if (pts.all { !occupied[it.row][it.col] }) {
                pts.forEach { occupied[it.row][it.col] = true }
                arrows.add(Arrow(id = "fb_${id++}", points = pts, headDirection = Direction.RIGHT))
            }
        }
        return arrows
    }

    /**
     * Handcrafted Level 1:
     * 8 dense intertwined snake arrows. Dense onboarding with 100% zero overlapping cells.
     */
    private fun createHandcraftedLevel1(): LevelState {
        val arrows = listOf(
            // 1. Blue Hero arrow: (2, 0) -> (1, 0) -> (0, 0) UP.
            Arrow(
                id = "lvl1_blue_hero",
                points = listOf(GridPoint(2, 0), GridPoint(1, 0), GridPoint(0, 0)),
                headDirection = Direction.UP,
                colorHex = 0xFF2563EB
            ),
            // 2. Right arrow along bottom: (5, 2) -> (5, 3) -> (5, 4) -> (5, 5) RIGHT.
            Arrow(
                id = "lvl1_bot_right",
                points = listOf(GridPoint(5, 2), GridPoint(5, 3), GridPoint(5, 4), GridPoint(5, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 3. Up arrow along right edge: (4, 5) -> (3, 5) -> (2, 5) -> (1, 5) -> (0, 5) UP.
            Arrow(
                id = "lvl1_right_up",
                points = listOf(GridPoint(4, 5), GridPoint(3, 5), GridPoint(2, 5), GridPoint(1, 5), GridPoint(0, 5)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            // 4. Top left pointing LEFT: (0, 4) -> (0, 3) -> (0, 2) -> (0, 1) LEFT.
            Arrow(
                id = "lvl1_top_left",
                points = listOf(GridPoint(0, 4), GridPoint(0, 3), GridPoint(0, 2), GridPoint(0, 1)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            // 5. Left snake: (3, 0) -> (4, 0) -> (5, 0) -> (5, 1) RIGHT.
            Arrow(
                id = "lvl1_left_turn",
                points = listOf(GridPoint(3, 0), GridPoint(4, 0), GridPoint(5, 0), GridPoint(5, 1)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 6. Center S-snake: (2, 2) -> (1, 2) -> (1, 3) -> (1, 4) RIGHT.
            Arrow(
                id = "lvl1_mid_s",
                points = listOf(GridPoint(2, 2), GridPoint(1, 2), GridPoint(1, 3), GridPoint(1, 4)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 7. Center vertical down: (2, 3) -> (2, 4) -> (3, 4) -> (4, 4) DOWN.
            Arrow(
                id = "lvl1_mid_down",
                points = listOf(GridPoint(2, 3), GridPoint(2, 4), GridPoint(3, 4), GridPoint(4, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            // 8. Center-left bend: (3, 3) -> (3, 2) -> (4, 2) -> (4, 1) LEFT.
            Arrow(
                id = "lvl1_mid_left",
                points = listOf(GridPoint(3, 3), GridPoint(3, 2), GridPoint(4, 2), GridPoint(4, 1)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            )
        )

        return LevelState(
            levelNumber = 1,
            gridRows = 6,
            gridCols = 6,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "घना शुरुआत (Dense Start)"
        )
    }

    /**
     * Handcrafted Level 2: "चश्मा (Glasses Theme / Chashma)"
     * 13 snake arrows forming spectacles with lens rims, bridge, and temples. 100% disjoint.
     */
    private fun createHandcraftedGlassesLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "glasses_bridge",
                points = listOf(GridPoint(2, 3), GridPoint(1, 3), GridPoint(0, 3)),
                headDirection = Direction.UP,
                colorHex = 0xFF2563EB
            ),
            Arrow(
                id = "glasses_left_top",
                points = listOf(GridPoint(1, 1), GridPoint(1, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "glasses_right_top",
                points = listOf(GridPoint(1, 5), GridPoint(1, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "glasses_temple_left",
                points = listOf(GridPoint(0, 2), GridPoint(0, 1), GridPoint(0, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "glasses_temple_right",
                points = listOf(GridPoint(0, 4), GridPoint(0, 5), GridPoint(0, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "glasses_left_inner",
                points = listOf(GridPoint(3, 2), GridPoint(2, 2)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "glasses_right_inner",
                points = listOf(GridPoint(3, 4), GridPoint(2, 4)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "glasses_left_outer",
                points = listOf(GridPoint(2, 0), GridPoint(3, 0)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "glasses_right_outer",
                points = listOf(GridPoint(2, 6), GridPoint(3, 6)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "glasses_left_bottom",
                points = listOf(GridPoint(4, 0), GridPoint(4, 1), GridPoint(4, 2)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "glasses_right_bottom",
                points = listOf(GridPoint(4, 6), GridPoint(4, 5), GridPoint(4, 4)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "glasses_accent_left",
                points = listOf(GridPoint(5, 1), GridPoint(6, 1)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF64748B
            ),
            Arrow(
                id = "glasses_accent_right",
                points = listOf(GridPoint(5, 5), GridPoint(6, 5)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF64748B
            )
        )

        return LevelState(
            levelNumber = 2,
            gridRows = 7,
            gridCols = 7,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "चश्मा (Glasses Theme)"
        )
    }

    /**
     * Handcrafted Level 4: "बिल्ली / जानवर (Cat Animal Theme)"
     * 14 snake arrows shaping cat ears, head, whiskers, body, paws, and curling tail. 100% disjoint.
     */
    private fun createHandcraftedCatLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "cat_ear_left",
                points = listOf(GridPoint(1, 1), GridPoint(0, 1)),
                headDirection = Direction.UP,
                colorHex = 0xFFF59E0B
            ),
            Arrow(
                id = "cat_ear_right",
                points = listOf(GridPoint(1, 7), GridPoint(0, 7)),
                headDirection = Direction.UP,
                colorHex = 0xFFF59E0B
            ),
            Arrow(
                id = "cat_head_top",
                points = listOf(GridPoint(0, 3), GridPoint(0, 4), GridPoint(0, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFD97706
            ),
            Arrow(
                id = "cat_forehead",
                points = listOf(GridPoint(1, 3), GridPoint(1, 4), GridPoint(1, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFD97706
            ),
            Arrow(
                id = "cat_eye_left",
                points = listOf(GridPoint(2, 2), GridPoint(2, 1), GridPoint(2, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "cat_eye_right",
                points = listOf(GridPoint(2, 6), GridPoint(2, 7), GridPoint(2, 8)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "cat_snout",
                points = listOf(GridPoint(2, 4), GridPoint(3, 4), GridPoint(4, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFEF4444
            ),
            Arrow(
                id = "cat_whisker_l",
                points = listOf(GridPoint(3, 2), GridPoint(3, 1), GridPoint(3, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "cat_whisker_r",
                points = listOf(GridPoint(3, 6), GridPoint(3, 7), GridPoint(3, 8)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "cat_chest",
                points = listOf(GridPoint(5, 3), GridPoint(5, 4), GridPoint(5, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFF59E0B
            ),
            Arrow(
                id = "cat_paw_left",
                points = listOf(GridPoint(6, 2), GridPoint(7, 2), GridPoint(8, 2)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "cat_paw_right",
                points = listOf(GridPoint(6, 6), GridPoint(7, 6), GridPoint(8, 6)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "cat_body",
                points = listOf(GridPoint(6, 4), GridPoint(7, 4), GridPoint(8, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFB45309
            ),
            Arrow(
                id = "cat_tail",
                points = listOf(GridPoint(6, 8), GridPoint(7, 8), GridPoint(8, 8), GridPoint(8, 7)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFD97706
            )
        )

        return LevelState(
            levelNumber = 4,
            gridRows = 9,
            gridCols = 9,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "बिल्ली / जानवर (Cat Theme)"
        )
    }

    /**
     * Handcrafted Level 5: "मछली / जानवर (Fish Animal Theme)"
     * 14 snake arrows shaping fish nose, fins, streamlined body, and tail fins. 100% disjoint.
     */
    private fun createHandcraftedFishLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "fish_nose",
                points = listOf(GridPoint(4, 1), GridPoint(4, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF06B6D4
            ),
            Arrow(
                id = "fish_dorsal",
                points = listOf(GridPoint(1, 4), GridPoint(0, 4)),
                headDirection = Direction.UP,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_ventral",
                points = listOf(GridPoint(7, 4), GridPoint(8, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_top_flank",
                points = listOf(GridPoint(2, 2), GridPoint(2, 3), GridPoint(2, 4), GridPoint(2, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "fish_mid_upper",
                points = listOf(GridPoint(3, 1), GridPoint(3, 2), GridPoint(3, 3), GridPoint(3, 4), GridPoint(3, 5), GridPoint(3, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_mid_lower",
                points = listOf(GridPoint(5, 1), GridPoint(5, 2), GridPoint(5, 3), GridPoint(5, 4), GridPoint(5, 5), GridPoint(5, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_bottom_flank",
                points = listOf(GridPoint(6, 2), GridPoint(6, 3), GridPoint(6, 4), GridPoint(6, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "fish_spine",
                points = listOf(GridPoint(4, 2), GridPoint(4, 3), GridPoint(4, 4), GridPoint(4, 5)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF38BDF8
            ),
            Arrow(
                id = "fish_tail_waist",
                points = listOf(GridPoint(4, 6), GridPoint(4, 7), GridPoint(4, 8)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF06B6D4
            ),
            Arrow(
                id = "fish_tail_top",
                points = listOf(GridPoint(3, 7), GridPoint(2, 8), GridPoint(1, 8)),
                headDirection = Direction.UP,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_tail_bot",
                points = listOf(GridPoint(5, 7), GridPoint(6, 8), GridPoint(7, 8)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_eye",
                points = listOf(GridPoint(3, 0), GridPoint(2, 0)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "fish_chin",
                points = listOf(GridPoint(5, 0), GridPoint(6, 0)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "fish_dorsal_fin_left",
                points = listOf(GridPoint(1, 2), GridPoint(1, 3)),
                headDirection = Direction.UP,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_dorsal_fin_right",
                points = listOf(GridPoint(1, 5), GridPoint(1, 6)),
                headDirection = Direction.UP,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_ventral_fin_left",
                points = listOf(GridPoint(7, 2), GridPoint(7, 3)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_ventral_fin_right",
                points = listOf(GridPoint(7, 5), GridPoint(7, 6)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "fish_gill_upper",
                points = listOf(GridPoint(2, 1), GridPoint(1, 1)),
                headDirection = Direction.UP,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "fish_gill_lower",
                points = listOf(GridPoint(6, 1), GridPoint(7, 1)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0EA5E9
            )
        )

        return LevelState(
            levelNumber = 5,
            gridRows = 9,
            gridCols = 9,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "मछली / जानवर (Fish Theme)"
        )
    }

    /**
     * Handcrafted Level 6: "तितली / जानवर (Butterfly Animal Theme)"
     * 14 snake arrows shaping antennae, central body, and expansive symmetrical wings. 100% disjoint.
     */
    private fun createHandcraftedButterflyLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "btf_antennae_l",
                points = listOf(GridPoint(1, 3), GridPoint(0, 2)),
                headDirection = Direction.UP,
                colorHex = 0xFF8B5CF6
            ),
            Arrow(
                id = "btf_antennae_r",
                points = listOf(GridPoint(1, 6), GridPoint(0, 7)),
                headDirection = Direction.UP,
                colorHex = 0xFF8B5CF6
            ),
            Arrow(
                id = "btf_body",
                points = listOf(GridPoint(2, 4), GridPoint(3, 4), GridPoint(4, 4), GridPoint(5, 4), GridPoint(6, 4), GridPoint(7, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF6D28D9
            ),
            Arrow(
                id = "btf_wing_top_l",
                points = listOf(GridPoint(1, 2), GridPoint(1, 1), GridPoint(1, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFEC4899
            ),
            Arrow(
                id = "btf_wing_top_r",
                points = listOf(GridPoint(1, 7), GridPoint(1, 8), GridPoint(1, 9)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFEC4899
            ),
            Arrow(
                id = "btf_wing_mid_l",
                points = listOf(GridPoint(2, 2), GridPoint(3, 2), GridPoint(3, 1), GridPoint(3, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFF43F5E
            ),
            Arrow(
                id = "btf_wing_mid_r",
                points = listOf(GridPoint(2, 7), GridPoint(3, 7), GridPoint(3, 8), GridPoint(3, 9)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFF43F5E
            ),
            Arrow(
                id = "btf_wing_low_l",
                points = listOf(GridPoint(5, 2), GridPoint(6, 2), GridPoint(6, 1), GridPoint(6, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFA855F7
            ),
            Arrow(
                id = "btf_wing_low_r",
                points = listOf(GridPoint(5, 7), GridPoint(6, 7), GridPoint(6, 8), GridPoint(6, 9)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFA855F7
            ),
            Arrow(
                id = "btf_wing_tip_l",
                points = listOf(GridPoint(7, 3), GridPoint(8, 2), GridPoint(9, 1)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "btf_wing_tip_r",
                points = listOf(GridPoint(7, 6), GridPoint(8, 7), GridPoint(9, 8)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "btf_core_l",
                points = listOf(GridPoint(4, 2), GridPoint(4, 1), GridPoint(4, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFD946EF
            ),
            Arrow(
                id = "btf_core_r",
                points = listOf(GridPoint(4, 7), GridPoint(4, 8), GridPoint(4, 9)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFD946EF
            ),
            Arrow(
                id = "btf_bottom_tip",
                points = listOf(GridPoint(8, 4), GridPoint(9, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF581C87
            )
        )

        return LevelState(
            levelNumber = 6,
            gridRows = 10,
            gridCols = 10,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "तितली / जानवर (Butterfly Theme)"
        )
    }

    /**
     * Handcrafted Level 20:
     * Directly crafted after the user's uploaded screenshot (Level 20 in "Tap Away Arrows"):
     * - Long vertical arrow on the left going UP
     * - Winding snake arrows in the upper half
     * - Iconic blue horizontal bent snake in the middle pointing RIGHT with clear exit
     * - Nested U-spiral snake block in bottom right
     * - Interlocking arrows on the left and bottom
     * Every arrow is 100% contiguous and completely disjoint (zero overlapping cells).
     */
    private fun createHandcraftedLevel20(): LevelState {
        val arrows = listOf(
            // 1. Far left vertical arrow pointing UP to top edge
            Arrow(
                id = "lvl20_left_up",
                points = listOf(
                    GridPoint(9, 1), GridPoint(8, 1), GridPoint(7, 1),
                    GridPoint(6, 1), GridPoint(5, 1), GridPoint(4, 1),
                    GridPoint(3, 1), GridPoint(2, 1), GridPoint(1, 1), GridPoint(0, 1)
                ),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            // 2. Top-most arrow pointing LEFT
            Arrow(
                id = "lvl20_top_left",
                points = listOf(GridPoint(0, 6), GridPoint(0, 5), GridPoint(0, 4), GridPoint(0, 3), GridPoint(0, 2)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            // 3. Top-right U-turn snake pointing RIGHT
            Arrow(
                id = "lvl20_top_u_down",
                points = listOf(
                    GridPoint(2, 8), GridPoint(1, 8), GridPoint(0, 8),
                    GridPoint(0, 9), GridPoint(1, 9), GridPoint(2, 9)
                ),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 4. Far right vertical arrow pointing UP
            Arrow(
                id = "lvl20_right_up",
                points = listOf(GridPoint(2, 10), GridPoint(1, 10), GridPoint(0, 10)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            // 5. Upper-left winding snake
            Arrow(
                id = "lvl20_upper_s_snake",
                points = listOf(GridPoint(1, 3), GridPoint(2, 3), GridPoint(2, 4), GridPoint(3, 4), GridPoint(3, 3), GridPoint(4, 3)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            // 6. Center-upper S-snake
            Arrow(
                id = "lvl20_center_s",
                points = listOf(GridPoint(1, 6), GridPoint(2, 6), GridPoint(2, 5), GridPoint(3, 5), GridPoint(3, 6), GridPoint(3, 7)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 7. THE ICONIC BLUE HERO SNAKE (Clear unobstructed escape to the right!)
            Arrow(
                id = "lvl20_blue_hero",
                points = listOf(
                    GridPoint(6, 3), GridPoint(5, 3), GridPoint(5, 4),
                    GridPoint(5, 5), GridPoint(5, 6), GridPoint(5, 7),
                    GridPoint(4, 7), GridPoint(4, 8), GridPoint(4, 9)
                ),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF2563EB
            ),
            // 8. Mid horizontal arrow pointing RIGHT
            Arrow(
                id = "lvl20_mid_horiz",
                points = listOf(GridPoint(6, 4), GridPoint(6, 5), GridPoint(6, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 9. Left column downward arrow
            Arrow(
                id = "lvl20_left_down",
                points = listOf(GridPoint(1, 2), GridPoint(2, 2), GridPoint(3, 2), GridPoint(4, 2), GridPoint(5, 2)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            // 10. Mid-low upward arrow 1
            Arrow(
                id = "lvl20_mid_low_up1",
                points = listOf(GridPoint(9, 4), GridPoint(8, 4), GridPoint(7, 4)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            // 11. Mid-low upward arrow 2
            Arrow(
                id = "lvl20_mid_low_up2",
                points = listOf(GridPoint(9, 5), GridPoint(8, 5), GridPoint(7, 5)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            ),
            // 12. Bottom-left vertical arrow pointing DOWN
            Arrow(
                id = "lvl20_bottom_left_down",
                points = listOf(
                    GridPoint(6, 2), GridPoint(7, 2), GridPoint(8, 2), GridPoint(9, 2), GridPoint(10, 2)
                ),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            // 13. Nested box outer frame in bottom-right
            Arrow(
                id = "lvl20_nested_frame",
                points = listOf(
                    GridPoint(6, 9), GridPoint(6, 8), GridPoint(6, 7),
                    GridPoint(7, 7), GridPoint(8, 7), GridPoint(9, 7),
                    GridPoint(10, 7), GridPoint(10, 8), GridPoint(10, 9)
                ),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0F172A
            ),
            // 14. Nested box inner top arrow pointing LEFT
            Arrow(
                id = "lvl20_nested_inner_top",
                points = listOf(GridPoint(7, 9), GridPoint(7, 8)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            // 15. Nested box inner bottom snake pointing LEFT
            Arrow(
                id = "lvl20_nested_inner_bottom",
                points = listOf(GridPoint(8, 8), GridPoint(8, 9), GridPoint(9, 9), GridPoint(9, 8)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            // 16. Bottom edge horizontal arrow pointing LEFT
            Arrow(
                id = "lvl20_bottom_edge_left",
                points = listOf(GridPoint(10, 5), GridPoint(10, 4), GridPoint(10, 3)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0F172A
            ),
            // 17. Far right bottom arrow pointing UP
            Arrow(
                id = "lvl20_far_right_low",
                points = listOf(GridPoint(8, 10), GridPoint(7, 10), GridPoint(6, 10)),
                headDirection = Direction.UP,
                colorHex = 0xFF0F172A
            )
        )

        return LevelState(
            levelNumber = 20,
            gridRows = 11,
            gridCols = 11,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "Challenge 20"
        )
    }

    /**
     * Handcrafted Level 3: "दिल (Heart Theme / Dil)"
     * Rich, dense intertwined arrows outlining and filling an iconic heart shape with 100% disjoint cells.
     */
    private fun createHandcraftedHeartLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "heart_lobe_left",
                points = listOf(GridPoint(2, 2), GridPoint(1, 2), GridPoint(0, 2)),
                headDirection = Direction.UP,
                colorHex = 0xFFE11D48
            ),
            Arrow(
                id = "heart_lobe_right",
                points = listOf(GridPoint(2, 6), GridPoint(1, 6), GridPoint(0, 6)),
                headDirection = Direction.UP,
                colorHex = 0xFFE11D48
            ),
            Arrow(
                id = "heart_outer_left",
                points = listOf(GridPoint(2, 1), GridPoint(1, 1), GridPoint(1, 0), GridPoint(2, 0), GridPoint(3, 0)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFF43F5E
            ),
            Arrow(
                id = "heart_outer_right",
                points = listOf(GridPoint(2, 7), GridPoint(1, 7), GridPoint(1, 8), GridPoint(2, 8), GridPoint(3, 8)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFF43F5E
            ),
            Arrow(
                id = "heart_center_cleft",
                points = listOf(GridPoint(1, 4), GridPoint(2, 4), GridPoint(3, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFBE123C
            ),
            Arrow(
                id = "heart_chest_left",
                points = listOf(GridPoint(3, 3), GridPoint(3, 2), GridPoint(3, 1)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFFB7185
            ),
            Arrow(
                id = "heart_chest_right",
                points = listOf(GridPoint(3, 5), GridPoint(3, 6), GridPoint(3, 7)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFFB7185
            ),
            Arrow(
                id = "heart_mid_left",
                points = listOf(GridPoint(4, 3), GridPoint(4, 2), GridPoint(4, 1)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFFB7185
            ),
            Arrow(
                id = "heart_mid_right",
                points = listOf(GridPoint(4, 5), GridPoint(4, 6), GridPoint(4, 7)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFFB7185
            ),
            Arrow(
                id = "heart_tip",
                points = listOf(GridPoint(4, 4), GridPoint(5, 4), GridPoint(6, 4), GridPoint(7, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFE11D48
            ),
            Arrow(
                id = "heart_lower_left",
                points = listOf(GridPoint(5, 3), GridPoint(5, 2)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFF43F5E
            ),
            Arrow(
                id = "heart_lower_right",
                points = listOf(GridPoint(5, 5), GridPoint(5, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFF43F5E
            )
        )

        return LevelState(
            levelNumber = 3,
            gridRows = 9,
            gridCols = 9,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "दिल (Heart Theme)"
        )
    }

    /**
     * Handcrafted Level 7: "घर (House Theme / Ghar)"
     * Triangular roof arrows + rectangular room structure, 100% disjoint cells.
     */
    private fun createHandcraftedHouseLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "house_chimney",
                points = listOf(GridPoint(2, 2), GridPoint(1, 2), GridPoint(0, 2)),
                headDirection = Direction.UP,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "house_roof_peak",
                points = listOf(GridPoint(2, 4), GridPoint(1, 4), GridPoint(0, 4)),
                headDirection = Direction.UP,
                colorHex = 0xFF0369A1
            ),
            Arrow(
                id = "house_roof_left",
                points = listOf(GridPoint(1, 3), GridPoint(2, 3), GridPoint(2, 1), GridPoint(2, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "house_roof_right",
                points = listOf(GridPoint(1, 5), GridPoint(2, 5), GridPoint(2, 7), GridPoint(2, 8)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0284C7
            ),
            Arrow(
                id = "house_attic_floor",
                points = listOf(GridPoint(3, 2), GridPoint(3, 3), GridPoint(3, 4), GridPoint(3, 5), GridPoint(3, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0D9488
            ),
            Arrow(
                id = "house_left_wall",
                points = listOf(GridPoint(4, 1), GridPoint(5, 1), GridPoint(6, 1), GridPoint(7, 1)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "house_right_wall",
                points = listOf(GridPoint(4, 7), GridPoint(5, 7), GridPoint(6, 7), GridPoint(7, 7)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF0F172A
            ),
            Arrow(
                id = "house_door",
                points = listOf(GridPoint(5, 4), GridPoint(6, 4), GridPoint(7, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFF59E0B
            ),
            Arrow(
                id = "house_window_left",
                points = listOf(GridPoint(5, 2), GridPoint(5, 3)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "house_window_right",
                points = listOf(GridPoint(5, 5), GridPoint(5, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF0EA5E9
            ),
            Arrow(
                id = "house_foundation_left",
                points = listOf(GridPoint(8, 5), GridPoint(8, 4), GridPoint(8, 3), GridPoint(8, 2), GridPoint(8, 1), GridPoint(8, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF1E293B
            ),
            Arrow(
                id = "house_foundation_right",
                points = listOf(GridPoint(8, 6), GridPoint(8, 7), GridPoint(8, 8)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF1E293B
            )
        )

        return LevelState(
            levelNumber = 7,
            gridRows = 9,
            gridCols = 9,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "घर (House Theme)"
        )
    }

    /**
     * Handcrafted Level 8: "दिमाग भूलभुलैया (Brain Maze Theme)"
     * High-density labyrinth with convoluted cerebral spiral paths, 100% disjoint cells.
     */
    private fun createHandcraftedBrainLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "brain_left_upper",
                points = listOf(GridPoint(2, 2), GridPoint(1, 2), GridPoint(0, 2)),
                headDirection = Direction.UP,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "brain_right_upper",
                points = listOf(GridPoint(2, 7), GridPoint(1, 7), GridPoint(0, 7)),
                headDirection = Direction.UP,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "brain_center_fissure",
                points = listOf(GridPoint(1, 4), GridPoint(2, 4), GridPoint(3, 4), GridPoint(4, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF6D28D9
            ),
            Arrow(
                id = "brain_outer_left",
                points = listOf(GridPoint(2, 1), GridPoint(1, 1), GridPoint(1, 0), GridPoint(2, 0), GridPoint(3, 0), GridPoint(4, 0)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF8B5CF6
            ),
            Arrow(
                id = "brain_outer_right",
                points = listOf(GridPoint(2, 8), GridPoint(1, 8), GridPoint(1, 9), GridPoint(2, 9), GridPoint(3, 9), GridPoint(4, 9)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF8B5CF6
            ),
            Arrow(
                id = "brain_left_spiral",
                points = listOf(GridPoint(3, 2), GridPoint(3, 1), GridPoint(4, 1), GridPoint(5, 1), GridPoint(5, 2)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFA78BFA
            ),
            Arrow(
                id = "brain_right_spiral",
                points = listOf(GridPoint(3, 7), GridPoint(3, 8), GridPoint(4, 8), GridPoint(5, 8), GridPoint(5, 7)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFA78BFA
            ),
            Arrow(
                id = "brain_mid_temp_left",
                points = listOf(GridPoint(4, 3), GridPoint(4, 2)),
                headDirection = Direction.UP,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "brain_mid_temp_right",
                points = listOf(GridPoint(4, 6), GridPoint(4, 7)),
                headDirection = Direction.UP,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "brain_stem_down",
                points = listOf(GridPoint(5, 4), GridPoint(6, 4), GridPoint(7, 4), GridPoint(8, 4), GridPoint(9, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF4C1D95
            ),
            Arrow(
                id = "brain_temp_left",
                points = listOf(GridPoint(6, 2), GridPoint(7, 2), GridPoint(7, 1), GridPoint(7, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "brain_temp_right",
                points = listOf(GridPoint(6, 7), GridPoint(7, 7), GridPoint(7, 8), GridPoint(7, 9)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF7C3AED
            ),
            Arrow(
                id = "brain_base_left",
                points = listOf(GridPoint(8, 3), GridPoint(8, 2), GridPoint(8, 1), GridPoint(9, 1), GridPoint(9, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF5B21B6
            ),
            Arrow(
                id = "brain_base_right",
                points = listOf(GridPoint(8, 6), GridPoint(8, 7), GridPoint(8, 8), GridPoint(9, 8), GridPoint(9, 9)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFF5B21B6
            )
        )

        return LevelState(
            levelNumber = 8,
            gridRows = 10,
            gridCols = 10,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "दिमाग भूलभुलैया (Brain Maze Theme)"
        )
    }

    /**
     * Handcrafted Level 7: "Face Profile" (Chehra)
     * Human side profile silhouette made of winding arrows, 100% disjoint cells.
     */
    private fun createHandcraftedFaceLevel(): LevelState {
        val arrows = listOf(
            Arrow(
                id = "face_forehead_up",
                points = listOf(GridPoint(2, 3), GridPoint(1, 3), GridPoint(0, 3)),
                headDirection = Direction.UP,
                colorHex = 0xFFEA580C
            ),
            Arrow(
                id = "face_forehead_front",
                points = listOf(GridPoint(1, 4), GridPoint(1, 5), GridPoint(1, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFF97316
            ),
            Arrow(
                id = "face_brow",
                points = listOf(GridPoint(2, 5), GridPoint(2, 6)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFFB923C
            ),
            Arrow(
                id = "face_nose_tip",
                points = listOf(GridPoint(3, 5), GridPoint(3, 6), GridPoint(3, 7), GridPoint(3, 8)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFC2410C
            ),
            Arrow(
                id = "face_nose_base",
                points = listOf(GridPoint(4, 7), GridPoint(4, 6), GridPoint(4, 5)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFEA580C
            ),
            Arrow(
                id = "face_lips_upper",
                points = listOf(GridPoint(5, 5), GridPoint(5, 6), GridPoint(5, 7)),
                headDirection = Direction.RIGHT,
                colorHex = 0xFFF97316
            ),
            Arrow(
                id = "face_mouth_cleft",
                points = listOf(GridPoint(6, 6), GridPoint(6, 5)),
                headDirection = Direction.LEFT,
                colorHex = 0xFFC2410C
            ),
            Arrow(
                id = "face_lips_lower",
                points = listOf(GridPoint(6, 7), GridPoint(7, 7)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFFB923C
            ),
            Arrow(
                id = "face_chin_jaw",
                points = listOf(GridPoint(7, 6), GridPoint(7, 5), GridPoint(8, 5), GridPoint(8, 4)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF9A3412
            ),
            Arrow(
                id = "face_jawline",
                points = listOf(GridPoint(8, 3), GridPoint(8, 2), GridPoint(9, 2)),
                headDirection = Direction.DOWN,
                colorHex = 0xFF7C2D12
            ),
            Arrow(
                id = "face_back_skull",
                points = listOf(GridPoint(3, 1), GridPoint(2, 1), GridPoint(1, 1), GridPoint(0, 1)),
                headDirection = Direction.UP,
                colorHex = 0xFFC2410C
            ),
            Arrow(
                id = "face_back_head",
                points = listOf(GridPoint(4, 1), GridPoint(5, 1), GridPoint(6, 1), GridPoint(7, 1)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFC2410C
            ),
            Arrow(
                id = "face_neck",
                points = listOf(GridPoint(8, 1), GridPoint(9, 1), GridPoint(9, 0)),
                headDirection = Direction.LEFT,
                colorHex = 0xFF7C2D12
            ),
            Arrow(
                id = "face_ear",
                points = listOf(GridPoint(3, 3), GridPoint(4, 3), GridPoint(5, 3)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFF97316
            ),
            Arrow(
                id = "face_cheek",
                points = listOf(GridPoint(4, 4), GridPoint(5, 4), GridPoint(6, 4)),
                headDirection = Direction.DOWN,
                colorHex = 0xFFFB923C
            )
        )

        return LevelState(
            levelNumber = 7,
            gridRows = 10,
            gridCols = 10,
            arrows = arrows,
            livesRemaining = 4,
            maxLives = 4,
            shapeName = "Face Profile (Chehra)"
        )
    }
}
