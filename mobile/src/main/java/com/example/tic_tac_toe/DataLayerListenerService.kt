package com.example.tic_tac_toe

import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        if (messageEvent.path == "/tic_tac_toe_move") {
            val moveData = String(messageEvent.data).split(",")
            val player = CellState.valueOf(moveData[0])
            val row = moveData[1].toInt()
            val col = moveData[2].toInt()

            // Switch to the main thread to safely update the ViewModel's state
            Handler(Looper.getMainLooper()).post {
                GameViewModelHolder.viewModel?.let { viewModel ->
                    if (row == -2 && col == -2) { // Reset code
                        viewModel.resetGame()
                    } else {
                        viewModel.processMove(row, col, player)
                    }
                }
            }
        }
    }
}
