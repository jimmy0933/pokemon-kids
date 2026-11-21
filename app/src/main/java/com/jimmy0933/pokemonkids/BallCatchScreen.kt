package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val BALL_CATCH_ENCOUNTER_COUNT = 109
const val BALL_CATCH_CHANCE = 0.7
const val BALL_CATCH_SHAKE_MS = 150L
const val BALL_CATCH_HIT_MS = 600L
const val BALL_CATCH_ROAM_TICK_MS = 50L
const val BALL_CATCH_FLIGHT_TICK_MS = 16L
const val BALL_CATCH_POKEMON_SPEED_DP = 120f
const val BALL_CATCH_TURN_MIN_MS = 800L
const val BALL_CATCH_TURN_MAX_MS = 1500L
const val BALL_CATCH_MIN_DRAG_DP = 20f
const val BALL_CATCH_MIN_THROW_SPEED_DP = 300f
const val BALL_CATCH_MAX_THROW_SPEED_DP = 900f
const val BALL_CATCH_BALL_SIZE_DP = 72f
const val BALL_CATCH_POKEMON_SIZE_DP = 110f
const val BALL_CATCH_GRAB_RADIUS_DP = 90f
const val BALL_CATCH_AIM_LENGTH_DP = 160f
const val BALL_CATCH_HIT_RADIUS_DP = 78f

/** Build the encounter list for one session: random uncaught Pokemon only,
 * up to [count]. Returns fewer than [count] when the uncaught pool is small,
 * and empty when everything is already caught — so a session ends early or
 * the all-caught celebration shows instead of an encounter. Pure function,
 * unit-tested. */
fun ballCatchEncounters(
    pool: List<Pokemon>,
    excludedIds: Set<Int>,
    count: Int,
    seed: Long,
): List<Pokemon> {
    val uncaught = pool.filter { it.id !in excludedIds }
    if (uncaught.isEmpty()) return emptyList()
    return uncaught.shuffled(kotlin.random.Random(seed)).take(count)
}

/** Roll the catch: [chance] (0.7) to catch on each hit. Pure function,
 * unit-tested (seeded, so deterministic in tests). */
fun rollCatch(random: kotlin.random.Random, chance: Double = BALL_CATCH_CHANCE): Boolean =
    random.nextDouble() < chance

/** Throw velocity from a drag: the direction of the drag, with speed scaled
 * by the drag length and clamped to [minSpeed, maxSpeed]. Returns
 * Offset.Zero when the drag is shorter than [minDrag] (too short to throw).
 * Pure function, unit-tested. */
fun dragThrowVelocity(
    start: Offset,
    end: Offset,
    minDrag: Float = BALL_CATCH_MIN_DRAG_DP,
    minSpeed: Float = BALL_CATCH_MIN_THROW_SPEED_DP,
    maxSpeed: Float = BALL_CATCH_MAX_THROW_SPEED_DP,
): Offset {
    val delta = end - start
    val length = delta.getDistance()
    if (length < minDrag) return Offset.Zero
    val speed = (length * 3f).coerceIn(minSpeed, maxSpeed)
    return delta / length * speed
}

/** Advance the ball one tick along its velocity. Pure function,
 * unit-tested. */
fun ballStep(position: Offset, velocity: Offset, dtSeconds: Float): Offset =
    position + velocity * dtSeconds

/** Did the ball hit the Pokemon? True when the ball centre is within
 * [hitRadius] of the Pokemon centre. Checked continuously while the ball
 * flies, because the Pokemon keeps moving. Pure function, unit-tested. */
fun ballHitsPokemon(
    ball: Offset,
    pokemon: Offset,
    hitRadius: Float = BALL_CATCH_HIT_RADIUS_DP,
): Boolean = (ball - pokemon).getDistance() <= hitRadius

/** Is the ball off the field (a miss)? True when the ball centre has left
 * the area, with a small margin so the ball visibly exits before the miss
 * is called. Pure function, unit-tested. */
fun ballOffField(
    ball: Offset,
    fieldWidth: Float,
    fieldHeight: Float,
    margin: Float = 60f,
): Boolean =
    ball.x < -margin || ball.x > fieldWidth + margin ||
        ball.y < -margin || ball.y > fieldHeight + margin

/** Aim-guide end point: from the ball, along the drag direction, [length]
 * dp ahead. Returns the ball position itself when there is no drag
 * direction yet. Pure function, unit-tested. */
fun aimGuideEnd(
    ball: Offset,
    dragStart: Offset,
    dragCurrent: Offset,
    length: Float = BALL_CATCH_AIM_LENGTH_DP,
): Offset {
    val delta = dragCurrent - dragStart
    val len = delta.getDistance()
    if (len < 1f) return ball
    return ball + delta / len * length
}

