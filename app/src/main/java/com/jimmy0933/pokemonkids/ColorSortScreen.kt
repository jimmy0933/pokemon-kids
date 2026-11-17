package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

const val COLOR_SORT_QUESTION_COUNT = 40

/** The four colour boxes the kid sorts Pokemon into. */
enum class ColorCategory(val label: String, val color: Color, val textColor: Color = Color.White) {
    RED("紅", Color(0xFFE53935)),
    BLUE("藍", Color(0xFF1E88E5)),
    YELLOW("黃", Color(0xFFFFCA28), Color.Black),
    GREEN("綠", Color(0xFF43A047)),
}

/** Curated colour-sorted Pokemon: only Pokemon whose main body colour is
 * unmistakable are listed. Ambiguous ones (orange, pink, purple, brown,
 * white, grey, black, two-tone or teal bodies — e.g. 小火龍, 胖丁, 耿鬼,
 * 妙蛙種子) are left out on purpose. 18 per colour, 72 in total. Every id
 * was checked against Pokemon.kt. */
val COLOR_SORT_MAP: Map<Int, ColorCategory> = mapOf(
    // 紅：18
    126 to ColorCategory.RED, // 鴨嘴火獸
    129 to ColorCategory.RED, // 鯉魚王
    165 to ColorCategory.RED, // 芭瓢蟲
    166 to ColorCategory.RED, // 安瓢蟲
    212 to ColorCategory.RED, // 巨鉗螳螂
    218 to ColorCategory.RED, // 熔岩蟲
    219 to ColorCategory.RED, // 熔岩蝸牛
    224 to ColorCategory.RED, // 章魚桶
    225 to ColorCategory.RED, // 信使鳥
    240 to ColorCategory.RED, // 鴨嘴寶寶
    256 to ColorCategory.RED, // 力壯雞
    257 to ColorCategory.RED, // 火焰雞
    265 to ColorCategory.RED, // 刺尾蟲
    311 to ColorCategory.RED, // 正電拍拍
    341 to ColorCategory.RED, // 龍蝦小兵
    342 to ColorCategory.RED, // 鐵螯龍蝦
    380 to ColorCategory.RED, // 拉帝亞斯
    383 to ColorCategory.RED, // 固拉多
    // 藍：18
    7 to ColorCategory.BLUE, // 傑尼龜
    9 to ColorCategory.BLUE, // 水箭龜
    55 to ColorCategory.BLUE, // 哥達鴨
    62 to ColorCategory.BLUE, // 蚊香泳士
    73 to ColorCategory.BLUE, // 毒刺水母
    117 to ColorCategory.BLUE, // 海刺龍
    130 to ColorCategory.BLUE, // 暴鯉龍
    131 to ColorCategory.BLUE, // 拉普拉斯
    134 to ColorCategory.BLUE, // 水伊布
    144 to ColorCategory.BLUE, // 急凍鳥
    148 to ColorCategory.BLUE, // 哈克龍
    171 to ColorCategory.BLUE, // 電燈怪
    184 to ColorCategory.BLUE, // 瑪力露麗
    195 to ColorCategory.BLUE, // 沼王
    230 to ColorCategory.BLUE, // 刺龍王
    260 to ColorCategory.BLUE, // 巨沼怪
    310 to ColorCategory.BLUE, // 雷電獸
    382 to ColorCategory.BLUE, // 蓋歐卡
    // 黃：18
    13 to ColorCategory.YELLOW, // 獨角蟲
    25 to ColorCategory.YELLOW, // 皮卡丘
    54 to ColorCategory.YELLOW, // 可達鴨
    70 to ColorCategory.YELLOW, // 口呆花
    96 to ColorCategory.YELLOW, // 催眠貘
    125 to ColorCategory.YELLOW, // 電擊獸
    135 to ColorCategory.YELLOW, // 雷伊布
    145 to ColorCategory.YELLOW, // 閃電鳥
    172 to ColorCategory.YELLOW, // 皮丘
    179 to ColorCategory.YELLOW, // 咩利羊
    181 to ColorCategory.YELLOW, // 電龍
    191 to ColorCategory.YELLOW, // 向日種子
    203 to ColorCategory.YELLOW, // 麒麟奇
    206 to ColorCategory.YELLOW, // 土龍弟弟
    213 to ColorCategory.YELLOW, // 壺壺
    239 to ColorCategory.YELLOW, // 電擊怪
    243 to ColorCategory.YELLOW, // 雷公
    303 to ColorCategory.YELLOW, // 大嘴娃
    // 綠：18
    10 to ColorCategory.GREEN, // 綠毛蟲
    11 to ColorCategory.GREEN, // 鐵甲蛹
    69 to ColorCategory.GREEN, // 喇叭芽
    71 to ColorCategory.GREEN, // 大食花
    123 to ColorCategory.GREEN, // 飛天螳螂
    152 to ColorCategory.GREEN, // 菊草葉
    154 to ColorCategory.GREEN, // 大竺葵
    182 to ColorCategory.GREEN, // 美麗花
    192 to ColorCategory.GREEN, // 向日花怪
    193 to ColorCategory.GREEN, // 蜻蜻蜓
    252 to ColorCategory.GREEN, // 木守宮
    254 to ColorCategory.GREEN, // 蜥蜴王
    272 to ColorCategory.GREEN, // 樂天河童
    315 to ColorCategory.GREEN, // 毒薔薇
    317 to ColorCategory.GREEN, // 吞食獸
    330 to ColorCategory.GREEN, // 沙漠蜻蜓
    352 to ColorCategory.GREEN, // 變隱龍
    384 to ColorCategory.GREEN, // 烈空坐
)

