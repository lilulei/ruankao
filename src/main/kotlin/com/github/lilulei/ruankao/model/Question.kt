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
    val chapter: String? = null, // 知识点章节
    val year: Int = 0, // 考试年份
    val examType: ExamType = ExamType.SOFTWARE_DESIGNER // 考试类型
)

/**
 * 难度等级枚举
 */
enum class DifficultyLevel(val displayName: String) {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难")
}

/**
 * 考试类型枚举
 */
enum class ExamType(val displayName: String) {
    // 软考高级 (5个)
    SYSTEM_ANALYST("系统分析师"),
    SYSTEM_ARCHITECT("系统架构设计师"),
    NETWORK_PLANNER("网络规划设计师"),
    PROJECT_MANAGER("信息系统项目管理师"),
    SYSTEM_PLANNING_MANAGER("系统规划与管理师"),
    
    // 软考中级 (15个)
    SYSTEM_INTEGRATION_ENGINEER("系统集成项目管理工程师"),
    NETWORK_ENGINEER("网络工程师"),
    INFORMATION_SYSTEM_MANAGEMENT_ENGINEER("信息系统管理工程师"),
    SOFTWARE_TESTER("软件评测师"),
    DATABASE_ENGINEER("数据库系统工程师"),
    MULTIMEDIA_DESIGNER("多媒体应用设计师"),
    SOFTWARE_DESIGNER("软件设计师"),
    INFORMATION_SYSTEM_SUPERVISOR("信息系统监理师"),
    E_COMMERCE_DESIGNER("电子商务设计师"),
    INFORMATION_SECURITY_ENGINEER("信息安全工程师"),
    EMBEDDED_SYSTEM_DESIGNER("嵌入式系统设计师"),
    SOFTWARE_PROCESS_EVALUATOR("软件过程能力评估师"),
    COMPUTER_AIDED_DESIGNER("计算机辅助设计师"),
    COMPUTER_HARDWARE_ENGINEER("计算机硬件工程师"),
    INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER("信息技术支持工程师"),
    
    // 软考初级 (7个)
    PROGRAMMER("程序员"),
    NETWORK_ADMINISTRATOR("网络管理员"),
    INFORMATION_PROCESSING_TECHNICIAN("信息处理技术员"),
    INFORMATION_SYSTEM_OPERATION_MANAGER("信息系统运行管理员"),
    MULTIMEDIA_APPLICATION_DESIGNER("多媒体应用制作技术员"),
    E_COMMERCE_TECHNICIAN("电子商务技术员"),
    WEB_DESIGNER("网页制作员")
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