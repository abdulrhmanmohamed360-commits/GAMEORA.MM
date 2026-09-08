package com.gameora.ui.common

import com.gameora.R
import com.gameora.domain.model.OrderStatus

object Formatters {

    fun price(value: Double?, currency: String?): String {
        val c = currency?.takeIf { it.isNotBlank() } ?: ""
        return when {
            value != null -> "%.2f %s".format(value, c).trim()
            c.isNotEmpty() -> c
            else -> "—"
        }
    }

    fun rating(value: Number): String =
        if (value.toDouble() > 0) "\u2605 $value" else ""

    fun orderStatusColorRes(status: OrderStatus): Int = when (status) {
        OrderStatus.COMPLETED -> R.color.success_color
        OrderStatus.CANCELLED, OrderStatus.REFUNDED -> R.color.error_color
        OrderStatus.PENDING_SELLER_APPROVAL -> R.color.warning_color
        OrderStatus.PROCESSING -> R.color.brand_primary
        OrderStatus.UNKNOWN -> R.color.text_secondary
    }
}
