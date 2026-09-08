package com.gameora.ui.common

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.gameora.R
import com.gameora.util.UiState

/**
 * Drives the Loading / Empty / Error / Retry overlay shared by every server-driven screen.
 * It never shows fake content — on failure it shows the server message and a retry button.
 */
class StateView(root: View, onRetry: () -> Unit) {

    private val progress = root.findViewById<View>(R.id.state_progress)
    private val error = root.findViewById<View>(R.id.state_error)
    private val empty = root.findViewById<View>(R.id.state_empty)
    private val errorText = root.findViewById<TextView>(R.id.state_error_text)
    private val emptyText = root.findViewById<TextView>(R.id.state_empty_text)
    private val retry = root.findViewById<Button>(R.id.state_retry)

    init {
        retry.setOnClickListener { onRetry() }
    }

    fun bind(state: UiState<*>) {
        hideAll()
        when (state) {
            is UiState.Loading -> progress.visibility = View.VISIBLE
            is UiState.Error -> {
                errorText.text = state.message
                error.visibility = View.VISIBLE
            }
            is UiState.Empty -> empty.visibility = View.VISIBLE
            is UiState.Success<*> -> { /* content visible */ }
        }
    }

    fun showLoading() { hideAll(); progress.visibility = View.VISIBLE }
    fun showError(message: String) { hideAll(); errorText.text = message; error.visibility = View.VISIBLE }
    fun showEmpty() { hideAll(); empty.visibility = View.VISIBLE }
    fun hide() { hideAll() }

    private fun hideAll() {
        progress.visibility = View.GONE
        error.visibility = View.GONE
        empty.visibility = View.GONE
    }
}
