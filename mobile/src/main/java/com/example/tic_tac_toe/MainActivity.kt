package com.example.tic_tac_toe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inject the clients into the ViewModel
        gameViewModel.messageClient = Wearable.getMessageClient(this)
        gameViewModel.nodeClient = Wearable.getNodeClient(this)

        // Make the ViewModel accessible from the service
        GameViewModelHolder.viewModel = gameViewModel

        setContent {
            TicTacToeScreen(
                board = gameViewModel.board.value,
                currentTurn = gameViewModel.currentTurn.value,
                message = gameViewModel.message.value,
                isGameOver = gameViewModel.isGameOver.value,
                winner = gameViewModel.winner.value,
                showDialog = gameViewModel.showDialog.value,
                onCellClick = { row, col -> gameViewModel.handlePlayerMove(row, col) },
                onReset = { gameViewModel.resetGame() },
                onDismissDialog = { gameViewModel.showDialog.value = false }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the ViewModel reference to avoid memory leaks
        GameViewModelHolder.viewModel = null
    }
}
