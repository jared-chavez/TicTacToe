package com.example.tic_tac_toe

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GameViewModel : ViewModel() {

    // Game State
    val board = mutableStateOf(initialBoard)
    val currentTurn = mutableStateOf(CellState.X)
    val message = mutableStateOf("Inicia la partida")
    val isGameOver = mutableStateOf(false)
    val winner = mutableStateOf(CellState.EMPTY)
    val showDialog = mutableStateOf(false)

    // Clients to be injected from Activity
    var messageClient: MessageClient? = null
    var nodeClient: NodeClient? = null

    // *** MODO DE PRUEBA ***
    // Cambia esto a `false` cuando quieras probar la conexión real con el reloj.
    private val isDebugMode = true
    private val myPlayer: CellState = CellState.O

    fun handlePlayerMove(row: Int, col: Int) {
        // No hacer nada si el juego terminó o la celda está ocupada
        if (isGameOver.value || board.value[row][col] != CellState.EMPTY) {
            return
        }

        // Si no estamos en modo de prueba, solo permitir mover si es nuestro turno.
        if (!isDebugMode && currentTurn.value != myPlayer) {
            return
        }

        // En modo de prueba, el jugador que mueve es el del turno actual.
        // Si no, es el jugador de este dispositivo (O).
        val playerMakingMove = if (isDebugMode) currentTurn.value else myPlayer

        processMove(row, col, playerMakingMove)
        sendMessageToWear(playerMakingMove, row, col)
    }

    fun processMove(row: Int, col: Int, player: CellState) {
        if (board.value[row][col] != CellState.EMPTY) return

        val newBoard = board.value.map { it.toMutableList() }.toMutableList()
        newBoard[row][col] = player
        board.value = newBoard.map { it.toList() }

        val newWinner = checkWinner(board.value)
        if (newWinner != CellState.EMPTY || isBoardFull(board.value)) {
            isGameOver.value = true
            winner.value = newWinner
            message.value = if (newWinner != CellState.EMPTY) "¡GANADOR: ${newWinner.name}!" else "¡EMPATE!"
            showDialog.value = true
        } else {
            currentTurn.value = if (player == CellState.X) CellState.O else CellState.X
            message.value = "Turno de"
        }
    }

    fun resetGame() {
        board.value = initialBoard
        currentTurn.value = CellState.X
        message.value = "Inicia la partida"
        isGameOver.value = false
        winner.value = CellState.EMPTY
        showDialog.value = false
        // No enviar mensaje de reinicio si estamos en modo debug para no interferir con el otro dispositivo
        if (!isDebugMode) {
            sendMessageToWear(CellState.EMPTY, -2, -2) // Special code for reset
        }
    }

    private fun sendMessageToWear(player: CellState, row: Int, col: Int) {
        if (messageClient == null || nodeClient == null || isDebugMode) return

        viewModelScope.launch {
            try {
                val nodes = nodeClient!!.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient!!.sendMessage(node.id, "/tic_tac_toe_move", "${player.name},$row,$col".toByteArray()).await()
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error sending message to wear: ", e)
            }
        }
    }
}
