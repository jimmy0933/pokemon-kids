package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonDataTest {

    @Test
    fun sixtyPokemonWithUniqueIdsAndNames() {
        assertEquals(60, ALL_POKEMON.size)
        assertEquals(60, ALL_POKEMON.map { it.id }.toSet().size)
        assertEquals(60, ALL_POKEMON.map { it.name }.toSet().size)
        assertTrue(ALL_POKEMON.all { it.name.isNotBlank() && it.type.isNotBlank() })
    }

    @Test
    fun tenRoundsOfSixDifferentPokemon() {
        val rs = rounds()
        assertEquals(TOTAL_ROUNDS, rs.size)
        assertEquals(10, TOTAL_ROUNDS)
        assertTrue(rs.all { it.size == ROUND_SIZE })
        assertEquals(60, rs.flatten().map { it.id }.toSet().size)
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

    @Test
    fun everyPokemonHasHeightWeightAndIntro() {
        ALL_POKEMON.forEach { pokemon ->
            assertTrue("${pokemon.name} must have a positive height", pokemon.heightM > 0)
            assertTrue("${pokemon.name} must have a positive weight", pokemon.weightKg > 0)
            assertTrue(
                "${pokemon.name} intro must be at least 10 characters",
                pokemon.intro.length >= 10,
            )
        }
    }

    @Test
    fun speakTextContainsNameTypeHeightAndWeight() {
        ALL_POKEMON.forEach { pokemon ->
            val text = pokemon.speakText()
            assertTrue(text.contains(pokemon.name))
            assertTrue(text.contains(pokemon.type))
            assertTrue(text.contains(formatMeasure(pokemon.heightM)))
            assertTrue(text.contains(formatMeasure(pokemon.weightKg)))
            assertTrue(text.contains("公尺"))
            assertTrue(text.contains("公斤"))
            assertTrue(text.contains(pokemon.intro))
        }
        // Spot check: Pikachu reads naturally, whole numbers have no ".0".
        assertEquals(
            "皮卡丘，電屬性，身高 0.4 公尺，體重 6 公斤。臉頰兩邊有儲存電力的小囊袋。開心的時候，臉頰還會發出啪滋啪滋的電光！",
            ALL_POKEMON.first { it.id == 25 }.speakText(),
        )
    }
}
