package com.jimmy0933.pokemonkids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val MAZE_COUNT = 109
const val MAZE_SIZE = 9

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
    // The count scales with the grid so bigger mazes keep a similar feel.
    val extras = (allCells - pathSet).shuffled(random).take(size)
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

/** The cell under a finger position inside the maze grid: [offsetX] and
 * [offsetY] are in pixels, [gridWidth] and [gridHeight] are the grid size
 * in pixels. Returns null when the position is outside the grid. Pure
 * function, unit-tested. */
fun mazeCellAtOffset(
    size: Int,
    gridWidth: Float,
    gridHeight: Float,
    offsetX: Float,
    offsetY: Float,
): MazeCell? {
    if (size <= 0 || gridWidth <= 0f || gridHeight <= 0f) return null
    if (offsetX < 0f || offsetY < 0f || offsetX >= gridWidth || offsetY >= gridHeight) return null
    val column = (offsetX / (gridWidth / size)).toInt().coerceIn(0, size - 1)
    val row = (offsetY / (gridHeight / size)).toInt().coerceIn(0, size - 1)
    return row to column
}

/** Try to extend the finger-drawn [path] with [cell]: the cell must be
 * inside the maze, not a wall, and orthogonally adjacent to the last cell
 * of the path. Dragging back onto a cell already in the path trims the path
 * back to that cell, so the kid can undo a wrong turn by dragging back.
 * Anything else leaves the path unchanged. Pure function, unit-tested. */
fun mazeExtendPath(maze: Maze, path: List<MazeCell>, cell: MazeCell): List<MazeCell> {
    if (cell.first !in 0 until maze.size || cell.second !in 0 until maze.size) return path
    if (cell in maze.walls) return path
    if (path.isEmpty()) {
        return if (cell == maze.start) listOf(cell) else emptyList()
    }
    val last = path.last()
    if (cell == last) return path
    val indexInPath = path.indexOf(cell)
    if (indexInPath >= 0) return path.subList(0, indexInPath + 1).toList()
    val adjacent = (cell.first == last.first && kotlin.math.abs(cell.second - last.second) == 1) ||
        (cell.second == last.second && kotlin.math.abs(cell.first - last.first) == 1)
    if (!adjacent) return path
    return path + cell
}

/** True when the drawn path has reached the apple (goal) cell. Pure
 * function, unit-tested. */
fun isMazePathComplete(maze: Maze, path: List<MazeCell>): Boolean =
    path.lastOrNull() == maze.goal

/** The polyline points of the finger-drawn route: the centre of each cell in
 * [path], in the order the finger drew them, in pixels inside a grid of
 * [gridWidth] × [gridHeight] with [size] cells per side. The screen draws
 * these points as one continuous line, so the kid sees the route their
 * finger drew. Empty path (or a non-positive size/grid) → no points. Pure
 * function, unit-tested. */
fun mazePathLinePoints(
    path: List<MazeCell>,
    size: Int,
    gridWidth: Float,
    gridHeight: Float,
): List<Offset> {
    if (path.isEmpty() || size <= 0 || gridWidth <= 0f || gridHeight <= 0f) return emptyList()
    return path.map { (row, column) ->
        Offset(
            (column + 0.5f) * gridWidth / size,
            (row + 0.5f) * gridHeight / size,
        )
    }
}

/** Walk the maze: a Pokemon starts at one corner and the kid draws a path
 * with their finger across the 9x9 grid to the apple at the opposite
 * corner. The path grows cell by cell through adjacent, non-wall cells;
 * lifting the finger keeps the drawn path, and the big 「重畫」 button
 * starts the maze over. Dragging into a wall is simply ignored, with no
 * penalty. The walked route is shown as a blue line joining the centres of
 * the path cells, in the order the finger drew them — the line grows as
 * the finger moves, shortens when the kid drags back, and clears on
 * 「重畫」. The cells themselves keep their colours (path cells look the
 * same as unvisited cells); the Pokemon sitting at the path's end shows
 * how far the kid has got. 109 mazes per session, no timer; finishing a
 * maze collects the Pokemon shown. */
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
    var path by remember(mazeKey) { mutableStateOf(listOf(maze?.start ?: (0 to 0))) }
    var solved by remember(mazeKey) { mutableStateOf(false) }
    var gridSize by remember(mazeKey) { mutableStateOf(IntSize.Zero) }

    fun nextMaze() {
        if (mazeIndex + 1 < mazes.size) {
            mazeIndex++
        } else {
            showComplete = true
        }
    }

    /** Extend the drawn path with the cell under the finger at [offset]. */
    fun tryDraw(offset: Offset) {
        val currentMaze = maze ?: return
        if (solved) return
        val cell = mazeCellAtOffset(
            currentMaze.size,
            gridSize.width.toFloat(),
            gridSize.height.toFloat(),
            offset.x,
            offset.y,
        ) ?: return
        val newPath = mazeExtendPath(currentMaze, path, cell)
        if (newPath == path) return
        path = newPath
        sounds.flip()
        if (isMazePathComplete(currentMaze, newPath)) {
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
            text = if (solved) "到了！是${pokemon?.name}！🎉" else "用手指畫一條路，把${pokemon?.name ?: "寶可夢"}帶到蘋果那裡！",
            fontSize = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (maze != null && pokemon != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { gridSize = it }
                    .pointerInput(mazeKey) {
                        detectDragGestures(
                            onDragStart = { offset -> tryDraw(offset) },
                            onDrag = { change, _ -> tryDraw(change.position) },
                        )
                    },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    for (row in 0 until maze.size) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {
                            for (column in 0 until maze.size) {
                                val cell = row to column
                                val isPlayer = cell == path.lastOrNull()
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
                                                // Walked path cells keep their colour on
                                                // purpose: the route is shown by the blue
                                                // line drawn over the grid, and the
                                                // Pokemon at the path's end shows
                                                // the progress.
                                                else -> Color(0xFFFFF59D)
                                            },
                                            RoundedCornerShape(6.dp),
                                        ),
                                ) {
                                    when {
                                        isPlayer -> Image(
                                            painter = painterResource(drawableId(context, pokemon.drawable)),
                                            contentDescription = pokemon.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(2.dp),
                                        )
                                        isGoal -> Text("🍎", fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                // The finger-drawn route as a blue line over the grid: it
                // joins the centres of the path cells in the order the
                // finger drew them, so it grows as the finger moves,
                // shortens when the kid drags back, and clears on 「重畫」.
                // The cells underneath keep their colours.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = mazePathLinePoints(
                        path,
                        maze.size,
                        gridSize.width.toFloat(),
                        gridSize.height.toFloat(),
                    )
                    if (points.isEmpty()) return@Canvas
                    val lineColor = Color(0xFF1976D2)
                    val strokeWidth = gridSize.width.toFloat() / maze.size * 0.3f
                    if (points.size == 1) {
                        drawCircle(
                            color = lineColor,
                            radius = strokeWidth / 2f,
                            center = points[0],
                        )
                    } else {
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = lineColor,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
        if (solved) {
            Button(
                onClick = { nextMaze() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
            ) { Text("下一個迷宮 ➜", fontSize = 28.sp) }
        } else {
            Button(
                onClick = { path = listOf(maze?.start ?: (0 to 0)) },
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
            ) { Text("↺ 重畫", fontSize = 28.sp) }
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
