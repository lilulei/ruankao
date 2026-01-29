package com.github.lilulei.ruankao.model

/**
 * 题目数据类
 * @param id 题目唯一标识符
 * @param title 题目标题/内容
 * @param options 选项集合，键为选项标识（如"A"、"B"），值为选项内容
 * @param correctAnswers 正确答案集合，支持多选题的多个答案
 * @param explanation 题目解析说明
 * @param level 题目难度等级，默认为中等难度
 * @param category 题目分类，默认为"通用"
 * @param year 考试年份，默认为0表示未指定年份
 * @param examType 考试类型，默认为软件设计师
 */
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

/**
 * 难度等级枚举
 */
enum class DifficultyLevel {
    EASY, MEDIUM, HARD
}

/**
 * 考试类型枚举
 */
enum class ExamType {
    SOFTWARE_DESIGNER, NETWORK_ENGINEER, DATABASE_ENGINEER, SYSTEM_ANALYST
}

/**
 * 答题记录数据类
 * @param questionId 对应题目的ID
 * @param selectedOptions 用户选择的答案选项集合
 * @param isCorrect 答题是否正确
 * @param answeredAt 答题时间戳，默认为当前系统时间
 */
data class AnswerRecord(
    val questionId: String,
    val selectedOptions: Set<String>,
    val isCorrect: Boolean,
    val answeredAt: Long = System.currentTimeMillis()
)