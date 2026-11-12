package com.example.iottest

data class Word(
    val en: String,
    val phonetic: String,
    val cn: String,
    var isWrong: Boolean = false
)
