package com.gameora.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gameora.R

/**
 * A small generic [RecyclerView.Adapter] that maps a domain item to an item-layout view via
 * a bind lambda (using findViewById in the lambda). Supports an optional click handler and
 * a load-more footer for paginated screens. Avoids one adapter class per list.
 */
class GenericAdapter<T>(
    private val layoutRes: Int,
    private val onBind: (View, T, Int) -> Unit,
    private val onClick: ((T, Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<T>()
    private var footerVisible = false

    fun submit(list: List<T>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun append(list: List<T>) {
        if (list.isEmpty()) return
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun getItems(): List<T> = items.toList()

    fun showFooter(visible: Boolean) {
        if (footerVisible == visible) return
        footerVisible = visible
        if (visible) notifyItemInserted(itemCount - 1) else notifyItemRemoved(itemCount)
    }

    override fun getItemViewType(position: Int): Int =
        if (footerVisible && position == itemCount - 1) TYPE_FOOTER else TYPE_ITEM

    override fun getItemCount(): Int = items.size + if (footerVisible) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FOOTER) {
            FooterHolder(inflater.inflate(R.layout.item_loading_footer, parent, false))
        } else {
            ItemHolder(inflater.inflate(layoutRes, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemHolder) {
            val item = items[position]
            onBind(holder.itemView, item, position)
            holder.itemView.setOnClickListener { onClick?.invoke(item, position) }
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class FooterHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
    }
}
