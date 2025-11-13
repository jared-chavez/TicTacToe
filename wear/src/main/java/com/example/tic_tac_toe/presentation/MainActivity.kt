package com.example.tic_tac_toe.presentation

import android.graphics.BlurMaskFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ===================== ESTILOS Y COLORES NEÓN =====================
val ColorX = Color(0xFF00BFFF) // Azul Neón
val ColorO = Color(0xFFFF2400) // Rojo Escarlata Neón
val ColorGrid = Color.White.copy(alpha = 0.7f) // Blanco semi-transparente para la cuadrícula
val ColorText = Color.White
val ColorBackground = Color.Black

fun Modifier.neonGlow(color: Color, radius: Dp = 12.dp, alpha: Float = 0.9f): Modifier = this.drawBehind {
    val glowRadius = radius.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.copy(alpha = alpha).toArgb()
        frameworkPaint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}
// ======================================================================

enum class CellState { EMPTY, X, O }
val initialBoard = List(3) { List(3) { CellState.EMPTY } }

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
    private lateinit var messageClient: MessageClient
    private val boardState = mutableStateOf(initialBoard)
    private val currentTurnState = mutableStateOf(CellState.X)
    private val messageState = mutableStateOf("Inicia la partida")
    private val isGameOverState = mutableStateOf(false)
    private val winnerState = mutableStateOf(CellState.EMPTY)
    private val showDialogState = mutableStateOf(false)

    // *** MODO DE PRUEBA ***
    // Cambia esto a `false` cuando conectes el móvil real.
    private val DEBUG_MODE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        messageClient = Wearable.getMessageClient(this)
        if (!DEBUG_MODE) {
            messageClient.addListener(this)
        }

        setContent {
            MaterialTheme {
                TicTacToeScreen(
                    board = boardState.value,
                    currentTurn = currentTurnState.value,
                    message = messageState.value,
                    isGameOver = isGameOverState.value,
                    winner = winnerState.value,
                    showDialog = showDialogState.value,
                    onCellClick = ::handlePlayerMove,
                    onReset = ::resetGame,
                    onDismissDialog = { showDialogState.value = false }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!DEBUG_MODE) {
            messageClient.removeListener(this)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val GAME_PATH = "/tic_tac_toe_move"
        if (messageEvent.path == GAME_PATH) {
            val moveData = String(messageEvent.data).split(",")
            if (moveData.size == 3 && moveData[0] == CellState.O.name) {
                processMove(moveData[1].toInt(), moveData[2].toInt(), CellState.O)
            }
        }
    }

    // Unifica el manejo de movimientos
    private fun handlePlayerMove(row: Int, col: Int) {
        if (isGameOverState.value || boardState.value[row][col] != CellState.EMPTY) return

        val currentPlayer = currentTurnState.value

        // Si estamos en modo de prueba, cualquier clic hace el movimiento del turno actual.
        // Si no, solo el jugador X (reloj) puede iniciar un movimiento aquí.
        if (DEBUG_MODE || currentPlayer == CellState.X) {
            processMove(row, col, currentPlayer)
            if (!DEBUG_MODE && currentPlayer == CellState.X) {
                sendMessageToMobile(CellState.X, row, col)
            }
        }
    }

    private fun processMove(row: Int, col: Int, player: CellState) {
        val newBoard = boardState.value.map { it.toMutableList() }.toMutableList()
        newBoard[row][col] = player
        boardState.value = newBoard.map { it.toList() }

        val winner = checkWinner(boardState.value)
        if (winner != CellState.EMPTY || isBoardFull(boardState.value)) {
            isGameOverState.value = true
            winnerState.value = winner
            messageState.value = if (winner != CellState.EMPTY) "¡GANADOR: ${winner.name}!" else "¡EMPATE!"
            showDialogState.value = true
            if (!DEBUG_MODE) sendMessageToMobile(winner, -1, -1) // Fin de juego
        } else {
            currentTurnState.value = if (player == CellState.X) CellState.O else CellState.X
            messageState.value = "Turno de"
        }
    }

    private fun resetGame() {
        boardState.value = initialBoard
        currentTurnState.value = CellState.X
        messageState.value = "Inicia la partida"
        isGameOverState.value = false
        winnerState.value = CellState.EMPTY
        showDialogState.value = false
        if (!DEBUG_MODE) sendMessageToMobile(CellState.EMPTY, -2, -2) // Reinicio
    }

    private fun sendMessageToMobile(player: CellState, row: Int, col: Int) {
        val GAME_PATH = "/tic_tac_toe_move"
        val payload = "${player.name},$row,$col".toByteArray()
        lifecycleScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, GAME_PATH, payload).await()
                }
            } catch (e: Exception) { /* Silently ignore */ }
        }
    }
}

