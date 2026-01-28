package com.github.lilulei.ruankao.model

data class Question(
    val id: String,
    val title: String,
    val options: Map<String, String>, // 例如 mapOf("A" to "选项A内容", "B" to "选项B内容", ...)
    val correctAnswers: Set<String>, // 多选题可能有多个答案
    val explanation: String = "",
    val level: DifficultyLevel = DifficultyLevel.MEDIUM,
    val category: String = "通用",
    val year: Int = 0, // 考试年份
    val examType: ExamType = ExamType.SOFTWARE_DESIGNER // 考试类型
)

enum class DifficultyLevel {
    EASY, MEDIUM, HARD
}

enum class ExamType {
    SOFTWARE_DESIGNER, NETWORK_ENGINEER, DATABASE_ENGINEER, SYSTEM_ANALYST
}

data class AnswerRecord(
    val questionId: String,
    val selectedOptions: Set<String>,
    val isCorrect: Boolean,
    val answeredAt: Long = System.currentTimeMillis()
)