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

    private val root = root
    private val progress = root.findViewById<View>(R.id.state_progress)
    private val error = root.findViewById<View>(R.id.state_error)
    private val empty = root.findViewById<View>(R.id.state_empty)
    private val errorText = root.findViewById<TextView>(R.id.state_error_text)
    private val emptyText = root.findViewById<TextView>(R.id.state_empty_text)
    private val retry = root.findViewById<Button>(R.id.state_retry)

    init {
        retry.setOnClickListener { onRetry() }
        root.visibility = View.GONE
    }

    fun bind(state: UiState<*>) {
        when (state) {
            is UiState.Loading -> showLoading()

            is UiState.Error -> {
                showError(state.message)
            }

            is UiState.Empty -> {
                showEmpty()
            }

            is UiState.Success<*> -> {
                hide()
            }
        }
    }

    fun showLoading() {
        root.visibility = View.VISIBLE
        hideChildren()
        progress.visibility = View.VISIBLE
    }

    fun showError(message: String) {
        root.visibility = View.VISIBLE
        hideChildren()
        errorText.text = message
        error.visibility = View.VISIBLE
    }

    fun showEmpty() {
        root.visibility = View.VISIBLE
        hideChildren()
        empty.visibility = View.VISIBLE
    }

    fun hide() {
        hideChildren()
        root.visibility = View.GONE
    }

    private fun hideChildren() {
        progress.visibility = View.GONE
        error.visibility = View.GONE
        empty.visibility = View.GONE
    }
}
