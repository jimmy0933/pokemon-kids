package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonDataTest {

    @Test
    fun eightHundredPokemonWithUniqueIdsAndNames() {
        assertEquals(800, ALL_POKEMON.size)
        assertEquals(800, ALL_POKEMON.map { it.id }.toSet().size)
        assertEquals(800, ALL_POKEMON.map { it.name }.toSet().size)
        assertEquals((1..800).toSet(), ALL_POKEMON.map { it.id }.toSet())
        assertTrue(ALL_POKEMON.all { it.name.isNotBlank() && it.type.isNotBlank() })
    }

    @Test
    fun hundredTwentyRoundsWithReviewRounds() {
        val rs = rounds()
        assertEquals(TOTAL_ROUNDS, rs.size)
        assertEquals(120, TOTAL_ROUNDS)
        assertTrue(rs.all { it.size == ROUND_SIZE })
        // Rounds 1-100 are strictly Dex-ordered: round 1 = 1-8 ... round 100 = 793-800.
        assertEquals((1..800).toList(), rs.take(100).flatten().map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), rounds()[0].map { it.id })
        assertEquals(listOf(9, 10, 11, 12, 13, 14, 15, 16), rounds()[1].map { it.id })
        assertEquals(listOf(145, 146, 147, 148, 149, 150, 151, 152), rounds()[18].map { it.id })
        assertEquals(listOf(393, 394, 395, 396, 397, 398, 399, 400), rounds()[49].map { it.id })
        assertEquals(listOf(793, 794, 795, 796, 797, 798, 799, 800), rounds()[99].map { it.id })
        // Rounds 101-120 are review rounds replaying Dex 1-160 in order.
        assertEquals((1..160).toList(), rs.drop(100).flatten().map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), rounds()[100].map { it.id })
        assertEquals(listOf(153, 154, 155, 156, 157, 158, 159, 160), rounds()[119].map { it.id })
    }

    @Test
    fun roundSizeMatchesRoundIndex() {
        assertEquals(8, roundSize(0))
        assertEquals(8, roundSize(19))
        assertEquals(8, roundSize(20))
        assertEquals(8, roundSize(49))
        assertEquals(8, roundSize(99))
        assertEquals(8, roundSize(100))
        assertEquals(8, roundSize(119))
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
        val lateRound = rounds()[99]
        assertTrue(isRoundComplete(lateRound.map { it.id }.toSet(), 99))
        assertTrue(!isRoundComplete(lateRound.take(7).map { it.id }.toSet(), 99))
        // A review round (101st) completes the same way, with Dex 1-8 again.
        val reviewRound = rounds()[100]
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), reviewRound.map { it.id })
        assertTrue(isRoundComplete(reviewRound.map { it.id }.toSet(), 100))
        assertTrue(!isRoundComplete(reviewRound.take(7).map { it.id }.toSet(), 100))
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
        // Drawable resources are bundled as pkm_001.png ... pkm_800.png; the name
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
        // Eevee's branched family: Eevee + 8 evolutions within Dex 1-800
        // (Gen 4 adds Leafeon 470 and Glaceon 471, Gen 6 adds Sylveon 700).
        val eevee = ALL_POKEMON.first { it.id == 133 }
        assertEquals(
            listOf(133, 134, 135, 136, 196, 197, 470, 471, 700),
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
        assertEquals(107, regionPokemon(Region.SINNOH).size)
        assertEquals(156, regionPokemon(Region.UNOVA).size)
        assertEquals(72, regionPokemon(Region.KALOS).size)
        assertEquals(79, regionPokemon(Region.ALOLA).size)
        assertEquals(Region.KANTO, regionOf(ALL_POKEMON.first { it.id == 1 }))
        assertEquals(Region.KANTO, regionOf(ALL_POKEMON.first { it.id == 151 }))
        assertEquals(Region.JOHTO, regionOf(ALL_POKEMON.first { it.id == 152 }))
        assertEquals(Region.JOHTO, regionOf(ALL_POKEMON.first { it.id == 251 }))
        assertEquals(Region.HOENN, regionOf(ALL_POKEMON.first { it.id == 252 }))
        assertEquals(Region.HOENN, regionOf(ALL_POKEMON.first { it.id == 386 }))
        assertEquals(Region.SINNOH, regionOf(ALL_POKEMON.first { it.id == 387 }))
        assertEquals(Region.SINNOH, regionOf(ALL_POKEMON.first { it.id == 493 }))
        assertEquals(Region.UNOVA, regionOf(ALL_POKEMON.first { it.id == 494 }))
        assertEquals(Region.UNOVA, regionOf(ALL_POKEMON.first { it.id == 649 }))
        assertEquals(Region.KALOS, regionOf(ALL_POKEMON.first { it.id == 650 }))
        assertEquals(Region.KALOS, regionOf(ALL_POKEMON.first { it.id == 721 }))
        assertEquals(Region.ALOLA, regionOf(ALL_POKEMON.first { it.id == 722 }))
        assertEquals(Region.ALOLA, regionOf(ALL_POKEMON.first { it.id == 800 }))
        assertEquals(800, Region.entries.sumOf { regionPokemon(it).size })
    }

    @Test
    fun quizQuestionsIncludeAnswerAndUniqueOptions() {
        val questions = makeQuizQuestions(ALL_POKEMON, emptySet(), 20, 4, seed = 7L)
        assertEquals(20, questions.size)
        assertEquals(20, questions.map { it.answer.id }.toSet().size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.options.contains(q.answer))
            assertEquals(4, q.options.map { it.id }.toSet().size)
        }
        // Silhouette mode uses 3 options.
        val silhouette = makeQuizQuestions(ALL_POKEMON, emptySet(), 20, 3, seed = 8L)
        assertTrue(silhouette.all { it.options.size == 3 && it.options.contains(it.answer) })
    }

    @Test
    fun quizQuestionsPreferUncollectedPokemon() {
        // With only 5 uncollected, the first 5 answers must be exactly those 5.
        val collected = (ALL_POKEMON.map { it.id } - setOf(1, 4, 7, 25, 133)).toSet()
        val questions = makeQuizQuestions(ALL_POKEMON, collected, 20, 4, seed = 9L)
        assertEquals(setOf(1, 4, 7, 25, 133), questions.take(5).map { it.answer.id }.toSet())
        // With everything collected, it still produces questions.
        val allCollected = makeQuizQuestions(ALL_POKEMON, ALL_POKEMON.map { it.id }.toSet(), 20, 4, seed = 10L)
        assertEquals(20, allCollected.size)
    }

    @Test
    fun sessionCountsAreOneHundredNineForV26() {
        // v2.6 raises every per-session count to 109 (matching stays 120 rounds,
        // already above 109 — it is not cut down).
        assertEquals(109, QUIZ_QUESTION_COUNT)
        assertEquals(109, PUZZLE_COUNT)
        assertEquals(109, FIND_IT_QUESTION_COUNT)
        assertEquals(109, COUNT_QUESTION_COUNT)
        assertEquals(109, COLOR_SORT_QUESTION_COUNT)
        assertEquals(109, MAZE_COUNT)
        assertEquals(109, CATCH_COUNT)
        assertEquals(109, SIZE_QUESTION_COUNT)
        // v2.3 games: 109 questions per session.
        assertEquals(109, FIND_MATCH_QUESTION_COUNT)
        assertEquals(109, DEX_ORDER_QUESTION_COUNT)
        assertEquals(109, WEIGHT_QUESTION_COUNT)
        assertEquals(109, SHADOW_MATCH_QUESTION_COUNT)
        // Matching is untouched at 120 rounds.
        assertEquals(120, TOTAL_ROUNDS)
    }

    @Test
    fun mazesAreAlwaysSolvable() {
        for (seed in 0L until 50L) {
            val maze = makeMaze(seed)
            assertEquals(MAZE_SIZE, maze.size)
            assertTrue("maze $seed: start must not be a wall", maze.start !in maze.walls)
            assertTrue("maze $seed: goal must not be a wall", maze.goal !in maze.walls)
            assertEquals("maze $seed: start must be the top-left corner", 0 to 0, maze.start)
            assertEquals(
                "maze $seed: goal must be the bottom-right corner",
                (MAZE_SIZE - 1) to (MAZE_SIZE - 1), maze.goal,
            )
            assertTrue("maze $seed must be solvable", isMazeSolvable(maze))
            val path = findMazePath(maze)
            assertNotNull("maze $seed must have a path", path)
            val found = path!!
            assertEquals(maze.start, found.first())
            assertEquals(maze.goal, found.last())
            // Every step moves one cell, and no step goes through a wall.
            found.zipWithNext().forEach { (a, b) ->
                val distance = kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)
                assertEquals("maze $seed: steps must be adjacent", 1, distance)
            }
            assertTrue(found.all { it !in maze.walls })
        }
    }

    @Test
    fun catchHoleNeverRepeatsTwiceInARow() {
        var previous: Int? = null
        val random = kotlin.random.Random(42L)
        repeat(200) {
            val hole = nextCatchHole(CATCH_HOLE_COUNT, previous, random)
            assertTrue("hole $hole must be in range", hole in 0 until CATCH_HOLE_COUNT)
            assertTrue("hole must not repeat twice in a row", hole != previous)
            previous = hole
        }
        // A single hole has nowhere else to go.
        assertEquals(0, nextCatchHole(1, 0, kotlin.random.Random(1L)))
    }

    @Test
    fun nextCatchPokemonPrefersUncollectedAndAvoidsCaught() {
        val random = kotlin.random.Random(3L)
        // Nothing caught yet: any Pokemon may come up.
        val first = nextCatchPokemon(ALL_POKEMON, emptySet(), emptySet(), random)
        assertNotNull(first)
        // With only one Pokemon left uncaught, it must be that one.
        val almostAllCaught = (ALL_POKEMON.map { it.id } - 25).toSet()
        val last = nextCatchPokemon(ALL_POKEMON, emptySet(), almostAllCaught, random)
        assertEquals(25, last?.id)
        // With everything caught, there is nothing left to pop up.
        assertNull(nextCatchPokemon(ALL_POKEMON, emptySet(), ALL_POKEMON.map { it.id }.toSet(), random))
    }

    @Test
    fun sizeQuestionsAreThreeDistinctPokemonSortedTallestFirst() {
        val questions = makeSizeQuestions(ALL_POKEMON, 10, seed = 42L)
        assertEquals(10, questions.size)
        questions.forEach { question ->
            assertEquals(3, question.options.size)
            assertEquals(3, question.options.map { it.id }.toSet().size)
            val heights = question.options.map { it.heightM }
            assertEquals(3, heights.toSet().size)
            assertTrue("heights must be tallest first: $heights", heights == heights.sortedDescending())
        }
        // Same seed, same questions.
        assertEquals(
            questions.map { it.options.map { pokemon -> pokemon.id } },
            makeSizeQuestions(ALL_POKEMON, 10, seed = 42L).map { it.options.map { pokemon -> pokemon.id } },
        )
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

    @Test
    fun colorSortQuestionsCycleThroughPoolToReach109() {
        // The curated pool has only 72 entries, but a session is 109 questions:
        // the pool is reshuffled and cycled, never silently capped at 72.
        val questions = colorSortQuestions(109, seed = 7L)
        assertEquals(109, questions.size)
        assertTrue(questions.all { colorOf(it.id) != null })
        // Each full pass is a shuffle with no repeat inside it: the first 72
        // are all distinct, and the remaining 37 are all distinct too.
        assertEquals(72, questions.take(72).map { it.id }.toSet().size)
        assertEquals(37, questions.drop(72).map { it.id }.toSet().size)
        // Same seed, same questions.
        assertEquals(
            questions.map { it.id },
            colorSortQuestions(109, seed = 7L).map { it.id },
        )
        // A short session still works without cycling.
        assertEquals(72, colorSortQuestions(72, seed = 7L).map { it.id }.toSet().size)
    }
}
