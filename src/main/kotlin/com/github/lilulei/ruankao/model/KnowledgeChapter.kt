package com.github.lilulei.ruankao.model

/**
 * 知识点章节数据类
 * @param id 章节唯一标识符
 * @param name 章节名称
 * @param level 考试级别（如"软考高级"、"软考中级"、"软考初级"），null表示不限级别
 * @param examType 考试类型（如"信息系统项目管理师"等），null表示不限考试类型
 * @param parentId 父章节ID，用于构建章节层级结构，null表示顶级章节
 * @param createdAt 创建时间戳
 * @param updatedAt 更新时间戳
 */
data class KnowledgeChapter(
    val id: String,
    val name: String,
    val level: String? = null, // 关联的考试级别
    val examType: String? = null, // 关联的考试类型
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)