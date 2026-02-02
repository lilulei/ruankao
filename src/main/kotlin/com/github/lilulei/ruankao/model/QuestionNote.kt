package com.github.lilulei.ruankao.model

/**
 * 试题笔记数据类
 * 用于记录用户对试题的备注和知识点标记
 *
 * @property questionId 关联的试题ID
 * @property note 用户添加的笔记内容
 * @property tags 试题的知识点标签集合
 * @property createdAt 创建时间戳
 * @property updatedAt 更新时间戳
 */
data class QuestionNote(
    val questionId: String,
    val note: String = "",
    val tags: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)