package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val MAZE_COUNT = 5
const val MAZE_SIZE = 5

/** One cell of the maze grid: (row, column), both 0-based. */
typealias MazeCell = Pair<Int, Int>

/** A maze on a square grid. [walls] are the blocked cells; every other cell
 * is walkable. The player starts at [start] and must reach [goal]. */
data class Maze(
    val size: Int,
    val walls: Set<MazeCell>,
    val start: MazeCell,
    val goal: MazeCell,
)

/** Build a maze that is always solvable. A random path is carved from the
 * top-left corner to the bottom-right corner (only moving down or right, so
 * it always reaches the goal), then a few extra cells are opened so the maze
 * is not just one corridor; every other cell becomes a wall. Pure function,
 * unit-tested. */
fun makeMaze(seed: Long, size: Int = MAZE_SIZE): Maze {
    val random = kotlin.random.Random(seed)
    val path = mutableListOf<MazeCell>()
    var row = 0
    var column = 0
    path.add(row to column)
    while (row != size - 1 || column != size - 1) {
        val canGoDown = row < size - 1
        val canGoRight = column < size - 1
        if (canGoDown && (!canGoRight || random.nextBoolean())) {
            row++
        } else {
            column++
        }
        path.add(row to column)
    }
    val pathSet = path.toSet()
    val allCells = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }
    // Open a few extra cells off the carved path so there is room to wander.
    val extras = (allCells - pathSet).shuffled(random).take(4)
    val open = pathSet + extras
    return Maze(
        size = size,
        walls = allCells.filter { it !in open }.toSet(),
        start = 0 to 0,
        goal = (size - 1) to (size - 1),
    )
}

/** The shortest walkable path from [Maze.start] to [Maze.goal] (breadth-first
 * search), or null if there is none. Pure function, unit-tested. */
fun findMazePath(maze: Maze): List<MazeCell>? {
    if (maze.start in maze.walls || maze.goal in maze.walls) return null
    val cameFrom = mutableMapOf<MazeCell, MazeCell?>()
    val queue = ArrayDeque<MazeCell>()
    cameFrom[maze.start] = null
    queue.add(maze.start)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        if (cell == maze.goal) break
        val (row, column) = cell
        listOf(
            row - 1 to column,
            row + 1 to column,
            row to column - 1,
            row to column + 1,
        ).forEach { next ->
            val (nextRow, nextColumn) = next
            if (nextRow !in 0 until maze.size || nextColumn !in 0 until maze.size) return@forEach
            if (next in maze.walls || next in cameFrom) return@forEach
            cameFrom[next] = cell
            queue.add(next)
        }
    }
    if (maze.goal !in cameFrom) return null
    val path = mutableListOf<MazeCell>()
    var cell: MazeCell? = maze.goal
    while (cell != null) {
        path.add(cell)
        cell = cameFrom[cell]
    }
    return path.reversed()
}

/** True when the maze can be solved (a walkable path exists from start to
 * goal). Pure function, unit-tested. */
fun isMazeSolvable(maze: Maze): Boolean = findMazePath(maze) != null

/** Walk the maze: a Pokemon starts at one corner and the kid taps the big
 * arrow buttons to move it step by step to the apple at the opposite corner.
 * Walls block movement with a gentle "走不通" and no penalty. 5 mazes per
 * session, no timer; finishing a maze collects the Pokemon shown. */
