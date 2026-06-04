package com.medha.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medha.app.R
import com.medha.app.data.MessageEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private var items: List<MessageEntry>
) : RecyclerView.Adapter<ConversationAdapter.VH>() {

    private val timeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val who = view.findViewById<TextView>(R.id.tvWho)
        val text = view.findViewById<TextView>(R.id.tvText)
        val time = view.findViewById<TextView>(R.id.tvTime)
    }

    fun submit(newItems: List<MessageEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val prefix = if (item.isAI) "Medha → ${item.contactName}" else item.contactName
        holder.who.text = prefix
        holder.text.text = item.text
        holder.time.text = timeFormat.format(Date(item.timestamp))
    }
}
