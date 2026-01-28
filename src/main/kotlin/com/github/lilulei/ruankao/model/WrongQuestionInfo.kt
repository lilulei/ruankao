package com.github.lilulei.ruankao.model

data class WrongQuestionInfo(
    val questionId: String,
    val errorCount: Int = 1,
    val lastErrorTime: Long = System.currentTimeMillis(),
    val mastered: Boolean = false,
    val consecutiveCorrectCount: Int = 0
)