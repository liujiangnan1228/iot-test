package com.example.iot_test

/**
 * 一个单词条目：英文、音标、中文释义
 */
data class Word(
    val en: String,
    val phonetic: String,
    val cn: String
)
