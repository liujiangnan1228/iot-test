package com.example.iot_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 错题本适配器
 */
class WrongAdapter(
    private val list: List<Word>,
    private val onSpeakClick: (Word) -> Unit,
    private val onRemoveClick: (Word) -> Unit
) : RecyclerView.Adapter<WrongAdapter.WrongViewHolder>() {

    inner class WrongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEn: TextView = itemView.findViewById(R.id.tvEn)
        private val tvPhonetic: TextView = itemView.findViewById(R.id.tvPhonetic)
        private val tvCn: TextView = itemView.findViewById(R.id.tvCn)
        private val btnSpeak: TextView = itemView.findViewById(R.id.btnSpeak)
        private val btnRemove: TextView = itemView.findViewById(R.id.btnRemove)

        fun bind(word: Word) {
            tvEn.text = word.en
            tvPhonetic.text = word.phonetic.ifEmpty { "—" }
            tvCn.text = word.cn.ifEmpty { "（暂无释义）" }
            btnSpeak.setOnClickListener { onSpeakClick(word) }
            btnRemove.setOnClickListener { onRemoveClick(word) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WrongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wrong, parent, false)
        return WrongViewHolder(view)
    }

    override fun onBindViewHolder(holder: WrongViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}
