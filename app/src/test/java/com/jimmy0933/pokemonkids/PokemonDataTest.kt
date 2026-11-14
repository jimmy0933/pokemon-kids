package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonDataTest {

    @Test
    fun fourHundredPokemonWithUniqueIdsAndNames() {
        assertEquals(400, ALL_POKEMON.size)
        assertEquals(400, ALL_POKEMON.map { it.id }.toSet().size)
        assertEquals(400, ALL_POKEMON.map { it.name }.toSet().size)
        assertEquals((1..400).toSet(), ALL_POKEMON.map { it.id }.toSet())
        assertTrue(ALL_POKEMON.all { it.name.isNotBlank() && it.type.isNotBlank() })
    }

    @Test
    fun fiftyRoundsWithCorrectSizes() {
        val rs = rounds()
        assertEquals(TOTAL_ROUNDS, rs.size)
        assertEquals(50, TOTAL_ROUNDS)
        assertTrue(rs.all { it.size == ROUND_SIZE })
        assertEquals(400, rs.flatten().map { it.id }.toSet().size)
        // Rounds are strictly Dex-ordered: round 1 = 1-8 ... round 50 = 393-400.
        assertEquals((1..400).toList(), rs.flatten().map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), rounds()[0].map { it.id })
        assertEquals(listOf(9, 10, 11, 12, 13, 14, 15, 16), rounds()[1].map { it.id })
        assertEquals(listOf(145, 146, 147, 148, 149, 150, 151, 152), rounds()[18].map { it.id })
        assertEquals(listOf(393, 394, 395, 396, 397, 398, 399, 400), rounds()[49].map { it.id })
    }

    @Test
    fun roundSizeMatchesRoundIndex() {
        assertEquals(8, roundSize(0))
        assertEquals(8, roundSize(19))
        assertEquals(8, roundSize(20))
        assertEquals(8, roundSize(49))
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
        assertTrue(!isRoundComplete(round.take(7).map { it.id }.toSet(), 0))
        assertTrue(!isRoundComplete(emptySet(), 0))
        val lateRound = rounds()[49]
        assertTrue(isRoundComplete(lateRound.map { it.id }.toSet(), 49))
        assertTrue(!isRoundComplete(lateRound.take(7).map { it.id }.toSet(), 49))
    }

    @Test
    fun roundOneIsDexOneToEight() {
        // v2.1 dropped the famous-first ordering: rounds are plain Dex order,
        // so round 1 is Dex 1-8 (the three starters are in it).
        val firstRoundIds = rounds()[0].map { it.id }.toSet()
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8), firstRoundIds)
        assertTrue(firstRoundIds.containsAll(listOf(1, 4, 7)))
        // Famous Pokemon that used to be in round 1 now sit in their Dex rounds:
        // Pikachu (25) is in round 4, Eevee (133) in round 17, Snorlax (143) in round 18.
        assertTrue(25 in rounds()[3].map { it.id }.toSet())
        assertTrue(133 in rounds()[16].map { it.id }.toSet())
        assertTrue(143 in rounds()[17].map { it.id }.toSet())
    }

    @Test
    fun everyPokemonHasDrawableResource() {
        // Drawable resources are bundled as pkm_001.png ... pkm_400.png; the name
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
        // Eevee's branched family: Eevee + 5 evolutions within Dex 1-400.
        val eevee = ALL_POKEMON.first { it.id == 133 }
        assertEquals(
            listOf(133, 134, 135, 136, 196, 197),
            eevee.evolutionFamily().map { it.id },
        )
        // A Gen 3 starter family: Treecko -> Grovyle -> Sceptile.
        val treecko = ALL_POKEMON.first { it.id == 252 }
        assertEquals(listOf(252, 253, 254), treecko.evolutionFamily().map { it.id })
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
        assertEquals(135, regionPokemon(Region.HOENN).size)
        assertEquals(14, regionPokemon(Region.SINNOH).size)
        assertEquals(Region.KANTO, regionOf(ALL_POKEMON.first { it.id == 1 }))
        assertEquals(Region.KANTO, regionOf(ALL_POKEMON.first { it.id == 151 }))
        assertEquals(Region.JOHTO, regionOf(ALL_POKEMON.first { it.id == 152 }))
        assertEquals(Region.JOHTO, regionOf(ALL_POKEMON.first { it.id == 251 }))
        assertEquals(Region.HOENN, regionOf(ALL_POKEMON.first { it.id == 252 }))
        assertEquals(Region.HOENN, regionOf(ALL_POKEMON.first { it.id == 386 }))
        assertEquals(Region.SINNOH, regionOf(ALL_POKEMON.first { it.id == 387 }))
        assertEquals(Region.SINNOH, regionOf(ALL_POKEMON.first { it.id == 400 }))
        assertEquals(400, Region.entries.sumOf { regionPokemon(it).size })
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

    @Test
    fun colorOfReturnsColourForEveryMappedId() {
        // Every id in the curated map resolves to exactly its listed colour.
        COLOR_SORT_MAP.forEach { (id, category) ->
            assertEquals("colorOf($id) must be $category", category, colorOf(id))
        }
        // Every mapped id is a Pokemon in the game.
        val gameIds = ALL_POKEMON.map { it.id }.toSet()
        assertTrue(COLOR_SORT_MAP.keys.all { it in gameIds })
        // Pokemon left out of the curated map (ambiguous colours) return null.
        assertNull(colorOf(1)) // 妙蛙種子：綠＋藍雙色
        assertNull(colorOf(4)) // 小火龍：橘色
        assertNull(colorOf(39)) // 胖丁：粉紅色
        assertNull(colorOf(94)) // 耿鬼：紫色
        assertNull(colorOf(150)) // 超夢：白色
        assertNull(colorOf(0))
        assertNull(colorOf(401))
    }

    @Test
    fun colorSortPoolHasFortyPlusEntriesAcrossFourColours() {
        val pool = colorSortPool()
        assertTrue("pool must have 40+ entries", pool.size >= 40)
        assertEquals(72, pool.size)
        assertEquals(72, pool.map { it.id }.toSet().size)
        // 18 per colour, all four colours represented.
        val byColour = pool.groupBy { colorOf(it.id) }
        assertEquals(4, byColour.size)
        ColorCategory.entries.forEach { category ->
            assertEquals(18, byColour[category]?.size)
        }
        // Every pooled Pokemon has a colour, and the pool matches the map exactly.
        assertTrue(pool.all { colorOf(it.id) != null })
        assertEquals(COLOR_SORT_MAP.keys, pool.map { it.id }.toSet())
    }
}
