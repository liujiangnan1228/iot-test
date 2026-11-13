package com.example.iot_test

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * 单词测试：看英文选中文；答错自动进错题本，答题数据写入看板
 * extra "wrong_only" = true 时只测错题本里的单词
 */
class QuizActivity : AppCompatActivity() {

    private lateinit var store: WordStore
    private lateinit var tts: TtsHelper

    private lateinit var tvProgress: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvWordEn: TextView
    private lateinit var tvWordPhonetic: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnSpeak: TextView
    private lateinit var btnNext: TextView
    private lateinit var optionViews: List<TextView>

    private var quizWords: List<Word> = emptyList()
    private var index = 0
    private var correctCount = 0
    private var correctIndex = -1
    private var answered = false
    private var wrongOnly = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        store = WordStore(this)
        tts = TtsHelper(this)
        wrongOnly = intent.getBooleanExtra("wrong_only", false)

        tvProgress = findViewById(R.id.tvProgress)
        tvScore = findViewById(R.id.tvScore)
        tvWordEn = findViewById(R.id.tvWordEn)
        tvWordPhonetic = findViewById(R.id.tvWordPhonetic)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnSpeak = findViewById(R.id.btnSpeakQuiz)
        btnNext = findViewById(R.id.btnNext)
        optionViews = listOf(
            findViewById(R.id.btnOption1),
            findViewById(R.id.btnOption2),
            findViewById(R.id.btnOption3),
            findViewById(R.id.btnOption4)
        )

        val source = if (wrongOnly) store.wrongWords() else store.allWords()
        if (source.size < 2) {
            Toast.makeText(this, if (wrongOnly) "错题少于 2 个，先去测试攒点错题吧" else "词库不足，无法开始测试", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        quizWords = source.shuffled().take(if (wrongOnly) source.size else minOf(MAX_QUESTIONS, source.size))

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        btnSpeak.setOnClickListener { tts.speak(quizWords.getOrNull(index)?.en ?: "") }
        btnNext.setOnClickListener { nextQuestion() }
        optionViews.forEachIndexed { i, view ->
            view.setOnClickListener { onOptionClick(i) }
        }

        showQuestion()
    }

    private fun showQuestion() {
        if (index >= quizWords.size) {
            showResult()
            return
        }
        answered = false
        val word = quizWords[index]
        tvProgress.text = "第 ${index + 1} / ${quizWords.size} 题"
        tvScore.text = "已答对 $correctCount 题"
        tvWordEn.text = word.en
        tvWordPhonetic.text = word.phonetic.ifEmpty { " " }
        tvFeedback.text = if (wrongOnly) "错题重练模式" else "选择正确的词义"
        tvFeedback.setTextColor(Color.parseColor("#8A94A6"))
        btnNext.visibility = View.GONE

        val correct = word.cn.ifEmpty { "（暂无释义）" }
        val pool = store.allWords().map { it.cn.ifEmpty { "（暂无释义）" } }
            .filter { it != correct }
            .distinct()
        val optionCount = minOf(4, pool.size + 1)
        val optionTexts = (listOf(correct) + pool.shuffled().take(optionCount - 1)).shuffled()
        correctIndex = optionTexts.indexOf(correct)

        optionViews.forEachIndexed { i, view ->
            if (i < optionCount) {
                view.visibility = View.VISIBLE
                view.isEnabled = true
                view.text = optionTexts[i]
                view.setTextColor(Color.parseColor("#1F2937"))
                view.setBackgroundResource(R.drawable.bg_option)
            } else {
                view.visibility = View.GONE
            }
        }
    }

    private fun onOptionClick(position: Int) {
        if (answered) return
        answered = true
        optionViews.forEach { it.isEnabled = false }
        val word = quizWords[index]
        val isCorrect = position == correctIndex
        store.recordAnswer(isCorrect)

        if (isCorrect) {
            correctCount++
            optionViews[position].setBackgroundResource(R.drawable.bg_option_right)
            optionViews[position].setTextColor(Color.parseColor("#0F8A4B"))
            tvFeedback.text = "回答正确"
            tvFeedback.setTextColor(Color.parseColor("#0F8A4B"))
            tts.speak(word.en)
        } else {
            optionViews[position].setBackgroundResource(R.drawable.bg_option_wrong)
            optionViews[position].setTextColor(Color.parseColor("#C53030"))
            if (correctIndex in optionViews.indices) {
                optionViews[correctIndex].setBackgroundResource(R.drawable.bg_option_right)
                optionViews[correctIndex].setTextColor(Color.parseColor("#0F8A4B"))
            }
            store.addWrong(word)
            tvFeedback.text = "回答错误，正确答案：${word.cn}\n已自动加入错题本"
            tvFeedback.setTextColor(Color.parseColor("#C53030"))
            tts.speak(word.en)
        }
        tvScore.text = "已答对 $correctCount 题"

        val delay = if (isCorrect) 900L else 1500L
        handler.postDelayed({ nextQuestion() }, delay)
    }

    private fun nextQuestion() {
        index++
        showQuestion()
    }

    private fun showResult() {
        val total = quizWords.size
        val rate = Math.round(correctCount * 100f / total)
        val newWrong = store.wrongCount()
        val message = "本次共 $total 题\n答对 $correctCount 题\n正确率 $rate%\n错题本当前共 $newWrong 个单词"

        AlertDialog.Builder(this)
            .setTitle("测试完成")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("再来一轮") { _, _ ->
                index = 0
                correctCount = 0
                quizWords = quizWords.shuffled()
                showQuestion()
            }
            .setNegativeButton("返回首页") { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        tts.shutdown()
    }

    companion object {
        private const val MAX_QUESTIONS = 10
    }
}
