package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.KnowledgeChapter
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element
import java.util.*

/**
 * 知识点章节服务类，负责管理软考的知识点章节
 * 实现PersistentStateComponent接口以支持状态持久化
 * @param State 注解指定状态名称和存储文件
 * @param Service 注解指定服务级别为项目级别
 */
@State(name = "KnowledgeChapterService", storages = [Storage("knowledge_chapters.xml")])
@Service(Service.Level.PROJECT)
class KnowledgeChapterService : PersistentStateComponent<Element> {
    private val logger = logger<KnowledgeChapterService>()
    private val _chapters = mutableMapOf<String, KnowledgeChapter>()

    /**
     * 获取所有章节的只读映射
     */
    val allChapters: Map<String, KnowledgeChapter>
        get() = _chapters.toMap()

    /**
     * 获取所有章节列表
     */
    val allChaptersList: List<KnowledgeChapter>
        get() = _chapters.values.toList()

    /**
     * 获取指定ID的章节
     * @param id 章节ID
     * @return 章节对象，如果不存在则返回null
     */
    fun getChapterById(id: String): KnowledgeChapter? {
        return _chapters[id]
    }

    /**
     * 根据章节名称获取章节
     * @param name 章节名称
     * @return 章节对象，如果不存在则返回null
     */
    fun getChapterByName(name: String): KnowledgeChapter? {
        return _chapters.values.find { it.name == name }
    }

    /**
     * 根据考试级别获取章节列表
     * @param level 考试级别（如"软考高级"）
     * @return 该级别的所有章节列表
     */
    fun getChaptersByLevel(level: String): List<KnowledgeChapter> {
        return _chapters.values.filter { it.level == null || it.level == level }
    }

    /**
     * 根据考试级别和考试类型获取章节列表（按用户身份绑定）
     * @param level 考试级别（如"软考高级"）
     * @param examType 考试类型（如"信息系统项目管理师"）
     * @return 该身份下的所有章节列表
     */
    fun getChaptersByIdentity(level: String, examType: String): List<KnowledgeChapter> {
        return _chapters.values.filter { 
            it.level == null || it.level == level 
        }.filter {
            it.examType == null || it.examType == examType
        }
    }

    /**
     * 获取所有章节名称列表
     * @return 所有章节名称的去重列表
     */
    fun getAllChapterNames(): List<String> {
        return _chapters.values.map { it.name }.distinct()
    }

    /**
     * 获取指定级别的章节名称列表
     * @param level 考试级别（如"软考高级"）
     * @return 该级别的所有章节名称列表
     */
    fun getChapterNamesByLevel(level: String): List<String> {
        return _chapters.values.filter { it.level == null || it.level == level }.map { it.name }.distinct()
    }

    /**
     * 获取指定身份（级别+考试类型）的章节名称列表
     * @param level 考试级别（如"软考高级"）
     * @param examType 考试类型（如"信息系统项目管理师"）
     * @return 该身份下的所有章节名称列表
     */
    fun getChapterNamesByIdentity(level: String, examType: String): List<String> {
        return _chapters.values.filter { 
            it.level == null || it.level == level 
        }.filter {
            it.examType == null || it.examType == examType
        }.map { it.name }.distinct()
    }

    /**
     * 添加章节
     * @param chapter 要添加的章节对象
     */
    fun addChapter(chapter: KnowledgeChapter) {
        _chapters[chapter.id] = chapter
        logger.info("添加章节: ${chapter.id}, 名称: ${chapter.name}, 级别: ${chapter.level ?: "全部"}, 考试类型: ${chapter.examType ?: "全部"}")
    }

    /**
     * 删除章节（仅当该章节下没有绑定试题时才允许删除）
     * @param id 要删除的章节ID
     * @return 如果删除成功返回true，否则返回false
     */
    fun removeChapter(id: String, questionService: QuestionService): Boolean {
        val chapter = _chapters[id]
        if (chapter != null) {
            // 检查该章节下是否已绑定试题
            val questionsInChapter = questionService.allQuestionsList.filter { 
                it.chapter != null && it.chapter.equals(chapter.name, ignoreCase = true)
            }
            if (questionsInChapter.isNotEmpty()) {
                logger.warn("章节 ${chapter.name} 下存在 ${questionsInChapter.size} 道试题，无法删除")
                return false
            }
            _chapters.remove(id)
            logger.info("删除章节: $id, 名称: ${chapter.name}")
            return true
        }
        return false
    }

