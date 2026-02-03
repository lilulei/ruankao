package com.github.lilulei.ruankao.model

import kotlinx.serialization.Serializable

/**
 * 每日练习记录数据类
 * 用于记录用户每日的学习练习情况，包括练习次数、答题情况和学习时长等统计信息
 *
 * @property practices 练习次数，默认值为0
 * @property questionsAnswered 已回答的问题数量，默认值为0
 * @property correctlyAnswered 回答正确的问题数量，默认值为0
 * @property timeSpentMinutes 学习花费时间（分钟），默认值为0
 */
@Serializable
data class DailyPracticeRecord(
    val practices: Int = 0,
    val questionsAnswered: Int = 0,
    val correctlyAnswered: Int = 0,
    val timeSpentMinutes: Int = 0
)