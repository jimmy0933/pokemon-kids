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

const val ODD_ONE_OUT_QUESTION_COUNT = 109

/** One odd-one-out question: [options] are 4 Pokemon, 3 sharing [sharedType]
 * and [oddOne] with a different type. */
data class OddOneOutQuestion(
    val options: List<Pokemon>,
    val sharedType: String,
    val oddOne: Pokemon,
)

/** Build [count] odd-one-out questions from [pool]. Three Pokemon share one
 * type; the odd one has a different type — it never has the shared type, so
 * there is exactly one right answer. Pure function, unit-tested. */
fun makeOddOneOutQuestions(
    pool: List<Pokemon>,
    count: Int,
    seed: Long,
): List<OddOneOutQuestion> {
    val random = kotlin.random.Random(seed)
    val byType = pool.groupBy { it.type }
    val usableTypes = byType.filter { it.value.size >= 3 }.keys.toList()
    val questions = mutableListOf<OddOneOutQuestion>()
    var attempts = 0
    while (questions.size < count && attempts < count * 50) {
        attempts++
        val sharedType = usableTypes.random(random)
        val sharing = byType.getValue(sharedType).shuffled(random).take(3)
        val odd = pool.filter { it.type != sharedType }.shuffled(random).firstOrNull()
            ?: continue
        if (sharing.size == 3) {
            questions.add(OddOneOutQuestion((sharing + odd).shuffled(random), sharedType, odd))
        }
    }
    return questions
}

/** Odd one out, for older kids: four Pokemon are shown, three share one type
 * and one does not — tap the odd one out. A wrong tap just says "再試一次"
 * with no penalty. 109 questions per session, no timer; a correct tap
 * collects the odd one out. After answering, each picture shows its type so
 * the kid learns the types. */
@Composable
fun OddOneOutScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        makeOddOneOutQuestions(ALL_POKEMON, ODD_ONE_OUT_QUESTION_COUNT, sessionSeed)
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var wrongIds by remember(sessionSeed) { mutableStateOf(setOf<Int>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val question = questions.getOrNull(index)
    // Two rows of two cards, in the same order for the whole question.
    val displayOrder = remember(sessionSeed, index) {
        question?.options?.shuffled(kotlin.random.Random(sessionSeed + index)) ?: emptyList()
    }
    val oddOne = question?.oddOne

    // Say the instruction out loud whenever a new question appears.
    LaunchedEffect(question) {
        question?.let { onSpeak("哪一隻和別人不一樣？") }
    }
    // After a correct answer, move on automatically.
    LaunchedEffect(answered) {
        if (answered) {
            delay(1800)
            if (index + 1 < questions.size) {
                index++
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
            text = "找不同 ★進階",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${index + 1} / ${questions.size} 題",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = if (answered) "答對了！${oddOne?.name}是${oddOne?.type}屬性，和其他的不一樣！🎉"
                else if (wrongIds.isNotEmpty()) "再試一次，加油！"
                else "點一點和別人不一樣的那一隻！",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (question != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                displayOrder.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        row.forEach { pokemon ->
                            val isWrong = pokemon.id in wrongIds
                            val isAnswer = answered && pokemon.id == oddOne?.id
                            Card(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isAnswer -> Color(0xFFC8E6C9)
                                        isWrong -> Color(0xFFEEEEEE)
                                        else -> Color(0xFFFFF59D)
                                    },
                                ),
                                onClick = {
                                    if (answered || isWrong) return@Card
                                    if (pokemon.id == oddOne?.id) {
                                        sounds.match()
                                        sounds.celebrate()
                                        onSpeak("答對了！${pokemon.name}是${pokemon.type}屬性，和其他的不一樣！")
                                        onCollect(pokemon.id)
                                        if (pokemon.id !in collectedIds) newCollected++
                                        answered = true
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
                                        text = if (answered) "${pokemon.type}屬性" else pokemon.name,
                                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    )
                                }
                            }
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
            title = { Text("🎉 找完 ${questions.size} 題了！", fontSize = 26.sp) },
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
