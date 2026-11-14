package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

const val MAX_HEARTS = 5

/** Care for a collected Pokemon: feed, wash and play with it. Only Pokemon
 * already in the Pokedex can be picked; each care action fills the heart
 * meter (♥) for the session and speaks a praise. No persistence beyond the
 * session, no timer, no penalty. */
@Composable
fun CareScreen(
    collectedIds: Set<Int>,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val collected = remember(collectedIds) {
        ALL_POKEMON.filter { it.id in collectedIds }
    }
    var selected by remember(collectedIds) { mutableStateOf(collected.firstOrNull()) }
    var hearts by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "照顧寶可夢",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (collected.isEmpty()) {
            Text(
                text = "還沒有寶可夢可以照顧！\n先去收集一些寶可夢吧！",
                fontSize = 26.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(88.dp),
                shape = RoundedCornerShape(24.dp),
            ) { Text("⬅ 回主頁去收集", fontSize = 28.sp) }
            return@Column
        }

        val current = selected ?: collected.first()
        Text(
            text = "選一隻寶可夢來照顧",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(collected) { pokemon ->
                val isSelected = pokemon.id == current.id
                Card(
                    modifier = Modifier.size(84.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFC8E6C9) else Color(0xFFFFF59D),
                    ),
                    onClick = {
                        selected = pokemon
                        sounds.flip()
                        onSpeak(pokemon.name)
                    },
                ) {
                    Image(
                        painter = painterResource(drawableId(context, pokemon.drawable)),
                        contentDescription = pokemon.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                }
            }
        }
        Image(
            painter = painterResource(drawableId(context, current.drawable)),
            contentDescription = current.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(220.dp).padding(8.dp),
        )
        Text(
            current.name,
            fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "♥".repeat(hearts) + "♡".repeat(MAX_HEARTS - hearts),
            fontSize = 36.sp, textAlign = TextAlign.Center, color = Color(0xFFE53935),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text(
            text = if (hearts >= MAX_HEARTS) "好開心！謝謝你照顧我！💗" else "照顧${current.name}，讓愛心滿滿！",
            fontSize = 22.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        listOf(
            "🍎 餵食" to { "好吃！謝謝你！${current.name}吃得好開心！" },
            "🛁 洗澡" to { "洗得香香的！${current.name}變得亮晶晶！" },
            "⚽ 玩耍" to { "玩得好開心！${current.name}最喜歡跟你玩了！" },
        ).forEach { (label, praise) ->
            Button(
                onClick = {
                    sounds.match()
                    onSpeak(praise())
                    val newHearts = (hearts + 1).coerceAtMost(MAX_HEARTS)
                    hearts = newHearts
                    if (newHearts >= MAX_HEARTS) {
                        sounds.celebrate()
                        onSpeak("好開心！謝謝你照顧我！")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
            ) { Text(label, fontSize = 30.sp) }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }
}
