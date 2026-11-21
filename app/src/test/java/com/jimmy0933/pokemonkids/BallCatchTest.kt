package com.jimmy0933.pokemonkids

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
}
