package com.jimmy0933.pokemonkids

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import java.util.Locale
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object Rounds : Screen
    data class Game(val roundIndex: Int) : Screen
    data object ListenQuiz : Screen
    data object Silhouette : Screen
    data object Collection : Screen
    data object FindIt : Screen
    data object Puzzle : Screen
    data object Count : Screen
    data object Care : Screen
    data object ColorSort : Screen
    data object Maze : Screen
    data object Size : Screen
    data object FindMatch : Screen
    data object DexOrder : Screen
    data object Weight : Screen
    data object ShadowMatch : Screen
    data object Memory : Screen
    data object EvoOrder : Screen
    data object Math : Screen
    data object OddOneOut : Screen
    data object BallCatch : Screen
}

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private val sounds = Sounds()
    private lateinit var store: CollectionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = CollectionStore(applicationContext)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.TRADITIONAL_CHINESE
            }
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PokemonKidsApp(store, ::speak, sounds)
                }
            }
        }
    }

    private fun speak(text: String) {
        try { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) } catch (_: Exception) { }
    }

    override fun onDestroy() {
        try { tts?.shutdown() } catch (_: Exception) { }
        sounds.release()
        super.onDestroy()
    }
}

@Composable
fun PokemonKidsApp(store: CollectionStore, onSpeak: (String) -> Unit, sounds: Sounds) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val collected by store.collectedIds.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            collectedCount = collected.size,
            onPlay = { screen = Screen.Rounds },
            onListenQuiz = { screen = Screen.ListenQuiz },
            onSilhouette = { screen = Screen.Silhouette },
            onCollection = { screen = Screen.Collection },
            onFindIt = { screen = Screen.FindIt },
            onPuzzle = { screen = Screen.Puzzle },
            onCount = { screen = Screen.Count },
            onCare = { screen = Screen.Care },
            onColorSort = { screen = Screen.ColorSort },
            onMaze = { screen = Screen.Maze },
            onSize = { screen = Screen.Size },
            onFindMatch = { screen = Screen.FindMatch },
            onDexOrder = { screen = Screen.DexOrder },
            onWeight = { screen = Screen.Weight },
            onShadowMatch = { screen = Screen.ShadowMatch },
            onMemory = { screen = Screen.Memory },
            onEvoOrder = { screen = Screen.EvoOrder },
            onMath = { screen = Screen.Math },
            onOddOneOut = { screen = Screen.OddOneOut },
            onBallCatch = { screen = Screen.BallCatch },
        )
        Screen.Rounds -> RoundSelectScreen(
            collectedIds = collected,
            onPick = { screen = Screen.Game(it) },
            onBack = { screen = Screen.Home },
        )
        is Screen.Game -> GameScreen(
            roundIndex = current.roundIndex,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Rounds },
            onNextRound = if (current.roundIndex + 1 < TOTAL_ROUNDS) {
                { screen = Screen.Game(current.roundIndex + 1) }
            } else null,
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.ListenQuiz -> ListenQuizScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Silhouette -> SilhouetteScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Collection -> CollectionScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            onBack = { screen = Screen.Home },
        )
        Screen.FindIt -> FindItScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Puzzle -> PuzzleScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Count -> CountScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Care -> CareScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
        )
        Screen.ColorSort -> ColorSortScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Maze -> MazeScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Size -> SizeScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.FindMatch -> FindMatchScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.DexOrder -> DexOrderScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Weight -> WeightScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.ShadowMatch -> ShadowMatchScreen(
            collectedIds = collected,
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Memory -> MemoryScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.EvoOrder -> EvoOrderScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.Math -> MathScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.OddOneOut -> OddOneOutScreen(
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
        Screen.BallCatch -> BallCatchScreen(
            collectedIds = collected,
            onCollect = { id -> scope.launch { store.addCollected(listOf(id)) } },
            onSpeak = onSpeak,
            sounds = sounds,
            onBack = { screen = Screen.Home },
            onOpenCollection = { screen = Screen.Collection },
        )
    }
}

@Composable
fun HomeScreen(
    collectedCount: Int,
    onPlay: () -> Unit,
    onListenQuiz: () -> Unit,
    onSilhouette: () -> Unit,
    onCollection: () -> Unit,
    onFindIt: () -> Unit,
    onPuzzle: () -> Unit,
    onCount: () -> Unit,
    onCare: () -> Unit,
    onColorSort: () -> Unit,
    onMaze: () -> Unit,
    onSize: () -> Unit,
    onFindMatch: () -> Unit,
    onDexOrder: () -> Unit,
    onWeight: () -> Unit,
    onShadowMatch: () -> Unit,
    onMemory: () -> Unit,
    onEvoOrder: () -> Unit,
    onMath: () -> Unit,
    onOddOneOut: () -> Unit,
    onBallCatch: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(drawableId(context, "pkm_025")),
            contentDescription = "皮卡丘",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(220.dp),
        )
        Text("寶可夢收集遊戲", fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("已收集 $collectedCount / ${ALL_POKEMON.size}", fontSize = 26.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("▶ 翻牌配對", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onListenQuiz,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔊 聽音猜", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSilhouette,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("👤 影子猜", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCollection,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("📖 我的圖鑑", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onFindIt,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔍 找一找", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onPuzzle,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🧩 拼圖", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCount,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔢 數一數", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCare,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("💗 照顧寶可夢", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onColorSort,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🎨 顏色分類", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onMaze,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🌀 走迷宮", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSize,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("📏 比大小", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onFindMatch,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔎 找相同", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onDexOrder,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔢 排順序", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onWeight,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("⚖️ 比重量", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onShadowMatch,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🎭 影子配對", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onMemory,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🧠 記憶翻牌（進階）", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onEvoOrder,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🔄 進化順序（進階）", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onMath,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("➕ 加減法（進階）", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onOddOneOut,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("👀 找不同（進階）", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onBallCatch,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Text("🎯 抓寶可夢（進階）", fontSize = 32.sp) }
        Spacer(modifier = Modifier.height(24.dp))
        Text("給 4 歲小朋友玩：看圖配對玩遊戲，抓寶可夢來收集圖鑑", fontSize = 18.sp, textAlign = TextAlign.Center, color = Color.Gray)
        Text("★ 進階玩法難一點，適合 8 歲小朋友", fontSize = 18.sp, textAlign = TextAlign.Center, color = Color.Gray)
    }
}

@Composable
fun RoundSelectScreen(collectedIds: Set<Int>, onPick: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp)) {
        Text(
            "選一關來玩", fontSize = 32.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(rounds()) { index, round ->
                val done = round.all { it.id in collectedIds }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPick(index) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (done) Color(0xFFC8E6C9) else Color(0xFFE3F2FD)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("第 ${index + 1} 關", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        round.take(3).forEach { pokemon ->
                            Image(
                                painter = painterResource(drawableId(context, pokemon.drawable)),
                                contentDescription = pokemon.name,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                        Text(if (done) " ✓" else " …", fontSize = 28.sp)
                    }
                }
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }
}
