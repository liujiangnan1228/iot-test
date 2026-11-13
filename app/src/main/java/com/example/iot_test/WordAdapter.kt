package com.example.iot_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 单词列表适配器（首页）
 */
class WordAdapter(
    private val list: List<Word>,
    private val onSpeakClick: (Word) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEn: TextView = itemView.findViewById(R.id.tvEn)
        private val tvPhonetic: TextView = itemView.findViewById(R.id.tvPhonetic)
        private val tvCn: TextView = itemView.findViewById(R.id.tvCn)
        private val btnSpeak: TextView = itemView.findViewById(R.id.btnSpeak)

        fun bind(word: Word) {
            tvEn.text = word.en
            tvPhonetic.text = word.phonetic.ifEmpty { "—" }
            tvCn.text = word.cn.ifEmpty { "（暂无释义）" }
            btnSpeak.setOnClickListener { onSpeakClick(word) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}
