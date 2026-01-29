package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.WrongQuestionInfo
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element

// 定义监听器接口
/**
 * 错题本变更监听器接口
 */
interface WrongQuestionChangeListener {
    /**
     * 当错题本数据更新时调用
     */
    fun onWrongQuestionUpdated()
}

/**
 * 错题本服务类，用于管理错题信息并持久化存储
 * 实现PersistentStateComponent接口以支持状态持久化
 * @param State 注解指定状态名称和存储文件
 * @param Service 注解指定服务级别为项目级别
 */
@State(name = "WrongQuestionService", storages = [Storage("softexam_wrong_questions.xml")])
@Service(Service.Level.PROJECT)
class WrongQuestionService : PersistentStateComponent<Element> {
    private val logger = logger<WrongQuestionService>()
    private val _wrongQuestions = mutableMapOf<String, WrongQuestionInfo>()
    private val listeners = mutableListOf<WrongQuestionChangeListener>()

    /**
     * 获取所有错题的只读映射
     */
    val allWrongQuestions: Map<String, WrongQuestionInfo>
        get() = _wrongQuestions.toMap()

    /**
     * 获取当前状态的XML元素表示，用于持久化保存
     * @return 包含所有错题信息的XML元素
     */
    override fun getState(): Element {
        val element = Element("WrongQuestionService")

        // 遍历所有错题信息并转换为XML元素
        _wrongQuestions.forEach { (questionId, wrongQuestionInfo) ->
            val wrongQuestionElement = Element("wrongQuestion")
            wrongQuestionElement.setAttribute("questionId", questionId)
            wrongQuestionElement.setAttribute("errorCount", wrongQuestionInfo.errorCount.toString())
            wrongQuestionElement.setAttribute("lastErrorTime", wrongQuestionInfo.lastErrorTime.toString())
            wrongQuestionElement.setAttribute("mastered", wrongQuestionInfo.mastered.toString())
            wrongQuestionElement.setAttribute("consecutiveCorrectCount", wrongQuestionInfo.consecutiveCorrectCount.toString())

            element.addContent(wrongQuestionElement)
        }

        return element
    }

    /**
     * 从XML元素加载状态数据
     * @param state 要加载的状态XML元素
     */
    override fun loadState(state: Element) {
        try {
            _wrongQuestions.clear()

            // 解析XML中的错题信息并重建错题映射
            state.getChildren("wrongQuestion").forEach { wrongQuestionElement ->
                val questionId = wrongQuestionElement.getAttributeValue("questionId")
                val errorCount = wrongQuestionElement.getAttributeValue("errorCount")?.toIntOrNull() ?: 1
                val lastErrorTime = wrongQuestionElement.getAttributeValue("lastErrorTime")?.toLongOrNull() ?: System.currentTimeMillis()
                val mastered = wrongQuestionElement.getAttributeValue("mastered")?.toBooleanStrictOrNull() ?: false
                val consecutiveCorrectCount = wrongQuestionElement.getAttributeValue("consecutiveCorrectCount")?.toIntOrNull() ?: 0

                _wrongQuestions[questionId] = WrongQuestionInfo(
                    questionId = questionId,
                    errorCount = errorCount,
                    lastErrorTime = lastErrorTime,
                    mastered = mastered,
                    consecutiveCorrectCount = consecutiveCorrectCount
                )
            }
        } catch (e: Exception) {
            logger.error("Error loading wrong questions", e)
        }
    }

    /**
     * 添加错题本变更监听器
     * @param listener 要添加的监听器
     */
    fun addWrongQuestionListener(listener: WrongQuestionChangeListener) {
        listeners.add(listener)
    }

    /**
     * 移除错题本变更监听器
     * @param listener 要移除的监听器
     */
    fun removeWrongQuestionListener(listener: WrongQuestionChangeListener) {
        listeners.remove(listener)
    }

    /**
     * 通知所有注册的监听器错题本已更新
     */
    private fun notifyListeners() {
        listeners.forEach { it.onWrongQuestionUpdated() }
    }

    /**
     * 记录错误答案，将题目加入错题本或更新错误次数
     * @param questionId 题目ID
     */
    fun recordWrongAnswer(questionId: String) {
        val currentInfo = _wrongQuestions[questionId]
        val newErrorCount = if (currentInfo != null) currentInfo.errorCount + 1 else 1

        _wrongQuestions[questionId] = WrongQuestionInfo(
            questionId = questionId,
            errorCount = newErrorCount,
            lastErrorTime = System.currentTimeMillis(),
            mastered = false,
            consecutiveCorrectCount = 0
        )

        notifyListeners()
    }

    /**
     * 记录正确答案，更新连续正确次数和掌握状态
     * @param questionId 题目ID
     */
    fun recordCorrectAnswer(questionId: String) {
        val currentInfo = _wrongQuestions[questionId]

        if (currentInfo != null) {
            val newConsecutiveCount = currentInfo.consecutiveCorrectCount + 1
            val mastered = newConsecutiveCount >= 3 // 默认连续3次答对算掌握

            _wrongQuestions[questionId] = WrongQuestionInfo(
                questionId = questionId,
                errorCount = currentInfo.errorCount,
                lastErrorTime = currentInfo.lastErrorTime,
                mastered = mastered,
                consecutiveCorrectCount = newConsecutiveCount
            )

            notifyListeners()
        } else {
            // 如果之前不在错题本中，这次答对不需要记录
            return
        }
    }

    /**
     * 从错题本中移除指定题目
     * @param questionId 要移除的题目ID
     */
    fun removeWrongQuestion(questionId: String) {
        _wrongQuestions.remove(questionId)
        notifyListeners()
    }

    /**
     * 获取未掌握的错题列表
     * @return 未掌握的错题信息列表
     */
    fun getUnmasteredWrongQuestions(): List<WrongQuestionInfo> {
        return _wrongQuestions.values.filter { !it.mastered }
    }

    /**
     * 获取已掌握的错题列表
     * @return 已掌握的错题信息列表
     */
    fun getMasteredWrongQuestions(): List<WrongQuestionInfo> {
        return _wrongQuestions.values.filter { it.mastered }
    }

    /**
     * 检查题目是否在错题本中
     * @param questionId 题目ID
     * @return 如果题目在错题本中返回true，否则返回false
     */
    fun isInWrongBook(questionId: String): Boolean {
        return _wrongQuestions.containsKey(questionId)
    }

    /**
     * 获取指定题目的错题信息
     * @param questionId 题目ID
     * @return 错题信息对象，如果不存在则返回null
     */
    fun getWrongQuestionInfo(questionId: String): WrongQuestionInfo? {
        return _wrongQuestions[questionId]
    }
}
