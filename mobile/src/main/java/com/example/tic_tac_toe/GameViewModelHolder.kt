package com.example.tic_tac_toe

/**
 * A simple static object to hold a reference to our GameViewModel.
 * This allows the background service to access the ViewModel instance
 * created by the MainActivity.
 */
object GameViewModelHolder {
    var viewModel: GameViewModel? = null
}
