package com.qvendor.momo.ui.history

import com.qvendor.momo.data.Transaction

sealed class TransactionListItem {
    data class Header(val label: String) : TransactionListItem()
    data class Entry(val transaction: Transaction) : TransactionListItem()
}

object TransactionGrouper {
    private val dayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    private val displayTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)

    fun sectionLabel(epoch: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epoch }
        val today = java.util.Calendar.getInstance()
        val sdf = dayFormat
        val sameDay = sdf.format(cal.time) == sdf.format(today.time)
        if (sameDay) return "Today"
        today.add(java.util.Calendar.DAY_OF_YEAR, -1)
        if (sdf.format(cal.time) == sdf.format(today.time)) return "Yesterday"
        return java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US).format(cal.time)
    }

    fun formatTime(epoch: Long): String = displayTime.format(java.util.Date(epoch))

    fun formatAmount(amount: Double): String = String.format(java.util.Locale.US, "-GHS %.2f", amount)

    fun title(actionName: String, phone: String): String {
        return if (phone.isBlank()) actionName else "$actionName · $phone"
    }

    fun avatarIndexFor(actionName: String): Int {
        if (actionName.isEmpty()) return 0
        return (actionName.hashCode().toLong() and 0x7fffffffL).toInt() % 3
    }

    fun buildItems(transactions: List<Transaction>): List<TransactionListItem> {
        val out = ArrayList<TransactionListItem>()
        var lastDay: String? = null
        for (t in transactions) {
            val label = sectionLabel(t.createdAt)
            if (label != lastDay) {
                out.add(TransactionListItem.Header(label))
                lastDay = label
            }
            out.add(TransactionListItem.Entry(t))
        }
        return out
    }
}
