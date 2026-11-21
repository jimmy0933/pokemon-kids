package com.jimmy0933.pokemonkids

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

const val BALL_CATCH_ENCOUNTER_COUNT = 109
const val BALL_CATCH_SPOT_COUNT = 3
const val BALL_CATCH_HOP_INTERVAL_MS = 1200L
const val BALL_CATCH_FLIGHT_MS = 400
const val BALL_CATCH_SHAKE_MS = 150L
const val BALL_CATCH_CHANCE = 0.7

/** Build the encounter list for one session: random uncaught Pokemon only,
 * up to [count]. Returns fewer than [count] when the uncaught pool is small,
 * and empty when everything is already caught — so a session ends early or
 * the all-caught celebration shows instead of an encounter. Pure function,
 * unit-tested. */
fun ballCatchEncounters(
    pool: List<Pokemon>,
    excludedIds: Set<Int>,
    count: Int,
    seed: Long,
): List<Pokemon> {
    val uncaught = pool.filter { it.id !in excludedIds }
    if (uncaught.isEmpty()) return emptyList()
    return uncaught.shuffled(kotlin.random.Random(seed)).take(count)
}

/** Did the ball hit? The ball was thrown at [targetSpot]; it hits only if
 * the Pokemon is still at that spot when the ball arrives. Pure function,
 * unit-tested. */
fun ballHitsTarget(targetSpot: Int, pokemonSpotAtArrival: Int): Boolean =
    targetSpot == pokemonSpotAtArrival

/** Roll the catch: [chance] (0.7) to catch on each hit. Pure function,
 * unit-tested (seeded, so deterministic in tests). */
fun rollCatch(random: kotlin.random.Random, chance: Double = BALL_CATCH_CHANCE): Boolean =
    random.nextDouble() < chance

private enum class BallPhase { Idle, Flying, Shaking, Caught, BrokeOut, Missed }

/** A Poke Ball drawn with Canvas: red top, white bottom, black band and a
 * white centre button. Pure drawing, no image asset needed. */
@Composable
fun PokeBallIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        // Red top half (180° -> 360° passes through the top).
        drawArc(
            color = Color(0xFFEE1515),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
        )
        // White bottom half (0° -> 180° passes through the bottom).
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
        )
        // Black band across the middle.
        drawLine(
            color = Color.Black,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = radius * 0.22f,
        )
        // Centre button: black ring with a white middle.
        drawCircle(color = Color.Black, radius = radius * 0.32f, center = center)
        drawCircle(color = Color.White, radius = radius * 0.22f, center = center)
    }
}

/** Throw-ball catching, for older kids: a wild Pokemon hops between 3 spots
 * and the kid taps 「丟球！」 to throw a Poke Ball at it. The ball takes
 * ~400ms to fly; it hits only if the Pokemon has not hopped away by then.
 * On a hit the ball shakes 3 times, then a 70% catch roll: caught Pokemon
 * go into the Pokedex, a break-out means throwing again at the same Pokemon.
 * A miss just means throwing again — unlimited balls, the Pokemon never
 * flees, no timer, no penalty. Encounters come only from the uncaught pool,
 * so a Pokemon already in the Pokedex never appears again. 109 encounters
 * per session (fewer if the uncaught pool runs out). */