    /**
     * 更新章节
     * @param chapter 更新后的章节对象
     */
    fun updateChapter(chapter: KnowledgeChapter) {
        _chapters[chapter.id] = chapter
        logger.info("更新章节: ${chapter.id}, 名称: ${chapter.name}")
    }

    /**
     * 检查章节是否存在
     * @param id 章节ID
     * @return 如果章节存在返回true，否则返回false
     */
    fun chapterExists(id: String): Boolean {
        return _chapters.containsKey(id)
    }

    /**
     * 检查章节名称是否存在
     * @param name 章节名称
     * @param level 考试级别，用于限定范围
     * @param examType 考试类型，用于限定范围
     * @return 如果章节名称存在返回true，否则返回false
     */
    fun chapterNameExists(name: String, level: String? = null, examType: String? = null): Boolean {
        return _chapters.values.any { 
            it.name == name && 
            (level == null || it.level == null || it.level == level) &&
            (examType == null || it.examType == null || it.examType == examType)
        }
    }

    /**
     * 获取章节总数
     * @return 章节总数
     */
    fun getTotalChapterCount(): Int {
        return _chapters.size
    }

    /**
     * 获取子章节列表
     * @param parentId 父章节ID
     * @return 指定父章节下的所有子章节列表
     */
    fun getChildChapters(parentId: String?): List<KnowledgeChapter> {
        return _chapters.values.filter { it.parentId == parentId }
    }

    /**
     * 获取根章节列表（没有父章节的章节）
     * @return 根章节列表
     */
    fun getRootChapters(): List<KnowledgeChapter> {
        return _chapters.values.filter { it.parentId == null }
    }

    /**
     * 获取当前状态元素，用于持久化保存
     * @return 包含所有章节信息的XML元素
     */
    override fun getState(): Element {
        val element = Element("KnowledgeChapterService")

        _chapters.forEach { (_, chapter) ->
            val chapterElement = Element("chapter")
            chapterElement.setAttribute("id", chapter.id)
            chapterElement.setAttribute("name", chapter.name)
            if (chapter.level != null) {
                chapterElement.setAttribute("level", chapter.level)
            }
            if (chapter.examType != null) {
                chapterElement.setAttribute("examType", chapter.examType)
            }
            chapterElement.setAttribute("parentId", chapter.parentId ?: "")
            chapterElement.setAttribute("createdAt", chapter.createdAt.toString())
            chapterElement.setAttribute("updatedAt", chapter.updatedAt.toString())

            element.addContent(chapterElement)
        }

        return element
    }

    /**
     * 从XML元素加载状态数据
     * @param state 要加载的状态XML元素
     */
    override fun loadState(state: Element) {
        try {
            _chapters.clear()

            state.getChildren("chapter").forEach { chapterElement ->
                val id = chapterElement.getAttributeValue("id")
                val name = chapterElement.getAttributeValue("name") ?: ""
                val level = chapterElement.getAttributeValue("level")
                val examType = chapterElement.getAttributeValue("examType")
                val parentIdAttr = chapterElement.getAttributeValue("parentId")
                val parentId = if (parentIdAttr.isNullOrEmpty()) null else parentIdAttr
                val createdAt = chapterElement.getAttributeValue("createdAt")?.toLong() ?: System.currentTimeMillis()
                val updatedAt = chapterElement.getAttributeValue("updatedAt")?.toLong() ?: System.currentTimeMillis()

                val chapter = KnowledgeChapter(
                    id = id,
                    name = name,
                    level = level,
                    examType = examType,
                    parentId = parentId,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )

                _chapters[id] = chapter
            }
        } catch (e: Exception) {
            logger.error("Error loading chapters", e)
        }
    }
}