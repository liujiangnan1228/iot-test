package com.example.iot_test

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

/**
 * 首页：单词列表 + 搜索 + 词库导入 + 入口按钮
 */
class MainActivity : AppCompatActivity() {

    private lateinit var store: WordStore
    private lateinit var tts: TtsHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBox: EditText
    private lateinit var tvSubtitle: TextView
    private lateinit var tvEmpty: TextView

    private var allWords: List<Word> = emptyList()

    // 选择本地词库文件（系统文件选择器，不需要存储权限）
    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> importFromUri(uri) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = WordStore(this)
        tts = TtsHelper(this)

        recyclerView = findViewById(R.id.recyclerView)
        searchBox = findViewById(R.id.etSearch)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvEmpty = findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = LinearLayoutManager(this)

        searchBox.doAfterTextChanged { text ->
            showWords(filter(text?.toString() ?: ""))
        }

        findViewById<TextView>(R.id.btnImport).setOnClickListener { pickImportFile() }
        findViewById<TextView>(R.id.btnQuiz).setOnClickListener {
            if (store.allWords().size < 2) {
                toast("词库不足 2 个单词，无法测试")
            } else {
                startActivity(Intent(this, QuizActivity::class.java))
            }
        }
        findViewById<TextView>(R.id.btnWrongBook).setOnClickListener {
            startActivity(Intent(this, WrongBookActivity::class.java))
        }
        findViewById<TextView>(R.id.btnDashboard).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        allWords = store.allWords()
        showWords(filter(searchBox.text?.toString() ?: ""))
        tvSubtitle.text = "四级词库 ${allWords.size} 词 · 错题 ${store.wrongCount()} 个 · 今日已练 ${store.todayCount()} 个"
    }

    private fun filter(keyword: String): List<Word> {
        val key = keyword.trim().lowercase(Locale.US)
        if (key.isEmpty()) return allWords
        return allWords.filter {
            it.en.lowercase(Locale.US).contains(key) || it.cn.contains(keyword.trim())
        }
    }

    private fun showWords(words: List<Word>) {
        recyclerView.adapter = WordAdapter(words) { word -> speak(word) }
        tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (words.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun speak(word: Word) {
        if (!tts.isReady()) {
            toast("语音引擎正在初始化，请稍后再点一次")
            return
        }
        tts.speak(word.en)
    }

    private fun pickImportFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
        importLauncher.launch(intent)
    }

    private fun importFromUri(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val added = store.importFromText(text)
            refresh()
            if (added > 0) {
                toast("导入成功，新增 $added 个单词")
            } else {
                toast("没有新增单词：文件格式不对，或单词已存在")
            }
        } catch (e: Exception) {
            toast("导入失败：${e.message}")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
