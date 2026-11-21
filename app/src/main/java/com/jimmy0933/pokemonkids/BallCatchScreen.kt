package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

const val BALL_CATCH_ENCOUNTER_COUNT = 109
const val BALL_CATCH_CHANCE = 0.7
const val BALL_CATCH_SHAKE_MS = 150L
const val BALL_CATCH_HIT_MS = 600L
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
// v2.12: smooth motion + polish.
const val BALL_CATCH_STEER_SECONDS = 0.3f
const val BALL_CATCH_BOB_AMPLITUDE_DP = 4f
const val BALL_CATCH_BOB_PERIOD_S = 1.6f
const val BALL_CATCH_MAX_TILT_DEGREES = 14f
const val BALL_CATCH_MAX_FRAME_DT_S = 0.05f
const val BALL_CATCH_CLOUD_SPEED_DP = 8f
const val BALL_CATCH_CLOUD_WRAP_DP = 360f
const val BALL_CATCH_GRASS_HEIGHT_DP = 46f
const val BALL_CATCH_HIT_PARTICLE_COUNT = 14
const val BALL_CATCH_PUFF_PARTICLE_COUNT = 10
const val BALL_CATCH_CONFETTI_COUNT = 24
const val BALL_CATCH_ENTRANCE_MS = 400f
const val BALL_CATCH_FLIGHT_ARC_MS = 500f
const val BALL_CATCH_TRAIL_MAX_AGE_MS = 180f
const val BALL_CATCH_AIM_DOT_COUNT = 7
const val BALL_CATCH_AIM_ARC_DP = 24f

private val CONFETTI_COLORS = listOf(
    Color(0xFFEF5350),
    Color(0xFFFFD600),
    Color(0xFF66BB6A),
    Color(0xFF42A5F5),
    Color(0xFFEC407A),
)

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

/** Can this press grab the ball? True when the press is within
 * [grabRadius] of the ball's centre. When [ball] is null (not placed yet),
 * the ball's home position is used instead, so the very first touch can
 * never be dropped just because the placement effect has not run yet.
 * Pure function, unit-tested (regression test for the v2.11 first-tap bug). */
fun canGrabBall(
    press: Offset,
    ball: Offset?,
    grabRadius: Float = BALL_CATCH_GRAB_RADIUS_DP,
    fieldWidth: Float,
    fieldHeight: Float,
): Boolean {
    val centre = ball ?: ballHome(fieldWidth, fieldHeight)
    return (press - centre).getDistance() <= grabRadius
}

/** Ease-out cubic: fast start, gentle landing. Pure function, unit-tested. */
fun easeOutCubic(t: Float): Float {
    val u = 1f - t.coerceIn(0f, 1f)
    return 1f - u * u * u
}

/** Ease-out back: overshoots slightly past 1, so entrances pop. Pure
 * function, unit-tested. */
fun easeOutBack(t: Float): Float {
    val u = t.coerceIn(0f, 1f)
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (u - 1f) * (u - 1f) * (u - 1f) + c1 * (u - 1f) * (u - 1f)
}

/** Steer the current heading toward the target heading: the heading turns
 * along the shortest arc, covering a fraction [dtSeconds] / [steerSeconds]
 * of the remaining angle, so direction changes glide instead of snapping.
 * Always returns a unit vector. Pure function, unit-tested. */
fun steerHeading(
    current: Offset,
    target: Offset,
    dtSeconds: Float,
    steerSeconds: Float = BALL_CATCH_STEER_SECONDS,
): Offset {
    val t = (dtSeconds / steerSeconds).coerceIn(0f, 1f)
    if (t <= 0f) return current
    if (t >= 1f) return target
    val pi = PI.toFloat()
    val currentAngle = atan2(current.y, current.x)
    val targetAngle = atan2(target.y, target.x)
    var delta = targetAngle - currentAngle
    while (delta > pi) delta -= 2f * pi
    while (delta < -pi) delta += 2f * pi
    val angle = currentAngle + delta * t
    return Offset(cos(angle), sin(angle))
}

/** Gentle vertical bob for the roaming Pokemon: a sine wave, so it bobs up
 * and down while it runs. Pure function, unit-tested. */
fun bobOffset(
    timeSeconds: Float,
    amplitudeDp: Float = BALL_CATCH_BOB_AMPLITUDE_DP,
    periodSeconds: Float = BALL_CATCH_BOB_PERIOD_S,
): Float = sin(timeSeconds * 2f * PI.toFloat() / periodSeconds) * amplitudeDp

/** Tilt of the Pokemon while it runs: it leans into its horizontal
 * movement, up to [BALL_CATCH_MAX_TILT_DEGREES]. Pure function, unit-tested. */
fun pokemonTilt(dirX: Float): Float = dirX * BALL_CATCH_MAX_TILT_DEGREES

