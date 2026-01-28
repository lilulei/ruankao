package com.github.lilulei.ruankao.model

import java.util.*

data class PracticeSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val questions: List<Question> = emptyList(),
    val answers: MutableMap<String, AnswerRecord> = mutableMapOf(),
    val sessionType: PracticeType = PracticeType.RANDOM_PRACTICE
)

enum class PracticeType {
    DAILY_PRACTICE, // 每日一练
    SPECIAL_TOPIC,  // 专项练习
    MOCK_EXAM,      // 模拟考试
    RANDOM_PRACTICE // 随机抽题
}

data class LearningStatistics(
    val totalPractices: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val studyTimeMinutes: Int = 0,
    val dailyStreak: Int = 0,
    val lastStudyDate: Long? = null,
    val categoryStats: MutableMap<String, CategoryStat> = mutableMapOf(),
    val achievements: MutableSet<String> = mutableSetOf()
)

data class CategoryStat(
    val categoryName: String,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val mastered: Boolean = false
)