@Composable
fun BallCatchScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    // Pokemon caught in this screen (including earlier sessions here) must
    // never be encountered again, even before the DataStore flow has caught
    // up with the latest catches — so they are excluded on top of the
    // collected set. This state is NOT keyed on sessionSeed: it survives
    // "再玩一次" and keeps the next session uncaught-only too.
    var caughtIds by remember { mutableStateOf(setOf<Int>()) }
    val encounters = remember(sessionSeed) {
        ballCatchEncounters(
            ALL_POKEMON,
            collectedIds + caughtIds,
            BALL_CATCH_ENCOUNTER_COUNT,
            sessionSeed,
        )
    }
    var index by remember(sessionSeed) { mutableStateOf(0) }
    var caughtCount by remember(sessionSeed) { mutableStateOf(0) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    var spot by remember(sessionSeed) { mutableStateOf(0) }
    var phase by remember(sessionSeed) { mutableStateOf(BallPhase.Idle) }
    var targetSpot by remember(sessionSeed) { mutableStateOf(0) }
    var shakeTick by remember(sessionSeed) { mutableStateOf(-1) }
    val pokemon = encounters.getOrNull(index)
    val allCaught = encounters.isEmpty()

    // The wild Pokemon hops to a new spot every ~1.2s while it is free —
    // while waiting and while the ball is flying (that is the timing
    // challenge: a throw hits only if it has not hopped away). It never
    // hops while it is in the ball, after breaking out, or after a miss.
    // This effect is keyed on the Pokemon only, so a throw does not reset
    // the hop timer — the Pokemon really can hop away mid-flight.
    LaunchedEffect(pokemon) {
        while (pokemon != null) {
            delay(BALL_CATCH_HOP_INTERVAL_MS)
            if (phase == BallPhase.Idle || phase == BallPhase.Flying) {
                spot = nextCatchHole(
                    BALL_CATCH_SPOT_COUNT,
                    spot,
                    kotlin.random.Random(System.nanoTime()),
                )
            }
        }
    }

    // Say the instruction out loud whenever a new Pokemon appears.
    LaunchedEffect(pokemon) {
        pokemon?.let { onSpeak("出現了野生的${it.name}！丟球抓住牠！") }
    }

    // Drive the throw / shake / catch flow. Each phase runs its own delays
    // and then moves to the next phase; changing phase restarts this effect
    // and cancels the previous phase's remaining delays.
    LaunchedEffect(phase) {
        when (phase) {
            BallPhase.Flying -> {
                delay(BALL_CATCH_FLIGHT_MS.toLong())
                if (ballHitsTarget(targetSpot, spot)) {
                    sounds.match()
                    onSpeak("丟中了！")
                    phase = BallPhase.Shaking
                } else {
                    sounds.flip()
                    onSpeak("沒丟中！再試一次")
                    phase = BallPhase.Missed
                }
            }
            BallPhase.Shaking -> {
                // 3 shakes: 6 toggles of the ball's sideways offset.
                repeat(6) { tick ->
                    shakeTick = tick
                    delay(BALL_CATCH_SHAKE_MS)
                }
                shakeTick = -1
                val caught = pokemon
                if (caught != null && rollCatch(kotlin.random.Random(System.nanoTime()))) {
                    sounds.match()
                    sounds.celebrate()
                    onSpeak("抓到了！${caught.name}！")
                    onCollect(caught.id)
                    caughtIds = caughtIds + caught.id
                    if (caught.id !in collectedIds) newCollected++
                    caughtCount++
                    phase = BallPhase.Caught
                } else {
                    sounds.flip()
                    onSpeak("跑出來了！再丟一次")
                    phase = BallPhase.BrokeOut
                }
            }
            BallPhase.Caught -> {
                delay(1800)
                if (index + 1 < encounters.size) {
                    index++
                    spot = nextCatchHole(
                        BALL_CATCH_SPOT_COUNT,
                        spot,
                        kotlin.random.Random(System.nanoTime()),
                    )
                    phase = BallPhase.Idle
                } else {
                    showComplete = true
                }
            }
            BallPhase.Missed -> {
                delay(1200)
                phase = BallPhase.Idle
            }
            BallPhase.BrokeOut -> {
                delay(1500)
                phase = BallPhase.Idle
            }
            BallPhase.Idle -> {
            }
        }
    }

    // Ball flight animation: 0 = at the thrower's hand (bottom centre),
    // 1 = at the target spot. The animation is visual only — the hit is
    // decided by the 400ms delay in the phase effect above.
    val flightProgress by animateFloatAsState(
        targetValue = if (phase == BallPhase.Flying || phase == BallPhase.Shaking ||
            phase == BallPhase.Caught || phase == BallPhase.Missed
        ) 1f else 0f,
        animationSpec = tween(BALL_CATCH_FLIGHT_MS),
        label = "ballFlight",
    )
    // The ball shows while it is flying, shaking, caught, or lying where it
    // missed; it is hidden before any throw and after a break-out (the ball
    // popped open). The Pokemon itself hides while it is inside the ball.
    val showBall = phase != BallPhase.Idle && phase != BallPhase.BrokeOut
    val pokemonVisible = pokemon != null &&
        phase != BallPhase.Shaking && phase != BallPhase.Caught

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "丟球抓寶 ★進階",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (allCaught) {
            Text(
                text = "🎉 全部抓完了！你好厲害！",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
            Text(
                text = "你已經收集了全部 ${ALL_POKEMON.size} 隻寶可夢！",
                fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("⬅ 回主頁", fontSize = 24.sp) }
            return@Column
        }
        Text(
            text = "第 ${index + 1} / ${encounters.size} 隻　抓到了 $caughtCount 隻",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = when (phase) {
                BallPhase.Idle -> "出現了野生的${pokemon?.name}！瞄準牠，丟球抓住牠！"
                BallPhase.Flying -> "球飛過去了……"
                BallPhase.Shaking -> "搖啊搖……會抓到嗎？"
                BallPhase.Caught -> "抓到了！是${pokemon?.name}！🎉"
                BallPhase.BrokeOut -> "啊！${pokemon?.name}跑出來了！再丟一次！"
                BallPhase.Missed -> "沒丟中！再試一次！"
            },
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (pokemon != null) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                val areaWidth = maxWidth.value
                val areaHeight = maxHeight.value
                val ballSize = 72f
                val slotWidth = areaWidth / BALL_CATCH_SPOT_COUNT
                val targetX = slotWidth * targetSpot + slotWidth / 2f - ballSize / 2f
                val targetY = areaHeight * 0.32f - ballSize / 2f
                val startX = areaWidth / 2f - ballSize / 2f
                val startY = areaHeight - ballSize - 8f
                val progress = flightProgress
                val arc = sin(progress * PI).toFloat() * 60f
                val shakeX = if (phase == BallPhase.Shaking && shakeTick >= 0) {
                    if (shakeTick % 2 == 0) 12f else -12f
                } else {
                    0f
                }
                val ballX = startX + (targetX - startX) * progress + shakeX
                val ballY = startY + (targetY - startY) * progress - arc
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    repeat(BALL_CATCH_SPOT_COUNT) { slot ->
                        val isActive = slot == spot && pokemonVisible
                        Card(
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) Color(0xFFC8E6C9)
                                else Color(0xFFDCEDC8),
                            ),
                        ) {
                            if (isActive) {
                                Image(
                                    painter = painterResource(drawableId(context, pokemon.drawable)),
                                    contentDescription = pokemon.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
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
                if (showBall) {
                    PokeBallIcon(
                        modifier = Modifier.size(ballSize.dp)
                            .offset(x = ballX.dp, y = ballY.dp),
                    )
                }
            }
        }
        Button(
            onClick = {
                targetSpot = spot
                sounds.flip()
                phase = BallPhase.Flying
            },
            enabled = phase == BallPhase.Idle && pokemon != null,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⚾ 丟球！", fontSize = 28.sp) }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 抓完 ${encounters.size} 隻了！", fontSize = 26.sp) },
            text = { Text("新收集到 $newCollected 隻寶可夢，好棒！", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    sessionSeed = System.currentTimeMillis()
                    index = 0
                    showComplete = false
                }) { Text("再玩一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