// ===================== UI COMPOSABLES (VERSIÓN FINAL CON LAYOUT CORREGIDO) =====================

@Composable
fun TicTacToeScreen(
    board: List<List<CellState>>, currentTurn: CellState, message: String, isGameOver: Boolean, winner: CellState,
    showDialog: Boolean, onCellClick: (row: Int, col: Int) -> Unit, onReset: () -> Unit, onDismissDialog: () -> Unit
) {
    val turnScale by animateFloatAsState(targetValue = if (!isGameOver) 1.05f else 1f, animationSpec = tween(durationMillis = 500), label = "turnAnimation")

    Scaffold(
        modifier = Modifier.fillMaxSize().background(ColorBackground),
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        timeText = { TimeText(timeTextStyle = TimeTextDefaults.timeTextStyle(color = ColorText.copy(alpha = 0.8f))) }
    ) {
        // Corrección de Layout: Usamos una Columna centrada, que es más simple y robusto.
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp), // Añadimos padding general
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Centra todo el contenido verticalmente
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = message,
                    color = when {
                        isGameOver && winner == CellState.X -> ColorX
                        isGameOver && winner == CellState.O -> ColorO
                        else -> ColorText
                    },
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, // Reducido de 20.sp
                    modifier = Modifier.scale(turnScale), textAlign = TextAlign.Center
                )
                if (!isGameOver) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Turno: ", color = ColorText.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(text = currentTurn.name, color = if (currentTurn == CellState.X) ColorX else ColorO, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            // El tablero ahora es responsivo, ocupando un porcentaje del ancho.
            Board(
                board = board,
                onCellClick = onCellClick,
                isEnabled = !isGameOver,
                modifier = Modifier.fillMaxWidth(0.8f) // Ocupa el 80% del ancho
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
    modifier: Modifier = Modifier // Usar modifier para controlar tamaño
) {
    Box(
        // El modifier se aplica aquí, y con aspectRatio forzamos que sea un cuadrado.
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Transparent),
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

        // La cuadrícula se dibuja basada en el tamaño del Canvas, que es ahora responsivo.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val third = size.width / 3 // Usamos width porque height y width son iguales (aspectRatio(1f))
            val strokeWidth = 2.dp.toPx()
            // Líneas verticales
            drawLine(ColorGrid, start = Offset(third, 0f), end = Offset(third, size.height), strokeWidth)
            drawLine(ColorGrid, start = Offset(third * 2, 0f), end = Offset(third * 2, size.height), strokeWidth)
            // Líneas horizontales
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

// ============== FUNCIONES DE DIBUJO PARA EL ESTILO NEÓN ============== 

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
        CellState.X -> "¡Ganaste!"
        CellState.O -> "¡Gana O!"
        CellState.EMPTY -> "¡Empate!"
    }
    Dialog(onDismissRequest = onDismiss) {
        // Modal con fondo sólido y borde de neón
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    // 1. El Box exterior simula el borde de neón.
                    .background(neonColor, shape = RoundedCornerShape(24.dp))
                    .padding(3.dp)
            ) {
                // 2. La Columna interior tiene fondo negro sólido.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, shape = RoundedCornerShape(22.dp))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = titleText,
                        color = neonColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    // Botón mejorado
                    Button(
                        onClick = { onReset(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = neonColor,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Jugar de Nuevo",
                                modifier = Modifier.size(ButtonDefaults.DefaultIconSize)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Jugar de Nuevo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun isBoardFull(board: List<List<CellState>>): Boolean = board.all { row -> row.all { it != CellState.EMPTY } }

fun checkWinner(board: List<List<CellState>>): CellState {
    for (i in 0..2) {
        if (board[i][0] != CellState.EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) return board[i][0]
        if (board[0][i] != CellState.EMPTY && board[0][i] == board[1][i] && board[1][i] == board[2][i]) return board[0][i]
    }
    if (board[0][0] != CellState.EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) return board[0][0]
    if (board[0][2] != CellState.EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) return board[0][2]
    return CellState.EMPTY
}
