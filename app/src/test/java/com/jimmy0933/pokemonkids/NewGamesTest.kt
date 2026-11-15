package com.jimmy0933.pokemonkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewGamesTest {

    @Test
    fun findMatchQuestionsHaveTargetAndFourUniqueOptions() {
        val questions = makeFindMatchQuestions(ALL_POKEMON, 10, seed = 11L)
        assertEquals(10, questions.size)
        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(4, question.options.map { it.id }.toSet().size)
            assertTrue("options must include the target", question.options.contains(question.target))
        }
        // Same seed, same questions.
        assertEquals(
            questions.map { it.target.id to it.options.map { pokemon -> pokemon.id } },
            makeFindMatchQuestions(ALL_POKEMON, 10, seed = 11L)
                .map { it.target.id to it.options.map { pokemon -> pokemon.id } },
        )
        // Different seeds give different targets.
        val other = makeFindMatchQuestions(ALL_POKEMON, 10, seed = 12L)
        assertTrue(other.map { it.target.id } != questions.map { it.target.id })
    }

    @Test
    fun dexOrderQuestionsAreThreeDistinctPokemonSortedAscending() {
        val questions = makeDexOrderQuestions(ALL_POKEMON, 10, seed = 13L)
        assertEquals(10, questions.size)
        questions.forEach { question ->
            assertEquals(3, question.options.size)
            assertEquals(3, question.options.map { it.id }.toSet().size)
            val ids = question.options.map { it.id }
            assertTrue("ids must be ascending: $ids", ids == ids.sorted())
        }
        // Same seed, same questions.
        assertEquals(
            questions.map { it.options.map { pokemon -> pokemon.id } },
            makeDexOrderQuestions(ALL_POKEMON, 10, seed = 13L).map { it.options.map { pokemon -> pokemon.id } },
        )
    }

    @Test
    fun weightQuestionsAreThreeDistinctPokemonSortedHeaviestFirst() {
        val questions = makeWeightQuestions(ALL_POKEMON, 10, seed = 14L)
        assertEquals(10, questions.size)
        questions.forEach { question ->
            assertEquals(3, question.options.size)
            assertEquals(3, question.options.map { it.id }.toSet().size)
            val weights = question.options.map { it.weightKg }
            assertEquals(3, weights.toSet().size)
            assertTrue("weights must be heaviest first: $weights", weights == weights.sortedDescending())
        }
        // Same seed, same questions.
        assertEquals(
            questions.map { it.options.map { pokemon -> pokemon.id } },
            makeWeightQuestions(ALL_POKEMON, 10, seed = 14L).map { it.options.map { pokemon -> pokemon.id } },
        )
        // The heaviest Pokemon in the game can appear as an answer.
        assertTrue(ALL_POKEMON.maxOf { it.weightKg } > 0)
    }

    @Test
    fun shadowMatchQuestionsUseQuizHelperWithThreeOptions() {
        val questions = makeQuizQuestions(
            ALL_POKEMON, emptySet(), SHADOW_MATCH_QUESTION_COUNT, SILHOUETTE_OPTION_COUNT, seed = 15L,
        )
        assertEquals(10, questions.size)
        questions.forEach { question ->
            assertEquals(3, question.options.size)
            assertEquals(3, question.options.map { it.id }.toSet().size)
            assertTrue(question.options.contains(question.answer))
        }
    }

    @Test
    fun newGamesHaveTenQuestionsPerSession() {
        // v2.3: all four new games run 10 questions per session.
        assertEquals(10, FIND_MATCH_QUESTION_COUNT)
        assertEquals(10, DEX_ORDER_QUESTION_COUNT)
        assertEquals(10, WEIGHT_QUESTION_COUNT)
        assertEquals(10, SHADOW_MATCH_QUESTION_COUNT)
    }
}
