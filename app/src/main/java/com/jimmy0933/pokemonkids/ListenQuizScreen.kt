package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

/** Listen-and-guess: TTS speaks a Pokemon's name, the kid taps the right
 * picture out of 4. 10 questions per session, no timer, no penalty — a wrong
 * tap just says "再試一次" and lets them try another picture. A correct tap
 * collects the Pokemon into the same Pokedex as matching. */
@Composable
fun ListenQuizScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        makeQuizQuestions(ALL_POKEMON, collectedIds, QUIZ_QUESTION_COUNT, LISTEN_OPTION_COUNT, sessionSeed)
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var wrongIds by remember(sessionSeed) { mutableStateOf(setOf<Int>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val question = questions.getOrNull(index)

    // Say the name out loud whenever a new question appears.
    LaunchedEffect(question) {
        question?.let { onSpeak(it.answer.name) }
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "聽音猜",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${index + 1} / ${questions.size} 題",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Button(
            onClick = { question?.let { onSpeak(it.answer.name) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔊 再聽一次", fontSize = 28.sp) }
        Text(
            text = if (answered) "答對了！🎉" else if (wrongIds.isNotEmpty()) "再試一次，加油！" else "聽聽看，是哪一隻寶可夢？",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (question != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(question.options) { option ->
                    val isWrong = option.id in wrongIds
                    val isAnswer = answered && option.id == question.answer.id
                    Card(
                        modifier = Modifier.aspectRatio(1f),
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
                            if (option.id == question.answer.id) {
                                sounds.match()
                                onSpeak("答對了！${option.name}")
                                onCollect(option.id)
                                if (option.id !in collectedIds) newCollected++
                                answered = true
                            } else {
                                sounds.flip()
                                onSpeak("再試一次")
                                wrongIds = wrongIds + option.id
                            }
                        },
                    ) {
                        Image(
                            painter = painterResource(drawableId(context, option.drawable)),
                            contentDescription = if (answered) option.name else "選項",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                        )
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
            title = { Text("🎉 答完 ${questions.size} 題了！", fontSize = 26.sp) },
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
