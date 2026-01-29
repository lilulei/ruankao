package com.github.lilulei.ruankao.model

/**
 * 错题信息数据类
 * 用于记录题目错误统计相关信息，包括错误次数、最后错误时间、掌握状态等
 *
 * @property questionId 题目ID，用于唯一标识一道题目
 * @property errorCount 错误次数，默认值为1
 * @property lastErrorTime 最后一次错误的时间戳，默认为当前系统时间
 * @property mastered 是否已掌握，默认为false
 * @property consecutiveCorrectCount 连续正确回答的次数，默认为0
 */
data class WrongQuestionInfo(
    val questionId: String,
    val errorCount: Int = 1,
    val lastErrorTime: Long = System.currentTimeMillis(),
    val mastered: Boolean = false,
    val consecutiveCorrectCount: Int = 0
)