package com.example.iot_test

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 数据仓库：
 * 1. 词库：assets 内置词库 + 用户导入词库（filesDir/imported_words.json）
 * 2. 错题本：filesDir/wrong_book.json
 * 3. 学习统计：SharedPreferences（累计答题、答对、每日答题记录）
 */
class WordStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val importedFile = File(context.filesDir, "imported_words.json")
    private val wrongFile = File(context.filesDir, "wrong_book.json")

    /** 全量词库缓存，避免每次答题都重新解析 4000+ 词的 JSON */
    private var cachedAll: List<Word>? = null

    // ---------------- 词库 ----------------

    /** 内置词库（assets/words.json） */
    fun assetWords(): List<Word> = try {
        val text = context.assets.open("words.json").bufferedReader().use { it.readText() }
        parseWords(text)
    } catch (e: Exception) {
        emptyList()
    }

    /** 用户导入的词库 */
    fun importedWords(): List<Word> = try {
        if (importedFile.exists()) parseWords(importedFile.readText()) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    /** 全部词库（导入的覆盖同名内置词） */
    fun allWords(): List<Word> {
        cachedAll?.let { return it }
        val map = LinkedHashMap<String, Word>()
        for (w in assetWords()) map[w.en.lowercase(Locale.US)] = w
        for (w in importedWords()) map[w.en.lowercase(Locale.US)] = w
        val list = map.values.toList()
        cachedAll = list
        return list
    }

    /** 导入一段文本（支持 JSON 数组，也支持每行 en,phonetic,cn 的纯文本） */
    fun importFromText(text: String): Int {
        val newWords = parseWords(text)
        if (newWords.isEmpty()) return 0
        val existing = importedWords().toMutableList()
        val known = allWords().map { it.en.lowercase(Locale.US) }.toMutableSet()
        var added = 0
        for (w in newWords) {
            val key = w.en.lowercase(Locale.US)
            if (key.isNotEmpty() && !known.contains(key)) {
                existing.add(w)
                known.add(key)
                added++
            }
        }
        if (added > 0) {
            saveImported(existing)
            cachedAll = null
        }
        return added
    }

    /** 清空用户导入的词库 */
    fun clearImported() {
        if (importedFile.exists()) importedFile.delete()
        cachedAll = null
    }

    private fun saveImported(list: List<Word>) {
        val arr = JSONArray()
        for (w in list) arr.put(wordToJson(w))
        importedFile.writeText(arr.toString())
    }

    private fun wordToJson(w: Word): JSONObject = JSONObject().apply {
        put("en", w.en)
        put("phonetic", w.phonetic)
        put("cn", w.cn)
    }

    private fun parseWords(text: String): List<Word> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        return if (trimmed.startsWith("[")) parseJsonWords(trimmed) else parseLineWords(trimmed)
    }

    private fun parseJsonWords(text: String): List<Word> = try {
        val arr = JSONArray(text)
        val list = ArrayList<Word>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val en = obj.optString("en", "").trim()
            if (en.isEmpty()) continue
            val cn = obj.optString("cn", obj.optString("meaning", "")).trim()
            list.add(Word(en, obj.optString("phonetic", "").trim(), cn))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }

    private fun parseLineWords(text: String): List<Word> {
        val list = ArrayList<Word>()
        for (raw in text.split("\n")) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split(",", "|", "\t", "，").map { it.trim() }
            val en = parts.getOrNull(0) ?: continue
            if (en.isEmpty()) continue
            val phonetic = if (parts.size >= 3) parts[1] else ""
            val cn = if (parts.size >= 3) parts[2] else parts.getOrNull(1) ?: ""
            list.add(Word(en, phonetic, cn))
        }
        return list
    }

    // ---------------- 错题本 ----------------

    /** 错题本里的英文列表（小写） */
    private fun wrongKeys(): List<String> = try {
        if (!wrongFile.exists()) {
            emptyList()
        } else {
            val arr = JSONArray(wrongFile.readText())
            (0 until arr.length()).map { arr.getString(it) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun wrongWords(): List<Word> {
        val dict = allWords().associateBy { it.en.lowercase(Locale.US) }
        return wrongKeys().mapNotNull { dict[it] }
    }

    fun wrongCount(): Int = wrongKeys().size

    fun addWrong(w: Word) {
        val key = w.en.lowercase(Locale.US)
        val keys = wrongKeys().toMutableList()
        if (!keys.contains(key)) keys.add(key)
        saveWrong(keys)
    }

    fun removeWrong(en: String) {
        val key = en.lowercase(Locale.US)
        saveWrong(wrongKeys().filter { it != key })
    }

    fun clearWrong() {
        saveWrong(emptyList())
    }

    private fun saveWrong(keys: List<String>) {
        val arr = JSONArray()
        for (k in keys) arr.put(k)
        wrongFile.writeText(arr.toString())
    }

    // ---------------- 学习统计 ----------------

    /** 记录一次答题 */
    fun recordAnswer(correct: Boolean) {
        val today = todayKey()
        if (prefs.getString(KEY_LAST_DAY, "") != today) {
            prefs.edit().putString(KEY_LAST_DAY, today).putInt(KEY_TODAY_COUNT, 0).apply()
        }
        val answered = prefs.getInt(KEY_ANSWERED, 0) + 1
        val correctTotal = prefs.getInt(KEY_CORRECT, 0) + if (correct) 1 else 0
        val todayCount = prefs.getInt(KEY_TODAY_COUNT, 0) + 1

        val days = dayCounts()
        days[today] = (days[today] ?: 0) + 1

        prefs.edit()
            .putInt(KEY_ANSWERED, answered)
            .putInt(KEY_CORRECT, correctTotal)
            .putInt(KEY_TODAY_COUNT, todayCount)
            .putString(KEY_DAYS, encodeDays(days))
            .apply()
    }

    fun totalAnswered(): Int = prefs.getInt(KEY_ANSWERED, 0)

    fun totalCorrect(): Int = prefs.getInt(KEY_CORRECT, 0)

    fun correctRate(): Int {
        val a = totalAnswered()
        return if (a == 0) 0 else Math.round(totalCorrect() * 100f / a)
    }

    fun todayCount(): Int {
        if (prefs.getString(KEY_LAST_DAY, "") != todayKey()) return 0
        return prefs.getInt(KEY_TODAY_COUNT, 0)
    }

    fun dayCounts(): MutableMap<String, Int> {
        val raw = prefs.getString(KEY_DAYS, "") ?: ""
        val map = HashMap<String, Int>()
        if (raw.isNotEmpty()) {
            try {
                val obj = JSONObject(raw)
                val it = obj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    map[k] = obj.optInt(k, 0)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return map
    }

    /** 最近 n 天的日期与答题数（最后一项是今天） */
    fun recentDays(n: Int): Pair<List<String>, List<Int>> {
        val counts = dayCounts()
        val fmt = SimpleDateFormat("M/d", Locale.CHINA)
        val labels = ArrayList<String>()
        val values = ArrayList<Int>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
        for (i in 0 until n) {
            val date = cal.time
            labels.add(fmt.format(date))
            values.add(counts[dayKey(date)] ?: 0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return labels to values
    }

    private fun encodeDays(map: Map<String, Int>): String {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v)
        return obj.toString()
    }

    private fun todayKey(): String = dayKey(Date())

    private fun dayKey(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(date)

    companion object {
        private const val PREFS_NAME = "word_app"
        private const val KEY_ANSWERED = "answered"
        private const val KEY_CORRECT = "correct"
        private const val KEY_TODAY_COUNT = "today_count"
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_DAYS = "days"
    }
}
