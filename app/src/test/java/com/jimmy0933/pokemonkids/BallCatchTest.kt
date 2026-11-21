package com.jimmy0933.pokemonkids

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class BallCatchTest {

    @Test
    fun encountersComeOnlyFromUncaughtPool() {
        val collected = setOf(1, 2, 3, 25, 150)
        val encounters = ballCatchEncounters(ALL_POKEMON, collected, 109, seed = 31L)
        assertEquals(109, encounters.size)
        assertTrue("no encounter may already be collected", encounters.none { it.id in collected })
        assertEquals("encounters must be distinct", 109, encounters.map { it.id }.toSet().size)
        // Same seed, same encounters.
        assertEquals(
            encounters.map { it.id },
            ballCatchEncounters(ALL_POKEMON, collected, 109, seed = 31L).map { it.id },
        )
    }

    @Test
    fun caughtPokemonNeverReappearInLaterSessions() {
        // Session 1: catch the first 10 encounters.
        val first = ballCatchEncounters(ALL_POKEMON, emptySet(), 109, seed = 32L)
        val caught = first.take(10).map { it.id }.toSet()
        // Session 2: the caught ones are excluded from the pool.
        val second = ballCatchEncounters(ALL_POKEMON, caught, 109, seed = 33L)
        assertTrue("caught Pokemon must never reappear", second.none { it.id in caught })
        assertEquals(109, second.size)
    }

    @Test
    fun encountersCapAtUncaughtPoolSize() {
        // Only 5 uncaught Pokemon left: the session has 5 encounters.
        val collected = ALL_POKEMON.drop(5).map { it.id }.toSet()
        val encounters = ballCatchEncounters(ALL_POKEMON, collected, 109, seed = 34L)
        assertEquals(5, encounters.size)
        assertEquals(
            ALL_POKEMON.take(5).map { it.id }.toSet(),
            encounters.map { it.id }.toSet(),
        )
    }

    @Test
    fun emptyPoolGivesNoEncounters() {
        val collected = ALL_POKEMON.map { it.id }.toSet()
        assertTrue(ballCatchEncounters(ALL_POKEMON, collected, 109, seed = 35L).isEmpty())
    }

    @Test
    fun catchRollRespectsSeventyPercentChance() {
        val random = kotlin.random.Random(36L)
        val trials = 10_000
        val caught = (0 until trials).count { rollCatch(random) }
        val rate = caught.toDouble() / trials
        // 70% chance: with a fixed seed the rate must land near 0.70
        // (standard error at n=10000 is ~0.005, so 0.67-0.73 is ~6 sigma).
        assertTrue("catch rate $rate should be near 0.70", rate in 0.67..0.73)
    }

    @Test
    fun catchRollRespectsCustomChance() {
        val random = kotlin.random.Random(37L)
        repeat(100) { assertFalse("0% chance must never catch", rollCatch(random, chance = 0.0)) }
        repeat(100) { assertTrue("100% chance must always catch", rollCatch(random, chance = 1.0)) }
    }

    @Test
    fun ballCatchSessionCounts() {
        assertEquals(109, BALL_CATCH_ENCOUNTER_COUNT)
        assertEquals(0.7, BALL_CATCH_CHANCE, 0.001)
    }

    // --- Drag-to-throw ---

    @Test
    fun dragThrowVelocityFollowsDragDirection() {
        // Drag straight up (toward a Pokemon above): the ball flies up.
        val up = dragThrowVelocity(Offset(100f, 300f), Offset(100f, 100f))
        assertTrue("upward drag must fly up", up.y < 0f)
        assertEquals("straight up has no sideways speed", 0f, up.x, 0.01f)
        // Drag up-right: the ball flies up and right.
        val upRight = dragThrowVelocity(Offset(100f, 300f), Offset(200f, 200f))
        assertTrue("up-right drag must fly right", upRight.x > 0f)
        assertTrue("up-right drag must fly up", upRight.y < 0f)
        // Drag down-left: the ball flies down and left.
        val downLeft = dragThrowVelocity(Offset(200f, 100f), Offset(100f, 200f))
        assertTrue(downLeft.x < 0f)
        assertTrue(downLeft.y > 0f)
    }

    @Test
    fun dragThrowVelocityScalesWithDragLength() {
        val short = dragThrowVelocity(Offset(0f, 0f), Offset(0f, 100f))
        val long = dragThrowVelocity(Offset(0f, 0f), Offset(0f, 250f))
        val shortSpeed = short.getDistance()
        val longSpeed = long.getDistance()
        assertTrue("longer drag must throw faster", longSpeed > shortSpeed)
        assertTrue("speed must stay at or above the minimum", shortSpeed >= BALL_CATCH_MIN_THROW_SPEED_DP)
        assertTrue("speed must stay at or below the maximum", longSpeed <= BALL_CATCH_MAX_THROW_SPEED_DP)
    }

    @Test
    fun tooShortDragThrowsNothing() {
        // A tap or a tiny nudge is not a throw.
        assertEquals(Offset.Zero, dragThrowVelocity(Offset(100f, 100f), Offset(100f, 100f)))
        assertEquals(Offset.Zero, dragThrowVelocity(Offset(100f, 100f), Offset(105f, 108f)))
        assertEquals(Offset.Zero, dragThrowVelocity(Offset(100f, 100f), Offset(100f, 119f)))
    }

    @Test
    fun ballStepMovesAlongVelocity() {
        val next = ballStep(Offset(100f, 200f), Offset(300f, -400f), 0.5f)
        assertEquals(250f, next.x, 0.01f)
        assertEquals(0f, next.y, 0.01f)
    }

    @Test
    fun ballHitsPokemonWithinRadius() {
        val pokemon = Offset(200f, 200f)
        assertTrue(ballHitsPokemon(Offset(200f, 200f), pokemon))
        assertTrue(ballHitsPokemon(Offset(250f, 200f), pokemon))
        assertFalse(ballHitsPokemon(Offset(300f, 200f), pokemon))
        assertFalse(ballHitsPokemon(Offset(200f, 100f), pokemon))
        // Exactly at the radius still counts as a hit.
        assertTrue(ballHitsPokemon(Offset(200f + BALL_CATCH_HIT_RADIUS_DP, 200f), pokemon))
    }

    @Test
    fun ballOffFieldDetection() {
        assertFalse(ballOffField(Offset(180f, 250f), 360f, 500f))
        assertFalse(ballOffField(Offset(0f, 0f), 360f, 500f))
        assertTrue(ballOffField(Offset(-100f, 250f), 360f, 500f))
        assertTrue(ballOffField(Offset(500f, 250f), 360f, 500f))
        assertTrue(ballOffField(Offset(180f, -100f), 360f, 500f))
        assertTrue(ballOffField(Offset(180f, 700f), 360f, 500f))
    }

    @Test
    fun aimGuideEndPointsAlongDragDirection() {
        val ball = Offset(180f, 400f)
        // Drag up: the guide points up from the ball.
        val up = aimGuideEnd(ball, Offset(180f, 400f), Offset(180f, 300f))
        assertEquals(180f, up.x, 0.01f)
        assertEquals(400f - BALL_CATCH_AIM_LENGTH_DP, up.y, 0.01f)
        // Drag right: the guide points right from the ball.
        val right = aimGuideEnd(ball, Offset(180f, 400f), Offset(280f, 400f))
        assertEquals(180f + BALL_CATCH_AIM_LENGTH_DP, right.x, 0.01f)
        assertEquals(400f, right.y, 0.01f)
        // No drag yet: the guide stays at the ball.
        assertEquals(ball, aimGuideEnd(ball, Offset(180f, 400f), Offset(180f, 400f)))
    }

    @Test
    fun ballHomeIsBottomCentre() {
        val home = ballHome(360f, 500f)
        assertEquals(180f, home.x, 0.01f)
        assertEquals(500f - BALL_CATCH_BALL_SIZE_DP / 2f - 8f, home.y, 0.01f)
    }

    // --- Free-roaming Pokemon ---

    @Test
    fun initialRoamStateIsInsideField() {
        val random = kotlin.random.Random(41L)
        repeat(20) {
            val state = initialRoamState(360f, 500f, 55f, random)
            assertTrue("start x must be inside", state.x in 55f..305f)
            assertTrue("start y must be inside", state.y in 55f..445f)
            val speed = kotlin.math.sqrt(state.dirX * state.dirX + state.dirY * state.dirY)
            assertEquals("direction must be a unit vector", 1f, speed, 0.001f)
            assertTrue("turn timer must be in range", state.turnInMs in 800L..1500L)
        }
    }

    @Test
    fun roamStepStaysInBoundsOverManySteps() {
        val random = kotlin.random.Random(42L)
        var state = initialRoamState(360f, 500f, 55f, random)
        repeat(1000) {
            state = roamStep(state, 50L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
            assertTrue("x must stay in bounds, was ${state.x}", state.x in 55f..305f)
            assertTrue("y must stay in bounds, was ${state.y}", state.y in 55f..445f)
            val speed = kotlin.math.sqrt(state.dirX * state.dirX + state.dirY * state.dirY)
            assertEquals("direction must stay a unit vector", 1f, speed, 0.001f)
        }
    }

    @Test
    fun roamStepBouncesOffEdges() {
        val random = kotlin.random.Random(43L)
        // Heading left into the left edge: it must bounce back right.
        var state = RoamState(x = 56f, y = 250f, dirX = -1f, dirY = 0f, turnInMs = 1000L)
        state = roamStep(state, 50L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
        assertEquals(55f, state.x, 0.01f)
        assertTrue("must bounce right after hitting the left edge", state.dirX > 0f)
        // Heading down into the bottom edge: it must bounce back up.
        state = RoamState(x = 180f, y = 444f, dirX = 0f, dirY = 1f, turnInMs = 1000L)
        state = roamStep(state, 50L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
        assertEquals(445f, state.y, 0.01f)
        assertTrue("must bounce up after hitting the bottom edge", state.dirY < 0f)
    }

    @Test
    fun roamStepTurnsAtRandomIntervals() {
        val random = kotlin.random.Random(44L)
        var state = initialRoamState(360f, 500f, 55f, random)
        val firstDirX = state.dirX
        val firstDirY = state.dirY
        var turned = false
        repeat(200) {
            val next = roamStep(state, 50L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
            if (abs(next.dirX - firstDirX) > 0.01f || abs(next.dirY - firstDirY) > 0.01f) {
                turned = true
            }
            state = next
        }
        assertTrue("the Pokemon must change direction within 10 seconds", turned)
        assertTrue("turn timer must stay in range", state.turnInMs in 800L..1500L)
    }

    @Test
    fun roamStepMovesAtConstantSpeed() {
        val random = kotlin.random.Random(45L)
        val state = initialRoamState(360f, 500f, 55f, random)
        // One 50ms step at 120 dp/s moves 6 dp.
        val next = roamStep(state, 50L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
        val moved = Offset(next.x - state.x, next.y - state.y).getDistance()
        assertEquals("one step must move speed * dt", 6f, moved, 0.01f)
    }

    // --- v2.12: first-tap grab (regression test for the v2.11 bug) ---
    //
    // The v2.11 bug ("first tap on the ball does nothing, second tap
    // works") had two causes in the gesture code: (1) pointerInput was
    // keyed on (areaWidth, areaHeight), so the detector restarted — and
    // dropped the in-progress gesture — when the field size settled after
    // the first layout, exactly when a kid grabs the ball for the first
    // time; (2) the grab check read ballPos, which starts null and is
    // only set by a LaunchedEffect. The fix: a stable pointerInput(Unit)
    // key with the size from onSizeChanged, a grab on the press itself
    // (not after the touch slop), and canGrabBall falling back to the
    // ball's home when it has not been placed yet. The gesture loop
    // itself can only run on a real device, so these tests cover the
    // pure grab check it relies on.

    @Test
    fun canGrabBallFallsBackToHomeWhenBallIsNull() {
        val home = ballHome(360f, 500f)
        assertTrue(
            "press on the home must grab even before placement",
            canGrabBall(home, null, BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f),
        )
        assertTrue(
            "press near the home must grab even before placement",
            canGrabBall(
                Offset(home.x + 80f, home.y), null,
                BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f,
            ),
        )
        assertFalse(
            "press far from the home must not grab",
            canGrabBall(Offset(0f, 0f), null, BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f),
        )
    }

    @Test
    fun canGrabBallUsesBallPositionWhenSet() {
        val ball = Offset(100f, 100f)
        assertTrue(
            canGrabBall(Offset(100f, 100f), ball, BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f),
        )
        assertFalse(
            canGrabBall(Offset(300f, 300f), ball, BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f),
        )
    }

    @Test
    fun canGrabBallBoundary() {
        val ball = Offset(180f, 400f)
        assertTrue(
            "exactly at the grab radius still grabs",
            canGrabBall(
                Offset(180f + BALL_CATCH_GRAB_RADIUS_DP, 400f), ball,
                BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f,
            ),
        )
        assertFalse(
            "just beyond the grab radius does not grab",
            canGrabBall(
                Offset(180f + BALL_CATCH_GRAB_RADIUS_DP + 1f, 400f), ball,
                BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f,
            ),
        )
    }

    // --- v2.13: ball unthrowable on the second encounter ---
    //
    // The v2.12 bug ("after catching the first Pokemon, the ball cannot
    // be thrown on the second encounter"): ballPos is
    // remember(sessionSeed, index), so a catch (index++) swaps in a fresh
    // state object with the ball back at home — but the gesture detector
    // was pointerInput(Unit), so it kept holding the FIRST encounter's
    // ballPos (the catch position, where the ball had snapped onto the
    // Pokemon). The press on the visible ball at home was checked against
    // that stale position and ignored, and even a grab would have written
    // to the stale state. The fix: pointerInput(sessionSeed, index), so
    // the detector restarts with the fresh state each encounter; the
    // restart can only happen at the Caught -> Idle transition, when no
    // drag can be in progress. The gesture loop itself can only run on a
    // real device, so this test pins the pure check it relies on: after a
    // catch the ball is home, so a press on the home grabs, while a press
    // on the OLD catch position (where the ball used to be) must not.

    @Test
    fun canGrabBallUsesFreshHomeAfterCatch() {
        val home = ballHome(360f, 500f)
        val oldCatchPosition = Offset(180f, 120f)
        assertTrue(
            "press on the ball at its post-catch home must grab",
            canGrabBall(home, home, BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f),
        )
        assertFalse(
            "press on the old catch position must not grab once the ball is home",
            canGrabBall(
                oldCatchPosition, home,
                BALL_CATCH_GRAB_RADIUS_DP, 360f, 500f,
            ),
        )
    }

    // --- v2.12: smooth motion ---

    @Test
    fun steerHeadingMovesTowardTarget() {
        val east = Offset(1f, 0f)
        val north = Offset(0f, -1f)
        // No time: no turn.
        val stayed = steerHeading(east, north, 0f, 0.3f)
        assertEquals(1f, stayed.x, 0.01f)
        assertEquals(0f, stayed.y, 0.01f)
        // Full steer time: reaches the target.
        val arrived = steerHeading(east, north, 0.3f, 0.3f)
        assertEquals(0f, arrived.x, 0.01f)
        assertEquals(-1f, arrived.y, 0.01f)
        // Halfway: 45 degrees between east and north.
        val half = steerHeading(east, north, 0.15f, 0.3f)
        assertEquals(0.707f, half.x, 0.01f)
        assertEquals(-0.707f, half.y, 0.01f)
        // Turns the short way: from -10 degrees to +10 degrees goes
        // through 0 degrees, not the long way around.
        val minusTen = Offset(
            cos(-10f * PI.toFloat() / 180f),
            sin(-10f * PI.toFloat() / 180f),
        )
        val plusTen = Offset(
            cos(10f * PI.toFloat() / 180f),
            sin(10f * PI.toFloat() / 180f),
        )
        val short = steerHeading(minusTen, plusTen, 0.15f, 0.3f)
        assertEquals(1f, short.x, 0.01f)
        assertEquals(0f, short.y, 0.01f)
    }

    @Test
    fun steerHeadingKeepsUnitLength() {
        val random = kotlin.random.Random(51L)
        repeat(100) {
            val a = random.nextFloat() * 2f * PI.toFloat()
            val b = random.nextFloat() * 2f * PI.toFloat()
            val steered = steerHeading(
                Offset(cos(a), sin(a)), Offset(cos(b), sin(b)), 0.05f, 0.3f,
            )
            assertEquals("steered heading must stay a unit vector", 1f, steered.getDistance(), 0.001f)
        }
    }

    @Test
    fun roamStepSteersSmoothlyInsteadOfSnapping() {
        val random = kotlin.random.Random(54L)
        // Heading east, target north (fresh turn): one 16ms frame turns
        // only a little, it must not snap to the new heading.
        var state = RoamState(
            x = 180f, y = 250f, dirX = 1f, dirY = 0f, turnInMs = 1000L,
            targetDirX = 0f, targetDirY = -1f,
        )
        state = roamStep(state, 16L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
        assertTrue("one frame must not snap to the new heading", state.dirX > 0.9f)
        assertTrue("heading must start turning", state.dirY < 0f)
        // After many frames it arrives at the target heading (the turn
        // timer fires after 1000ms, i.e. after 62 frames, so 60 more
        // frames stay on the same target).
        repeat(60) {
            state = roamStep(state, 16L, BALL_CATCH_POKEMON_SPEED_DP, 360f, 500f, 55f, random)
        }
        assertTrue("heading must converge on the target", state.dirY < -0.9f)
    }

    @Test
    fun bobOffsetValues() {
        assertEquals(0f, bobOffset(0f, 4f, 1.6f), 0.01f)
        assertEquals(4f, bobOffset(0.4f, 4f, 1.6f), 0.01f)
        assertEquals(0f, bobOffset(0.8f, 4f, 1.6f), 0.01f)
        assertEquals(-4f, bobOffset(1.2f, 4f, 1.6f), 0.01f)
    }

    @Test
    fun pokemonTiltLeansWithMovement() {
        assertEquals(14f, pokemonTilt(1f), 0.01f)
        assertEquals(-14f, pokemonTilt(-1f), 0.01f)
        assertEquals(0f, pokemonTilt(0f), 0.01f)
        assertEquals(7f, pokemonTilt(0.5f), 0.01f)
    }

    @Test
    fun spinDeltaIsProportionalToDistance() {
        // One full roll (2 * PI * radius) is 360 degrees.
        assertEquals(360f, spinDelta(2f * PI.toFloat() * 36f, 36f), 0.5f)
        assertEquals(180f, spinDelta(PI.toFloat() * 36f, 36f), 0.5f)
        assertEquals(0f, spinDelta(0f, 36f), 0.01f)
    }

    @Test
    fun ballArcHelpersPeakMidFlight() {
        assertEquals(1f, ballArcScale(0f), 0.01f)
        assertEquals(1.18f, ballArcScale(0.5f), 0.01f)
        assertEquals(1f, ballArcScale(1f), 0.01f)
        assertEquals(1f, ballShadowScale(0f), 0.01f)
        assertEquals(0.55f, ballShadowScale(0.5f), 0.01f)
        assertEquals(1f, ballShadowScale(1f), 0.01f)
        assertEquals(0.22f, ballShadowAlpha(0f), 0.01f)
        assertEquals(14f, ballLiftDp(0.5f), 0.01f)
        assertEquals(0f, ballLiftDp(0f), 0.01f)
        assertEquals(0f, ballLiftDp(1f), 0.01f)
    }

    @Test
    fun aimPreviewDotsFadeAlongTheLine() {
        val ball = Offset(180f, 400f)
        val dots = aimPreviewDots(ball, Offset(180f, 400f), Offset(180f, 300f))
        assertEquals(BALL_CATCH_AIM_DOT_COUNT, dots.size)
        // First dot is just ahead of the ball, last dot furthest along.
        assertTrue(dots.first().pos.y < ball.y)
        assertTrue(dots.last().pos.y < dots.first().pos.y)
        // Dots run along the drag direction (same x for a straight-up drag).
        dots.forEach { assertEquals(180f, it.pos.x, 0.5f) }
        // Alpha and size fade along the length.
        assertTrue(dots.first().alpha > dots.last().alpha)
        assertTrue(dots.first().radius > dots.last().radius)
        // No drag direction: no dots.
        assertTrue(aimPreviewDots(ball, Offset(180f, 400f), Offset(180f, 400f)).isEmpty())
    }

    // --- v2.12: hit / catch feedback ---

    @Test
    fun particleBurstStartsAtCentreAndSpreadsOut() {
        val centre = Offset(200f, 200f)
        val burst = particleBurst(14, seed = 52L)
        assertEquals(14, burst.size)
        burst.forEach { particle ->
            val start = particlePosition(particle, centre, 0f)
            assertEquals(centre.x, start.x, 0.01f)
            assertEquals(centre.y, start.y, 0.01f)
            val end = particlePosition(particle, centre, 1f)
            val travelled = Offset(end.x - centre.x, end.y - centre.y).getDistance()
            assertEquals(
                "particle must travel its full distance by the end",
                particle.distance, travelled, 0.5f,
            )
            assertEquals("particle starts fully visible", 1f, particleAlpha(particle, 0f), 0.01f)
            assertEquals("particle ends invisible", 0f, particleAlpha(particle, 1f), 0.01f)
        }
    }

    @Test
    fun ringAndFlashFadeWithProgress() {
        assertEquals(0f, ringRadius(0f, 90f), 0.01f)
        assertEquals(90f, ringRadius(1f, 90f), 0.01f)
        assertTrue("ease-out ring grows more than half-way by mid-time", ringRadius(0.5f, 90f) > 45f)
        assertEquals(0.8f, ringAlpha(0f), 0.01f)
        assertEquals(0f, ringAlpha(1f), 0.01f)
        assertTrue("flash starts bright", flashAlpha(0f) > 0f)
        assertEquals("flash is gone by mid-hit", 0f, flashAlpha(0.5f), 0.01f)
    }

    @Test
    fun confettiBurstsOutAndFalls() {
        val centre = Offset(200f, 200f)
        val pieces = confettiBurst(24, seed = 53L)
        assertEquals(24, pieces.size)
        pieces.forEach { piece ->
            val start = confettiPosition(piece, centre, 0f)
            assertEquals(centre.x, start.x, 0.01f)
            assertEquals(centre.y, start.y, 0.01f)
            val end = confettiPosition(piece, centre, 1f)
            assertTrue("confetti must end up below the centre", end.y > centre.y)
        }
    }

    @Test
    fun entranceScalePopsIn() {
        assertEquals(0f, entranceScale(0f), 0.01f)
        assertEquals(1f, entranceScale(400f), 0.01f)
        assertTrue("ease-out-back overshoots mid-way", entranceScale(200f) > 1f)
        assertEquals(0f, entranceAlpha(0f), 0.01f)
        assertEquals(1f, entranceAlpha(200f), 0.01f)
        assertEquals(1f, bubbleAlpha(0f), 0.01f)
        assertEquals(0f, bubbleAlpha(600f), 0.01f)
    }

    @Test
    fun smoothProgressEasesTowardTarget() {
        assertEquals(0f, smoothProgress(0f, 1f, 0f, 6f), 0.001f)
        val step = smoothProgress(0f, 1f, 0.1f, 6f)
        assertTrue("progress must move toward the target", step > 0f && step < 1f)
        assertEquals(1f, smoothProgress(1f, 1f, 0.1f, 6f), 0.001f)
        assertTrue("progress must never overshoot", smoothProgress(0.9f, 1f, 1f, 6f) <= 1f)
    }
}
