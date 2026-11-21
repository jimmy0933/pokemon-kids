package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedGamesTest {

    @Test
    fun threeStageChainsAreValidLinearChains() {
        val chains = threeStageChains(ALL_POKEMON)
        // Dex 1-800 holds 88 strict linear 3-stage chains.
        assertEquals(88, chains.size)
        chains.forEach { chain ->
            assertEquals(3, chain.size)
            assertEquals(3, chain.map { it.id }.toSet().size)
            val (baby, middle, final) = chain
            assertTrue("baby must have no evolvesFrom", baby.evolvesFrom == null)
            assertEquals(listOf(middle.id), baby.evolvesTo)
            assertEquals(baby.id, middle.evolvesFrom)
            assertEquals(listOf(final.id), middle.evolvesTo)
            assertEquals(middle.id, final.evolvesFrom)
            assertTrue("final stage must have no further evolution", final.evolvesTo.isEmpty())
        }
        // No chain may repeat a Pokemon, and chains are distinct.
        assertEquals(88, chains.map { it.map { pokemon -> pokemon.id } }.toSet().size)
    }

    @Test
    fun evoOrderQuestionsUseValidChainsWithVariety() {
        val questions = makeEvoOrderQuestions(ALL_POKEMON, 109, seed = 21L)
        assertEquals(109, questions.size)
        questions.forEach { question ->
            assertEquals(3, question.chain.size)
            assertEquals(3, question.chain.map { it.id }.toSet().size)
            val (baby, middle, final) = question.chain
            assertEquals(listOf(middle.id), baby.evolvesTo)
            assertEquals(listOf(final.id), middle.evolvesTo)
        }
        // All 88 chains are used once before any chain repeats.
        val distinctChains = questions.map { it.chain.map { pokemon -> pokemon.id } }.toSet()
        assertEquals(88, distinctChains.size)
        // Same seed, same questions.
        assertEquals(
            questions.map { it.chain.map { pokemon -> pokemon.id } },
            makeEvoOrderQuestions(ALL_POKEMON, 109, seed = 21L)
                .map { it.chain.map { pokemon -> pokemon.id } },
        )
    }

    @Test
    fun mathQuestionsAreValidWithinTwenty() {
        val questions = makeMathQuestions(109, seed = 22L)
        assertEquals(109, questions.size)
        var additions = 0
        var subtractions = 0
        questions.forEach { question ->
            assertEquals(3, question.choices.size)
            assertEquals(3, question.choices.toSet().size)
            assertTrue("choices must include the answer", question.choices.contains(question.answer))
            assertTrue("answer must be within 0-20: ${question.answer}", question.answer in 0..20)
            question.choices.forEach { choice ->
                assertTrue("choice must be within 0-20: $choice", choice in 0..20)
            }
            if (question.isAddition) {
                additions++
                assertEquals(question.a + question.b, question.answer)
                assertTrue("addition sum must stay within 20", question.answer <= 20)
            } else {
                subtractions++
                assertEquals(question.a - question.b, question.answer)
                assertTrue("subtraction must never go negative", question.answer >= 0)
            }
        }
        // Both operations appear in a session.
        assertTrue("additions should appear", additions > 0)
        assertTrue("subtractions should appear", subtractions > 0)
        // Same seed, same questions.
        assertEquals(
            questions.map { listOf(it.a, it.b, it.answer) },
            makeMathQuestions(109, seed = 22L).map { listOf(it.a, it.b, it.answer) },
        )
    }

    @Test
    fun oddOneOutQuestionsHaveExactlyThreeSharingOneType() {
        val questions = makeOddOneOutQuestions(ALL_POKEMON, 109, seed = 23L)
        assertEquals(109, questions.size)
        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(4, question.options.map { it.id }.toSet().size)
            assertTrue("options must include the odd one", question.options.contains(question.oddOne))
            val sharing = question.options.filter { it.id != question.oddOne.id }
            assertEquals(3, sharing.size)
            assertTrue("the three sharing Pokemon must all have the shared type",
                sharing.all { it.type == question.sharedType })
            assertTrue("the odd one must not have the shared type",
                question.oddOne.type != question.sharedType)
        }
        // Same seed, same questions.
        assertEquals(
            questions.map { it.oddOne.id to it.options.map { pokemon -> pokemon.id } },
            makeOddOneOutQuestions(ALL_POKEMON, 109, seed = 23L)
                .map { it.oddOne.id to it.options.map { pokemon -> pokemon.id } },
        )
    }

    @Test
    fun memoryBoardsHaveSixPairsEach() {
        val boards = makeMemoryBoards(ALL_POKEMON, 10, seed = 24L)
        assertEquals(10, boards.size)
        boards.forEach { board ->
            assertEquals(12, board.size)
            val counts = board.groupingBy { it.id }.eachCount()
            assertEquals(6, counts.size)
            assertTrue("each Pokemon must appear exactly twice on a board",
                counts.values.all { it == 2 })
        }
        // Same seed, same boards.
        assertEquals(
            boards.map { it.map { pokemon -> pokemon.id } },
            makeMemoryBoards(ALL_POKEMON, 10, seed = 24L)
                .map { it.map { pokemon -> pokemon.id } },
        )
    }

    @Test
    fun advancedGamesSessionCounts() {
        // v2.7: the three advanced quiz games run 109 questions per session.
        assertEquals(109, EVO_ORDER_QUESTION_COUNT)
        assertEquals(109, MATH_QUESTION_COUNT)
        assertEquals(109, ODD_ONE_OUT_QUESTION_COUNT)
        // Memory is 10 boards per session: one board takes much longer than
        // a single quiz question, so it does NOT use 109.
        assertEquals(10, MEMORY_BOARD_COUNT)
        assertEquals(6, MEMORY_PAIRS_PER_BOARD)
    }
}
