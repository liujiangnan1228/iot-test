package com.example.iot_test

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * P2 数据看板：词库规模、错题数、累计答题、正确率、近 7 天趋势
 */
class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val store = WordStore(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val totalWords = store.allWords().size
        val importedCount = store.importedWords().size
        val wrongCount = store.wrongCount()
        val answered = store.totalAnswered()
        val rate = store.correctRate()
        val today = store.todayCount()

        findViewById<TextView>(R.id.tvTotalWords).text = totalWords.toString()
        findViewById<TextView>(R.id.tvTotalWordsHint).text = "内置 + 导入（导入 $importedCount 个）"
        findViewById<TextView>(R.id.tvWrongCount).text = wrongCount.toString()
        findViewById<TextView>(R.id.tvAnswered).text = answered.toString()
        findViewById<TextView>(R.id.tvCorrectRate).text = "$rate%"
        findViewById<TextView>(R.id.tvToday).text = "今天练了 $today 个单词"

        findViewById<ProgressBar>(R.id.progressRate).progress = rate

        val (labels, values) = store.recentDays(7)
        findViewById<BarChartView>(R.id.barChart).setData(labels, values)
        findViewById<TextView>(R.id.tvWeekTotal).text = "近 7 天合计答题 ${values.sum()} 次"

        findViewById<TextView>(R.id.tvSummary).text = buildSummary(totalWords, wrongCount, answered, rate, today, values.sum())
    }

    private fun buildSummary(
        totalWords: Int,
        wrongCount: Int,
        answered: Int,
        rate: Int,
        today: Int,
        weekTotal: Int
    ): String {
        val sb = StringBuilder()
        sb.append("· 词库共 $totalWords 个单词\n")
        sb.append("· 累计答题 $answered 次，正确率 $rate%\n")
        sb.append("· 错题本 $wrongCount 个，建议每天重做一遍错题\n")
        sb.append("· 今天已练 $today 个，近 7 天共 $weekTotal 次\n")
        sb.append(
            when {
                answered == 0 -> "还没有测试记录，去首页点“开始测试”试试吧。"
                rate >= 90 -> "正确率很棒，可以试着导入更难的词库。"
                rate >= 70 -> "整体不错，重点把错题本里的单词过一遍。"
                else -> "正确率偏低，建议先背熟词库，再重做错题。"
            }
        )
        return sb.toString()
    }
}
