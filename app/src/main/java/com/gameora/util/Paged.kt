package com.gameora.util

/**
 * Pagination metadata returned by repositories for list endpoints.
 * All list screens load one page at a time and request more from the server.
 */
data class Paged<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
    val hasMore: Boolean
)