/** Where the ball rests before a throw: bottom centre of the field. Pure
 * function, unit-tested. */
fun ballHome(fieldWidth: Float, fieldHeight: Float): Offset =
    Offset(fieldWidth / 2f, fieldHeight - BALL_CATCH_BALL_SIZE_DP / 2f - 8f)

/** The wild Pokemon's roaming state: centre position (dp), unit direction,
 * and ms until the next random turn. */
data class RoamState(
    val x: Float,
    val y: Float,
    val dirX: Float,
    val dirY: Float,
    val turnInMs: Long,
)

/** Start state: the Pokemon starts near the top-centre of the field,
 * heading in a random direction. Pure function, unit-tested. */
fun initialRoamState(
    fieldWidth: Float,
    fieldHeight: Float,
    radius: Float,
    random: kotlin.random.Random,
): RoamState {
    val angle = random.nextFloat() * 2f * PI.toFloat()
    return RoamState(
        x = fieldWidth / 2f,
        y = fieldHeight * 0.35f,
        dirX = cos(angle),
        dirY = sin(angle),
        turnInMs = random.nextLong(BALL_CATCH_TURN_MIN_MS, BALL_CATCH_TURN_MAX_MS),
    )
}

/** One wander step: move along the direction, bounce off the field edges,
 * and pick a new random direction when the turn timer runs out. Speed stays
 * constant and the Pokemon stays inside the field. Pure function,
 * unit-tested. */
fun roamStep(
    state: RoamState,
    dtMs: Long,
    speedDpPerSecond: Float,
    fieldWidth: Float,
    fieldHeight: Float,
    radius: Float,
    random: kotlin.random.Random,
): RoamState {
    var dirX = state.dirX
    var dirY = state.dirY
    var turnInMs = state.turnInMs - dtMs
    if (turnInMs <= 0) {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        dirX = cos(angle)
        dirY = sin(angle)
        turnInMs = random.nextLong(BALL_CATCH_TURN_MIN_MS, BALL_CATCH_TURN_MAX_MS)
    }
    val step = speedDpPerSecond * dtMs / 1000f
    var x = state.x + dirX * step
    var y = state.y + dirY * step
    if (x < radius) {
        x = radius
        dirX = -dirX
    }
    if (x > fieldWidth - radius) {
        x = fieldWidth - radius
        dirX = -dirX
    }
    if (y < radius) {
        y = radius
        dirY = -dirY
    }
    if (y > fieldHeight - radius) {
        y = fieldHeight - radius
        dirY = -dirY
    }
    return RoamState(x, y, dirX, dirY, turnInMs)
}

private enum class BallPhase {
    Idle, Dragging, Flying, Hit, Shaking, Caught, BrokeOut, Missed,
}

/** A Poke Ball drawn with Canvas: red top, white bottom, black band and a
 * white centre button. Pure drawing, no image asset needed. */
@Composable
fun PokeBallIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        // Red top half (180° -> 360° passes through the top).
        drawArc(
            color = Color(0xFFEE1515),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
        )
        // White bottom half (0° -> 180° passes through the bottom).
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
        )
        // Black band across the middle.
        drawLine(
            color = Color.Black,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = radius * 0.22f,
        )
        // Centre button: black ring with a white middle.
        drawCircle(color = Color.Black, radius = radius * 0.32f, center = center)
        drawCircle(color = Color.White, radius = radius * 0.22f, center = center)
    }
}

/** Throw-ball catching, for older kids: the wild Pokemon runs around the
 * field freely and the kid throws a Poke Ball by dragging it with the
 * finger — press the ball, slide to aim (a dashed guide shows the angle),
 * release to throw in the drag direction. The ball is checked against the
 * moving Pokemon continuously while it flies; a hit shows a starburst
 * impact effect, then the ball shakes 3 times and a 70% catch roll decides:
 * caught Pokemon go into the Pokedex, a break-out means throwing again at
 * the same Pokemon. A miss just means throwing again — unlimited balls, the
 * Pokemon never flees, no timer, no penalty. Encounters come only from the
 * uncaught pool, so a Pokemon already in the Pokedex never appears again.
 * 109 encounters per session (fewer if the uncaught pool runs out). */
