package com.example.tic_tac_toe // Make sure this package name matches your mobile module's package

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * This service runs in the background on the mobile device and listens for
 * messages sent from the Wear OS device.
 */
class DataLayerListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        // Check if the message path is the one we expect from our game
        if (messageEvent.path == "/game_end_status") {
            // Retrieve the message payload, which is the game status text
            val gameStatus = String(messageEvent.data)
            Log.d("DataLayerListener", "Message received on phone: $gameStatus")

            // Create an Intent to broadcast the received status to our MainActivity.
            // This is how we pass the data from this background service to our active UI.
            val messageIntent = Intent("GameStatusUpdate")
            messageIntent.putExtra("status", gameStatus)

            // Use LocalBroadcastManager to send the data safely and efficiently within our app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent)
        }
    }
}
