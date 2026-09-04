package com.example.arrowescape.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.engine.GameSolver
import com.example.arrowescape.model.Arrow
import com.example.arrowescape.model.Direction
import com.example.arrowescape.model.FlyingArrow
import com.example.arrowescape.model.GridPoint
import com.example.arrowescape.model.LevelState
import com.example.arrowescape.ui.theme.GameColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sign
import kotlin.math.sqrt

@Composable
fun GameBoard(
    levelState: LevelState,
    reducedMotion: Boolean,
    onTileClick: (Arrow) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .testTag("game_board"),
        contentAlignment = Alignment.Center
    ) {
        val rows = levelState.gridRows
        val cols = levelState.gridCols
        val boardWidth = maxWidth
        val boardHeight = maxHeight

        val cellWidth = boardWidth / cols
        val cellHeight = boardHeight / rows
        val cellSize = minOf(cellWidth, cellHeight)
        val boardPixelWidth = cellSize * cols
        val boardPixelHeight = cellSize * rows

        // Pulse animation for active hint / first move guide
        val infiniteTransition = rememberInfiniteTransition(label = "board_pulse")
        val hintPulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hint_pulse_alpha"
        )

        val rippleScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hand_ripple"
        )

        val rippleAlpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hand_ripple_alpha"
        )

        // Shake animatable for invalid taps
        val shakeAnim = remember { Animatable(0f) }
        LaunchedEffect(levelState.shakeArrowId) {
            if (levelState.shakeArrowId != null) {
                shakeAnim.snapTo(0f)
                shakeAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                )
                shakeAnim.snapTo(0f)
            }
        }

        // Tap ripple feedback location
        var tapFeedbackOffset by remember { mutableStateOf<Offset?>(null) }
        val tapFeedbackAlpha = remember { Animatable(0f) }

        LaunchedEffect(tapFeedbackOffset) {
            if (tapFeedbackOffset != null) {
                tapFeedbackAlpha.snapTo(0.8f)
                tapFeedbackAlpha.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                tapFeedbackOffset = null
            }
        }

        // Rocket escape flight progress animators
        val flyingProgressMap = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

        LaunchedEffect(levelState.flyingArrows) {
            val currentIds = levelState.flyingArrows.map { it.arrow.id }.toSet()
            flyingProgressMap.keys.retainAll(currentIds)
            for (flying in levelState.flyingArrows) {
                val arrowId = flying.arrow.id
                if (!flyingProgressMap.containsKey(arrowId)) {
                    val anim = Animatable(0f)
                    flyingProgressMap[arrowId] = anim
                    launch {
                        anim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 440, easing = LinearEasing)
                        )
                    }
                }
            }
        }

        // Candidate hint arrow or tutorial arrow
        val tutorialArrow = remember(levelState.levelNumber, levelState.arrows) {
            if (levelState.levelNumber == 1 || levelState.levelNumber == 20) {
                GameSolver.findRemovableArrows(levelState).firstOrNull()
            } else null
        }

        // Pinch to zoom and pan state
        var zoomScale by remember(levelState.levelNumber) { mutableFloatStateOf(1f) }
        var panOffset by remember(levelState.levelNumber) { mutableStateOf(Offset.Zero) }
        val isDenseLevel = levelState.levelNumber >= 5 || rows >= 8 || cols >= 8 || levelState.arrows.size >= 14

        Box(
            modifier = Modifier
                .size(boardPixelWidth, boardPixelHeight)
                .clipToBounds()
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
                    .pointerInput(levelState.levelNumber) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1f, 4.0f)
                            zoomScale = newScale
                            if (newScale > 1.05f) {
                                val maxPanX = (boardPixelWidth.toPx() * (newScale - 1f)) / 2f
                                val maxPanY = (boardPixelHeight.toPx() * (newScale - 1f)) / 2f
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                    y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                )
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(levelState.arrows, levelState.status, zoomScale, panOffset) {
                        detectTapGestures(
                            onDoubleTap = {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            onTap = { screenTap ->
                                val cellSizePx = cellSize.toPx()
                                val cx = (cols * cellSizePx) / 2f
                                val cy = (rows * cellSizePx) / 2f
                                val localX = (screenTap.x - cx - panOffset.x) / zoomScale + cx
                                val localY = (screenTap.y - cy - panOffset.y) / zoomScale + cy
                                val tapOffset = Offset(localX, localY)

                                tapFeedbackOffset = tapOffset

                                // Find the arrow closest to tapOffset
                                var closestArrow: Arrow? = null
                                var minDistance = Float.MAX_VALUE
                                val touchThreshold = (cellSizePx * 0.75f).coerceAtLeast(36.dp.toPx() / zoomScale)

                                for (arrow in levelState.arrows) {
                                    if (!arrow.filled) continue
                                    val dist = computeDistanceToArrow(tapOffset, arrow, cellSizePx)
                                    if (dist < minDistance && dist <= touchThreshold) {
                                        minDistance = dist
                                        closestArrow = arrow
                                    }
                                }

                                closestArrow?.let { arrow ->
                                    onTileClick(arrow)
                                }
                            }
                        )
                    }
            ) {
                val cellSizePx = cellSize.toPx()
                val boardPixelSize = maxOf(size.width, size.height)
                // Stroke width: clean slender line ensuring generous air gap between arrows
                val baseStrokeWidth = (cellSizePx * 0.10f).coerceIn(3.0.dp.toPx(), 4.8.dp.toPx())
                val cornerRadius = (cellSizePx * 0.30f).coerceIn(4.dp.toPx(), 12.dp.toPx())

                // 1. Draw subtle grid dots at cell centers
                val dotRadius = 1.8.dp.toPx()
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = (c + 0.5f) * cellSizePx
                        val cy = (r + 0.5f) * cellSizePx
                        drawCircle(
                            color = GameColors.GridDot.copy(alpha = 0.35f),
                            radius = dotRadius,
                            center = Offset(cx, cy)
                        )
                    }
                }

                // 2. Draw static & shaking arrows
                for (arrow in levelState.arrows) {
                    if (!arrow.filled) continue

                    val isShake = arrow.id == levelState.shakeArrowId
                    val isHint = arrow.id == levelState.activeHintArrowId ||
                        (levelState.levelNumber == 20 && arrow.id == "lvl20_blue_hero")

                    // Shake offset - restrained so shaking arrows never clip adjacent cells
                    val shakeOffset = if (isShake && !reducedMotion) {
                        val progress = shakeAnim.value
                        val displacement = sin(progress * PI.toFloat() * 4f) * (cellSizePx * 0.10f)
                        if (arrow.headDirection == Direction.UP || arrow.headDirection == Direction.DOWN) {
                            Offset(displacement, 0f)
                        } else {
                            Offset(0f, displacement)
                        }
                    } else Offset.Zero

                    val strokeColor = when {
                        isShake -> GameColors.ArrowErrorRed
                        isHint -> GameColors.ArrowActiveBlue.copy(alpha = hintPulseAlpha)
                        else -> Color(arrow.colorHex)
                    }

                    val strokeWidth = if (isHint) baseStrokeWidth * 1.25f else baseStrokeWidth

                    drawCurvedSnakeArrow(
                        arrow = arrow,
                        cellSizePx = cellSizePx,
                        cornerRadius = cornerRadius,
                        strokeWidth = strokeWidth,
                        strokeColor = strokeColor,
                        globalOffset = shakeOffset
                    )
                }

                // 3. Draw flying escaping arrows with rocket launch illusion ("shurrr se udta hua")
                for (flying in levelState.flyingArrows) {
                    val progress = flyingProgressMap[flying.arrow.id]?.value ?: 0.5f
                    drawRocketFlyingArrow(
                        flying = flying,
                        progress = progress,
                        cellSizePx = cellSizePx,
                        cornerRadius = cornerRadius,
                        baseStrokeWidth = baseStrokeWidth,
                        boardPixelSize = boardPixelSize
                    )
                }

                // 4. Draw collision impact sparks ("lade to aawaz aur visual effect")
                levelState.activeCollision?.let { impact ->
                    val cx = (impact.col + 0.5f) * cellSizePx
                    val cy = (impact.row + 0.5f) * cellSizePx
                    val impactCenter = Offset(cx, cy)

                    // Flash shockwave ring
                    drawCircle(
                        color = GameColors.ArrowErrorRed.copy(alpha = 0.8f),
                        radius = cellSizePx * 0.45f,
                        center = impactCenter,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = 0.9f),
                        radius = cellSizePx * 0.25f,
                        center = impactCenter,
                        style = Fill
                    )
                    // Draw 4 diagonal spark rays
                    val sparkLen = cellSizePx * 0.35f
                    val diagDirs = listOf(
                        Offset(-1f, -1f),
                        Offset(1f, -1f),
                        Offset(-1f, 1f),
                        Offset(1f, 1f)
                    )
                    for (d in diagDirs) {
                        drawLine(
                            color = GameColors.ArrowErrorRed,
                            start = impactCenter + d * (cellSizePx * 0.12f),
                            end = impactCenter + d * sparkLen,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 5. Draw touch ripple feedback
                tapFeedbackOffset?.let { tap ->
                    val alpha = tapFeedbackAlpha.value
                    if (alpha > 0f) {
                        drawCircle(
                            color = GameColors.PrimaryBlue.copy(alpha = alpha * 0.3f),
                            radius = cellSizePx * 0.45f,
                            center = tap,
                            style = Fill
                        )
                        drawCircle(
                            color = GameColors.PrimaryBlue.copy(alpha = alpha),
                            radius = cellSizePx * 0.75f,
                            center = tap,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // 5. Draw hand / guide pointer on Level 1 or Level 20 hero arrow
                val targetHero = if (levelState.activeHintArrowId != null) {
                    levelState.arrows.firstOrNull { it.id == levelState.activeHintArrowId }
                } else if (levelState.levelNumber == 1 && levelState.movesUsed == 0) {
                    tutorialArrow
                } else if (levelState.levelNumber == 20 && levelState.movesUsed == 0) {
                    levelState.arrows.firstOrNull { it.id == "lvl20_blue_hero" }
                } else null

                if (targetHero != null && targetHero.filled) {
                    val headPt = targetHero.head
                    val handCenter = Offset(
                        (headPt.col + 0.5f) * cellSizePx,
                        (headPt.row + 0.5f) * cellSizePx
                    )

                    // Concentric blue ripples
                    drawCircle(
                        color = GameColors.PrimaryBlue.copy(alpha = rippleAlpha * 0.7f),
                        radius = (cellSizePx * 0.5f) * rippleScale,
                        center = handCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawCircle(
                        color = GameColors.PrimaryBlue.copy(alpha = 0.25f),
                        radius = cellSizePx * 0.25f,
                        center = handCenter,
                        style = Fill
                    )

                    // Stylized hand pointer icon
                    drawHandPointer(
                        center = handCenter + Offset(cellSizePx * 0.32f, cellSizePx * 0.32f),
                        size = cellSizePx * 0.75f
                    )
                }
            }

            // Guidance & Zoom controls for dense board configurations
            if (isDenseLevel) {
                // Top floating indicator pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xD90F172A))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (zoomScale <= 1.05f) "🔍 2 उंगलियों से ज़ूम करें (2-Finger Zoom)" else "🔍 ${((zoomScale * 10).toInt() / 10f)}x • ड्रैग करें / डबल-टैप से रीसेट",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Floating quick zoom controller
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xEEFFFFFF))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(18.dp))
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newScale = (zoomScale + 0.50f).coerceAtMost(4.0f)
                            zoomScale = newScale
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = GameColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val newScale = (zoomScale - 0.35f).coerceAtLeast(1f)
                            zoomScale = newScale
                            if (newScale <= 1.05f) panOffset = Offset.Zero
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = GameColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (zoomScale > 1.05f) {
                        IconButton(
                            onClick = {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Zoom",
                                tint = GameColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a snake arrow with rounded corners at every turn and a sharp arrowhead.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurvedSnakeArrow(
    arrow: Arrow,
    cellSizePx: Float,
    cornerRadius: Float,
    strokeWidth: Float,
    strokeColor: Color,
    globalOffset: Offset = Offset.Zero
) {
    val points = arrow.points
    if (points.isEmpty()) return

    // Convert GridPoints to pixel offsets
    val rawOffsets = points.map { pt ->
        Offset(
            (pt.col + 0.5f) * cellSizePx + globalOffset.x,
            (pt.row + 0.5f) * cellSizePx + globalOffset.y
        )
    }

    // Collapse collinear points into corner vertices
    val vertices = simplifyCollinearPoints(rawOffsets)

    if (vertices.size >= 2) {
        val path = Path()
        val start = vertices.first()
        path.moveTo(start.x, start.y)

        // Draw through vertices with rounded 90-degree corner fillets
        for (i in 1 until vertices.size - 1) {
            val prev = vertices[i - 1]
            val curr = vertices[i]
            val next = vertices[i + 1]

            val dirPrev = (prev - curr).normalized()
            val dirNext = (next - curr).normalized()

            val distPrev = (prev - curr).getDistance()
            val distNext = (next - curr).getDistance()
            val fillet = cornerRadius.coerceAtMost(minOf(distPrev / 2f, distNext / 2f))

            val pStart = curr + dirPrev * fillet
            val pEnd = curr + dirNext * fillet

            path.lineTo(pStart.x, pStart.y)
            path.quadraticTo(curr.x, curr.y, pEnd.x, pEnd.y)
        }

        val end = vertices.last()
        path.lineTo(end.x, end.y)

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Draw the arrowhead at the head
    val headOffset = rawOffsets.last()
    drawArrowHead(
        center = headOffset,
        direction = arrow.headDirection,
        strokeWidth = strokeWidth,
        color = strokeColor
    )
}

/**
 * Draws filled arrowhead triangle pointing in headDirection.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    center: Offset,
    direction: Direction,
    strokeWidth: Float,
    color: Color
) {
    val dirVec = Offset(direction.dc.toFloat(), direction.dr.toFloat())
    val perpVec = Offset(-dirVec.y, dirVec.x)

    val headLength = strokeWidth * 2.4f
    val halfBaseWidth = strokeWidth * 1.5f

    // Tip extends slightly in direction but stays well within current cell
    val tip = center + dirVec * (headLength * 0.72f)
    val base = center - dirVec * (headLength * 0.35f)
    val left = base + perpVec * halfBaseWidth
    val right = base - perpVec * halfBaseWidth

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Fill
    )
}

/**
 * High-octane rocket launch animation ("jaise rocket udta shurr se waise illusion").
 * Implements:
 * 1. Pre-launch micro tension/ignition recoil (progress 0..0.12)
 * 2. Exponential supersonic acceleration forward (progress 0.12..1.0)
 * 3. Rocket exhaust flame cone at the tail (yellow core + fiery orange plasma)
 * 4. Billowing expanding smoke puffs trailing in the wake
 * 5. Motion speed blur streaks
 * 6. Takeoff sonic shockwave ripple ring at the launch origin
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRocketFlyingArrow(
    flying: FlyingArrow,
    progress: Float,
    cellSizePx: Float,
    cornerRadius: Float,
    baseStrokeWidth: Float,
    boardPixelSize: Float
) {
    val dir = flying.direction
    val dirVec = Offset(dir.dc.toFloat(), dir.dr.toFloat())
    val perpVec = Offset(-dirVec.y, dirVec.x)
    val totalFlightDist = boardPixelSize * 2.2f

    // 1. Rocket physics trajectory
    val (rocketOffset, velocityFactor) = if (progress < 0.12f) {
        val recoilP = progress / 0.12f
        val recoilDist = sin(recoilP * PI.toFloat()) * (cellSizePx * 0.15f)
        Offset(-dirVec.x * recoilDist, -dirVec.y * recoilDist) to 0.1f
    } else {
        val tNorm = (progress - 0.12f) / 0.88f
        val accel = tNorm * tNorm * tNorm * 1.15f + tNorm * 0.15f
        Offset(dirVec.x * (accel * totalFlightDist), dirVec.y * (accel * totalFlightDist)) to (tNorm * 2.5f + 0.4f)
    }

    // 2. Takeoff sonic shockwave ring (expands and fades at original head location)
    val origHeadCenter = Offset(
        (flying.arrow.head.col + 0.5f) * cellSizePx,
        (flying.arrow.head.row + 0.5f) * cellSizePx
    )
    if (progress in 0.03f..0.85f) {
        val waveProgress = (progress - 0.03f) / 0.82f
        val waveRadius = cellSizePx * 1.8f * waveProgress
        val waveAlpha = ((1f - waveProgress) * 0.85f).coerceIn(0f, 1f)
        drawCircle(
            color = Color(flying.arrow.colorHex).copy(alpha = waveAlpha),
            radius = waveRadius,
            center = origHeadCenter,
            style = Stroke(width = 2.8.dp.toPx())
        )
    }

    // 3. Motion blur speed streaks behind the rocket
    if (progress > 0.12f) {
        val streakLength = (velocityFactor * cellSizePx * 1.5f).coerceAtMost(cellSizePx * 4.5f)
        for (streakIndex in listOf(-1, 1)) {
            val lateralOffset = perpVec * (baseStrokeWidth * 1.3f * streakIndex)
            val streakAlpha = (0.35f * (1f - progress)).coerceIn(0f, 1f)
            drawCurvedSnakeArrow(
                arrow = flying.arrow,
                cellSizePx = cellSizePx,
                cornerRadius = cornerRadius,
                strokeWidth = baseStrokeWidth * 0.65f,
                strokeColor = Color.White.copy(alpha = streakAlpha),
                globalOffset = rocketOffset - dirVec * (streakLength * 0.45f) + lateralOffset
            )
        }
    }

    // 4. Tail position for rocket engine exhaust (tail is arrow.points.first())
    val tailPt = flying.arrow.points.first()
    val tailCenter = Offset(
        (tailPt.col + 0.5f) * cellSizePx,
        (tailPt.row + 0.5f) * cellSizePx
    ) + rocketOffset

    // 5. Billowing smoke puffs left behind in the rocket's wake
    if (progress > 0.08f) {
        val numPuffs = 5
        for (p in 1..numPuffs) {
            val puffDist = cellSizePx * (0.4f + p * 0.70f) * (progress * 1.4f)
            val puffCenter = tailCenter - dirVec * puffDist
            val puffRadius = cellSizePx * (0.16f + p * 0.08f)
            val puffAlpha = ((1f - p.toFloat() / (numPuffs + 1)) * 0.55f * (1f - progress)).coerceIn(0f, 1f)
            drawCircle(
                color = Color(0xFFE2E8F0).copy(alpha = puffAlpha),
                radius = puffRadius,
                center = puffCenter,
                style = Fill
            )
            drawCircle(
                color = Color(0xFF94A3B8).copy(alpha = puffAlpha * 0.4f),
                radius = puffRadius,
                center = puffCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }

    // 6. Rocket exhaust thrust flame cone
    if (progress > 0.02f) {
        val flameLength = (cellSizePx * (0.8f + velocityFactor * 1.4f)).coerceAtMost(cellSizePx * 4f)
        val flameHalfWidth = (baseStrokeWidth * 1.8f).coerceAtLeast(3.dp.toPx())

        // Outer fiery orange/red plasma cone
        val flameTip = tailCenter - dirVec * flameLength
        val flameLeft = tailCenter + perpVec * flameHalfWidth
        val flameRight = tailCenter - perpVec * flameHalfWidth
        val outerFlamePath = Path().apply {
            moveTo(tailCenter.x, tailCenter.y)
            lineTo(flameLeft.x, flameLeft.y)
            lineTo(flameTip.x, flameTip.y)
            lineTo(flameRight.x, flameRight.y)
            close()
        }
        drawPath(
            path = outerFlamePath,
            color = Color(0xFFFF5722).copy(alpha = 0.90f),
            style = Fill
        )

        // Inner white-hot / bright yellow flame core
        val innerTip = tailCenter - dirVec * (flameLength * 0.58f)
        val innerLeft = tailCenter + perpVec * (flameHalfWidth * 0.52f)
        val innerRight = tailCenter - perpVec * (flameHalfWidth * 0.52f)
        val innerFlamePath = Path().apply {
            moveTo(tailCenter.x, tailCenter.y)
            lineTo(innerLeft.x, innerLeft.y)
            lineTo(innerTip.x, innerTip.y)
            lineTo(innerRight.x, innerRight.y)
            close()
        }
        drawPath(
            path = innerFlamePath,
            color = Color(0xFFFFF59D).copy(alpha = 0.98f),
            style = Fill
        )
    }

    // 7. Render arrow body with flight glow
    val arrowAlpha = if (progress > 0.85f) ((1f - progress) / 0.15f).coerceIn(0f, 1f) else 1f
    val arrowColor = Color(flying.arrow.colorHex).copy(alpha = arrowAlpha)

    drawCurvedSnakeArrow(
        arrow = flying.arrow,
        cellSizePx = cellSizePx,
        cornerRadius = cornerRadius,
        strokeWidth = baseStrokeWidth * 1.15f,
        strokeColor = arrowColor,
        globalOffset = rocketOffset
    )
}

/**
 * Draws a cartoon white glove hand pointer tapping at center.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandPointer(
    center: Offset,
    size: Float
) {
    // Stylized clean white pointer with dark outline
    val path = Path().apply {
        val w = size
        val h = size
        moveTo(center.x - w * 0.2f, center.y - h * 0.2f)
        lineTo(center.x - w * 0.05f, center.y - h * 0.55f) // Index finger pointing up-left
        lineTo(center.x + w * 0.12f, center.y - h * 0.40f)
        lineTo(center.x + w * 0.05f, center.y - h * 0.15f)
        lineTo(center.x + w * 0.35f, center.y - h * 0.05f) // Other folded fingers
        lineTo(center.x + w * 0.30f, center.y + h * 0.25f)
        lineTo(center.x - w * 0.05f, center.y + h * 0.35f)
        lineTo(center.x - w * 0.25f, center.y + h * 0.10f)
        close()
    }

    // Hand fill
    drawPath(
        path = path,
        color = Color.White,
        style = Fill
    )
    // Hand stroke
    drawPath(
        path = path,
        color = Color(0xFF1E293B),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Reduces contiguous points along the same line into corners.
 */
private fun simplifyCollinearPoints(points: List<Offset>): List<Offset> {
    if (points.size <= 2) return points

    val result = mutableListOf<Offset>()
    result.add(points.first())

    for (i in 1 until points.size - 1) {
        val prev = points[i - 1]
        val curr = points[i]
        val next = points[i + 1]

        val d1x = curr.x - prev.x
        val d1y = curr.y - prev.y
        val d2x = next.x - curr.x
        val d2y = next.y - curr.y

        // Check if directions are strictly identical (same direction along the axis)
        // If it turns 90 degrees or reverses, this is a corner vertex and MUST be kept!
        val sign1x = sign(d1x).toInt()
        val sign1y = sign(d1y).toInt()
        val sign2x = sign(d2x).toInt()
        val sign2y = sign(d2y).toInt()

        val isCollinear = (sign1x == sign2x && sign1y == sign2y)
        if (!isCollinear) {
            result.add(curr)
        }
    }

    result.add(points.last())
    return result
}

/**
 * Computes minimum distance from tapPoint to any segment or head of arrow.
 */
private fun computeDistanceToArrow(tapPoint: Offset, arrow: Arrow, cellSizePx: Float): Float {
    val offsets = arrow.points.map { pt ->
        Offset((pt.col + 0.5f) * cellSizePx, (pt.row + 0.5f) * cellSizePx)
    }

    var minDist = Float.MAX_VALUE
    for (i in 0 until offsets.size - 1) {
        val p1 = offsets[i]
        val p2 = offsets[i + 1]
        val dist = distanceToSegment(tapPoint, p1, p2)
        if (dist < minDist) minDist = dist
    }

    // Also check distance to head
    val headDist = (tapPoint - offsets.last()).getDistance()
    if (headDist < minDist) minDist = headDist

    return minDist
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val ap = p - a
    val abLenSq = ab.getDistanceSquared()
    if (abLenSq == 0f) return ap.getDistance()

    val t = ((ap.x * ab.x + ap.y * ab.y) / abLenSq).coerceIn(0f, 1f)
    val proj = a + ab * t
    return (p - proj).getDistance()
}

private fun Offset.normalized(): Offset {
    val d = getDistance()
    return if (d > 0.0001f) Offset(x / d, y / d) else Offset.Zero
}
