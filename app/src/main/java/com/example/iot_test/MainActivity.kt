package com.example.iot_test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.iot_test.R

class MainActivity : AppCompatActivity() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val words = loadWordList()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = WordAdapter(words) { word ->
            // 点击发音回调，后面再加TTS
        }
    }

    // 这个函数刚才丢了！！
    private fun loadWordList(): List<Word> {
        val sb = StringBuilder()
        val reader = BufferedReader(InputStreamReader(assets.open("words.json")))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        return json.decodeFromString(sb.toString())
    }
}
