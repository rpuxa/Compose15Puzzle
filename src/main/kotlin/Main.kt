import androidx.compose.animation.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.random.Random

const val FIELD_SIZE = 4

fun main() {
    Window(undecorated = true) {
        MaterialTheme {
            val window = AppWindowAmbient.current!!
            val density = DensityAmbient.current
            Box(
                    Modifier
                            .sizeIn(maxWidth = Dp.Infinity, maxHeight = Dp.Infinity)
                            .onSizeChanged {
                                with(density) {
                                    window.setSize(
                                            it.width.toDp().value.toInt(),
                                            it.height.toDp().value.toInt()
                                    )
                                    window.window.isResizable = false
                                }
                            }
            ) {
                The15Puzzle(FIELD_SIZE)
            }
        }
    }
}

val directions = arrayOf(
        1 to 0,
        0 to 1,
        -1 to 0,
        0 to -1
)

data class Tile(val number: Int, var x: Int, var y: Int, var offset: Offset = Offset.Zero)

@Composable
private fun The15Puzzle(fieldSize: Int) {
    val tilesCount = fieldSize * fieldSize - 1

    var shuffling by remember { mutableStateOf(false) }
    var side = remember { true }
    val tiles = remember {
        List(tilesCount) {
            Tile(it, it % fieldSize, it / fieldSize)
        }
    }
    val offsets = remember {
        mutableStateListOf(
                *Array(tilesCount) {
                    Offset((it % fieldSize).toFloat(), (it / fieldSize).toFloat())
                }
        )
    }


    fun get(x: Int, y: Int) = tiles.find { it.x == x && it.y == y }

    fun move(tile: Tile) {
        loop@ for (direction in directions) {
            var x = tile.x
            var y = tile.y
            val collection = ArrayList<Triple<Tile?, Int, Int>>()
            while (x in 0 until fieldSize && y in 0 until fieldSize) {
                collection += Triple(get(x, y), x, y)
                x += direction.first
                y += direction.second
            }

            if (collection.any { it.first == null }) {
                var previous: Tile? = null
                for ((current, x, y) in collection) {
                    try {
                        if (previous == null) {
                            continue
                        }
                        previous.x = x
                        previous.y = y
                        offsets[previous.number] = Offset(x.toFloat(), y.toFloat())
                    } finally {
                        if (current == null) break@loop
                        previous = current
                    }
                }
            }
        }

    }

    fun moveRandom() {
        val present = Array(fieldSize) { BooleanArray(fieldSize) }
        tiles.forEach {
            present[it.x][it.y] = true
        }
        repeat(fieldSize) { x ->
            repeat(fieldSize) { y ->
                if (!present[x][y]) {
                    var rand = Random.nextInt(fieldSize - 1)
                    val result = if (side) {
                        if (rand >= y) {
                            rand++
                        }
                        get(x, rand)
                    } else {
                        if (rand >= x) {
                            rand++
                        }
                        get(rand, y)
                    }
                    side = !side
                    move(result!!)
                }
            }
        }
    }

    for (i in offsets.indices) {
        tiles[i].offset = if (!shuffling) {
            animate(offsets[i], animSpec = remember { spring(0.5f) })
        } else {
            animate(offsets[i], animSpec = tween(100))
        }
    }

    val scope = rememberCoroutineScope()
    remember(shuffling) {
        scope.launch {
            while (shuffling) {
                moveRandom()
                delay(100)
            }
        }
    }

    Column {
        Grid(
                fieldSize = fieldSize,
                grid = tiles,
        ) { tile ->
            TileWidget(
                    tile.number + 1,
                    onClick = {
                        if (!shuffling) {
                            move(tile)
                        }
                    }
            )
        }

        Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                    onClick = {
                        shuffling = !shuffling
                    },
                    modifier = Modifier.padding(8.dp)
            ) {
                Text(if (shuffling) "Stop shuffling" else "Shuffle")
            }
            if (tiles.all { it.number == (it.x + it.y * fieldSize) }) {
                Text(
                        "Solved!",
                        fontSize = 22.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TileWidget(number: Int, onClick: () -> Unit) {
    Button(
            onClick = onClick,
            modifier = Modifier
                    .size(100.dp)
                    .padding(2.dp)
    ) {
        Text(number.toString(), fontSize = 28.sp)
    }
}

@Composable
private fun Grid(
        fieldSize: Int,
        grid: List<Tile>,
        modifier: Modifier = Modifier,
        child: @Composable (Tile) -> Unit
) {
    Layout(
            children = {
                grid.forEach { child(it) }
            },
            modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        var width = 0
        var height = 0
        width = placeables.first().width
        height = placeables.first().height
        layout(width * fieldSize, height * fieldSize) {
            placeables.forEachIndexed { index, placeable ->
                require(width == placeable.width && height == placeable.height) {
                    "Children must have the same size"
                }
                val (x, y) = grid[index].offset
                placeable.place((x * width).toInt(), (y * height).toInt())
            }
        }
    }
}