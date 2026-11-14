package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollectionScreen(
    collectedIds: Set<Int>,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Pokemon?>(null) }
    var region by remember { mutableStateOf(Region.KANTO) }
    val regionList = regionPokemon(region)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "我的圖鑑",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "已收集 ${collectedIds.size} / ${ALL_POKEMON.size}",
            fontSize = 26.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        LinearProgressIndicator(
            progress = { collectedIds.size.toFloat() / ALL_POKEMON.size },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
        // Region tabs: each with its own progress, and a star when complete.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        ) {
            Region.entries.forEach { entry ->
                val list = regionPokemon(entry)
                val done = list.count { it.id in collectedIds }
                val complete = done == list.size
                Button(
                    onClick = { region = entry },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (region == entry) Color(0xFF1976D2) else Color(0xFFE3F2FD),
                        contentColor = if (region == entry) Color.White else Color.Black,
                    ),
                ) {
                    Text(
                        "${entry.label}${if (complete) " ⭐" else ""}\n$done / ${list.size}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2,
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(regionList) { pokemon ->
                val collected = pokemon.id in collectedIds
                Card(
                    modifier = Modifier.aspectRatio(1f).clickable(enabled = collected) {
                        selected = pokemon
                        onSpeak(pokemon.speakText())
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (collected) Color(0xFFE3F2FD) else Color(0xFFEEEEEE),
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            Image(
                                painter = painterResource(drawableId(context, pokemon.drawable)),
                                contentDescription = if (collected) pokemon.name else "還沒收集到",
                                contentScale = ContentScale.Fit,
                                colorFilter = if (collected) null else ColorFilter.tint(Color.Black),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            Text(
                                if (collected) pokemon.name else "?",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1,
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

    selected?.let { pokemon ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("${pokemon.name}  No.${pokemon.id}", fontSize = 26.sp) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Image(
                        painter = painterResource(drawableId(context, pokemon.drawable)),
                        contentDescription = pokemon.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                    Text("屬性：${pokemon.type}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("身高：${formatMeasure(pokemon.heightM)} 公尺", fontSize = 22.sp)
                    Text("體重：${formatMeasure(pokemon.weightKg)} 公斤", fontSize = 22.sp)
                    Text(
                        pokemon.intro,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    // Evolution chain: the whole family, current one highlighted.
                    // Uncollected family members show as a silhouette + "?".
                    val family = pokemon.evolutionFamily()
                    if (family.size > 1) {
                        Text(
                            "進化", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            family.forEachIndexed { familyIndex, member ->
                                val memberCollected = member.id in collectedIds
                                val isCurrent = member.id == pokemon.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Image(
                                        painter = painterResource(drawableId(context, member.drawable)),
                                        contentDescription = if (memberCollected) member.name else "還沒收集到",
                                        contentScale = ContentScale.Fit,
                                        colorFilter = if (memberCollected) null else ColorFilter.tint(Color.Black),
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    )
                                    Text(
                                        if (memberCollected) member.name else "?",
                                        fontSize = 16.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) Color(0xFF1976D2) else Color.Black,
                                        maxLines = 1,
                                    )
                                }
                                if (familyIndex < family.size - 1) {
                                    Text("➜", fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSpeak(pokemon.speakText()) }) { Text("🔊 再唸一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("關閉", fontSize = 22.sp) }
            },
        )
    }
}
