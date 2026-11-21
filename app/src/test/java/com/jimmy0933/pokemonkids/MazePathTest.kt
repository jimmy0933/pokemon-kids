package com.jimmy0933.pokemonkids

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MazePathTest {

    /** A small fixed maze: 3x3, walls at (0,2) and (1,0); start (0,0), goal (2,2).
     * Open cells: (0,0) (0,1) (1,1) (1,2) (2,0) (2,1) (2,2). */
    private fun testMaze() = Maze(
        size = 3,
        walls = setOf(0 to 2, 1 to 0),
        start = 0 to 0,
        goal = 2 to 2,
    )

    @Test
    fun extendPathAppendsAdjacentWalkableCell() {
        val maze = testMaze()
        assertEquals(listOf(0 to 0, 0 to 1), mazeExtendPath(maze, listOf(0 to 0), 0 to 1))
        assertEquals(
            listOf(0 to 0, 0 to 1, 1 to 1),
            mazeExtendPath(maze, listOf(0 to 0, 0 to 1), 1 to 1),
        )
    }

    @Test
    fun extendPathIgnoresWallCell() {
        val maze = testMaze()
        val path = listOf(0 to 0, 0 to 1)
        assertEquals(path, mazeExtendPath(maze, path, 0 to 2))
    }

    @Test
    fun extendPathIgnoresNonAdjacentCell() {
        val maze = testMaze()
        val path = listOf(0 to 0)
        assertEquals(path, mazeExtendPath(maze, path, 1 to 1))
        assertEquals(path, mazeExtendPath(maze, path, 2 to 2))
    }

    @Test
    fun extendPathIgnoresOutOfBoundsCell() {
        val maze = testMaze()
        val path = listOf(0 to 0)
        assertEquals(path, mazeExtendPath(maze, path, -1 to 0))
        assertEquals(path, mazeExtendPath(maze, path, 0 to 3))
        assertEquals(path, mazeExtendPath(maze, path, 3 to 0))
    }

    @Test
    fun extendPathDoesNotDuplicateLastCell() {
        val maze = testMaze()
        val path = listOf(0 to 0, 0 to 1)
        assertEquals(path, mazeExtendPath(maze, path, 0 to 1))
    }

    @Test
    fun extendPathTrimsBackWhenDraggingBack() {
        val maze = testMaze()
        val path = listOf(0 to 0, 0 to 1, 1 to 1)
        assertEquals(listOf(0 to 0, 0 to 1), mazeExtendPath(maze, path, 0 to 1))
        assertEquals(listOf(0 to 0), mazeExtendPath(maze, path, 0 to 0))
    }

    @Test
    fun extendPathFromEmptyOnlyAcceptsStart() {
        val maze = testMaze()
        assertEquals(listOf(0 to 0), mazeExtendPath(maze, emptyList(), 0 to 0))
        assertEquals(emptyList<MazeCell>(), mazeExtendPath(maze, emptyList(), 0 to 1))
    }

    @Test
    fun pathCompleteOnlyAtGoal() {
        val maze = testMaze()
        assertFalse(isMazePathComplete(maze, emptyList()))
        assertFalse(isMazePathComplete(maze, listOf(0 to 0)))
        assertFalse(isMazePathComplete(maze, listOf(0 to 0, 0 to 1)))
        assertTrue(isMazePathComplete(maze, listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2)))
    }

    @Test
    fun cellAtOffsetMapsPixelsToCells() {
        // 3x3 grid, 300x300 px: each cell is 100x100 px.
        assertEquals(0 to 0, mazeCellAtOffset(3, 300f, 300f, 0f, 0f))
        assertEquals(0 to 0, mazeCellAtOffset(3, 300f, 300f, 99f, 99f))
        assertEquals(0 to 1, mazeCellAtOffset(3, 300f, 300f, 100f, 50f))
        assertEquals(1 to 0, mazeCellAtOffset(3, 300f, 300f, 50f, 100f))
        assertEquals(2 to 2, mazeCellAtOffset(3, 300f, 300f, 299f, 299f))
        assertEquals(1 to 2, mazeCellAtOffset(3, 300f, 300f, 250f, 150f))
    }

    @Test
    fun cellAtOffsetReturnsNullOutsideGrid() {
        assertNull(mazeCellAtOffset(3, 300f, 300f, -1f, 50f))
        assertNull(mazeCellAtOffset(3, 300f, 300f, 50f, -1f))
        assertNull(mazeCellAtOffset(3, 300f, 300f, 300f, 50f))
        assertNull(mazeCellAtOffset(3, 300f, 300f, 50f, 300f))
        assertNull(mazeCellAtOffset(3, 0f, 300f, 50f, 50f))
        assertNull(mazeCellAtOffset(3, 300f, 0f, 50f, 50f))
    }

    @Test
    fun defaultMazeIsNineByNine() {
        // v2.8 grows the maze from 7x7 to 9x9.
        assertEquals(9, MAZE_SIZE)
        val maze = makeMaze(0L)
        assertEquals(9, maze.size)
        assertEquals(0 to 0, maze.start)
        assertEquals(8 to 8, maze.goal)
        assertTrue(isMazeSolvable(maze))
        // Finger-drawing the BFS shortest path still completes a 9x9 maze.
        val solution = findMazePath(maze)!!
        var path = listOf(maze.start)
        for (cell in solution.drop(1)) {
            path = mazeExtendPath(maze, path, cell)
        }
        assertTrue(isMazePathComplete(maze, path))
        assertEquals(solution, path)
    }

    @Test
    fun nineByNineMazeOpenWallBalance() {
        // 9x9 = 81 cells: the carved path is 2*9-1 = 17 cells, plus 9 extra
        // open cells, so 26 open and 55 walls — a maze, not a corridor,
        // and not almost all walls either.
        val maze = makeMaze(0L)
        assertEquals(55, maze.walls.size)
        assertEquals(26, 81 - maze.walls.size)
        assertTrue(maze.start !in maze.walls)
        assertTrue(maze.goal !in maze.walls)
    }

    @Test
    fun cellAtOffsetMapsPixelsToCellsOnNineByNine() {
        // 9x9 grid, 900x900 px: each cell is 100x100 px.
        assertEquals(0 to 0, mazeCellAtOffset(9, 900f, 900f, 0f, 0f))
        assertEquals(0 to 3, mazeCellAtOffset(9, 900f, 900f, 350f, 50f))
        assertEquals(3 to 0, mazeCellAtOffset(9, 900f, 900f, 50f, 350f))
        assertEquals(8 to 8, mazeCellAtOffset(9, 900f, 900f, 899f, 899f))
        assertEquals(3 to 5, mazeCellAtOffset(9, 900f, 900f, 550f, 350f))
        assertNull(mazeCellAtOffset(9, 900f, 900f, 900f, 50f))
        assertNull(mazeCellAtOffset(9, 900f, 900f, 50f, 900f))
    }

    @Test
    fun drawingAlongShortestPathCompletesAnyMaze() {
        // Finger-draw the BFS shortest path for 50 random mazes (default
        // 9x9 grid since v2.8): every step must be accepted and the path
        // must end at the apple.
        for (seed in 0L until 50L) {
            val maze = makeMaze(seed)
            val solution = findMazePath(maze)!!
            var path = listOf(maze.start)
            for (cell in solution.drop(1)) {
                val next = mazeExtendPath(maze, path, cell)
                assertTrue("seed $seed: step to $cell must be accepted", next.size == path.size + 1)
                path = next
            }
            assertTrue("seed $seed: drawn path must reach the goal", isMazePathComplete(maze, path))
            assertEquals(solution, path)
        }
    }

    @Test
    fun drawingIntoWallNeverMovesThePath() {
        // Walk the solution, but try to step into a wall at every stop: the
        // path must stay exactly where it was.
        for (seed in 0L until 20L) {
            val maze = makeMaze(seed)
            val solution = findMazePath(maze)!!
            var path = listOf(maze.start)
            for (cell in solution.drop(1)) {
                val wall = maze.walls.firstOrNull() ?: break
                assertEquals(path, mazeExtendPath(maze, path, wall))
                path = mazeExtendPath(maze, path, cell)
            }
        }
    }

    @Test
    fun pathLinePointsAreCellCentresInPathOrder() {
        // 3x3 grid, 300x300 px: each cell is 100x100 px, so the centre of
        // (row, column) is (column * 100 + 50, row * 100 + 50).
        val points = mazePathLinePoints(
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2),
            3, 300f, 300f,
        )
        assertEquals(
            listOf(
                Offset(50f, 50f),
                Offset(150f, 50f),
                Offset(150f, 150f),
                Offset(250f, 150f),
                Offset(250f, 250f),
            ),
            points,
        )
    }

    @Test
    fun pathLinePointsEmptyForEmptyPathOrBadGrid() {
        assertEquals(emptyList<Offset>(), mazePathLinePoints(emptyList(), 3, 300f, 300f))
        assertEquals(
            emptyList<Offset>(),
            mazePathLinePoints(listOf(0 to 0), 3, 0f, 300f),
        )
        assertEquals(
            emptyList<Offset>(),
            mazePathLinePoints(listOf(0 to 0), 0, 300f, 300f),
        )
    }

    @Test
    fun pathLinePointsShortenWhenPathTrims() {
        // The line follows the path: trimming the path back (dragging back)
        // shortens the point list to the matching prefix.
        val full = mazePathLinePoints(
            listOf(0 to 0, 0 to 1, 1 to 1),
            3, 300f, 300f,
        )
        val trimmed = mazePathLinePoints(
            listOf(0 to 0, 0 to 1),
            3, 300f, 300f,
        )
        assertEquals(3, full.size)
        assertEquals(2, trimmed.size)
        assertEquals(full.subList(0, 2), trimmed)
        // A single-cell path is a single point (the start dot).
        assertEquals(
            listOf(Offset(50f, 50f)),
            mazePathLinePoints(listOf(0 to 0), 3, 300f, 300f),
        )
    }

    @Test
    fun pathLinePointsOnNineByNine() {
        // 9x9 grid, 900x900 px: the goal cell (8, 8) has centre (850, 850).
        val points = mazePathLinePoints(listOf(0 to 0, 8 to 8), 9, 900f, 900f)
        assertEquals(listOf(Offset(50f, 50f), Offset(850f, 850f)), points)
    }

    @Test
    fun pathLinePointsFollowDrawnSolution() {
        // For 20 random 9x9 mazes, the line points of the finger-drawn
        // solution have one point per path cell, in order, ending at the
        // goal cell's centre.
        for (seed in 0L until 20L) {
            val maze = makeMaze(seed)
            val solution = findMazePath(maze)!!
            val points = mazePathLinePoints(solution, maze.size, 900f, 900f)
            assertEquals(solution.size, points.size)
            assertEquals(Offset(850f, 850f), points.last())
            assertEquals(Offset(50f, 50f), points.first())
        }
    }
}
