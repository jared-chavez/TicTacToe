package com.example.tic_tac_toe.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Definición de los estados del juego
enum class CellState { EMPTY, X, O }

// Estado inicial del tablero 3x3
val initialBoard = List(3) { List(3) { CellState.EMPTY } }

class MainActivity : ComponentActivity() {

    // Cliente de Messaging API de Wear OS
    private lateinit var messageClient: MessageClient

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        messageClient = Wearable.getMessageClient(this)

        // La función setContent define la UI de tu actividad usando Compose
        setContent {
            // FIX: Using the default Wear Material theme until WearOSTicTacToeTheme is created.
            MaterialTheme {
                // FIX: Removed the incorrect Material3 Surface.
                // TicTacToeScreen is the main content.
                TicTacToeScreen(
                    onGameEnd = { winner -> sendMessageToMobile(winner) }
                )
            }
        }
    }

    // Función que implementa el envío de datos al móvil
    private fun sendMessageToMobile(winner: CellState) {
        val messagePath = "/game_end_status"
        val winnerText = if (winner == CellState.EMPTY) "EMPATE" else "GANADOR: ${winner.name}"
        val payload = winnerText.toByteArray()

        lifecycleScope.launch {
            try {
                // 1. Obtener todos los nodos (dispositivos) conectados
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()

                // 2. Enviar el mensaje a cada nodo (móvil emparejado)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, messagePath, payload).await()
                }
                println("Mensaje enviado a ${nodes.size} nodo(s) con estado: $winnerText")

            } catch (e: Exception) {
                println("Error al enviar mensaje a dispositivo móvil: ${e.message}")
            }
        }
    }
}

// --- Componentes Composable Actualizados ---

@Composable
fun TicTacToeScreen(onGameEnd: (CellState) -> Unit) {
    var board by remember { mutableStateOf(initialBoard) }
    var currentTurn by remember { mutableStateOf(CellState.X) }
    var message by remember { mutableStateOf("Turno de X") }
    var isGameOver by remember { mutableStateOf(false) }

    fun resetGame() {
        board = initialBoard
        currentTurn = CellState.X
        message = "Turno de X"
        isGameOver = false
    }

    fun handleCellClick(row: Int, col: Int) {
        if (isGameOver || board[row][col] != CellState.EMPTY) {
            return
        }

        val newBoard = board.map { it.toMutableList() }.toMutableList()
        newBoard[row][col] = currentTurn
        board = newBoard

        val winner = checkWinner(board)

        if (winner != CellState.EMPTY) {
            message = "¡Ganador: ${winner.name}!"
            isGameOver = true
            onGameEnd(winner)
        } else if (isBoardFull(board)) {
            message = "¡Empate!"
            isGameOver = true
            onGameEnd(CellState.EMPTY)
        } else {
            currentTurn = if (currentTurn == CellState.X) CellState.O else CellState.X
            message = "Turno de ${currentTurn.name}"
        }
    }

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            autoCentering = AutoCenteringParams(itemIndex = 1) // Center the board
        ) {
            item {
                Text(
                    text = message,
                    color = if (isGameOver) Color.Red else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Board(board = board, onCellClick = ::handleCellClick)
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = ::resetGame,
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                    enabled = isGameOver || isBoardFull(board)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reiniciar Juego",
                        // FIX: Replaced unresolved 'IconSize' with a standard size modifier.
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Lógica del juego
fun isBoardFull(board: List<List<CellState>>): Boolean {
    return board.all { row -> row.all { it != CellState.EMPTY } }
}

fun checkWinner(board: List<List<CellState>>): CellState {
    for (i in 0..2) {
        if (board[i][0] != CellState.EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
            return board[i][0]
        }
        if (board[0][i] != CellState.EMPTY && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
            return board[0][i]
        }
    }
    if (board[0][0] != CellState.EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
        return board[0][0]
    }
    if (board[0][2] != CellState.EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
        return board[0][2]
    }
    return CellState.EMPTY
}

// Componentes Composable
@Composable
fun Board(board: List<List<CellState>>, onCellClick: (row: Int, col: Int) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(150.dp)
    ) {
        board.forEachIndexed { row, rowData ->
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                rowData.forEachIndexed { col, cellState ->
                    Cell(
                        state = cellState,
                        onClick = { onCellClick(row, col) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun Cell(state: CellState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (state) {
                CellState.X -> "X"
                CellState.O -> "O"
                CellState.EMPTY -> ""
            },
            color = if (state == CellState.X) Color.Cyan else Color.Magenta,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// Preview
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    // FIX: Using the default theme for the preview as well.
    MaterialTheme {
        TicTacToeScreen(onGameEnd = {})
    }
}
