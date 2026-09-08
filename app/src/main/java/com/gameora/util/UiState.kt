package com.gameora.util

/**
 * Generic UI state used by every screen so the UI always renders one of:
 * Loading / Success / Error / Empty — never fake content.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
