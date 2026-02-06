package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element
import java.util.*

/**
 * 试题服务类，负责管理软考试题
 * 实现PersistentStateComponent接口以支持状态持久化
 * @param State 注解指定状态名称和存储文件
 * @param Service 注解指定服务级别为项目级别
 */
@State(name = "QuestionService", storages = [Storage("softexam_questions.xml")])
@Service(Service.Level.PROJECT)
class QuestionService : PersistentStateComponent<Element> {
    private val logger = logger<QuestionService>()
    private val _questions = mutableMapOf<String, Question>()

    /**
     * 获取所有试题的只读映射
     */
    val allQuestions: Map<String, Question>
        get() = _questions.toMap()

    /**
     * 获取所有试题列表
     */
    val allQuestionsList: List<Question>
        get() = _questions.values.toList()

    /**
     * 获取指定ID的试题
     * @param id 试题ID
     * @return 试题对象，如果不存在则返回null
     */
    fun getQuestionById(id: String): Question? {
        return _questions[id]
    }

    /**
     * 根据难度获取试题
     * @param difficulty 难度等级
     * @return 该难度下的所有试题列表
     */
    fun getQuestionsByDifficulty(difficulty: DifficultyLevel): List<Question> {
        return _questions.values.filter { it.level == difficulty }
    }

    /**
     * 根据考试类型获取试题
     * @param examType 考试类型
     * @return 该考试类型下的所有试题列表
     */
    fun getQuestionsByExamType(examType: ExamType): List<Question> {
        return _questions.values.filter { it.examType == examType }
    }

    /**
     * 获取随机试题
     * @param count 需要获取的试题数量
     * @return 随机选取的试题列表
     */
    fun getRandomQuestions(count: Int): List<Question> {
        val questions = _questions.values.toList()
        return if (questions.size <= count) {
            questions.shuffled()
        } else {
            questions.shuffled().take(count)
        }
    }

    /**
     * 获取指定难度的随机试题
     * @param difficulty 难度等级
     * @param count 需要获取的试题数量
     * @return 指定难度中随机选取的试题列表
     */
    fun getRandomQuestionsByDifficulty(difficulty: DifficultyLevel, count: Int): List<Question> {
        val questions = getQuestionsByDifficulty(difficulty)
        return if (questions.size <= count) {
            questions.shuffled()
        } else {
            questions.shuffled().take(count)
        }
    }

    /**
     * 添加试题
     * @param question 要添加的试题对象
     */
    fun addQuestion(question: Question) {
        _questions[question.id] = question
        logger.info("添加试题: ${question.id}")
    }

    /**
     * 删除试题
     * @param id 要删除的试题ID
     */
    fun removeQuestion(id: String) {
        val removed = _questions.remove(id)
        if (removed != null) {
            logger.info("删除试题: $id")
            // IntelliJ的PersistentStateComponent会自动处理持久化
            // 删除后确保界面刷新即可
        }
    }

    /**
     * 更新试题
     * @param question 更新后的试题对象
     */
    fun updateQuestion(question: Question) {
        _questions[question.id] = question
        logger.info("更新试题: ${question.id}")
    }

    /**
     * 检查试题是否存在
     * @param id 试题ID
     * @return 如果试题存在返回true，否则返回false
     */
    fun questionExists(id: String): Boolean {
        return _questions.containsKey(id)
    }

    /**
     * 刷新试题列表（从持久化存储重新加载）
     */
    fun refreshQuestions() {
        logger.info("刷新试题列表，当前试题数量: ${_questions.size}")
    }

    /**
     * 获取所有难度等级列表
     * @return 所有试题难度等级的去重列表
     */
    fun getAllDifficultyLevels(): List<DifficultyLevel> {
        return _questions.values.map { it.level }.distinct()
    }

    /**
     * 获取所有考试类型列表
     * @return 所有考试类型的去重列表
     */
    fun getAllExamTypes(): List<ExamType> {
        return _questions.values.map { it.examType }.distinct()
    }

    /**
     * 根据考试级别获取试题
     * @param level 考试级别（"软考高级", "软考中级", "软考初级"）
     * @return 该级别的所有试题列表
     */
    fun getQuestionsByLevel(level: String): List<Question> {
        val examTypesInLevel = when (level) {
            "软考高级" -> listOf(
                ExamType.SYSTEM_ANALYST,
                ExamType.SYSTEM_ARCHITECT,
                ExamType.NETWORK_PLANNER,
                ExamType.PROJECT_MANAGER,
                ExamType.SYSTEM_PLANNING_MANAGER
            )
            "软考中级" -> listOf(
                ExamType.SYSTEM_INTEGRATION_ENGINEER,
                ExamType.NETWORK_ENGINEER,
                ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER,
                ExamType.SOFTWARE_TESTER,
                ExamType.DATABASE_ENGINEER,
                ExamType.MULTIMEDIA_DESIGNER,
                ExamType.SOFTWARE_DESIGNER,
                ExamType.INFORMATION_SYSTEM_SUPERVISOR,
                ExamType.E_COMMERCE_DESIGNER,
                ExamType.INFORMATION_SECURITY_ENGINEER,
                ExamType.EMBEDDED_SYSTEM_DESIGNER,
                ExamType.SOFTWARE_PROCESS_EVALUATOR,
                ExamType.COMPUTER_AIDED_DESIGNER,
                ExamType.COMPUTER_HARDWARE_ENGINEER,
                ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER
            )
            "软考初级" -> listOf(
                ExamType.PROGRAMMER,
                ExamType.NETWORK_ADMINISTRATOR,
                ExamType.INFORMATION_PROCESSING_TECHNICIAN,
                ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER,
                ExamType.MULTIMEDIA_APPLICATION_DESIGNER,
                ExamType.E_COMMERCE_TECHNICIAN,
                ExamType.WEB_DESIGNER
            )
            else -> emptyList() // 未知级别返回空列表
        }
        
        return _questions.values.filter { it.examType in examTypesInLevel }
    }

