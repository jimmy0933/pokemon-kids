package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

const val SIZE_QUESTION_COUNT = 109

/** One size question: three distinct Pokemon, sorted tallest first
 * ([options] is the answer order, tallest to shortest). */
data class SizeQuestion(val options: List<Pokemon>)

/** Build [count] size questions from [pool]. Each question is 3 distinct
 * Pokemon with 3 different heights, sorted tallest first. Pure function,
 * unit-tested. */
fun makeSizeQuestions(
    pool: List<Pokemon>,
    count: Int,
    seed: Long,
): List<SizeQuestion> {
    val random = kotlin.random.Random(seed)
    val questions = mutableListOf<SizeQuestion>()
    var attempts = 0
    while (questions.size < count && attempts < count * 50) {
        attempts++
        val trio = pool.shuffled(random).take(3)
        if (trio.size == 3 && trio.map { it.heightM }.toSet().size == 3) {
            questions.add(SizeQuestion(trio.sortedByDescending { it.heightM }))
        }
    }
    return questions
}

/** Compare sizes: three Pokemon pictures are shown and the kid taps them in
 * order from tallest to shortest. A wrong tap just says "再試一次" with no
 * penalty. 109 questions per session, no timer; a correct full ordering
 * collects one of the shown Pokemon. */
@Composable
fun SizeScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        makeSizeQuestions(ALL_POKEMON, SIZE_QUESTION_COUNT, sessionSeed)
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var picked by remember(sessionSeed) { mutableStateOf(listOf<Pokemon>()) }
    var wrongIds by remember(sessionSeed) { mutableStateOf(setOf<Int>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val question = questions.getOrNull(index)
    // The three pictures are shown shuffled, so the kid has to work out the
    // tallest-to-shortest order instead of reading it off the screen.
    val displayOrder = remember(sessionSeed, index) {
        question?.options?.shuffled(kotlin.random.Random(sessionSeed + index)) ?: emptyList()
    }
    // The next Pokemon in the tallest-first order the kid still has to tap.
    val nextExpected = question?.options?.getOrNull(picked.size)

    // Say the instruction out loud whenever a new question appears.
    LaunchedEffect(question) {
        question?.let { onSpeak("從最高點到最矮，排排看！") }
    }
    // After a correct full ordering, move on automatically.
    LaunchedEffect(answered) {
        if (answered) {
            delay(1800)
            if (index + 1 < questions.size) {
                index++
                picked = emptyList()
                wrongIds = emptySet()
                answered = false
            } else {
                showComplete = true
            }
        }
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "比大小",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${index + 1} / ${questions.size} 題",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = when {
                answered -> "排對了！從最高到最矮！🎉"
                wrongIds.isNotEmpty() -> "再試一次，加油！"
                picked.isEmpty() -> "先點最高的那一隻！"
                else -> "再點下一隻高的！"
            },
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (question != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                displayOrder.forEach { pokemon ->
                    val pickedPosition = picked.indexOfFirst { it.id == pokemon.id }
                    val isPicked = pickedPosition >= 0
                    val isWrong = pokemon.id in wrongIds
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(0.8f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isPicked -> Color(0xFFC8E6C9)
                                isWrong -> Color(0xFFEEEEEE)
                                else -> Color(0xFFFFF59D)
                            },
                        ),
                        onClick = {
                            if (answered || isPicked || isWrong) return@Card
                            if (pokemon.id == nextExpected?.id) {
                                sounds.match()
                                onSpeak(pokemon.name)
                                val newPicked = picked + pokemon
                                picked = newPicked
                                if (newPicked.size == question.options.size) {
                                    sounds.celebrate()
                                    onSpeak("排對了！好棒！")
                                    val collected = question.options.firstOrNull { it.id !in collectedIds }
                                        ?: question.options.first()
                                    onCollect(collected.id)
                                    if (collected.id !in collectedIds) newCollected++
                                    answered = true
                                }
                            } else {
                                sounds.flip()
                                onSpeak("再試一次")
                                wrongIds = wrongIds + pokemon.id
                            }
                        },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            Image(
                                painter = painterResource(drawableId(context, pokemon.drawable)),
                                contentDescription = if (answered) pokemon.name else "寶可夢",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            Text(
                                text = when {
                                    isPicked -> "${pickedPosition + 1} ✓"
                                    answered -> "${formatMeasure(pokemon.heightM)} 公尺"
                                    else -> "？"
                                },
                                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 比完 ${questions.size} 題了！", fontSize = 26.sp) },
            text = { Text("新收集到 $newCollected 隻寶可夢，好棒！", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    sessionSeed = System.currentTimeMillis()
                    showComplete = false
                }) { Text("再玩一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
