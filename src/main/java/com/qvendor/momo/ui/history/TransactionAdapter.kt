package com.qvendor.momo.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qvendor.momo.R
import com.qvendor.momo.data.Transaction
import com.qvendor.momo.data.TransactionStatus

class TransactionAdapter(
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<TransactionListItem>()

    fun submit(list: List<Transaction>) {
        items.clear()
        items.addAll(TransactionGrouper.buildItems(list))
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is TransactionListItem.Header -> TYPE_HEADER
            is TransactionListItem.Entry -> TYPE_ENTRY
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inf.inflate(R.layout.item_transaction_section, parent, false))
        } else {
            EntryHolder(inf.inflate(R.layout.item_transaction, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.Header -> (holder as HeaderHolder).bind(item)
            is TransactionListItem.Entry -> (holder as EntryHolder).bind(item.transaction, onClick)
        }
    }

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.tvSectionDate)
        fun bind(item: TransactionListItem.Header) {
            label.text = item.label
        }
    }

    private class EntryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val avatar: FrameLayout = view.findViewById(R.id.avatarHolder)
        private val title: TextView = view.findViewById(R.id.tvTitle)
        private val time: TextView = view.findViewById(R.id.tvTime)
        private val amount: TextView = view.findViewById(R.id.tvAmount)
        private val status: TextView = view.findViewById(R.id.tvStatus)

        fun bind(t: Transaction, onClick: (Transaction) -> Unit) {
            val ctx = itemView.context
            title.text = TransactionGrouper.title(t.actionName, t.phone)
            time.text = TransactionGrouper.formatTime(t.createdAt)
            amount.text = TransactionGrouper.formatAmount(t.amount)

            when (t.status) {
                TransactionStatus.SUCCESSFUL -> {
                    status.setTextColor(ctx.getColor(R.color.qvendor_green_dark))
                    status.text = ctx.getString(R.string.status_successful)
                }
                TransactionStatus.PENDING -> {
                    status.setTextColor(0xFFB45309.toInt())
                    status.text = ctx.getString(R.string.status_pending)
                }
                TransactionStatus.FAILED -> {
                    status.setTextColor(ctx.getColor(R.color.qvendor_error))
                    status.text = ctx.getString(R.string.status_failed)
                }
            }

            val avatarRes = when (TransactionGrouper.avatarIndexFor(t.actionName)) {
                1 -> R.drawable.bg_avatar_beige
                2 -> R.drawable.bg_avatar_pink
                else -> R.drawable.bg_avatar_mint
            }
            avatar.setBackgroundResource(avatarRes)
            itemView.setOnClickListener { onClick(t) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }
}
