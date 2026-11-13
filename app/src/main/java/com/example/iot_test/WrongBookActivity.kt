package com.example.iot_test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 错题本：查看错题、移除单个、清空、重做错题
 */
class WrongBookActivity : AppCompatActivity() {

    private lateinit var store: WordStore
    private lateinit var tts: TtsHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrong_book)

        store = WordStore(this)
        tts = TtsHelper(this)
        recyclerView = findViewById(R.id.recyclerView)
        tvCount = findViewById(R.id.tvCount)
        tvEmpty = findViewById(R.id.tvEmpty)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnClear).setOnClickListener { confirmClear() }
        findViewById<TextView>(R.id.btnReview).setOnClickListener { startReview() }

        load()
    }

    private fun load() {
        val words = store.wrongWords()
        recyclerView.adapter = WrongAdapter(
            words,
            { word -> tts.speak(word.en) },
            { word -> removeWord(word) }
        )
        tvCount.text = "共 ${words.size} 个错题"
        val empty = words.isEmpty()
        tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun removeWord(word: Word) {
        store.removeWrong(word.en)
        Toast.makeText(this, "已移除 ${word.en}", Toast.LENGTH_SHORT).show()
        load()
    }

    private fun confirmClear() {
        if (store.wrongCount() == 0) {
            Toast.makeText(this, "错题本已经是空的", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("清空错题本")
            .setMessage("确定要清空所有错题吗？此操作不可撤销。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                store.clearWrong()
                load()
            }
            .show()
    }

    private fun startReview() {
        if (store.wrongCount() < 2) {
            Toast.makeText(this, "错题少于 2 个，先去首页测试攒点错题吧", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, QuizActivity::class.java).putExtra("wrong_only", true))
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
