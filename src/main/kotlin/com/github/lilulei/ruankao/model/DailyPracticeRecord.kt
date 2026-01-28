package com.github.lilulei.ruankao.model

data class DailyPracticeRecord(
    val practices: Int = 0,
    val questionsAnswered: Int = 0,
    val correctlyAnswered: Int = 0,
    val timeSpentMinutes: Int = 0
)