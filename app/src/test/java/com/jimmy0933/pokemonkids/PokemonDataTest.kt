package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonDataTest {

    @Test
    fun thirtyPokemonWithUniqueIdsAndNames() {
        assertEquals(30, ALL_POKEMON.size)
        assertEquals(30, ALL_POKEMON.map { it.id }.toSet().size)
        assertEquals(30, ALL_POKEMON.map { it.name }.toSet().size)
        assertTrue(ALL_POKEMON.all { it.name.isNotBlank() && it.type.isNotBlank() })
    }

    @Test
    fun fiveRoundsOfSixDifferentPokemon() {
        val rs = rounds()
        assertEquals(TOTAL_ROUNDS, rs.size)
        assertTrue(rs.all { it.size == ROUND_SIZE })
        assertEquals(30, rs.flatten().map { it.id }.toSet().size)
    }

    @Test
    fun roundCardsAreSixPairsShuffled() {
        for (round in 0 until TOTAL_ROUNDS) {
            val cards = roundCards(round, seed = 42L)
            assertEquals(12, cards.size)
            assertEquals(6, cards.map { it.id }.toSet().size)
            cards.groupBy { it.id }.values.forEach { assertEquals(2, it.size) }
        }
    }

    @Test
    fun roundCompleteOnlyWhenAllSixMatched() {
        val round = rounds()[0]
        assertTrue(isRoundComplete(round.map { it.id }.toSet(), 0))
        assertTrue(!isRoundComplete(round.take(5).map { it.id }.toSet(), 0))
        assertTrue(!isRoundComplete(emptySet(), 0))
    }

    @Test
    fun famousPokemonAreInRoundOne() {
        val firstRoundIds = rounds()[0].map { it.id }.toSet()
        assertTrue(firstRoundIds.containsAll(listOf(1, 4, 7, 25, 133, 143)))
    }
}
