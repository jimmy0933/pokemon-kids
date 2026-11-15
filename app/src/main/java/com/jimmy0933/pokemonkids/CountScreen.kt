package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

const val COUNT_QUESTION_COUNT = 20

/** One count question: [shown] Pokemon pictures to count. */
data class CountQuestion(val shown: List<Pokemon>, val count: Int)

/** Count-the-Pokemon: 1-5 Pokemon pictures are shown and the kid taps the
 * matching big number button. 20 questions per session, no timer, no
 * penalty — a wrong tap just says "再試一次" and lets them try again. A
 * correct answer collects one of the shown Pokemon (an uncollected one if
 * any is still uncollected). */
@Composable
fun CountScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        val random = kotlin.random.Random(sessionSeed)
        List(COUNT_QUESTION_COUNT) {
            val count = random.nextInt(1, 6)
            CountQuestion(ALL_POKEMON.shuffled(random).take(count), count)
        }
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var wrongNumbers by remember(sessionSeed) { mutableStateOf(setOf<Int>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val question = questions.getOrNull(index)

    // Say the instruction out loud whenever a new question appears.
    LaunchedEffect(question) {
        question?.let { onSpeak("數一數，有幾隻寶可夢？") }
    }
    // After a correct answer, move on automatically.
    LaunchedEffect(answered) {
        if (answered) {
            delay(1800)
            if (index + 1 < questions.size) {
                index++
                wrongNumbers = emptySet()
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
            text = "數一數",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${index + 1} / ${questions.size} 題",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = if (answered) "答對了！有 ${question?.count} 隻！🎉" else if (wrongNumbers.isNotEmpty()) "再試一次，加油！" else "數一數，有幾隻寶可夢？",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (question != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(question.shown) { pokemon ->
                    Card(
                        modifier = Modifier.aspectRatio(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (answered) Color(0xFFC8E6C9) else Color(0xFFFFF59D),
                        ),
                    ) {
                        Image(
                            painter = painterResource(drawableId(context, pokemon.drawable)),
                            contentDescription = if (answered) pokemon.name else "寶可夢",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                (1..5).forEach { number ->
                    val isWrong = number in wrongNumbers
                    val isAnswer = answered && number == question.count
                    Button(
                        onClick = {
                            if (answered || isWrong) return@Button
                            if (number == question.count) {
                                sounds.match()
                                onSpeak("答對了！有 $number 隻！")
                                val collected = question.shown.firstOrNull { it.id !in collectedIds }
                                    ?: question.shown.first()
                                onCollect(collected.id)
                                if (collected.id !in collectedIds) newCollected++
                                answered = true
                            } else {
                                sounds.flip()
                                onSpeak("再試一次")
                                wrongNumbers = wrongNumbers + number
                            }
                        },
                        modifier = Modifier.weight(1f).height(88.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = when {
                                isAnswer -> Color(0xFFC8E6C9)
                                isWrong -> Color(0xFFEEEEEE)
                                else -> Color(0xFFE3F2FD)
                            },
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("$number", fontSize = 32.sp, fontWeight = FontWeight.Bold)
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
            title = { Text("🎉 數完 ${questions.size} 題了！", fontSize = 26.sp) },
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