/** How far the ball spins (in degrees) for rolling [distanceDp] with the
 * given radius: one full roll (2 * PI * radius) is 360 degrees. Pure
 * function, unit-tested. */
fun spinDelta(distanceDp: Float, radiusDp: Float): Float =
    distanceDp / radiusDp * (180f / PI.toFloat())

/** Fake-height arc of the ball mid-flight: 0 at throw and at landing, 1 at
 * the top of the lob. Pure function, unit-tested. */
fun arcLift(progress: Float): Float =
    sin(progress.coerceIn(0f, 1f) * PI.toFloat())

/** Ball scale along the flight arc: grows slightly mid-flight, as if it
 * rises toward the viewer. Pure function, unit-tested. */
fun ballArcScale(progress: Float): Float = 1f + 0.18f * arcLift(progress)

/** Ground-shadow scale along the flight arc: shrinks mid-flight, as the
 * ball rises away from the ground. Pure function, unit-tested. */
fun ballShadowScale(progress: Float): Float = 1f - 0.45f * arcLift(progress)

/** Ground-shadow alpha along the flight arc: fades mid-flight. Pure
 * function, unit-tested. */
fun ballShadowAlpha(progress: Float): Float = 0.22f * (1f - 0.6f * arcLift(progress))

/** How far the drawn ball lifts off its ground position mid-flight (dp).
 * Pure function, unit-tested. */
fun ballLiftDp(progress: Float): Float = 14f * arcLift(progress)

/** One dot of the dotted arc aim preview: position, alpha and radius. */
data class AimDot(val pos: Offset, val alpha: Float, val radius: Float)

/** Dotted arc aim preview while dragging: dots along the drag direction,
 * arcing slightly upward like a lob and fading (alpha and size) along
 * their length, so the kid can see the throw angle and distance. Empty
 * when there is no drag direction yet. Pure function, unit-tested. */
fun aimPreviewDots(
    ball: Offset,
    dragStart: Offset,
    dragCurrent: Offset,
    count: Int = BALL_CATCH_AIM_DOT_COUNT,
    length: Float = BALL_CATCH_AIM_LENGTH_DP,
): List<AimDot> {
    val delta = dragCurrent - dragStart
    val len = delta.getDistance()
    if (len < 1f) return emptyList()
    val dir = delta / len
    return (1..count).map { i ->
        val t = i.toFloat() / count
        val pos = ball + dir * (length * t) +
            Offset(0f, -BALL_CATCH_AIM_ARC_DP * arcLift(t))
        AimDot(pos, alpha = 0.9f * (1f - t * 0.75f), radius = 7f - 3f * t)
    }
}

/** One sparkle of the hit burst: its angle, travel distance, size and
 * start delay (seconds, staggered so the burst unfolds). */
data class BurstParticle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val delay: Float,
)

/** A burst of [count] sparkles radiating in all directions. Pure function,
 * unit-tested. */
fun particleBurst(
    count: Int,
    seed: Long,
    maxDistance: Float = 70f,
): List<BurstParticle> {
    val random = kotlin.random.Random(seed)
    return (0 until count).map { i ->
        val baseAngle = 2f * PI.toFloat() * i / count
        BurstParticle(
            angle = baseAngle + (random.nextFloat() - 0.5f) * 0.5f,
            distance = maxDistance * (0.5f + 0.5f * random.nextFloat()),
            size = 5f + 5f * random.nextFloat(),
            delay = random.nextFloat() * 0.06f,
        )
    }
}

/** A particle's own progress (0..1), accounting for its start delay. Pure
 * function, unit-tested. */
fun particleProgress(progress: Float, delay: Float): Float =
    ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)

/** Where a burst particle is at overall [progress] (0..1): it eases out
 * from the centre along its angle. Pure function, unit-tested. */
fun particlePosition(
    particle: BurstParticle,
    centre: Offset,
    progress: Float,
): Offset {
    val p = easeOutCubic(particleProgress(progress, particle.delay))
    return centre +
        Offset(cos(particle.angle), sin(particle.angle)) * (particle.distance * p)
}

/** A burst particle's alpha at overall [progress]: fully visible, then
 * fading out. Pure function, unit-tested. */
fun particleAlpha(particle: BurstParticle, progress: Float): Float =
    1f - particleProgress(progress, particle.delay)

/** Radius of the expanding hit ring at [progress] (0..1). Pure function,
 * unit-tested. */
fun ringRadius(progress: Float, maxRadius: Float): Float =
    maxRadius * easeOutCubic(progress)

/** Alpha of the expanding hit ring: fades as it grows. Pure function,
 * unit-tested. */
fun ringAlpha(progress: Float): Float = 0.8f * (1f - progress.coerceIn(0f, 1f))

/** Alpha of the brief soft flash on a hit: bright at first, gone by 30%
 * of the hit. Pure function, unit-tested. */
fun flashAlpha(progress: Float): Float =
    if (progress < 0.3f) 0.3f * (1f - progress / 0.3f) else 0f

