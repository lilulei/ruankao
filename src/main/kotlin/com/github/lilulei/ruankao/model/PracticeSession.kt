package com.github.lilulei.ruankao.model

import java.util.*

/**
 * 练习会话数据类，用于记录用户的一次练习活动
 * @property sessionId 会话唯一标识符，默认生成UUID
 * @property startTime 练习开始时间戳
 * @property endTime 练习结束时间戳，可为空表示未结束
 * @property questions 练习中涉及的问题列表
 * @property answers 用户答案记录映射，键为问题ID，值为答案记录
 * @property sessionType 练习类型枚举
 */
data class PracticeSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val questions: List<Question> = emptyList(),
    val answers: MutableMap<String, AnswerRecord> = mutableMapOf(),
    val sessionType: PracticeType = PracticeType.RANDOM_PRACTICE
)

/**
 * 练习类型枚举，定义了不同的练习模式
 */
enum class PracticeType {
    DAILY_PRACTICE, // 每日一练
    SPECIAL_TOPIC,  // 专项练习
    MOCK_EXAM,      // 模拟考试
    RANDOM_PRACTICE // 随机抽题
}

/**
 * 学习统计信息数据类，用于记录用户的学习进度和成绩统计
 * @property totalPractices 总练习次数
 * @property totalQuestions 总答题数量
 * @property correctAnswers 正确回答数量
 * @property studyTimeMinutes 学习时长（分钟）
 * @property dailyStreak 连续学习天数
 * @property lastStudyDate 最后学习日期时间戳，可为空
 * @property categoryStats 各分类统计信息映射
 * @property achievements 获得的成就集合
 */
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

/**
 * 分类统计信息数据类，用于记录特定分类下的学习情况
 * @property categoryName 分类名称
 * @property totalQuestions 该分类下总题目数量
 * @property correctAnswers 该分类下正确回答数量
 * @property mastered 是否已掌握该分类
 */
data class CategoryStat(
    val categoryName: String,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val mastered: Boolean = false
)