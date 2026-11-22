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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

const val SHADOW_MATCH_QUESTION_COUNT = 109

/** Match the silhouette: one Pokemon is shown as a black silhouette on top
 * and the kid taps the matching colour picture out of 3 below. 109 questions
 * per session, no timer, no penalty — a wrong tap just says "再試一次" and
 * lets them try again. A correct tap just moves on — only the catch game
 * adds Pokemon to the Pokedex. Questions reuse [makeQuizQuestions] with 3 options (same helper
 * as the silhouette quiz); only the session count differs. */
@Composable
fun ShadowMatchScreen(
    collectedIds: Set<Int>,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        makeQuizQuestions(ALL_POKEMON, collectedIds, SHADOW_MATCH_QUESTION_COUNT, SILHOUETTE_OPTION_COUNT, sessionSeed)
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var wrongIds by remember(sessionSeed) { mutableStateOf(setOf<Int>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val question = questions.getOrNull(index)

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
            text = "影子配對",
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
                painter = painterResource(drawableId(context, question.answer.drawable)),
                contentDescription = "影子",
                contentScale = ContentScale.Fit,
                colorFilter = if (answered) null else ColorFilter.tint(Color.Black),
                modifier = Modifier.size(240.dp).padding(8.dp),
            )
            Text(
                text = if (answered) "配對成功！是${question.answer.name}！🎉" else if (wrongIds.isNotEmpty()) "再試一次，加油！" else "這個影子是哪一隻寶可夢？",
                fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                question.options.forEach { option ->
                    val isWrong = option.id in wrongIds
                    val isAnswer = answered && option.id == question.answer.id
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
                            if (option.id == question.answer.id) {
                                sounds.match()
                                sounds.celebrate()
                                onSpeak("配對成功！${option.name}")
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
                            modifier = Modifier.fillMaxSize().padding(10.dp),
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
            title = { Text("🎉 配對完 ${questions.size} 題了！", fontSize = 26.sp) },
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