@Composable
fun MazeScreen(
    collectedIds: Set<Int>,
    onCollect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    sounds: Sounds,
    onBack: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    var sessionSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    val mazes = remember(sessionSeed) {
        List(MAZE_COUNT) { makeMaze(sessionSeed + it) }
    }
    val mazePokemon = remember(sessionSeed) {
        val random = kotlin.random.Random(sessionSeed)
        val uncollected = ALL_POKEMON.filter { it.id !in collectedIds }.shuffled(random)
        val collected = ALL_POKEMON.filter { it.id in collectedIds }.shuffled(random)
        (uncollected + collected).take(MAZE_COUNT)
    }
    var mazeIndex by remember(sessionSeed) { mutableStateOf(0) }
    var newCollected by remember(sessionSeed) { mutableStateOf(0) }
    var showComplete by remember(sessionSeed) { mutableStateOf(false) }
    val maze = mazes.getOrNull(mazeIndex)
    val pokemon = mazePokemon.getOrNull(mazeIndex)

    // Per-maze state, keyed so a new maze (or a new session) resets it.
    val mazeKey = "$sessionSeed-$mazeIndex"
    var position by remember(mazeKey) { mutableStateOf(maze?.start ?: (0 to 0)) }
    var solved by remember(mazeKey) { mutableStateOf(false) }
    var blocked by remember(mazeKey) { mutableStateOf(false) }

    fun nextMaze() {
        if (mazeIndex + 1 < mazes.size) {
            mazeIndex++
        } else {
            showComplete = true
        }
    }

    fun move(dRow: Int, dColumn: Int) {
        val currentMaze = maze ?: return
        if (solved) return
        val next = (position.first + dRow) to (position.second + dColumn)
        if (next.first !in 0 until currentMaze.size ||
            next.second !in 0 until currentMaze.size ||
            next in currentMaze.walls
        ) {
            sounds.flip()
            onSpeak("走不通，再試試別的方向")
            blocked = true
            return
        }
        blocked = false
        position = next
        sounds.flip()
        if (next == currentMaze.goal) {
            solved = true
            sounds.celebrate()
            pokemon?.let {
                onSpeak("到了！是${it.name}！")
                onCollect(it.id)
                if (it.id !in collectedIds) newCollected++
            }
        }
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "走迷宮",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "第 ${mazeIndex + 1} / ${mazes.size} 關",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            text = when {
                solved -> "到了！是${pokemon?.name}！🎉"
                blocked -> "走不通喔！換個方向試試！"
                else -> "把${pokemon?.name ?: "寶可夢"}帶到蘋果那裡！"
            },
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (maze != null && pokemon != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                for (row in 0 until maze.size) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        for (column in 0 until maze.size) {
                            val cell = row to column
                            val isPlayer = cell == position
                            val isGoal = cell == maze.goal
                            val isWall = cell in maze.walls
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isWall -> Color(0xFF78909C)
                                            isGoal -> Color(0xFFFFCCBC)
                                            isPlayer -> Color(0xFFC8E6C9)
                                            else -> Color(0xFFFFF59D)
                                        },
                                        RoundedCornerShape(12.dp),
                                    ),
                            ) {
                                when {
                                    isPlayer -> Image(
                                        painter = painterResource(drawableId(context, pokemon.drawable)),
                                        contentDescription = pokemon.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                    )
                                    isGoal -> Text("🍎", fontSize = 40.sp)
                                }
                            }
                        }
                    }
                }
            }
            // Big arrow pad: up on top, left / down / right below.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Button(
                    onClick = { move(-1, 0) },
                    modifier = Modifier.fillMaxWidth(0.33f).height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                ) { Text("⬆", fontSize = 36.sp) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Button(
                        onClick = { move(0, -1) },
                        modifier = Modifier.weight(1f).height(72.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("⬅", fontSize = 36.sp) }
                    Button(
                        onClick = { move(1, 0) },
                        modifier = Modifier.weight(1f).height(72.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("⬇", fontSize = 36.sp) }
                    Button(
                        onClick = { move(0, 1) },
                        modifier = Modifier.weight(1f).height(72.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("➡", fontSize = 36.sp) }
                }
            }
        }
        if (solved) {
            Button(
                onClick = { nextMaze() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
            ) { Text("下一個迷宮 ➜", fontSize = 28.sp) }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("⬅ 回主頁", fontSize = 24.sp) }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 走完 ${mazes.size} 個迷宮了！", fontSize = 26.sp) },
            text = { Text("新收集到 $newCollected 隻寶可夢，好棒！", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    sessionSeed = System.currentTimeMillis()
                    mazeIndex = 0
                    showComplete = false
                }) { Text("再玩一次", fontSize = 22.sp) }
            },
            dismissButton = {
                TextButton(onClick = onOpenCollection) { Text("看圖鑑", fontSize = 22.sp) }
            },
        )
    }
}