/** One piece of catch confetti: its angle, travel distance, size and
 * colour index into CONFETTI_COLORS. */
data class ConfettiPiece(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val colorIndex: Int,
)

/** A burst of [count] confetti pieces. Pure function, unit-tested. */
fun confettiBurst(count: Int, seed: Long): List<ConfettiPiece> {
    val random = kotlin.random.Random(seed)
    return (0 until count).map { i ->
        ConfettiPiece(
            angle = 2f * PI.toFloat() * i / count + (random.nextFloat() - 0.5f) * 0.6f,
            distance = 60f + 70f * random.nextFloat(),
            size = 6f + 6f * random.nextFloat(),
            colorIndex = random.nextInt(CONFETTI_COLORS.size),
        )
    }
}

/** Where a confetti piece is at [progress] (0..1): it bursts outward with
 * an ease-out, then falls down. Pure function, unit-tested. */
fun confettiPosition(
    piece: ConfettiPiece,
    centre: Offset,
    progress: Float,
): Offset {
    val p = easeOutCubic(progress.coerceIn(0f, 1f))
    return centre +
        Offset(cos(piece.angle), sin(piece.angle)) * (piece.distance * p) +
        Offset(0f, 160f * progress * progress)
}

/** Scale of a newly appearing Pokemon, [elapsedMs] after the encounter
 * starts: pops in with an ease-out-back overshoot. Pure function,
 * unit-tested. */
fun entranceScale(
    elapsedMs: Float,
    durationMs: Float = BALL_CATCH_ENTRANCE_MS,
): Float = easeOutBack((elapsedMs / durationMs).coerceIn(0f, 1f))

/** Alpha of a newly appearing Pokemon: fades in over the first 200ms.
 * Pure function, unit-tested. */
fun entranceAlpha(elapsedMs: Float): Float = (elapsedMs / 200f).coerceIn(0f, 1f)

/** Alpha of the "！" bubble above a newly appearing Pokemon: visible at
 * first, gone after 600ms. Pure function, unit-tested. */
fun bubbleAlpha(elapsedMs: Float): Float = 1f - (elapsedMs / 600f).coerceIn(0f, 1f)

/** Smooth the displayed progress toward the real [target]: an exponential
 * approach, so the bar glides instead of jumping. Never overshoots.
 * Pure function, unit-tested. */
fun smoothProgress(
    current: Float,
    target: Float,
    dtSeconds: Float,
    rate: Float = 6f,
): Float {
    if (dtSeconds <= 0f) return current
    val k = 1f - exp(-rate * dtSeconds)
    return current + (target - current) * k
}

/** The wild Pokemon's roaming state: centre position (dp), unit direction,
 * the heading it is steering toward, and ms until the next random turn. */
data class RoamState(
    val x: Float,
    val y: Float,
    val dirX: Float,
    val dirY: Float,
    val turnInMs: Long,
    val targetDirX: Float = dirX,
    val targetDirY: Float = dirY,
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
        targetDirX = cos(angle),
        targetDirY = sin(angle),
    )
}

/** One wander step: steer smoothly toward the target heading, move along
 * it, bounce off the field edges, and pick a new random target heading
 * when the turn timer runs out. Speed stays constant and the Pokemon stays
 * inside the field. Pure function, unit-tested. */
fun roamStep(
    state: RoamState,
    dtMs: Long,
    speedDpPerSecond: Float,
    fieldWidth: Float,
    fieldHeight: Float,
    radius: Float,
    random: kotlin.random.Random,
): RoamState {
    var targetDirX = state.targetDirX
    var targetDirY = state.targetDirY
    var turnInMs = state.turnInMs - dtMs
    if (turnInMs <= 0) {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        targetDirX = cos(angle)
        targetDirY = sin(angle)
        turnInMs = random.nextLong(BALL_CATCH_TURN_MIN_MS, BALL_CATCH_TURN_MAX_MS)
    }
    // Steer toward the target heading instead of snapping to it.
    val steered = steerHeading(
        Offset(state.dirX, state.dirY),
        Offset(targetDirX, targetDirY),
        dtMs / 1000f,
    )
    var dirX = steered.x
    var dirY = steered.y
    val step = speedDpPerSecond * dtMs / 1000f
    var x = state.x + dirX * step
    var y = state.y + dirY * step
    if (x < radius) {
        x = radius
        dirX = -dirX
        targetDirX = -targetDirX
    }
    if (x > fieldWidth - radius) {
        x = fieldWidth - radius
        dirX = -dirX
        targetDirX = -targetDirX
    }
    if (y < radius) {
        y = radius
        dirY = -dirY
        targetDirY = -targetDirY
    }
    if (y > fieldHeight - radius) {
        y = fieldHeight - radius
        dirY = -dirY
        targetDirY = -targetDirY
    }
    return RoamState(x, y, dirX, dirY, turnInMs, targetDirX, targetDirY)
}

