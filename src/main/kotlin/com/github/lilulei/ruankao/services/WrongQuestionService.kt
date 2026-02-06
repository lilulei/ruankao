package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.WrongQuestionInfo
import com.github.lilulei.ruankao.model.ExamLevel
import com.github.lilulei.ruankao.model.ExamType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
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
class WrongQuestionService(private val project: Project) : PersistentStateComponent<Element> {
    private val logger = logger<WrongQuestionService>()
    private val userIdentityService = project.getService(UserIdentityService::class.java)
    private val _wrongQuestions = mutableMapOf<String, WrongQuestionInfo>()
    private val listeners = mutableListOf<WrongQuestionChangeListener>()
    
    init {
        logger.info("=== 初始化错题本服务 ===")
        logger.info("注册身份变更监听器")
        // 注册身份变更监听器
        userIdentityService.addIdentityChangeListener(object : UserIdentityChangeListener {
            override fun onIdentityChanged(newLevel: ExamLevel, newExamType: ExamType) {
                logger.info("=== 错题本服务收到身份变更通知 ===")
                logger.info("新身份: ${'$'}{newLevel.displayName} - ${'$'}{newExamType.displayName}")
                logger.info("切换前错题数量: ${'$'}{_wrongQuestions.size}")
                switchToIdentity(newLevel.displayName, newExamType.displayName)
                logger.info("切换后错题数量: ${'$'}{_wrongQuestions.size}")
                logger.info("=== 错题本身份切换完成 ===")
            }
        })
        logger.info("错题本服务初始化完成")
        logger.info("当前错题数量: ${'$'}{_wrongQuestions.size}")
        logger.info("=======================")
    }

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
            
            // 保存身份信息
            if (wrongQuestionInfo.examLevel != null) {
                wrongQuestionElement.setAttribute("examLevel", wrongQuestionInfo.examLevel)
            }
            if (wrongQuestionInfo.examType != null) {
                wrongQuestionElement.setAttribute("examType", wrongQuestionInfo.examType)
            }

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
                val examLevel = wrongQuestionElement.getAttributeValue("examLevel")
                val examType = wrongQuestionElement.getAttributeValue("examType")

                _wrongQuestions[questionId] = WrongQuestionInfo(
                    questionId = questionId,
                    errorCount = errorCount,
                    lastErrorTime = lastErrorTime,
                    mastered = mastered,
                    consecutiveCorrectCount = consecutiveCorrectCount,
                    examLevel = examLevel,
                    examType = examType
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
        
        // 获取当前用户身份信息
        val currentLevel = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel().displayName
        } else {
            null
        }
        val currentExamType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType().displayName
        } else {
            null
        }

        _wrongQuestions[questionId] = WrongQuestionInfo(
            questionId = questionId,
            errorCount = newErrorCount,
            lastErrorTime = System.currentTimeMillis(),
            mastered = false,
            consecutiveCorrectCount = 0,
            examLevel = currentLevel,
            examType = currentExamType
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
            
            // 保持原有的身份信息
            val currentLevel = currentInfo.examLevel
            val currentExamType = currentInfo.examType

            _wrongQuestions[questionId] = WrongQuestionInfo(
                questionId = questionId,
                errorCount = currentInfo.errorCount,
                lastErrorTime = currentInfo.lastErrorTime,
                mastered = mastered,
                consecutiveCorrectCount = newConsecutiveCount,
                examLevel = currentLevel,
                examType = currentExamType
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
     * 获取当前身份下的错题列表
     * @return 当前身份下的错题信息列表
     */
    fun getWrongQuestionsForCurrentIdentity(): List<WrongQuestionInfo> {
        logger.info("=== 获取当前身份错题数据 ===")
        val currentLevel = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel().displayName
        } else {
            "软考高级"
        }
        val currentExamType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType().displayName
        } else {
            ExamType.PROJECT_MANAGER.displayName
        }
        
        logger.info("当前用户身份: $currentLevel - $currentExamType")
        logger.info("总错题数量: ${_wrongQuestions.size}")
        
        val filteredQuestions = _wrongQuestions.values.filter { 
            it.examLevel == currentLevel && it.examType == currentExamType 
        }
        
        logger.info("过滤后错题数量: ${filteredQuestions.size}")
        logger.info("=== 错题数据获取完成 ===")
        return filteredQuestions
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

    /**
     * 清空所有错题本数据
     * 重置为初始状态
     */
    fun clearAllData() {
        _wrongQuestions.clear()
        notifyListeners()
        logger.info("错题本数据已清空")
    }

    /**
     * 切换到指定身份的错题本数据
     * 
     * @param level 考试级别
     * @param examType 考试类型
     */
    fun switchToIdentity(examLevel: String, examType: String) {
        // 更新所有错题信息的身份信息
        _wrongQuestions.replaceAll { _, info ->
            info.copy(
                examLevel = examLevel,
                examType = examType
            )
        }
        
        notifyListeners()
        logger.info("错题本已切换到身份: $examLevel - $examType")
    }

    /**
     * 获取当前身份标识符
     * 用于区分不同身份的数据
     * 
     * @return 身份标识符字符串，格式为 "level_examType"
     */
    fun getCurrentIdentityKey(): String {
        // 取第一条记录的身份信息作为代表
        val sampleInfo = _wrongQuestions.values.firstOrNull()
        val examLevel = sampleInfo?.examLevel ?: "unknown_examLevel"
        val examType = sampleInfo?.examType ?: "unknown_examType"
        return "${'$'}{examLevel}_${'$'}{examType}"
    }
}
