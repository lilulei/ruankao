package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element
import java.time.LocalDate

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
    
    init {
        logger.info("=== 初始化试题服务 ===")
        logger.info("试题存储文件: softexam_questions.xml")
        logger.info("初始试题数量: ${_questions.size}")
        logger.info("=======================")
    }

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
     * @param level 难度等级
     * @return 该难度下的所有试题列表
     */
    fun getQuestionsByDifficulty(level: DifficultyLevel): List<Question> {
        return _questions.values.filter { it.level == level }
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
     * @param level 难度等级
     * @param count 需要获取的试题数量
     * @return 指定难度中随机选取的试题列表
     */
    fun getRandomQuestionsByDifficulty(level: DifficultyLevel, count: Int): List<Question> {
        val questions = getQuestionsByDifficulty(level)
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
        logger.info("试题详情: 标题='${question.title}', 难度=${question.level.displayName}, 类型=${question.examType.displayName}")
        logger.info("添加后试题总数: ${_questions.size}")
        
        // 强制触发持久化保存
        forceSave()
    }

    /**
     * 删除试题
     * @param id 要删除的试题ID
     */
    fun removeQuestion(id: String) {
        val removed = _questions.remove(id)
        if (removed != null) {
            logger.info("删除试题: $id")
            logger.info("删除后试题总数: ${_questions.size}")
            // IntelliJ的PersistentStateComponent会自动处理持久化
            // 删除后确保界面刷新即可
            forceSave()
        } else {
            logger.warn("尝试删除不存在的试题: $id")
        }
    }

    /**
     * 更新试题
     * @param question 更新后的试题对象
     */
    fun updateQuestion(question: Question) {
        _questions[question.id] = question
        logger.info("更新试题: ${question.id}")
        logger.info("更新后试题总数: ${_questions.size}")
        forceSave()
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
     * 强制触发持久化保存
     * 通过重新设置状态来强制保存数据
     */
    private fun forceSave() {
        try {
            // 获取当前项目
            val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                // 重新获取服务实例并触发保存
                val service = project.getService(QuestionService::class.java)
                // 通过重新设置相同的状态来触发保存
                service.loadState(getState())
                logger.info("强制保存试题数据完成")
            }
        } catch (e: Exception) {
            logger.warn("强制保存试题数据失败", e)
        }
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
     * 根据当前用户身份获取试题
     * @param userIdentityService 用户身份服务实例
     * @return 符合当前用户身份的所有试题列表
     */
    fun getQuestionsForCurrentUser(userIdentityService: UserIdentityService): List<Question> {
        val currentLevel = userIdentityService.getSelectedExamLevel().displayName
        val currentExamType = userIdentityService.getSelectedExamType().displayName
        return getQuestionsByIdentity(currentLevel, currentExamType)
    }

    /**
     * 根据当前身份获取试题
     * @param examLevel 考试级别
     * @param examType 考试类型
     * @return 符合当前身份的所有试题列表
     */
    fun getQuestionsByIdentity(examLevel: String, examType: String): List<Question> {
        return _questions.values.filter { 
            it.examLevel.displayName == examLevel && it.examType.displayName == examType 
        }
    }

    /**
     * 根据考试级别和章节获取试题
     * @param examLevel 考试级别
     * @param examType 考试类型
     * @param chapter 章节名称
     * @return 该身份和章节下的所有试题列表
     */
    fun getQuestionsByIdentityAndChapter(examLevel: String, examType: String, chapter: String): List<Question> {
        val questionsByIdentity = getQuestionsByIdentity(examLevel, examType)
        return questionsByIdentity.filter { it.chapter != null && it.chapter.equals(chapter, ignoreCase = true) }
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

            questionElement.setAttribute("level", question.level.displayName)
            questionElement.setAttribute("examType", question.examType.displayName)
            questionElement.setAttribute("examLevel", question.examLevel.displayName)
            questionElement.setAttribute("year", question.year.toString())
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
            logger.info("开始加载试题数据...")
            val oldSize = _questions.size
            _questions.clear()
            
            val questionElements = state.getChildren("question")
            logger.info("XML文件中找到 ${questionElements.size} 道试题")
            
            questionElements.forEach { questionElement ->
                val id = questionElement.getAttributeValue("id")
                val title = questionElement.getAttributeValue("title") ?: ""
                
                logger.debug("正在加载试题: $id - $title")
                
                val levelName = questionElement.getAttributeValue("level") ?: "中等"
                val examTypeName = questionElement.getAttributeValue("examType") ?: "SOFTWARE_DESIGNER"
                val examLevelName = questionElement.getAttributeValue("examLevel") ?: "软考高级"
                val yearStr = questionElement.getAttributeValue("year") ?: "2025-11-08"

                val level = try {
                    // 尝试匹配中文显示名称
                    DifficultyLevel.values().find { it.displayName == levelName } ?: 
                    // 如果没找到，尝试匹配枚举名称
                    DifficultyLevel.valueOf(levelName.uppercase())
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

                val examLevel = try {
                    // 尝试匹配中文显示名称
                    ExamLevel.values().find { it.displayName == examLevelName } ?: 
                    // 如果没找到，尝试匹配枚举名称
                    ExamLevel.valueOf(examLevelName.uppercase())
                } catch (e: IllegalArgumentException) {
                    ExamLevel.SENIOR
                }

                // 解析考试年份
                val year = try {
                    LocalDate.parse(yearStr)
                } catch (e: Exception) {
                    LocalDate.of(2025, 11, 8) // 默认日期
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
                    options = options,
                    correctAnswers = correctAnswers,
                    explanation = explanation,
                    level = level,
                    chapter = questionElement.getAttributeValue("chapter") ?: null,
                    year = year,
                    examType = examType,
                    examLevel = examLevel
                )

                _questions[id] = question
            }
            
            logger.info("试题数据加载完成，加载了 ${_questions.size} 道试题 (之前: $oldSize)")
            
        } catch (e: Exception) {
            logger.error("Error loading questions", e)
        }
    }
}