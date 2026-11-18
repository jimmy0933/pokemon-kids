package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

const val CATCH_COUNT = 109
const val CATCH_HOLE_COUNT = 6

/** Pick the next hole (0 until [holeCount]) for a Pokemon to pop out of,
 * never the same hole twice in a row. Pure function, unit-tested. */
fun nextCatchHole(
    holeCount: Int,
    previousHole: Int?,
    random: kotlin.random.Random,
): Int {
    if (holeCount <= 1) return 0
    var hole = random.nextInt(holeCount)
    while (hole == previousHole) {
        hole = random.nextInt(holeCount)
    }
    return hole
}

/** Pick the next Pokemon to catch: uncollected ones first, never one already
 * caught this session. Pure function, unit-tested. */
fun nextCatchPokemon(
    pool: List<Pokemon>,
    collectedIds: Set<Int>,
    caughtIds: Set<Int>,
    random: kotlin.random.Random,
): Pokemon? {
    val remaining = pool.filter { it.id !in caughtIds }
    if (remaining.isEmpty()) return null
    val uncollected = remaining.filter { it.id !in collectedIds }.shuffled(random)
    val collected = remaining.filter { it.id in collectedIds }.shuffled(random)
    return (uncollected + collected).first()
}

/** Catch the Pokemon: a Pokemon pops up out of one of 6 bushes and the kid
 * taps it to catch it and collect it. No timer, no penalty — the Pokemon
 * waits until it is tapped, and tapping an empty bush just makes it hop to
 * another bush. 109 catches per session. */
@Composable
fun CatchScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val targets = remember(sessionSeed) {
        val random = kotlin.random.Random(sessionSeed)
        val uncollected = ALL_POKEMON.filter { it.id !in collectedIds }.shuffled(random)
        val collected = ALL_POKEMON.filter { it.id in collectedIds }.shuffled(random)
        (uncollected + collected).take(CATCH_COUNT)
    }
    var caughtCount by remember(sessionSeed) { mutableStateOf(0) }
    var hole by remember(sessionSeed) {
        mutableStateOf(nextCatchHole(CATCH_HOLE_COUNT, null, kotlin.random.Random(sessionSeed)))
    }
    var justCaught by remember(sessionSeed) { mutableStateOf(false) }
    var hidMessage by remember(sessionSeed) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val pokemon = targets.getOrNull(caughtCount)

    // After a catch, pause for the praise, then pop the next Pokemon up.
    LaunchedEffect(justCaught) {
        if (justCaught) {
            delay(1800)
            if (caughtCount + 1 < targets.size) {
                caughtCount++
                hole = nextCatchHole(CATCH_HOLE_COUNT, hole, kotlin.random.Random(System.nanoTime()))
                justCaught = false
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
            text = "抓寶可夢",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "抓到了 ${caughtCount} / ${targets.size} 隻",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = when {
                justCaught -> "抓到了！是${pokemon?.name}！🎉"
                hidMessage -> "躲起來了！再找找！"
                else -> "點一下跑出來的寶可夢，把它抓住！"
            },
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (pokemon != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(CATCH_HOLE_COUNT) { index ->
                    val isPopUp = index == hole && !justCaught
                    Card(
                        modifier = Modifier.aspectRatio(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isPopUp -> Color(0xFFC8E6C9)
                                justCaught && index == hole -> Color(0xFFC8E6C9)
                                else -> Color(0xFFDCEDC8)
                            },
                        ),
                        onClick = {
                            if (justCaught) return@Card
                            if (index == hole) {
                                sounds.match()
                                onSpeak("抓到了！${pokemon.name}")
                                onCollect(pokemon.id)
                                if (pokemon.id !in collectedIds) newCollected++
                                justCaught = true
                                hidMessage = false
                            } else {
                                // Empty bush: the Pokemon hops to another bush.
                                sounds.flip()
                                onSpeak("躲起來了！再找找！")
                                hidMessage = true
                                hole = nextCatchHole(
                                    CATCH_HOLE_COUNT, hole,
                                    kotlin.random.Random(System.nanoTime()),
                                )
                            }
                        },
                    ) {
                        if (index == hole) {
                            Image(
                                painter = painterResource(drawableId(context, pokemon.drawable)),
                                contentDescription = if (justCaught) pokemon.name else "寶可夢",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                            )
                        } else {
                            Text(
                                text = "🌿",
                                fontSize = 64.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize().padding(top = 24.dp),
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
            title = { Text("🎉 抓到 ${targets.size} 隻寶可夢了！", fontSize = 26.sp) },
            text = { Text("新收集到 $newCollected 隻寶可夢，好棒！", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    sessionSeed = System.currentTimeMillis()
                    caughtCount = 0
                    showComplete = false
                }) { Text("再玩一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
