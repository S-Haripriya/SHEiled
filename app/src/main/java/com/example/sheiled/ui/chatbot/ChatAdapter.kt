package com.example.sheiled.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R

class ChatAdapter(
    private val list: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvMessage: TextView = v.findViewById(R.id.tvMessage)
        val root: LinearLayout = v.findViewById(R.id.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = list[position]
        holder.tvMessage.text = msg.message

        if (msg.isUser) {
            holder.root.gravity = android.view.Gravity.END
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_user)
        } else {
            holder.root.gravity = android.view.Gravity.START
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chatbot)
        }
    }

    override fun getItemCount(): Int = list.size
}
