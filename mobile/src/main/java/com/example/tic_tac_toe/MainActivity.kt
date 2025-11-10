package com.example.tic_tac_toe // Make sure this package name is correct

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var gameStatusTextView: TextView

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Get the status from the intent and update the TextView
            val status = intent?.getStringExtra("status") ?: "No status received"
            gameStatusTextView.text = status
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameStatusTextView = findViewById(R.id.game_status_text)

        // Register the receiver to listen for "GameStatusUpdate" broadcasts
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(messageReceiver, IntentFilter("GameStatusUpdate"))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to prevent memory leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }
}
