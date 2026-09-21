package com.gameora.ui.common

import com.gameora.R
import com.gameora.domain.model.OrderStatus
import com.gameora.domain.model.TicketStatus
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
        OrderStatus.CANCELLED, OrderStatus.REFUNDED, OrderStatus.DISPUTED -> R.color.error_color
        OrderStatus.PENDING_SELLER_APPROVAL -> R.color.warning_color
        OrderStatus.SELLER_ACCEPTED,
        OrderStatus.CHAT_ACTIVE,
        OrderStatus.ACCOUNT_DELIVERED,
        OrderStatus.BUYER_TESTING,
        OrderStatus.PROCESSING -> R.color.brand_primary
        OrderStatus.UNKNOWN -> R.color.text_secondary
    }

    fun ticketStatusColorRes(status: TicketStatus): Int = when (status) {
        TicketStatus.RESOLVED -> R.color.success_color
        TicketStatus.CLOSED -> R.color.text_secondary
        TicketStatus.WAITING_FOR_USER -> R.color.warning_color
        TicketStatus.OPEN, TicketStatus.IN_PROGRESS -> R.color.brand_primary
        TicketStatus.UNKNOWN -> R.color.text_secondary
    }

    fun ticketStatusLabel(status: TicketStatus): Int = when (status) {
        TicketStatus.OPEN -> R.string.ticket_status_open
        TicketStatus.IN_PROGRESS -> R.string.ticket_status_in_progress
        TicketStatus.WAITING_FOR_USER -> R.string.ticket_status_waiting_for_user
        TicketStatus.RESOLVED -> R.string.ticket_status_resolved
        TicketStatus.CLOSED -> R.string.ticket_status_closed
        TicketStatus.UNKNOWN -> R.string.ticket_status_open
    }

    /** يحوّل وقت ISO-8601 قادم من السيرفر لوقت محلي مختصر (HH:mm) لعرضه في القوائم. */
    fun shortTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(iso)
            val local = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
            "%02d:%02d".format(local.hour, local.minute)
        } catch (_: Exception) {
            ""
        }
    }
}
