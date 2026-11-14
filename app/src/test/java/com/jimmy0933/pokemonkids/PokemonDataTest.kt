package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonDataTest {

    @Test
    fun threeHundredPokemonWithUniqueIdsAndNames() {
        assertEquals(300, ALL_POKEMON.size)
        assertEquals(300, ALL_POKEMON.map { it.id }.toSet().size)
        assertEquals(300, ALL_POKEMON.map { it.name }.toSet().size)
        assertEquals((1..300).toSet(), ALL_POKEMON.map { it.id }.toSet())
        assertTrue(ALL_POKEMON.all { it.name.isNotBlank() && it.type.isNotBlank() })
    }

    @Test
    fun fiftyRoundsWithCorrectSizes() {
        val rs = rounds()
        assertEquals(TOTAL_ROUNDS, rs.size)
        assertEquals(50, TOTAL_ROUNDS)
        assertTrue(rs.all { it.size == ROUND_SIZE })
        assertEquals(300, rs.flatten().map { it.id }.toSet().size)
        // Rounds 1-20 are unchanged from v1.3.
        assertEquals(listOf(1, 4, 7, 25, 133, 143), rounds()[0].map { it.id })
        assertEquals(listOf(96, 98, 100, 102, 105, 108), rounds()[19].map { it.id })
        // Rounds 21-50 hold the new Pokemon in Dex order.
        assertEquals(listOf(89, 97, 99, 101, 106, 107), rounds()[20].map { it.id })
        assertEquals(listOf(295, 296, 297, 298, 299, 300), rounds()[49].map { it.id })
    }

    @Test
    fun roundSizeMatchesRoundIndex() {
        assertEquals(6, roundSize(0))
        assertEquals(6, roundSize(19))
        assertEquals(6, roundSize(20))
        assertEquals(6, roundSize(49))
    }

    @Test
    fun roundCardsArePairsShuffled() {
        for (round in 0 until TOTAL_ROUNDS) {
            val size = roundSize(round)
            val cards = roundCards(round, seed = 42L)
            assertEquals(size * 2, cards.size)
            assertEquals(size, cards.map { it.id }.toSet().size)
            cards.groupBy { it.id }.values.forEach { assertEquals(2, it.size) }
        }
    }

    @Test
    fun roundCompleteOnlyWhenAllMatched() {
        val round = rounds()[0]
        assertTrue(isRoundComplete(round.map { it.id }.toSet(), 0))
        assertTrue(!isRoundComplete(round.take(5).map { it.id }.toSet(), 0))
        assertTrue(!isRoundComplete(emptySet(), 0))
        val lateRound = rounds()[49]
        assertTrue(isRoundComplete(lateRound.map { it.id }.toSet(), 49))
        assertTrue(!isRoundComplete(lateRound.take(5).map { it.id }.toSet(), 49))
    }

    @Test
    fun famousPokemonAreInRoundOne() {
        val firstRoundIds = rounds()[0].map { it.id }.toSet()
        assertTrue(firstRoundIds.containsAll(listOf(1, 4, 7, 25, 133, 143)))
    }

    @Test
    fun everyPokemonHasDrawableResource() {
        // Drawable resources are bundled as pkm_001.png ... pkm_300.png; the name
        // must match the id so the UI can resolve it with resources.getIdentifier.
        ALL_POKEMON.forEach { pokemon ->
            assertEquals(
                "pkm_%03d".format(pokemon.id),
                pokemon.drawable,
            )
        }
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

    @Test
    fun evolutionLinksPointAtPokemonInTheGame() {
        val ids = ALL_POKEMON.map { it.id }.toSet()
        ALL_POKEMON.forEach { pokemon ->
            if (pokemon.evolvesFrom != null) {
                assertTrue("${pokemon.name} evolvesFrom must be in the game", pokemon.evolvesFrom in ids)
                assertTrue("${pokemon.name} cannot evolve from itself", pokemon.evolvesFrom != pokemon.id)
            }
            pokemon.evolvesTo.forEach { target ->
                assertTrue("${pokemon.name} evolvesTo $target must be in the game", target in ids)
                assertTrue("${pokemon.name} cannot evolve into itself", target != pokemon.id)
            }
            // The reverse link must agree: if A lists B in evolvesTo, B lists A in evolvesFrom.
            pokemon.evolvesTo.forEach { target ->
                val next = ALL_POKEMON.first { it.id == target }
                assertEquals(pokemon.id, next.evolvesFrom)
            }
            if (pokemon.evolvesFrom != null) {
                val prev = ALL_POKEMON.first { it.id == pokemon.evolvesFrom }
                assertTrue(pokemon.id in prev.evolvesTo)
            }
        }
    }

    @Test
    fun evolutionFamiliesAreCompleteAndSorted() {
        // Pikachu's family: Pichu -> Pikachu -> Raichu.
        val pikachu = ALL_POKEMON.first { it.id == 25 }
        assertEquals(listOf(25, 26, 172), pikachu.evolutionFamily().map { it.id })
        // Eevee's branched family: Eevee + 5 evolutions within Dex 1-300.
        val eevee = ALL_POKEMON.first { it.id == 133 }
        assertEquals(
            listOf(133, 134, 135, 136, 196, 197),
            eevee.evolutionFamily().map { it.id },
        )
        // A Pokemon with no evolutions is a family of one.
        val mew = ALL_POKEMON.first { it.id == 151 }
        assertEquals(listOf(151), mew.evolutionFamily().map { it.id })
        // Every family contains the Pokemon itself, is sorted, and stays in the game.
        ALL_POKEMON.forEach { pokemon ->
            val family = pokemon.evolutionFamily()
            assertTrue(family.contains(pokemon))
            assertEquals(family.map { it.id }.sorted(), family.map { it.id })
        }
    }

    @Test
    fun regionsCoverAllPokemon() {
        assertEquals(151, regionPokemon(Region.KANTO).size)
        assertEquals(100, regionPokemon(Region.JOHTO).size)
        assertEquals(49, regionPokemon(Region.HOENN).size)
        assertEquals(Region.KANTO, regionOf(ALL_POKEMON.first { it.id == 1 }))
        assertEquals(Region.KANTO, regionOf(ALL_POKEMON.first { it.id == 151 }))
        assertEquals(Region.JOHTO, regionOf(ALL_POKEMON.first { it.id == 152 }))
        assertEquals(Region.JOHTO, regionOf(ALL_POKEMON.first { it.id == 251 }))
        assertEquals(Region.HOENN, regionOf(ALL_POKEMON.first { it.id == 252 }))
        assertEquals(Region.HOENN, regionOf(ALL_POKEMON.first { it.id == 300 }))
        assertEquals(300, Region.entries.sumOf { regionPokemon(it).size })
    }

    @Test
    fun quizQuestionsIncludeAnswerAndUniqueOptions() {
        val questions = makeQuizQuestions(ALL_POKEMON, emptySet(), 10, 4, seed = 7L)
        assertEquals(10, questions.size)
        assertEquals(10, questions.map { it.answer.id }.toSet().size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.options.contains(q.answer))
            assertEquals(4, q.options.map { it.id }.toSet().size)
        }
        // Silhouette mode uses 3 options.
        val silhouette = makeQuizQuestions(ALL_POKEMON, emptySet(), 10, 3, seed = 8L)
        assertTrue(silhouette.all { it.options.size == 3 && it.options.contains(it.answer) })
    }

    @Test
    fun quizQuestionsPreferUncollectedPokemon() {
        // With only 5 uncollected, the first 5 answers must be exactly those 5.
        val collected = (ALL_POKEMON.map { it.id } - setOf(1, 4, 7, 25, 133)).toSet()
        val questions = makeQuizQuestions(ALL_POKEMON, collected, 10, 4, seed = 9L)
        assertEquals(setOf(1, 4, 7, 25, 133), questions.take(5).map { it.answer.id }.toSet())
        // With everything collected, it still produces questions.
        val allCollected = makeQuizQuestions(ALL_POKEMON, ALL_POKEMON.map { it.id }.toSet(), 10, 4, seed = 10L)
        assertEquals(10, allCollected.size)
    }
}