/** The colour category of a Pokemon, or null if it is not in the curated
 * colour-sort subset. Pure function, unit-testable. */
fun colorOf(id: Int): ColorCategory? = COLOR_SORT_MAP[id]

/** The Pokemon usable in colour sorting. */
fun colorSortPool(): List<Pokemon> = ALL_POKEMON.filter { it.id in COLOR_SORT_MAP }

/** Colour sorting: a Pokemon is shown and the kid taps which colour box it
 * belongs to (紅 / 藍 / 黃 / 綠). 40 questions per session, no timer, no
 * penalty — a wrong tap just says "再試一次" and lets them try again. A
 * correct tap collects the Pokemon into the same Pokedex as matching. */
@Composable
fun ColorSortScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val questions = remember(sessionSeed) {
        colorSortPool().shuffled(kotlin.random.Random(sessionSeed)).take(COLOR_SORT_QUESTION_COUNT)
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var wrongCategories by remember(sessionSeed) { mutableStateOf(setOf<ColorCategory>()) }
    var answered by remember(sessionSeed) { mutableStateOf(false) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val pokemon = questions.getOrNull(index)
    val answerCategory = pokemon?.let { colorOf(it.id) }

    // Say the instruction out loud whenever a new question appears.
    LaunchedEffect(pokemon) {
        pokemon?.let { onSpeak("這隻寶可夢是什麼顏色？") }
    }
    // After a correct answer, move on automatically.
    LaunchedEffect(answered) {
        if (answered) {
            delay(1800)
            if (index + 1 < questions.size) {
                index++
                wrongCategories = emptySet()
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
            text = "顏色分類",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${index + 1} / ${questions.size} 題",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        if (pokemon != null && answerCategory != null) {
            Image(
                painter = painterResource(drawableId(context, pokemon.drawable)),
                contentDescription = if (answered) pokemon.name else "寶可夢",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(240.dp).padding(8.dp),
            )
            Text(
                text = if (answered) "答對了！${pokemon.name}是${answerCategory.label}色的！🎉" else if (wrongCategories.isNotEmpty()) "再試一次，加油！" else "這隻寶可夢是什麼顏色？",
                fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(ColorCategory.entries) { category ->
                    val isWrong = category in wrongCategories
                    val isAnswer = answered && category == answerCategory
                    Button(
                        onClick = {
                            if (answered || isWrong) return@Button
                            if (category == answerCategory) {
                                sounds.match()
                                onSpeak("答對了！${pokemon.name}是${category.label}色的！")
                                onCollect(pokemon.id)
                                if (pokemon.id !in collectedIds) newCollected++
                                answered = true
                            } else {
                                sounds.flip()
                                onSpeak("再試一次")
                                wrongCategories = wrongCategories + category
                            }
                        },
                        modifier = Modifier.aspectRatio(1.6f).fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isAnswer -> Color(0xFFC8E6C9)
                                isWrong -> Color(0xFFEEEEEE)
                                else -> category.color
                            },
                            contentColor = if (isWrong || isAnswer) Color.Black else category.textColor,
                        ),
                    ) {
                        Text("${category.label}色", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 分完 ${questions.size} 題了！", fontSize = 26.sp) },
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
