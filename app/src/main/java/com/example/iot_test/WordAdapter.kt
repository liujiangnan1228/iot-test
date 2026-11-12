package com.example.iottest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.iottest.databinding.ItemWordBinding

class WordAdapter(
    private val list: List<Word>,
    private val onSpeakClick: (Word) -> Unit
) : RecyclerView.Adapter<WordAdapter.VH>() {

    inner class VH(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val w = list[position]
        holder.binding.tvEn.text = w.en
        holder.binding.tvPhonetic.text = w.phonetic
        holder.binding.tvCn.text = w.cn
        holder.binding.btnSpeak.setOnClickListener {
            onSpeakClick(w)
        }
    }

    override fun getItemCount() = list.size
}
