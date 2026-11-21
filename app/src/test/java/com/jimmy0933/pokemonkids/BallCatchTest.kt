package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun ballHitsOnlyWhenPokemonStillAtTargetSpot() {
        assertTrue(ballHitsTarget(targetSpot = 1, pokemonSpotAtArrival = 1))
        assertFalse(ballHitsTarget(targetSpot = 1, pokemonSpotAtArrival = 0))
        assertFalse(ballHitsTarget(targetSpot = 2, pokemonSpotAtArrival = 1))
        assertFalse(ballHitsTarget(targetSpot = 0, pokemonSpotAtArrival = 2))
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
        assertEquals(3, BALL_CATCH_SPOT_COUNT)
        assertEquals(0.7, BALL_CATCH_CHANCE, 0.001)
    }
}
