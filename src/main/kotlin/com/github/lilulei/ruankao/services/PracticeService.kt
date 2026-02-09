package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.*
import com.github.lilulei.ruankao.services.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element
import java.util.*

/**
 * 练习服务类，用于管理练习会话的创建、保存、加载和统计
 * 实现了PersistentStateComponent接口以支持数据持久化
 */
@State(name = "PracticeService", storages = [Storage("softexam_practices.xml")])
@Service(Service.Level.PROJECT)
class PracticeService : PersistentStateComponent<Element> {
    private val logger = logger<PracticeService>()
    private val _practiceSessions = mutableListOf<PracticeSession>()
    private var currentSession: PracticeSession? = null

    /**
     * 获取所有练习会话的只读列表
     */
    val allPracticeSessions: List<PracticeSession>
        get() = _practiceSessions.toList()

    /**
     * 获取当前状态元素，用于持久化练习会话数据
     * @return 包含所有练习会话信息的XML元素
     */
    override fun getState(): Element {
        val element = Element("PracticeService")

        _practiceSessions.forEach { session ->
            val sessionElement = Element("session")
            sessionElement.setAttribute("sessionId", session.sessionId)
            sessionElement.setAttribute("startTime", session.startTime.toString())
            if (session.endTime != null) {
                sessionElement.setAttribute("endTime", session.endTime.toString())
            }
            sessionElement.setAttribute("sessionType", session.sessionType.name)

            // 添加问题
            val questionsElement = Element("questions")
            session.questions.forEach { question ->
                val questionElement = Element("question")
                questionElement.setAttribute("id", question.id)
                questionsElement.addContent(questionElement)
            }
            sessionElement.addContent(questionsElement)

            // 添加答案记录
            val answersElement = Element("answers")
            session.answers.forEach { (questionId, answerRecord) ->
                val answerElement = Element("answer")
                answerElement.setAttribute("questionId", questionId)
                answerElement.setAttribute("isCorrect", answerRecord.isCorrect.toString())
                answerElement.setAttribute("answeredAt", answerRecord.answeredAt.toString())

                val selectedOptionsElement = Element("selectedOptions")
                answerRecord.selectedOptions.forEach { option ->
                    val optionElement = Element("option")
                    optionElement.text = option
                    selectedOptionsElement.addContent(optionElement)
                }
                answerElement.addContent(selectedOptionsElement)

                answersElement.addContent(answerElement)
            }
            sessionElement.addContent(answersElement)

            element.addContent(sessionElement)
        }

        return element
    }

    /**
     * 加载状态元素中的练习会话数据
     * @param state 包含练习会话信息的XML元素
     */
    override fun loadState(state: Element) {
        try {
            _practiceSessions.clear()

            state.getChildren("session").forEach { sessionElement ->
                val sessionId = sessionElement.getAttributeValue("sessionId")
                val startTime = sessionElement.getAttributeValue("startTime")?.toLongOrNull() ?: System.currentTimeMillis()
                val endTime = sessionElement.getAttributeValue("endTime")?.toLongOrNull()
                val sessionType = try {
                    PracticeType.valueOf(sessionElement.getAttributeValue("sessionType") ?: "RANDOM_PRACTICE")
                } catch (e: IllegalArgumentException) {
                    PracticeType.RANDOM_PRACTICE
                }

                val questions = mutableListOf<Question>()
                val questionsElement = sessionElement.getChild("questions")
                questionsElement?.getChildren("question")?.forEach { questionElement ->
                    val questionId = questionElement.getAttributeValue("id")
                    val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
                    if (project != null) {
                        val questionService = project.getService(QuestionService::class.java)
                        val question = questionService.getQuestionById(questionId)
                        if (question != null) {
                            questions.add(question)
                        }
                    }
                }

                val answers = mutableMapOf<String, AnswerRecord>()
                val answersElement = sessionElement.getChild("answers")
                answersElement?.getChildren("answer")?.forEach { answerElement ->
                    val questionId = answerElement.getAttributeValue("questionId")
                    val isCorrect = answerElement.getAttributeValue("isCorrect")?.toBooleanStrictOrNull() ?: false
                    val answeredAt = answerElement.getAttributeValue("answeredAt")?.toLongOrNull() ?: System.currentTimeMillis()

                    val selectedOptions = mutableSetOf<String>()
                    val selectedOptionsElement = answerElement.getChild("selectedOptions")
                    selectedOptionsElement?.getChildren("option")?.forEach { optionElement ->
                        selectedOptions.add(optionElement.text)
                    }

                    answers[questionId] = AnswerRecord(
                        questionId = questionId,
                        selectedOptions = selectedOptions.toSet(),
                        isCorrect = isCorrect,
                        answeredAt = answeredAt
                    )
                }

                val session = PracticeSession(
                    sessionId = sessionId,
                    startTime = startTime,
                    endTime = endTime,
                    questions = questions,
                    answers = answers,
                    sessionType = sessionType
                )

                _practiceSessions.add(session)
            }
        } catch (e: Exception) {
            logger.error("Error loading practice sessions", e)
        }
    }

