package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.QuestionNote
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element

/**
 * 试题笔记服务类
 * 用于管理用户对试题的笔记和标签
 */
@State(name = "QuestionNoteService", storages = [Storage("softexam_question_notes.xml")])
@Service(Service.Level.PROJECT)
class QuestionNoteService : PersistentStateComponent<Element> {
    private val logger = logger<QuestionNoteService>()
    private val _notes = mutableMapOf<String, QuestionNote>() // key: questionId

    /**
     * 获取所有笔记
     */
    val allNotes: Map<String, QuestionNote>
        get() = _notes.toMap()

    /**
     * 获取指定试题的笔记
     * @param questionId 试题ID
     * @return 笔记对象，如果不存在则返回null
     */
    fun getNoteByQuestionId(questionId: String): QuestionNote? {
        return _notes[questionId]
    }

    /**
     * 添加或更新试题笔记
     * @param questionId 试题ID
     * @param note 笔记内容
     * @param tags 标签集合
     */
    fun addOrUpdateNote(questionId: String, note: String, tags: Set<String>) {
        val existingNote = _notes[questionId]
        val newNote = if (existingNote != null) {
            QuestionNote(
                questionId = questionId,
                note = note,
                tags = tags,
                createdAt = existingNote.createdAt, // 保持原始创建时间
                updatedAt = System.currentTimeMillis()
            )
        } else {
            QuestionNote(
                questionId = questionId,
                note = note,
                tags = tags,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        _notes[questionId] = newNote
        logger.info("更新试题笔记: $questionId")
    }

    /**
     * 添加标签到试题
     * @param questionId 试题ID
     * @param tag 要添加的标签
     */
    fun addTagToQuestion(questionId: String, tag: String) {
        val existingNote = _notes[questionId]
        val tags = if (existingNote != null) {
            existingNote.tags + tag
        } else {
            setOf(tag)
        }
        addOrUpdateNote(questionId, existingNote?.note ?: "", tags)
    }

    /**
     * 从试题移除标签
     * @param questionId 试题ID
     * @param tag 要移除的标签
     */
    fun removeTagFromQuestion(questionId: String, tag: String) {
        val existingNote = _notes[questionId]
        if (existingNote != null) {
            val newTags = existingNote.tags - tag
            addOrUpdateNote(questionId, existingNote.note, newTags)
        }
    }

    /**
     * 获取试题的所有标签
     * @param questionId 试题ID
     * @return 标签集合
     */
    fun getTagsForQuestion(questionId: String): Set<String> {
        return _notes[questionId]?.tags ?: emptySet()
    }

    /**
     * 获取所有标签
     * @return 所有标签的去重集合
     */
    fun getAllTags(): Set<String> {
        return _notes.values.flatMap { it.tags }.toSet()
    }

    /**
     * 获取包含特定标签的试题ID列表
     * @param tag 标签
     * @return 包含该标签的试题ID列表
     */
    fun getQuestionsByTag(tag: String): List<String> {
        return _notes.filter { (_, note) -> tag in note.tags }.keys.toList()
    }

    /**
     * 删除试题笔记
     * @param questionId 试题ID
     */
    fun deleteNote(questionId: String) {
        _notes.remove(questionId)
        logger.info("删除试题笔记: $questionId")
    }

    /**
     * 获取当前状态元素，用于持久化保存
     * @return 包含所有笔记信息的XML元素
     */
    override fun getState(): Element {
        val element = Element("QuestionNoteService")

        _notes.forEach { (questionId, note) ->
            val noteElement = Element("note")
            noteElement.setAttribute("questionId", questionId)
            noteElement.setAttribute("note", note.note)
            noteElement.setAttribute("createdAt", note.createdAt.toString())
            noteElement.setAttribute("updatedAt", note.updatedAt.toString())

            // 添加标签
            val tagsElement = Element("tags")
            note.tags.forEach { tag ->
                val tagElement = Element("tag")
                tagElement.text = tag
                tagsElement.addContent(tagElement)
            }
            noteElement.addContent(tagsElement)

            element.addContent(noteElement)
        }

        return element
    }

    /**
     * 从XML元素加载状态数据
     * @param state 要加载的状态XML元素
     */
    override fun loadState(state: Element) {
        try {
            _notes.clear()

            state.getChildren("note").forEach { noteElement ->
                val questionId = noteElement.getAttributeValue("questionId")
                val noteText = noteElement.getAttributeValue("note") ?: ""
                val createdAt = noteElement.getAttributeValue("createdAt")?.toLongOrNull() ?: System.currentTimeMillis()
                val updatedAt = noteElement.getAttributeValue("updatedAt")?.toLongOrNull() ?: System.currentTimeMillis()

                // 加载标签
                val tags = mutableSetOf<String>()
                val tagsElement = noteElement.getChild("tags")
                tagsElement?.getChildren("tag")?.forEach { tagElement ->
                    tags.add(tagElement.text)
                }

                val note = QuestionNote(
                    questionId = questionId,
                    note = noteText,
                    tags = tags.toSet(),
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )

                _notes[questionId] = note
            }
        } catch (e: Exception) {
            logger.error("Error loading question notes", e)
        }
    }
}