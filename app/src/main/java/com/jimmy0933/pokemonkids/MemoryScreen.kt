package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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

const val MEMORY_BOARD_COUNT = 10
const val MEMORY_PAIRS_PER_BOARD = 6

/** Build [count] memory boards from [pool]. Each board is 6 distinct Pokemon
 * laid out as 12 cards (each Pokemon twice), shuffled. Pure function,
 * unit-tested. */
fun makeMemoryBoards(
    pool: List<Pokemon>,
    count: Int,
    seed: Long,
): List<List<Pokemon>> {
    val random = kotlin.random.Random(seed)
    return List(count) {
        val picked = pool.shuffled(random).take(MEMORY_PAIRS_PER_BOARD)
        (picked + picked).shuffled(random)
    }
}

/** Memory / concentration, for older kids: 12 cards face down (6 pairs of
 * Pokemon pictures). Tap two cards to flip them; a matching pair stays face
 * up and the Pokemon is collected, a non-matching pair flips back after a
 * short moment. No timer, no fail penalty. One board takes much longer than
 * a single quiz question, so a session is 10 boards (not 109). */
@Composable
fun MemoryScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val boards = remember(sessionSeed) {
        makeMemoryBoards(ALL_POKEMON, MEMORY_BOARD_COUNT, sessionSeed)
    }
    var boardIndex by remember(sessionSeed) { mutableStateOf(0) }
    var flipped by remember(sessionSeed, boardIndex) { mutableStateOf(listOf<Int>()) }
    var matched by remember(sessionSeed, boardIndex) { mutableStateOf(setOf<Int>()) }
    var busy by remember(sessionSeed, boardIndex) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val deck = boards.getOrNull(boardIndex) ?: emptyList()

    // Say the instruction out loud whenever a new board appears.
    LaunchedEffect(deck) {
        onSpeak("記住寶可夢的位置，翻出相同的兩張！")
    }
    // Judge a flipped pair: a match stays face up, a miss flips back.
    LaunchedEffect(flipped) {
        if (flipped.size != 2) return@LaunchedEffect
        val first = deck[flipped[0]]
        val second = deck[flipped[1]]
        if (first.id == second.id) {
            sounds.match()
            onSpeak("配對成功！${first.name}")
            val newMatched = matched + first.id
            matched = newMatched
            onCollect(first.id)
            if (first.id !in collectedIds) newCollected++
            if (newMatched.size == MEMORY_PAIRS_PER_BOARD) {
                sounds.celebrate()
                onSpeak("這一盤都找到了！好棒！")
                // Keep the last pair face up for a moment before moving on.
                delay(1500)
                flipped = emptyList()
                if (boardIndex + 1 < boards.size) {
                    boardIndex++
                } else {
                    showComplete = true
                }
            } else {
                flipped = emptyList()
            }
        } else {
            busy = true
            delay(900)
            flipped = emptyList()
            busy = false
        }
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "記憶翻牌 ★進階",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${boardIndex + 1} / ${boards.size} 盤",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        )
        Text(
            text = "已找到 ${matched.size} / $MEMORY_PAIRS_PER_BOARD 對",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            itemsIndexed(deck) { cardIndex, pokemon ->
                val isUp = cardIndex in flipped || pokemon.id in matched
                val isMatched = pokemon.id in matched
                Card(
                    modifier = Modifier.aspectRatio(0.8f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isMatched -> Color(0xFFC8E6C9)
                            isUp -> Color(0xFFFFF59D)
                            else -> Color(0xFF90CAF9)
                        },
                    ),
                    onClick = {
                        if (busy || flipped.size >= 2 || isUp) return@Card
                        sounds.flip()
                        flipped = flipped + cardIndex
                    },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                    ) {
                        if (isUp) {
                            Image(
                                painter = painterResource(drawableId(context, pokemon.drawable)),
                                contentDescription = pokemon.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            Text(
                                text = pokemon.name,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        } else {
                            Text(
                                text = "？",
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
            title = { Text("🎉 翻完 ${boards.size} 盤了！", fontSize = 26.sp) },
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
