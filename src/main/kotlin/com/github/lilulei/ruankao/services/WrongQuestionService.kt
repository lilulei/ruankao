package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.WrongQuestionInfo
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element

// 定义监听器接口
interface WrongQuestionChangeListener {
    fun onWrongQuestionUpdated()
}

@State(name = "WrongQuestionService", storages = [Storage("softexam_wrong_questions.xml")])
@Service(Service.Level.PROJECT)
class WrongQuestionService : PersistentStateComponent<Element> {
    private val logger = logger<WrongQuestionService>()
    private val _wrongQuestions = mutableMapOf<String, WrongQuestionInfo>()
    private val listeners = mutableListOf<WrongQuestionChangeListener>()
    
    val allWrongQuestions: Map<String, WrongQuestionInfo>
        get() = _wrongQuestions.toMap()

    override fun getState(): Element {
        val element = Element("WrongQuestionService")
        
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

    override fun loadState(state: Element) {
        try {
            _wrongQuestions.clear()
            
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

    // 添加监听器
    fun addWrongQuestionListener(listener: WrongQuestionChangeListener) {
        listeners.add(listener)
    }

    // 移除监听器
    fun removeWrongQuestionListener(listener: WrongQuestionChangeListener) {
        listeners.remove(listener)
    }

    // 触发监听器
    private fun notifyListeners() {
        listeners.forEach { it.onWrongQuestionUpdated() }
    }

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

    fun removeWrongQuestion(questionId: String) {
        _wrongQuestions.remove(questionId)
        notifyListeners()
    }

    fun getUnmasteredWrongQuestions(): List<WrongQuestionInfo> {
        return _wrongQuestions.values.filter { !it.mastered }
    }

    fun getMasteredWrongQuestions(): List<WrongQuestionInfo> {
        return _wrongQuestions.values.filter { it.mastered }
    }

    fun isInWrongBook(questionId: String): Boolean {
        return _wrongQuestions.containsKey(questionId)
    }

    fun getWrongQuestionInfo(questionId: String): WrongQuestionInfo? {
        return _wrongQuestions[questionId]
    }
}