    /**
     * 开始一个新的练习会话
     * @param sessionType 练习类型（如模拟考试、专项练习等）
     * @param questions 要练习的问题列表
     * @return 新创建的练习会话对象
     */
    fun startNewSession(sessionType: PracticeType, questions: List<Question>): PracticeSession {
        currentSession = PracticeSession(
            sessionType = sessionType,
            questions = questions
        )
        return currentSession!!
    }

    /**
     * 开始一个新的练习会话，自动根据用户选择的身份获取对应级别的试题
     * @param sessionType 练习类型（如模拟考试、专项练习等）
     * @param project 当前项目实例
     * @return 新创建的练习会话对象
     */
    fun startNewSessionWithUserLevel(sessionType: PracticeType, project: Project): PracticeSession {
        val userIdentityService = project.getService(UserIdentityService::class.java)
        val selectedLevel = userIdentityService.getSelectedExamLevel()
        val selectedExamType = userIdentityService.getSelectedExamType()
        
        val questionService = project.getService(QuestionService::class.java)
        val questions = when (sessionType) {
            PracticeType.RANDOM_PRACTICE -> questionService.getRandomQuestions(10)
            PracticeType.SPECIAL_TOPIC -> questionService.getQuestionsByIdentity(selectedLevel.displayName, selectedExamType.displayName)
            PracticeType.MOCK_EXAM -> questionService.getRandomQuestionsByDifficulty(com.github.lilulei.ruankao.model.DifficultyLevel.MEDIUM, 50)
            PracticeType.DAILY_PRACTICE -> questionService.getRandomQuestions(10)
            else -> questionService.getRandomQuestions(10) // 默认返回随机题目
        }
        
        return startNewSession(sessionType, questions)
    }

    /**
     * 获取当前正在进行的练习会话
     * @return 当前练习会话对象，如果没有则返回null
     */
    fun getCurrentSession(): PracticeSession? {
        return currentSession
    }

    /**
     * 提交问题答案并更新会话状态
     * @param questionId 问题ID
     * @param selectedOptions 用户选择的选项集合
     * @param isCorrect 答案是否正确
     */
    fun submitAnswer(questionId: String, selectedOptions: Set<String>, isCorrect: Boolean) {
        val session = currentSession ?: return

        session.answers[questionId] = AnswerRecord(
            questionId = questionId,
            selectedOptions = selectedOptions,
            isCorrect = isCorrect
        )

        // 如果题目全部回答完毕，结束会话
        if (session.answers.size == session.questions.size) {
            endCurrentSession()
        }
    }

    /**
     * 记录错误答案到错题本服务
     * @param questionId 问题ID
     */
    fun updateWrongAnswer(questionId: String) {
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            val wrongQuestionService = project.getService(WrongQuestionService::class.java)
            wrongQuestionService.recordWrongAnswer(questionId)
        }
    }

    /**
     * 记录正确答案到错题本服务
     * @param questionId 问题ID
     */
    fun updateCorrectAnswer(questionId: String) {
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            val wrongQuestionService = project.getService(WrongQuestionService::class.java)
            wrongQuestionService.recordCorrectAnswer(questionId)
        }
    }

    /**
     * 结束当前练习会话并保存结果
     */
    fun endCurrentSession() {
        val session = currentSession ?: return
        val endedSession = session.copy(endTime = System.currentTimeMillis())
        _practiceSessions.add(endedSession)
        currentSession = null

        // 记录到学习统计
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            val statsService = project.getService(LearningStatisticsService::class.java)
            statsService.recordPracticeSession(endedSession)
        }

        // 更新错题本
        updateWrongQuestions(endedSession)
    }

    /**
     * 根据练习会话结果更新错题本
     * @param session 练习会话对象
     */
    private fun updateWrongQuestions(session: PracticeSession) {
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            val wrongQuestionService = project.getService(WrongQuestionService::class.java)

            session.answers.forEach { (questionId, answerRecord) ->
                if (answerRecord.isCorrect) {
                    wrongQuestionService.recordCorrectAnswer(questionId)
                } else {
                    wrongQuestionService.recordWrongAnswer(questionId)
                }
            }
        }
    }

    /**
     * 获取练习历史记录
     * @return 所有已完成的练习会话列表
     */
    fun getPracticeHistory(): List<PracticeSession> {
        return _practiceSessions.toList()
    }

    /**
     * 根据会话ID获取特定的练习会话
     * @param sessionId 会话ID
     * @return 对应的练习会话对象，如果不存在则返回null
     */
    fun getSessionById(sessionId: String): PracticeSession? {
        return _practiceSessions.find { it.sessionId == sessionId }
    }
}
