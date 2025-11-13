package com.example.tic_tac_toe

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ===================== ESTILOS, COLORES Y LÓGICA COMPARTIDA =====================

enum class CellState { EMPTY, X, O }
val initialBoard = List(3) { List(3) { CellState.EMPTY } }

val ColorX = Color(0xFF00BFFF) // Azul Neón
val ColorO = Color(0xFFFF2400) // Rojo Escarlata Neón
val ColorGrid = Color.White.copy(alpha = 0.7f)
val ColorText = Color.White
val ColorBackground = Color.Black

fun Modifier.neonGlow(color: Color, radius: Dp = 16.dp, alpha: Float = 0.9f): Modifier = this.drawBehind {
    val glowRadius = radius.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.copy(alpha = alpha).toArgb()
        frameworkPaint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

fun checkWinner(board: List<List<CellState>>): CellState {
    for (i in 0..2) {
        if (board[i][0] != CellState.EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) return board[i][0]
        if (board[0][i] != CellState.EMPTY && board[0][i] == board[1][i] && board[1][i] == board[2][i]) return board[0][i]
    }
    if (board[0][0] != CellState.EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) return board[0][0]
    if (board[0][2] != CellState.EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) return board[0][2]
    return CellState.EMPTY
}

fun isBoardFull(board: List<List<CellState>>): Boolean = board.all { row -> row.all { it != CellState.EMPTY } }


// ===================== UI COMPOSABLES (ADAPTADOS PARA MÓVIL) =====================

@Composable
fun TicTacToeScreen(
    board: List<List<CellState>>, currentTurn: CellState, message: String, isGameOver: Boolean, winner: CellState,
    showDialog: Boolean, onCellClick: (row: Int, col: Int) -> Unit, onReset: () -> Unit, onDismissDialog: () -> Unit
) {
    val turnScale by animateFloatAsState(targetValue = if (!isGameOver) 1.05f else 1f, animationSpec = tween(durationMillis = 500), label = "turnAnimation")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Tic-Tac-Toe") }, backgroundColor = Color.Black) },
        backgroundColor = ColorBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(it).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp) // Más espacio en móvil
            ) {
                Text(
                    text = message,
                    color = when {
                        isGameOver && winner == CellState.X -> ColorX
                        isGameOver && winner == CellState.O -> ColorO
                        else -> ColorText
                    },
                    fontWeight = FontWeight.Bold, fontSize = 28.sp, // Letra más grande para móvil
                    modifier = Modifier.scale(turnScale), textAlign = TextAlign.Center
                )
                if (!isGameOver) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Turno: ", color = ColorText.copy(alpha = 0.7f), fontSize = 20.sp)
                        Text(text = currentTurn.name, color = if (currentTurn == CellState.X) ColorX else ColorO, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }
            }

            Board(
                board = board,
                onCellClick = onCellClick,
                isEnabled = !isGameOver,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }

    if (showDialog) {
        GameResultDialog(winner = winner, onReset = onReset, onDismiss = onDismissDialog)
    }
}

@Composable
fun Board(
    board: List<List<CellState>>,
    onCellClick: (row: Int, col: Int) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(1f).background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            board.forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.weight(1f)) {
                    row.forEachIndexed { colIndex, cellState ->
                        Cell(
                            state = cellState,
                            onClick = { onCellClick(rowIndex, colIndex) },
                            modifier = Modifier.weight(1f),
                            isEnabled = isEnabled
                        )
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val third = size.width / 3
            val strokeWidth = 4.dp.toPx() // Líneas más gruesas en móvil
            drawLine(ColorGrid, start = Offset(third, 0f), end = Offset(third, size.height), strokeWidth)
            drawLine(ColorGrid, start = Offset(third * 2, 0f), end = Offset(third * 2, size.height), strokeWidth)
            drawLine(ColorGrid, start = Offset(0f, third), end = Offset(size.width, third), strokeWidth)
            drawLine(ColorGrid, start = Offset(0f, third * 2), end = Offset(size.width, third * 2), strokeWidth)
        }
    }
}

@Composable
fun Cell(state: CellState, onClick: () -> Unit, modifier: Modifier, isEnabled: Boolean) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick, enabled = isEnabled && state == CellState.EMPTY),
        contentAlignment = Alignment.Center
    ) {
        val playerColor = when (state) {
            CellState.X -> ColorX
            CellState.O -> ColorO
            else -> Color.Transparent
        }

        if (state != CellState.EMPTY) {
            Box(modifier = Modifier.fillMaxSize(0.8f).neonGlow(color = playerColor))
            Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
                when (state) {
                    CellState.X -> drawChromaticSymbol(ColorX, ColorO) { drawX(it) }
                    CellState.O -> drawChromaticSymbol(ColorO, ColorX) { drawO(it) }
                    else -> {}
                }
            }
        }
    }
}

fun DrawScope.drawX(color: Color) {
    val strokeWidth = size.width * 0.2f
    drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(0f, size.height), Offset(size.width, 0f), strokeWidth, StrokeCap.Round)
}

fun DrawScope.drawO(color: Color) {
    val strokeWidth = size.width * 0.2f
    drawCircle(color, style = Stroke(width = strokeWidth))
}

fun DrawScope.drawChromaticSymbol(primary: Color, secondary: Color, drawCall: (Color) -> Unit) {
    val offset = size.width * 0.05f
    translate(left = offset, top = offset) {
        drawCall(secondary.copy(alpha = 0.7f))
    }
    drawCall(Color.White)
}

@Composable
fun GameResultDialog(winner: CellState, onReset: () -> Unit, onDismiss: () -> Unit) {
    val neonColor = when (winner) {
        CellState.X -> ColorX
        CellState.O -> ColorO
        CellState.EMPTY -> ColorGrid
    }
    val titleText = when (winner) {
        CellState.X -> "¡Gana X!"
        CellState.O -> "¡Gana O!"
        CellState.EMPTY -> "¡Empate!"
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .background(neonColor, shape = RoundedCornerShape(24.dp))
                .padding(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, shape = RoundedCornerShape(22.dp))
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = titleText,
                    color = neonColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { onReset(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = neonColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Jugar de Nuevo")
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Jugar de Nuevo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