    /**
     * 获取试题总数
     * @return 试题总数
     */
    fun getTotalQuestionCount(): Int {
        return _questions.size
    }

    /**
     * 获取指定难度的试题数量
     * @param difficulty 难度等级
     * @return 该难度下的试题数量
     */
    fun getQuestionCountByDifficulty(difficulty: DifficultyLevel): Int {
        return getQuestionsByDifficulty(difficulty).size
    }

    /**
     * 获取指定考试类型的试题数量
     * @param examType 考试类型
     * @return 该考试类型的试题数量
     */
    fun getQuestionCountByExamType(examType: ExamType): Int {
        return getQuestionsByExamType(examType).size
    }

    /**
     * 根据章节获取试题
     * @param chapter 章节名称
     * @return 该章节下的所有试题列表
     */
    fun getQuestionsByChapter(chapter: String): List<Question> {
        return _questions.values.filter { it.chapter != null && it.chapter.equals(chapter, ignoreCase = true) }
    }

    /**
     * 获取所有章节列表
     * @return 所有试题章节的去重列表
     */
    fun getAllChapters(): List<String> {
        return _questions.values.mapNotNull { it.chapter }.distinct()
    }

    /**
     * 根据级别和章节获取试题
     * @param level 考试级别（如"软考高级"）
     * @param chapter 章节名称
     * @return 该级别和章节下的所有试题列表
     */
    fun getQuestionsByLevelAndChapter(level: String, chapter: String): List<Question> {
        val questionsByLevel = getQuestionsByLevel(level)
        return questionsByLevel.filter { it.chapter != null && it.chapter.equals(chapter, ignoreCase = true) }
    }

    /**
     * 获取当前状态元素，用于持久化保存
     * @return 包含所有试题信息的XML元素
     */
    override fun getState(): Element {
        val element = Element("QuestionService")

        _questions.forEach { (_, question) ->
            val questionElement = Element("question")
            questionElement.setAttribute("id", question.id)
            questionElement.setAttribute("title", question.title)

            questionElement.setAttribute("difficulty", question.level.displayName)
            questionElement.setAttribute("examType", question.examType.displayName)
            if (question.chapter != null) {
                questionElement.setAttribute("chapter", question.chapter)
            }

            // 添加选项
            val optionsElement = Element("options")
            question.options.forEach { (key, value) ->
                val optionElement = Element("option")
                optionElement.setAttribute("key", key)
                optionElement.text = value
                optionsElement.addContent(optionElement)
            }
            questionElement.addContent(optionsElement)

            // 添加正确答案
            val answersElement = Element("correctAnswers")
            question.correctAnswers.forEach { answer ->
                val answerElement = Element("answer")
                answerElement.text = answer
                answersElement.addContent(answerElement)
            }
            questionElement.addContent(answersElement)

            // 添加解析
            val explanationElement = Element("explanation")
            explanationElement.text = question.explanation
            questionElement.addContent(explanationElement)

            element.addContent(questionElement)
        }

        return element
    }

    /**
     * 从XML元素加载状态数据
     * @param state 要加载的状态XML元素
     */
    override fun loadState(state: Element) {
        try {
            _questions.clear()

            state.getChildren("question").forEach { questionElement ->
                val id = questionElement.getAttributeValue("id")
                val title = questionElement.getAttributeValue("title") ?: ""

                val difficultyName = questionElement.getAttributeValue("difficulty") ?: "中等"
                val examTypeName = questionElement.getAttributeValue("examType") ?: "SOFTWARE_DESIGNER"

                val difficulty = try {
                    // 尝试匹配中文显示名称
                    DifficultyLevel.values().find { it.displayName == difficultyName } ?: 
                    // 如果没找到，尝试匹配枚举名称
                    DifficultyLevel.valueOf(difficultyName.uppercase())
                } catch (e: IllegalArgumentException) {
                    DifficultyLevel.MEDIUM
                }

                val examType = try {
                    // 尝试匹配中文显示名称
                    ExamType.values().find { it.displayName == examTypeName } ?: 
                    // 如果没找到，尝试匹配枚举名称
                    ExamType.valueOf(examTypeName)
                } catch (e: IllegalArgumentException) {
                    ExamType.SOFTWARE_DESIGNER
                }

                // 解析选项
                val options = mutableMapOf<String, String>()
                val optionsElement = questionElement.getChild("options")
                optionsElement?.getChildren("option")?.forEach { optionElement ->
                    val key = optionElement.getAttributeValue("key")
                    val value = optionElement.text
                    if (key != null) {
                        options[key] = value
                    }
                }

                // 解析正确答案
                val correctAnswers = mutableSetOf<String>()
                val answersElement = questionElement.getChild("correctAnswers")
                answersElement?.getChildren("answer")?.forEach { answerElement ->
                    correctAnswers.add(answerElement.text)
                }

                // 解析解析
                val explanationElement = questionElement.getChild("explanation")
                val explanation = explanationElement?.text ?: ""

                val question = Question(
                    id = id,
                    title = title,

                    level = difficulty,
                    examType = examType,
                    options = options,
                    correctAnswers = correctAnswers,
                    explanation = explanation,
                    chapter = questionElement.getAttributeValue("chapter") ?: null
                )

                _questions[id] = question
            }
        } catch (e: Exception) {
            logger.error("Error loading questions", e)
        }
    }
}