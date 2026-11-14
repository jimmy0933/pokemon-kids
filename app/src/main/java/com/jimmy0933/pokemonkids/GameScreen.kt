package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

fun drawableId(context: android.content.Context, name: String): Int =
    context.resources.getIdentifier(name, "drawable", context.packageName)

@Composable
fun GameScreen(
    roundIndex: Int,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onNextRound: (() -> Unit)?,
    onOpenCollection: () -> Unit,
) {
    val context = LocalContext.current
    val round = rounds()[roundIndex]
    val cards = remember(roundIndex) { roundCards(roundIndex) }
    var flipped by remember(roundIndex) { mutableStateOf(setOf<Int>()) }
    var matched by remember(roundIndex) { mutableStateOf(setOf<Int>()) }
    var locked by remember(roundIndex) { mutableStateOf(false) }
    var showComplete by remember(roundIndex) { mutableStateOf(false) }

    LaunchedEffect(flipped) {
        if (flipped.size == 2) {
            val (a, b) = flipped.toList()
            if (cards[a].id == cards[b].id) {
                val pokemon = cards[a]
                sounds.match()
                onSpeak(pokemon.name)
                matched = matched + pokemon.id
                onCollect(pokemon.id)
                flipped = emptySet()
                if (matched.size + 1 == ROUND_SIZE) {
                    // matched was just updated above in the same frame; re-check below.
                }
            } else {
                locked = true
                delay(900)
                flipped = emptySet()
                locked = false
            }
        }
    }
    LaunchedEffect(matched) {
        if (matched.size == ROUND_SIZE && matched.isNotEmpty()) {
            delay(400)
            sounds.celebrate()
            showComplete = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "第 ${roundIndex + 1} 關",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "⭐ ${matched.size} / $ROUND_SIZE",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(cards) { index, pokemon ->
                val isFlipped = index in flipped || pokemon.id in matched
                val isMatched = pokemon.id in matched
                Card(
                    modifier = Modifier.aspectRatio(1f).clickable(enabled = !isFlipped && !locked) {
                        sounds.flip()
                        flipped = flipped + index
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMatched) Color(0xFFC8E6C9) else Color(0xFFFFF59D),
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isFlipped) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(drawableId(context, pokemon.drawable)),
                                    contentDescription = pokemon.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(0.78f),
                                )
                                Text(pokemon.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("?", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEE3E36))
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
            title = { Text("🎉 收集到 6 隻新朋友！", fontSize = 26.sp) },
            text = { Text("這一關的寶可夢都進圖鑑了，好棒！", fontSize = 20.sp) },
            confirmButton = {
                if (onNextRound != null) {
                    TextButton(onClick = onNextRound) { Text("下一關 ➜", fontSize = 22.sp) }
                }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
