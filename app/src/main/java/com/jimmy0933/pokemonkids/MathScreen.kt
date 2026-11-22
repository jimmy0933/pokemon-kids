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
import androidx.compose.foundation.layout.size
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

const val MATH_QUESTION_COUNT = 109

/** One math question: a + b or a − b, with [answer] and 3 distinct choices
 * (the answer plus two near misses). Answers are always within 0-20 and
 * subtraction never goes negative. */
data class MathQuestion(
    val a: Int,
    val b: Int,
    val isAddition: Boolean,
    val answer: Int,
    val choices: List<Int>,
)

/** Build [count] math questions. Addition sums stay ≤ 20, subtraction never
 * goes negative, and the two wrong choices are near the answer (never the
 * answer, never duplicates, always 0-20). Pure function, unit-tested. */
fun makeMathQuestions(count: Int, seed: Long): List<MathQuestion> {
    val random = kotlin.random.Random(seed)
    return List(count) {
        val isAddition = random.nextBoolean()
        val (a, b) = if (isAddition) {
            // a in 1..19, b in 1..(20-a) so the sum is at most 20.
            val x = random.nextInt(1, 20)
            val y = random.nextInt(1, 21 - x)
            x to y
        } else {
            // a in 2..20, b in 1..(a-1) so the answer is at least 1.
            val x = random.nextInt(2, 21)
            val y = random.nextInt(1, x)
            x to y
        }
        val answer = if (isAddition) a + b else a - b
        val wrongs = mutableSetOf<Int>()
        val nearMisses = listOf(answer - 2, answer - 1, answer + 1, answer + 2, answer + 3, answer - 3)
            .shuffled(random)
        for (candidate in nearMisses) {
            if (wrongs.size == 2) break
            if (candidate in 0..20 && candidate != answer) wrongs.add(candidate)
        }
        // Fallback for edge answers where near misses run out.
        var probe = 0
        while (wrongs.size < 2) {
            if (probe in 0..20 && probe != answer) wrongs.add(probe)
            probe++
        }
        MathQuestion(a, b, isAddition, answer, (wrongs + answer).shuffled(random))
    }
}

/** Simple addition and subtraction for 8-year-olds: a big equation in digits
 * (numbers within 20), with dot counters to help count, and 3 big number
 * buttons as choices. A wrong tap just says "再試一次" with no penalty. 109
 * questions per session, no timer; a correct answer just finishes the
 * question. The helper Pokemon is shown with the question. */
@Composable
fun MathScreen(
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        makeMathQuestions(MATH_QUESTION_COUNT, sessionSeed)
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var wrongChoices by remember(sessionSeed) { mutableStateOf(setOf<Int>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val question = questions.getOrNull(index)
    // One helper Pokemon per question, shown with the equation.
    val mascot = remember(sessionSeed, index) {
        ALL_POKEMON.shuffled(kotlin.random.Random(sessionSeed + index)).first()
    }
    val operator = if (question?.isAddition == true) "+" else "−"
    val operatorWord = if (question?.isAddition == true) "加" else "減"

    // Say the equation out loud whenever a new question appears.
    LaunchedEffect(question) {
        question?.let { onSpeak("${it.a} $operatorWord ${it.b} 等於多少？") }
    }
    // After a correct answer, move on automatically.
    LaunchedEffect(answered) {
        if (answered) {
            delay(1800)
            if (index + 1 < questions.size) {
                index++
                wrongChoices = emptySet()
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
            text = "加減法 ★進階",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${index + 1} / ${questions.size} 題",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        if (question != null) {
            Image(
                painter = painterResource(drawableId(context, mascot.drawable)),
                contentDescription = mascot.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(140.dp),
            )
            Text(
                text = "小幫手：${mascot.name}",
                fontSize = 22.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            )
            Text(
                text = "${question.a} $operator ${question.b} = ${if (answered) question.answer else "？"}",
                fontSize = 64.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            // Dot counters: count the dots to work out the answer.
            Text(
                text = "●".repeat(question.a),
                fontSize = 24.sp, textAlign = TextAlign.Center, color = Color(0xFF42A5F5),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "$operator " + "●".repeat(question.b),
                fontSize = 24.sp, textAlign = TextAlign.Center,
                color = if (question.isAddition) Color(0xFF66BB6A) else Color(0xFF9E9E9E),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Text(
                text = if (answered) "答對了！${question.a} $operator ${question.b} = ${question.answer}！🎉"
                    else if (wrongChoices.isNotEmpty()) "再試一次，加油！"
                    else "點一點正確的答案！",
                fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                question.choices.forEach { choice ->
                    val isWrong = choice in wrongChoices
                    val isAnswer = answered && choice == question.answer
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
                            if (choice == question.answer) {
                                sounds.match()
                                sounds.celebrate()
                                onSpeak("答對了！${question.a} $operatorWord ${question.b} 等於 ${question.answer}！")
                                answered = true
                            } else {
                                sounds.flip()
                                onSpeak("再試一次")
                                wrongChoices = wrongChoices + choice
                            }
                        },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = "$choice",
                                fontSize = 48.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
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
            title = { Text("🎉 算完 ${questions.size} 題了！", fontSize = 26.sp) },
            text = { Text("完成了！好棒！", fontSize = 20.sp) },
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