@Composable
fun BallCatchScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    // Pokemon caught in this screen (including earlier sessions here) must
    // never be encountered again, even before the DataStore flow has caught
    // up with the latest catches — so they are excluded on top of the
    // collected set. This state is NOT keyed on sessionSeed: it survives
    // "再玩一次" and keeps the next session uncaught-only too.
    var caughtIds by remember { mutableStateOf(setOf<Int>()) }
    val encounters = remember(sessionSeed) {
        ballCatchEncounters(
            ALL_POKEMON,
            collectedIds + caughtIds,
            BALL_CATCH_ENCOUNTER_COUNT,
            sessionSeed,
        )
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var caughtCount by remember(sessionSeed) { mutableStateOf(0) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    var phase by remember(sessionSeed) { mutableStateOf(BallPhase.Idle) }
    var shakeTick by remember(sessionSeed) { mutableStateOf(-1) }
    // Ball and Pokemon positions, in dp relative to the field. Keyed on the
    // session + encounter so each new Pokemon starts fresh: the ball at its
    // home at the bottom centre, the Pokemon near the top centre.
    var ballPos by remember(sessionSeed, index) { mutableStateOf<Offset?>(null) }
    var roam by remember(sessionSeed, index) { mutableStateOf<RoamState?>(null) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var throwVelocity by remember { mutableStateOf(Offset.Zero) }
    var hitPoint by remember { mutableStateOf<Offset?>(null) }
    val pokemon = encounters.getOrNull(index)
    val allCaught = encounters.isEmpty()

    // Say the instruction out loud whenever a new Pokemon appears.
    LaunchedEffect(sessionSeed, index) {
        pokemon?.let { onSpeak("出現了野生的${it.name}！按住球，往牠的方向滑！") }
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "丟球抓寶 ★進階",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (allCaught) {
            Text(
                text = "🎉 全部抓完了！你好厲害！",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
            Text(
                text = "你已經收集了全部 ${ALL_POKEMON.size} 隻寶可夢！",
                fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("⬅ 回主頁", fontSize = 24.sp) }
            return@Column
        }
        Text(
            text = "第 ${index + 1} / ${encounters.size} 隻　抓到了 $caughtCount 隻",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = when (phase) {
                BallPhase.Idle -> "出現了野生的${pokemon?.name}！按住球，往牠的方向滑！"
                BallPhase.Dragging -> "瞄準中……放開就把球丟出去！"
                BallPhase.Flying -> "球飛過去了……"
                BallPhase.Hit -> "打中了！💥"
                BallPhase.Shaking -> "搖啊搖……會抓到嗎？"
                BallPhase.Caught -> "抓到了！是${pokemon?.name}！🎉"
                BallPhase.BrokeOut -> "啊！${pokemon?.name}跑出來了！再丟一次！"
                BallPhase.Missed -> "沒丟中！再試一次！"
            },
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (pokemon != null) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                val areaWidth = maxWidth.value
                val areaHeight = maxHeight.value
                val density = LocalDensity.current.density
                val ballRadius = BALL_CATCH_BALL_SIZE_DP / 2f
                val pokemonRadius = BALL_CATCH_POKEMON_SIZE_DP / 2f

                // Place the ball at its home and the Pokemon near the top
                // centre whenever a new encounter starts (or the field
                // size changes).
                LaunchedEffect(sessionSeed, index, areaWidth, areaHeight) {
                    ballPos = ballHome(areaWidth, areaHeight)
                    roam = initialRoamState(
                        areaWidth,
                        areaHeight,
                        pokemonRadius,
                        kotlin.random.Random(System.nanoTime()),
                    )
                }

                // The wild Pokemon runs around the field freely: it wanders
                // in a random direction, turns at random intervals and
                // bounces off the edges. It keeps moving while the kid aims
                // and while the ball flies — that is the challenge. It
                // stops only while it is hit or inside the ball.
                LaunchedEffect(sessionSeed, index) {
                    // pokemon is non-null here (this whole block is inside
                    // `if (pokemon != null)`), so the loop just runs until
                    // the effect restarts for the next encounter.
                    while (true) {
                        delay(BALL_CATCH_ROAM_TICK_MS)
                        val current = roam
                        if (current != null && (
                                phase == BallPhase.Idle || phase == BallPhase.Dragging ||
                                    phase == BallPhase.Flying || phase == BallPhase.Missed ||
                                    phase == BallPhase.BrokeOut
                                )
                        ) {
                            roam = roamStep(
                                current,
                                BALL_CATCH_ROAM_TICK_MS,
                                BALL_CATCH_POKEMON_SPEED_DP,
                                areaWidth,
                                areaHeight,
                                pokemonRadius,
                                kotlin.random.Random(System.nanoTime()),
                            )
                        }
                    }
                }

                // Drive the throw / hit / shake / catch flow. Each phase
                // runs its own delays and then moves to the next phase;
                // changing phase restarts this effect and cancels the
                // previous phase's remaining delays.
                LaunchedEffect(phase) {
                    when (phase) {
                        BallPhase.Flying -> {
                            // Move the ball step by step until it hits the
                            // moving Pokemon or leaves the field.
                            while (true) {
                                delay(BALL_CATCH_FLIGHT_TICK_MS)
                                val pos = ballPos
                                val centre = roam?.let { Offset(it.x, it.y) }
                                if (pos == null || centre == null) {
                                    phase = BallPhase.Idle
                                    break
                                }
                                val next = ballStep(
                                    pos,
                                    throwVelocity,
                                    BALL_CATCH_FLIGHT_TICK_MS / 1000f,
                                )
                                ballPos = next
                                if (ballHitsPokemon(next, centre)) {
                                    // Hit! Snap the ball onto the Pokemon,
                                    // show the impact effect, then shake.
                                    hitPoint = next
                                    ballPos = centre
                                    sounds.match()
                                    onSpeak("打中了！")
                                    phase = BallPhase.Hit
                                    break
                                }
                                if (ballOffField(next, areaWidth, areaHeight)) {
                                    sounds.flip()
                                    onSpeak("沒丟中！再試一次")
                                    phase = BallPhase.Missed
                                    break
                                }
                            }
                        }
                        BallPhase.Hit -> {
                            delay(BALL_CATCH_HIT_MS)
                            hitPoint = null
                            phase = BallPhase.Shaking
                        }
                        BallPhase.Shaking -> {
                            // 3 shakes: 6 toggles of the ball's sideways offset.
                            repeat(6) { tick ->
                                shakeTick = tick
                                delay(BALL_CATCH_SHAKE_MS)
                            }
                            shakeTick = -1
                            val caught = pokemon
                            if (rollCatch(kotlin.random.Random(System.nanoTime()))) {
                                sounds.match()
                                sounds.celebrate()
                                onSpeak("抓到了！${caught.name}！")
                                onCollect(caught.id)
                                caughtIds = caughtIds + caught.id
                                if (caught.id !in collectedIds) newCollected++
                                caughtCount++
                                phase = BallPhase.Caught
                            } else {
                                sounds.flip()
                                onSpeak("跑出來了！再丟一次")
                                phase = BallPhase.BrokeOut
                            }
                        }
                        BallPhase.Caught -> {
                            delay(1800)
                            if (index + 1 < encounters.size) {
                                index++
                                phase = BallPhase.Idle
                            } else {
                                showComplete = true
                            }
                        }
                        BallPhase.Missed -> {
                            delay(1200)
                            ballPos = ballHome(areaWidth, areaHeight)
                            phase = BallPhase.Idle
                        }
                        BallPhase.BrokeOut -> {
                            delay(1500)
                            ballPos = ballHome(areaWidth, areaHeight)
                            phase = BallPhase.Idle
                        }
                        BallPhase.Idle, BallPhase.Dragging -> {
                        }
                    }
                }

                // The ball shows while it is waiting, being dragged, flying,
                // hitting, shaking, or holding a caught Pokemon; it is
                // hidden after a break-out (the ball popped open).
                val showBall = phase != BallPhase.BrokeOut
                // The Pokemon hides while it is inside the ball.
                val pokemonVisible =
                    phase != BallPhase.Shaking && phase != BallPhase.Caught
                val ball = ballPos
                val shakeX = if (phase == BallPhase.Shaking && shakeTick >= 0) {
                    if (shakeTick % 2 == 0) 12f else -12f
                } else {
                    0f
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            Color(0xFFFFF8E1),
                            RoundedCornerShape(20.dp),
                        )
                        .pointerInput(areaWidth, areaHeight) {
                            detectDragGestures(
                                onDragStart = { offsetPx ->
                                    if (phase != BallPhase.Idle) return@detectDragGestures
                                    val start = Offset(
                                        offsetPx.x / density,
                                        offsetPx.y / density,
                                    )
                                    // Read the state directly (not the
                                    // composition-time snapshot): the gesture
                                    // block only restarts when its keys
                                    // change, so a captured value would go
                                    // stale.
                                    val home = ballPos
                                    if (home == null) return@detectDragGestures
                                    // Only a press on (or near) the ball
                                    // grabs it; drags starting elsewhere
                                    // are ignored.
                                    if ((start - home).getDistance() > BALL_CATCH_GRAB_RADIUS_DP) {
                                        return@detectDragGestures
                                    }
                                    dragStart = start
                                    dragCurrent = start
                                    phase = BallPhase.Dragging
                                },
                                onDrag = { change, _ ->
                                    if (phase != BallPhase.Dragging) return@detectDragGestures
                                    val pos = Offset(
                                        change.position.x / density,
                                        change.position.y / density,
                                    )
                                    dragCurrent = pos
                                    // The ball follows the finger, clamped
                                    // inside the field.
                                    ballPos = Offset(
                                        pos.x.coerceIn(ballRadius, areaWidth - ballRadius),
                                        pos.y.coerceIn(ballRadius, areaHeight - ballRadius),
                                    )
                                },
                                onDragEnd = {
                                    if (phase != BallPhase.Dragging) return@detectDragGestures
                                    val start = dragStart
                                    val end = dragCurrent
                                    dragStart = null
                                    dragCurrent = null
                                    if (start == null || end == null) {
                                        phase = BallPhase.Idle
                                        return@detectDragGestures
                                    }
                                    val velocity = dragThrowVelocity(start, end)
                                    if (velocity == Offset.Zero) {
                                        // Too short a drag: no throw, the
                                        // ball springs back home.
                                        ballPos = ballHome(areaWidth, areaHeight)
                                        phase = BallPhase.Idle
                                    } else {
                                        throwVelocity = velocity
                                        phase = BallPhase.Flying
                                    }
                                },
                                onDragCancel = {
                                    dragStart = null
                                    dragCurrent = null
                                    if (phase == BallPhase.Dragging) {
                                        ballPos = ballHome(areaWidth, areaHeight)
                                        phase = BallPhase.Idle
                                    }
                                },
                            )
                        },
                ) {
                    // Aim guide while dragging: a dashed blue line from the
                    // ball in the current drag direction, so the kid can see
                    // the throw angle. (Canvas draws in pixels, so dp
                    // positions are scaled by the density.)
                    if (phase == BallPhase.Dragging && dragStart != null &&
                        dragCurrent != null && ball != null
                    ) {
                        val guideEnd = aimGuideEnd(ball, dragStart!!, dragCurrent!!)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawLine(
                                color = Color(0xFF1976D2),
                                start = Offset(ball.x * density, ball.y * density),
                                end = Offset(guideEnd.x * density, guideEnd.y * density),
                                strokeWidth = 6f * density,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(18f * density, 14f * density),
                                ),
                            )
                        }
                    }
                    // The roaming Pokemon.
                    val roaming = roam
                    if (pokemonVisible && roaming != null) {
                        Image(
                            painter = painterResource(drawableId(context, pokemon.drawable)),
                            contentDescription = pokemon.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(BALL_CATCH_POKEMON_SIZE_DP.dp)
                                .offset(
                                    x = (roaming.x - pokemonRadius).dp,
                                    y = (roaming.y - pokemonRadius).dp,
                                ),
                        )
                    }
                    // The ball.
                    if (showBall && ball != null) {
                        PokeBallIcon(
                            modifier = Modifier.size(BALL_CATCH_BALL_SIZE_DP.dp)
                                .offset(
                                    x = (ball.x + shakeX - ballRadius).dp,
                                    y = (ball.y - ballRadius).dp,
                                ),
                        )
                    }
                    // Hit effect: a yellow starburst plus a big 💥 at the
                    // impact point, shown briefly before the ball shakes.
                    val impact = hitPoint
                    if (phase == BallPhase.Hit && impact != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centre = Offset(impact.x * density, impact.y * density)
                            for (i in 0 until 8) {
                                val angle = i * PI / 4.0
                                val dir = Offset(
                                    cos(angle).toFloat(),
                                    sin(angle).toFloat(),
                                )
                                drawLine(
                                    color = Color(0xFFFFD600),
                                    start = centre + dir * 18f * density,
                                    end = centre + dir * 44f * density,
                                    strokeWidth = 8f * density,
                                    cap = StrokeCap.Round,
                                )
                            }
                            drawCircle(
                                color = Color(0xFFFFEA00),
                                radius = 26f * density,
                                center = centre,
                            )
                            drawCircle(
                                color = Color(0xFFFF6D00),
                                radius = 14f * density,
                                center = centre,
                            )
                        }
                        Text(
                            text = "💥",
                            fontSize = 64.sp,
                            modifier = Modifier.offset(
                                x = (impact.x - 32f).dp,
                                y = (impact.y - 32f).dp,
                            ),
                        )
                    }
                }
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 抓完 ${encounters.size} 隻了！", fontSize = 26.sp) },
            text = { Text("新收集到 $newCollected 隻寶可夢，好棒！", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    sessionSeed = System.currentTimeMillis()
                    index = 0
                    showComplete = false
                }) { Text("再玩一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
