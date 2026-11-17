package com.jimmy0933.pokemonkids

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val PUZZLE_COUNT = 30

/** Shuffle the 4 pieces (0..3) so the picture never starts out solved. */
private fun shuffledPieceOrder(seed: Long): List<Int> {
    val random = kotlin.random.Random(seed)
    var order = listOf(0, 1, 2, 3)
    while (order == listOf(0, 1, 2, 3)) {
        order = order.shuffled(random)
    }
    return order
}

/** Picture puzzle: one Pokemon picture is split into 4 pieces (2x2) and
 * shuffled. The kid taps two pieces to swap them; when the picture is whole
 * again, the Pokemon is collected. One puzzle at a time, 30 puzzles per
 * session, no timer, no penalty. */
@Composable
fun PuzzleScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val puzzles = remember(sessionSeed) {
        val random = kotlin.random.Random(sessionSeed)
        val uncollected = ALL_POKEMON.filter { it.id !in collectedIds }.shuffled(random)
        val collected = ALL_POKEMON.filter { it.id in collectedIds }.shuffled(random)
        (uncollected + collected).take(PUZZLE_COUNT)
    }
    var puzzleIndex by remember(sessionSeed) { mutableStateOf(0) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val pokemon = puzzles.getOrNull(puzzleIndex)

    // Per-puzzle state, keyed so a new puzzle (or a new session) resets it.
    val puzzleKey = "$sessionSeed-$puzzleIndex"
    var order by remember(puzzleKey) { mutableStateOf(shuffledPieceOrder(puzzleKey.hashCode().toLong())) }
    var selectedPos by remember(puzzleKey) { mutableStateOf<Int?>(null) }
    var solved by remember(puzzleKey) { mutableStateOf(false) }

    fun nextPuzzle() {
        if (puzzleIndex + 1 < puzzles.size) {
            puzzleIndex++
        } else {
            showComplete = true
        }
    }

    val context = LocalContext.current
    // Split the artwork into 4 quadrants (2x2). Null only if the drawable
    // cannot be decoded, in which case the puzzle is skipped gracefully.
    val pieces: List<ImageBitmap>? = remember(pokemon) {
        val target = pokemon ?: return@remember null
        val source = try {
            BitmapFactory.decodeResource(context.resources, drawableId(context, target.drawable))
        } catch (_: Exception) {
            null
        }
        source?.let {
            val halfW = it.width / 2
            val halfH = it.height / 2
            listOf(
                Bitmap.createBitmap(it, 0, 0, halfW, halfH).asImageBitmap(),
                Bitmap.createBitmap(it, halfW, 0, it.width - halfW, halfH).asImageBitmap(),
                Bitmap.createBitmap(it, 0, halfH, halfW, it.height - halfH).asImageBitmap(),
                Bitmap.createBitmap(it, halfW, halfH, it.width - halfW, it.height - halfH).asImageBitmap(),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "拼圖",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${puzzleIndex + 1} / ${puzzles.size} 張",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = if (solved) "拼好了！是${pokemon?.name}！🎉" else "點兩塊拼圖，把它們換位置！",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (pokemon != null && pieces != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(order) { pos, pieceIndex ->
                    val isSelected = selectedPos == pos
                    Card(
                        modifier = Modifier.aspectRatio(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                solved -> Color(0xFFC8E6C9)
                                isSelected -> Color(0xFFFFCC80)
                                else -> Color(0xFFFFF59D)
                            },
                        ),
                        onClick = {
                            if (solved) return@Card
                            val first = selectedPos
                            if (first == null) {
                                sounds.flip()
                                selectedPos = pos
                            } else if (first == pos) {
                                selectedPos = null
                            } else {
                                val newOrder = order.toMutableList()
                                val tmp = newOrder[first]
                                newOrder[first] = newOrder[pos]
                                newOrder[pos] = tmp
                                order = newOrder
                                selectedPos = null
                                sounds.flip()
                                if (newOrder == listOf(0, 1, 2, 3)) {
                                    solved = true
                                    sounds.celebrate()
                                    onSpeak("拼好了！是${pokemon.name}！")
                                    onCollect(pokemon.id)
                                    if (pokemon.id !in collectedIds) newCollected++
                                }
                            }
                        },
                    ) {
                        Image(
                            bitmap = pieces[pieceIndex],
                            contentDescription = if (solved) pokemon.name else "拼圖一塊",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        )
                    }
                }
            }
        } else if (pokemon != null) {
            // Artwork could not be decoded: show the whole picture and let
            // the kid move on without collecting.
            Image(
                painter = painterResource(drawableId(context, pokemon.drawable)),
                contentDescription = pokemon.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(240.dp).padding(8.dp).weight(1f),
            )
            Text(
                text = "這張圖打不開，換下一張吧！",
                fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        if (solved || pieces == null) {
            Button(
                onClick = { nextPuzzle() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
            ) { Text("下一張 ➜", fontSize = 28.sp) }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 拼好 ${puzzles.size} 張拼圖了！", fontSize = 26.sp) },
            text = { Text("新收集到 $newCollected 隻寶可夢，好棒！", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    sessionSeed = System.currentTimeMillis()
                    puzzleIndex = 0
                    showComplete = false
                }) { Text("再玩一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