/** One dot of the ball's motion trail: position and age in ms. */
private data class TrailDot(val pos: Offset, val ageMs: Float)

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
 * finger — press the ball, slide to aim (a dotted arc shows the angle),
 * release to throw in the drag direction. The ball is checked against the
 * moving Pokemon continuously while it flies; a hit shows a particle
 * burst, an expanding ring and a soft flash, then the ball shakes 3 times
 * and a 70% catch roll decides: caught Pokemon go into the Pokedex, a
 * break-out means throwing again at the same Pokemon. A miss just means
 * throwing again — unlimited balls, the Pokemon never flees, no timer, no
 * penalty. Encounters come only from the uncaught pool, so a Pokemon
 * already in the Pokedex never appears again. 109 encounters per session
 * (fewer if the uncaught pool runs out). */
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
    // v2.12: the field size comes from onSizeChanged (see the fix note
    // below), so it is known as soon as the field is laid out, and the
    // gesture detector never restarts when the size settles.
    var fieldSizeDp by remember { mutableStateOf(Offset.Zero) }
    // v2.12 polish state, all driven by the frame loop.
    var clockSeconds by remember { mutableStateOf(0f) }
    var cloudDrift by remember { mutableStateOf(0f) }
    var encounterElapsedMs by remember(sessionSeed, index) { mutableStateOf(0f) }
    var phaseElapsedMs by remember { mutableStateOf(0f) }
    var flightTrail by remember { mutableStateOf(emptyList<TrailDot>()) }
    var ballRotation by remember(sessionSeed, index) { mutableStateOf(0f) }
    var hitParticles by remember { mutableStateOf(emptyList<BurstParticle>()) }
    var catchParticles by remember { mutableStateOf(emptyList<ConfettiPiece>()) }
    var puffParticles by remember { mutableStateOf(emptyList<BurstParticle>()) }
    var effectCentre by remember { mutableStateOf<Offset?>(null) }
    var displayedProgress by remember(sessionSeed) { mutableStateOf(0f) }
    val pokemon = encounters.getOrNull(index)
    val allCaught = encounters.isEmpty()

    // The how-to-play hint stays as on-screen text only — the game never
    // speaks the instructions out loud (only outcomes like "打中了！" /
    // "抓到了！" are spoken).
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "抓寶可夢 ★進階",
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
            modifier = Modifier.fillMaxWidth()
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
        // Smooth animated progress bar.
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                .height(12.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(6.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(displayedProgress.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .background(Color(0xFF66BB6A), RoundedCornerShape(6.dp)),
            ) {}
        }
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (pokemon != null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                val density = LocalDensity.current.density
                val ballRadius = BALL_CATCH_BALL_SIZE_DP / 2f
                val pokemonRadius = BALL_CATCH_POKEMON_SIZE_DP / 2f
                // The ball is drawable and grabbable from the very first
                // frame: when it has not been placed yet, its home
                // position is used instead of nothing.
                val effectiveBall = ballPos ?: if (fieldSizeDp.x > 0f && fieldSizeDp.y > 0f) {
                    ballHome(fieldSizeDp.x, fieldSizeDp.y)
                } else {
                    null
                }

                // Place the ball at its home and the Pokemon near the top
                // centre whenever a new encounter starts (or the field
                // size arrives). The null guards keep a later size change
                // from yanking the ball or the Pokemon mid-encounter.
                LaunchedEffect(sessionSeed, index, fieldSizeDp) {
                    if (fieldSizeDp.x > 0f && fieldSizeDp.y > 0f) {
                        if (ballPos == null) {
                            ballPos = ballHome(fieldSizeDp.x, fieldSizeDp.y)
                        }
                        if (roam == null) {
                            roam = initialRoamState(
                                fieldSizeDp.x,
                                fieldSizeDp.y,
                                pokemonRadius,
                                kotlin.random.Random(System.nanoTime()),
                            )
                        }
                    }
                }

                // One frame loop drives everything that moves: the roaming
                // Pokemon (smooth steering, delta-time steps), the flying
                // ball (hit / miss checks every frame), the trail, the
                // spin, the clouds, the bob clock and the effect timers.
                // Delta time keeps motion consistent at any frame rate.
                LaunchedEffect(sessionSeed, index) {
                    var lastNanos = -1L
                    var lastPhase: BallPhase? = null
                    while (true) {
                        withFrameNanos { nanos ->
                            if (lastNanos < 0L) {
                                lastNanos = nanos
                                return@withFrameNanos
                            }
                            val dt = ((nanos - lastNanos) / 1_000_000_000f)
                                .coerceIn(0.001f, BALL_CATCH_MAX_FRAME_DT_S)
                            lastNanos = nanos
                            val dtMs = dt * 1000f
                            // Phase timer: resets on every phase change.
                            if (phase != lastPhase) {
                                phaseElapsedMs = 0f
                                lastPhase = phase
                            } else {
                                phaseElapsedMs += dtMs
                            }
                            // Ambient clock: Pokemon bob + drifting clouds.
                            clockSeconds += dt
                            if (fieldSizeDp.x > 0f) {
                                cloudDrift = (cloudDrift + dt * BALL_CATCH_CLOUD_SPEED_DP) %
                                    (fieldSizeDp.x + BALL_CATCH_CLOUD_WRAP_DP)
                            }
                            if (encounterElapsedMs < 2000f) encounterElapsedMs += dtMs
                            // Progress bar eases toward the real progress.
                            val targetProgress = if (encounters.isEmpty()) {
                                1f
                            } else {
                                (index + 1f) / encounters.size
                            }
                            displayedProgress = smoothProgress(
                                displayedProgress, targetProgress, dt, 6f,
                            )
                            if (fieldSizeDp.x <= 0f || fieldSizeDp.y <= 0f) {
                                return@withFrameNanos
                            }
                            // The wild Pokemon runs around the field
                            // freely: it wanders, steers smoothly toward
                            // new headings and bounces off the edges. It
                            // keeps moving while the kid aims and while
                            // the ball flies — that is the challenge. It
                            // stops only while it is hit or inside the ball.
                            val current = roam
                            if (current != null && (
                                    phase == BallPhase.Idle || phase == BallPhase.Dragging ||
                                        phase == BallPhase.Flying || phase == BallPhase.Missed ||
                                        phase == BallPhase.BrokeOut
                                    )
                            ) {
                                roam = roamStep(
                                    current,
                                    dtMs.toLong(),
                                    BALL_CATCH_POKEMON_SPEED_DP,
                                    fieldSizeDp.x,
                                    fieldSizeDp.y,
                                    pokemonRadius,
                                    kotlin.random.Random(System.nanoTime()),
                                )
                            }
                            // Ball flight: step along the velocity, check
                            // the moving Pokemon every frame.
                            if (phase == BallPhase.Flying) {
                                val pos = ballPos
                                val centre = roam?.let { Offset(it.x, it.y) }
                                if (pos == null || centre == null) {
                                    phase = BallPhase.Idle
                                    return@withFrameNanos
                                }
                                val next = ballStep(pos, throwVelocity, dt)
                                ballPos = next
                                ballRotation += spinDelta(
                                    (next - pos).getDistance(), ballRadius,
                                )
                                flightTrail = (listOf(TrailDot(next, 0f)) +
                                    flightTrail.map { it.copy(ageMs = it.ageMs + dtMs) })
                                    .filter { it.ageMs <= BALL_CATCH_TRAIL_MAX_AGE_MS }
                                    .take(10)
                                if (ballHitsPokemon(next, centre)) {
                                    // Hit! Snap the ball onto the Pokemon,
                                    // burst the impact effect, then shake.
                                    hitPoint = next
                                    effectCentre = centre
                                    hitParticles = particleBurst(
                                        BALL_CATCH_HIT_PARTICLE_COUNT,
                                        System.nanoTime(),
                                    )
                                    ballPos = centre
                                    sounds.match()
                                    onSpeak("打中了！")
                                    phase = BallPhase.Hit
                                    return@withFrameNanos
                                }
                                if (ballOffField(next, fieldSizeDp.x, fieldSizeDp.y)) {
                                    sounds.flip()
                                    onSpeak("沒丟中！再試一次")
                                    phase = BallPhase.Missed
                                    return@withFrameNanos
                                }
                            } else {
                                if (flightTrail.isNotEmpty()) flightTrail = emptyList()
                                if (ballRotation != 0f) ballRotation = 0f
                            }
                        }
                    }
                }

                // Drive the hit / shake / catch flow. Each phase runs its
                // own delays and then moves to the next phase; changing
                // phase restarts this effect and cancels the previous
                // phase's remaining delays.
                LaunchedEffect(phase) {
                    when (phase) {
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
                                catchParticles = confettiBurst(
                                    BALL_CATCH_CONFETTI_COUNT,
                                    System.nanoTime(),
                                )
                                phase = BallPhase.Caught
                            } else {
                                sounds.flip()
                                onSpeak("跑出來了！再丟一次")
                                puffParticles = particleBurst(
                                    BALL_CATCH_PUFF_PARTICLE_COUNT,
                                    System.nanoTime(),
                                    34f,
                                )
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
                            ballPos = if (fieldSizeDp.x > 0f) {
                                ballHome(fieldSizeDp.x, fieldSizeDp.y)
                            } else {
                                null
                            }
                            phase = BallPhase.Idle
                        }
                        BallPhase.BrokeOut -> {
                            delay(1500)
                            ballPos = if (fieldSizeDp.x > 0f) {
                                ballHome(fieldSizeDp.x, fieldSizeDp.y)
                            } else {
                                null
                            }
                            phase = BallPhase.Idle
                        }
                        BallPhase.Idle, BallPhase.Dragging, BallPhase.Flying -> {
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
                val ball = effectiveBall
                val shakeX = if (phase == BallPhase.Shaking && shakeTick >= 0) {
                    if (shakeTick % 2 == 0) 12f else -12f
                } else {
                    0f
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF81D4FA),
                                    Color(0xFFB3E5FC),
                                    Color(0xFFDCEDC8),
                                ),
                            ),
                            RoundedCornerShape(20.dp),
                        )
                        .onSizeChanged { px ->
                            fieldSizeDp = Offset(px.width / density, px.height / density)
                        }
                        .pointerInput(Unit) {
                            // v2.11 first-tap bug, fixed here: the old code
                            // keyed pointerInput on (areaWidth, areaHeight),
                            // so the gesture detector restarted — and
                            // dropped the in-progress gesture — whenever
                            // the field size settled after the first layout
                            // (weight + navigation-bar insets arriving),
                            // which is exactly when a kid grabs the ball
                            // for the first time. The key is now stable
                            // (Unit); the size arrives via onSizeChanged
                            // state instead, which the block reads live.
                            // The grab also happens on the press itself
                            // (detectDragGestures only fires after the
                            // touch slop, so a first press showed no
                            // feedback at all): the ball is grabbed the
                            // moment the finger lands on it.
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val start = Offset(
                                    down.position.x / density,
                                    down.position.y / density,
                                )
                                if (phase != BallPhase.Idle) return@awaitEachGesture
                                if (fieldSizeDp.x <= 0f || fieldSizeDp.y <= 0f) {
                                    return@awaitEachGesture
                                }
                                // Only a press on (or near) the ball grabs
                                // it; presses elsewhere are ignored. The
                                // check falls back to the ball's home when
                                // it has not been placed yet.
                                if (!canGrabBall(
                                        start, ballPos, BALL_CATCH_GRAB_RADIUS_DP,
                                        fieldSizeDp.x, fieldSizeDp.y,
                                    )
                                ) {
                                    return@awaitEachGesture
                                }
                                val home = ballPos
                                    ?: ballHome(fieldSizeDp.x, fieldSizeDp.y)
                                dragStart = start
                                dragCurrent = start
                                phase = BallPhase.Dragging
                                // Track the finger until it lifts. A press
                                // with no real move just springs the ball
                                // home — the grab itself already showed
                                // (the ball grows while held).
                                val completed = drag(down.id) { change ->
                                    val pos = Offset(
                                        change.position.x / density,
                                        change.position.y / density,
                                    )
                                    dragCurrent = pos
                                    // The ball follows the finger, clamped
                                    // inside the field.
                                    ballPos = Offset(
                                        pos.x.coerceIn(ballRadius, fieldSizeDp.x - ballRadius),
                                        pos.y.coerceIn(ballRadius, fieldSizeDp.y - ballRadius),
                                    )
                                }
                                val dragFrom = dragStart
                                val dragTo = dragCurrent
                                dragStart = null
                                dragCurrent = null
                                if (!completed || dragFrom == null || dragTo == null) {
                                    ballPos = home
                                    phase = BallPhase.Idle
                                    return@awaitEachGesture
                                }
                                val velocity = dragThrowVelocity(dragFrom, dragTo)
                                if (velocity == Offset.Zero) {
                                    // Too short a drag: no throw, the
                                    // ball springs back home.
                                    ballPos = home
                                    phase = BallPhase.Idle
                                } else {
                                    throwVelocity = velocity
                                    phase = BallPhase.Flying
                                }
                            }
                        },
                ) {
                    // Scene: sky, clouds, grass, shadows, trail, aim
                    // dots and all effect particles, drawn back to front
                    // in one Canvas (positions are dp, scaled by density).
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val wPx = size.width
                        val hPx = size.height
                        val fieldW = wPx / density
                        // 1. Drifting soft clouds.
                        val cloudDefs = listOf(
                            Triple(0.15f, 46f, 0f),
                            Triple(0.55f, 96f, 120f),
                            Triple(0.85f, 66f, 240f),
                        )
                        for ((baseX, baseY, phaseOff) in cloudDefs) {
                            val cx = ((baseX * fieldW + cloudDrift + phaseOff) %
                                (fieldW + BALL_CATCH_CLOUD_WRAP_DP) - 180f) * density
                            val cy = baseY * density
                            drawCircle(
                                color = Color.White, radius = 26f * density,
                                center = Offset(cx, cy), alpha = 0.85f,
                            )
                            drawCircle(
                                color = Color.White, radius = 20f * density,
                                center = Offset(cx + 22f * density, cy + 6f * density),
                                alpha = 0.85f,
                            )
                            drawCircle(
                                color = Color.White, radius = 18f * density,
                                center = Offset(cx - 22f * density, cy + 8f * density),
                                alpha = 0.85f,
                            )
                        }
                        // 2. Grass strip at the bottom, with tufts.
                        val grassTop = hPx - BALL_CATCH_GRASS_HEIGHT_DP * density
                        drawRect(
                            color = Color(0xFF81C784),
                            topLeft = Offset(0f, grassTop),
                            size = Size(wPx, BALL_CATCH_GRASS_HEIGHT_DP * density),
                        )
                        for (fx in listOf(0.08f, 0.22f, 0.38f, 0.55f, 0.72f, 0.88f)) {
                            val bx = fx * wPx
                            drawLine(
                                color = Color(0xFF558B2F),
                                start = Offset(bx, grassTop),
                                end = Offset(bx - 4f * density, grassTop - 10f * density),
                                strokeWidth = 3f * density, cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = Color(0xFF558B2F),
                                start = Offset(bx, grassTop),
                                end = Offset(bx, grassTop - 12f * density),
                                strokeWidth = 3f * density, cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = Color(0xFF558B2F),
                                start = Offset(bx, grassTop),
                                end = Offset(bx + 4f * density, grassTop - 10f * density),
                                strokeWidth = 3f * density, cap = StrokeCap.Round,
                            )
                        }
                        // 3. Soft shadow under the Pokemon.
                        val roamingShadow = roam
                        if (pokemonVisible && roamingShadow != null) {
                            drawOval(
                                color = Color.Black,
                                topLeft = Offset(
                                    (roamingShadow.x - 45f) * density,
                                    (roamingShadow.y + pokemonRadius * 0.72f) * density,
                                ),
                                size = Size(90f * density, 16f * density),
                                alpha = 0.18f,
                            )
                        }
                        // 4. Soft shadow under the ball: it shrinks and
                        // fades while the ball is up in its arc.
                        val shadowBall = ball
                        if (showBall && shadowBall != null) {
                            val flightP = (phaseElapsedMs / BALL_CATCH_FLIGHT_ARC_MS)
                                .coerceIn(0f, 1f)
                            val sScale = if (phase == BallPhase.Flying) {
                                ballShadowScale(flightP)
                            } else {
                                1f
                            }
                            val sAlpha = if (phase == BallPhase.Flying) {
                                ballShadowAlpha(flightP)
                            } else {
                                0.22f
                            }
                            val sw = ballRadius * 1.4f * sScale
                            drawOval(
                                color = Color.Black,
                                topLeft = Offset(
                                    (shadowBall.x - sw) * density,
                                    (shadowBall.y + ballRadius * 0.72f) * density,
                                ),
                                size = Size(sw * 2f * density, 12f * density),
                                alpha = sAlpha,
                            )
                        }
                        // 5. Fading motion trail behind the flying ball.
                        for (dot in flightTrail) {
                            val ageT = dot.ageMs / BALL_CATCH_TRAIL_MAX_AGE_MS
                            drawCircle(
                                color = Color.White,
                                radius = 10f * density * (1f - ageT * 0.5f),
                                center = Offset(dot.pos.x * density, dot.pos.y * density),
                                alpha = (1f - ageT) * 0.45f,
                            )
                        }
                        // 6. Dotted arc aim preview while dragging.
                        if (phase == BallPhase.Dragging && dragStart != null &&
                            dragCurrent != null && ball != null
                        ) {
                            for (dot in aimPreviewDots(ball, dragStart!!, dragCurrent!!)) {
                                drawCircle(
                                    color = Color(0xFF1976D2),
                                    radius = dot.radius * density,
                                    center = Offset(dot.pos.x * density, dot.pos.y * density),
                                    alpha = dot.alpha,
                                )
                            }
                        }
                        // 7. Hit burst: particles, expanding ring, flash.
                        val hitCentre = effectCentre
                        if (phase == BallPhase.Hit && hitCentre != null) {
                            val p = (phaseElapsedMs / BALL_CATCH_HIT_MS).coerceIn(0f, 1f)
                            drawRect(
                                color = Color.White,
                                topLeft = Offset.Zero,
                                size = size,
                                alpha = flashAlpha(p),
                            )
                            drawCircle(
                                color = Color(0xFFFFD600),
                                radius = ringRadius(p, 90f) * density,
                                center = Offset(hitCentre.x * density, hitCentre.y * density),
                                alpha = ringAlpha(p),
                                style = Stroke(6f * density),
                            )
                            hitParticles.forEachIndexed { i, particle ->
                                val pos = particlePosition(particle, hitCentre, p)
                                val color = when (i % 3) {
                                    0 -> Color(0xFFFFD600)
                                    1 -> Color(0xFFFF6D00)
                                    else -> Color.White
                                }
                                drawCircle(
                                    color = color,
                                    radius = particle.size * density,
                                    center = Offset(pos.x * density, pos.y * density),
                                    alpha = particleAlpha(particle, p),
                                )
                            }
                        }
                        // 8. Catch confetti.
                        val catchCentre = effectCentre
                        if (phase == BallPhase.Caught && catchCentre != null) {
                            val p = (phaseElapsedMs / 1200f).coerceIn(0f, 1f)
                            for (piece in catchParticles) {
                                val pos = confettiPosition(piece, catchCentre, p)
                                drawCircle(
                                    color = CONFETTI_COLORS[piece.colorIndex % CONFETTI_COLORS.size],
                                    radius = piece.size * density,
                                    center = Offset(pos.x * density, pos.y * density),
                                    alpha = 1f - p,
                                )
                            }
                        }
                        // 9. Break-out puff where the ball popped open.
                        val puffCentre = effectCentre
                        if (phase == BallPhase.BrokeOut && puffCentre != null) {
                            val p = (phaseElapsedMs / 800f).coerceIn(0f, 1f)
                            for (particle in puffParticles) {
                                val pos = particlePosition(particle, puffCentre, p)
                                drawCircle(
                                    color = Color(0xFFECEFF1),
                                    radius = particle.size * density,
                                    center = Offset(pos.x * density, pos.y * density),
                                    alpha = particleAlpha(particle, p) * 0.7f,
                                )
                            }
                        }
                    }
                    // The roaming Pokemon: bobs, leans into its movement
                    // and pops in with a scale/fade on each encounter.
                    val roaming = roam
                    if (pokemonVisible && roaming != null) {
                        val bob = bobOffset(
                            clockSeconds,
                            BALL_CATCH_BOB_AMPLITUDE_DP,
                            BALL_CATCH_BOB_PERIOD_S,
                        )
                        Image(
                            painter = painterResource(drawableId(context, pokemon.drawable)),
                            contentDescription = pokemon.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(BALL_CATCH_POKEMON_SIZE_DP.dp)
                                .offset(
                                    x = (roaming.x - pokemonRadius).dp,
                                    y = (roaming.y - pokemonRadius + bob).dp,
                                )
                                .rotate(pokemonTilt(roaming.dirX))
                                .scale(entranceScale(encounterElapsedMs))
                                .alpha(entranceAlpha(encounterElapsedMs)),
                        )
                    }
                    // "！" bubble above a newly appearing Pokemon.
                    if (pokemonVisible && roaming != null && encounterElapsedMs < 600f) {
                        Text(
                            text = "！",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935),
                            modifier = Modifier.offset(
                                x = (roaming.x + pokemonRadius * 0.55f).dp,
                                y = (roaming.y - pokemonRadius - 34f).dp,
                            ).alpha(bubbleAlpha(encounterElapsedMs)),
                        )
                    }
                    // The ball: grows while held, spins and arcs while
                    // flying, shakes while the catch is decided.
                    if (showBall && ball != null) {
                        val flightProgress = (phaseElapsedMs / BALL_CATCH_FLIGHT_ARC_MS)
                            .coerceIn(0f, 1f)
                        val arcScale = if (phase == BallPhase.Flying) {
                            ballArcScale(flightProgress)
                        } else {
                            1f
                        }
                        val lift = if (phase == BallPhase.Flying) {
                            ballLiftDp(flightProgress)
                        } else {
                            0f
                        }
                        val grabScale = if (phase == BallPhase.Dragging) 1.12f else 1f
                        PokeBallIcon(
                            modifier = Modifier.size(BALL_CATCH_BALL_SIZE_DP.dp)
                                .offset(
                                    x = (ball.x + shakeX - ballRadius).dp,
                                    y = (ball.y - lift - ballRadius).dp,
                                )
                                .rotate(ballRotation)
                                .scale(arcScale * grabScale),
                        )
                    }
                    // Hit effect: a big 💥 at the impact point, shown
                    // briefly before the ball shakes.
                    val impact = hitPoint
                    if (phase == BallPhase.Hit && impact != null) {
                        Text(
                            text = "💥",
                            fontSize = 64.sp,
                            modifier = Modifier.offset(
                                x = (impact.x - 32f).dp,
                                y = (impact.y - 32f).dp,
                            ),
                        )
                    }
                    // Catch banner: scales in with popping stars.
                    if (phase == BallPhase.Caught) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val bannerScale = easeOutBack(
                                    (phaseElapsedMs / 350f).coerceIn(0f, 1f),
                                )
                                Text(
                                    text = "🎉 抓到了！🎉",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.background(
                                        Color.White.copy(alpha = 0.9f),
                                        RoundedCornerShape(20.dp),
                                    )
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .scale(bannerScale),
                                )
                                val starScale = easeOutBack(
                                    ((phaseElapsedMs - 120f) / 250f).coerceIn(0f, 1f),
                                )
                                Text(
                                    text = "⭐ ⭐ ⭐",
                                    fontSize = 36.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp).scale(starScale),
                                )
                            }
                        }
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
