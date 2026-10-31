package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic paginated envelope. Fields are nullable with defaults so the parser is
 * tolerant of servers that return only `items`, or `data`, or omit page metadata.
 */
data class PaginatedDto<T>(
    @SerializedName("items") val items: List<T> = emptyList(),
    @SerializedName("data") val data: List<T>? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("hasMore") val hasMore: Boolean? = null,
    @SerializedName("nextPage") val nextPage: Int? = null
)
