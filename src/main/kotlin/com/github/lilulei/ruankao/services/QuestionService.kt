package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.model.ExamType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import java.io.StringReader
import java.util.*
import kotlin.random.Random

@State(name = "QuestionService", storages = [Storage("softexam_questions.xml")])
@Service(Service.Level.PROJECT)
class QuestionService : PersistentStateComponent<Element> {
    private val logger = logger<QuestionService>()
    private val _questions = mutableListOf<Question>()
    private val _customQuestions = mutableListOf<Question>()
    
    val allQuestions: List<Question>
        get() = (_questions + _customQuestions).distinctBy { it.id }

    init {
        loadDefaultQuestions()
    }

    override fun getState(): Element {
        val element = Element("QuestionService")
        val customQuestionsElement = Element("customQuestions")
        
        _customQuestions.forEach { question ->
            val questionElement = Element("question")
            questionElement.setAttribute("id", question.id)
            questionElement.setAttribute("title", question.title)
            questionElement.setAttribute("explanation", question.explanation)
            questionElement.setAttribute("level", question.level.name)
            questionElement.setAttribute("category", question.category)
            questionElement.setAttribute("year", question.year.toString())
            questionElement.setAttribute("examType", question.examType.name)
            
            // 添加选项
            val optionsElement = Element("options")
            question.options.forEach { (key, value) ->
                val optionElement = Element("option")
                optionElement.setAttribute("key", key)
                optionElement.text = value
                optionsElement.addContent(optionElement)
            }
            questionElement.addContent(optionsElement)
            
            // 添加答案
            val answersElement = Element("answers")
            question.correctAnswers.forEach { answer ->
                val answerElement = Element("answer")
                answerElement.text = answer
                answersElement.addContent(answerElement)
            }
            questionElement.addContent(answersElement)
            
            customQuestionsElement.addContent(questionElement)
        }
        
        element.addContent(customQuestionsElement)
        return element
    }

    override fun loadState(state: Element) {
        try {
            val customQuestionsElement = state.getChild("customQuestions")
            if (customQuestionsElement != null) {
                _customQuestions.clear()
                
                customQuestionsElement.getChildren("question").forEach { questionElement ->
                    val id = questionElement.getAttributeValue("id")
                    val title = questionElement.getAttributeValue("title")
                    val explanation = questionElement.getAttributeValue("explanation")
                    val level = enumValueOf<com.github.lilulei.ruankao.model.DifficultyLevel>(
                        questionElement.getAttributeValue("level")
                    )
                    val category = questionElement.getAttributeValue("category")
                    val year = questionElement.getAttributeValue("year")?.toIntOrNull() ?: 0
                    val examType = enumValueOf<ExamType>(questionElement.getAttributeValue("examType"))
                    
                    val options = mutableMapOf<String, String>()
                    val optionsElement = questionElement.getChild("options")
                    optionsElement?.getChildren("option")?.forEach { optionElement ->
                        val key = optionElement.getAttributeValue("key")
                        val value = optionElement.text
                        options[key] = value
                    }
                    
                    val correctAnswers = mutableSetOf<String>()
                    val answersElement = questionElement.getChild("answers")
                    answersElement?.getChildren("answer")?.forEach { answerElement ->
                        correctAnswers.add(answerElement.text)
                    }
                    
                    val question = Question(
                        id = id,
                        title = title,
                        options = options,
                        correctAnswers = correctAnswers,
                        explanation = explanation,
                        level = level,
                        category = category,
                        year = year,
                        examType = examType
                    )
                    
                    _customQuestions.add(question)
                }
            }
        } catch (e: Exception) {
            logger.error("Error loading custom questions", e)
        }
    }

    private fun loadDefaultQuestions() {
        // 这里加载一些示例题目作为默认题库
        val sampleQuestions = listOf(
            Question(
                id = "1",
                title = "以下哪个是Java的关键字？",
                options = mapOf(
                    "A" to "class",
                    "B" to "String",
                    "C" to "println",
                    "D" to "System"
                ),
                correctAnswers = setOf("A"),
                explanation = "class 是Java语言的关键字，用于定义类。",
                level = com.github.lilulei.ruankao.model.DifficultyLevel.EASY,
                category = "Java基础知识",
                year = 2023,
                examType = ExamType.SOFTWARE_DESIGNER
            ),
            Question(
                id = "2",
                title = "在TCP/IP协议中，HTTP协议使用的默认端口号是？",
                options = mapOf(
                    "A" to "21",
                    "B" to "23",
                    "C" to "80",
                    "D" to "443"
                ),
                correctAnswers = setOf("C"),
                explanation = "HTTP协议使用的默认端口号是80，HTTPS使用的默认端口号是443。",
                level = com.github.lilulei.ruankao.model.DifficultyLevel.EASY,
                category = "计算机网络",
                year = 2023,
                examType = ExamType.NETWORK_ENGINEER
            ),
            Question(
                id = "3",
                title = "在关系数据库中，主键的作用是什么？",
                options = mapOf(
                    "A" to "加快查询速度",
                    "B" to "唯一标识表中的每一条记录",
                    "C" to "减少数据冗余",
                    "D" to "提高安全性"
                ),
                correctAnswers = setOf("B"),
                explanation = "主键的主要作用是唯一标识表中的每一条记录，确保数据的唯一性。",
                level = com.github.lilulei.ruankao.model.DifficultyLevel.MEDIUM,
                category = "数据库",
                year = 2023,
                examType = ExamType.DATABASE_ENGINEER
            )
        )
        
        _questions.addAll(sampleQuestions)
    }

    fun addCustomQuestion(question: Question) {
        _customQuestions.add(question)
    }

    fun removeCustomQuestion(id: String) {
        _customQuestions.removeIf { it.id == id }
    }

    fun getQuestionsByCategory(category: String): List<Question> {
        return allQuestions.filter { it.category == category }
    }

    fun getQuestionsByExamType(examType: ExamType): List<Question> {
        return allQuestions.filter { it.examType == examType }
    }

    fun getQuestionsByDifficulty(level: com.github.lilulei.ruankao.model.DifficultyLevel): List<Question> {
        return allQuestions.filter { it.level == level }
    }

    fun getRandomQuestions(count: Int, examType: ExamType? = null): List<Question> {
        val filteredQuestions = if (examType != null) {
            allQuestions.filter { it.examType == examType }
        } else {
            allQuestions
        }
        
        return if (filteredQuestions.size <= count) {
            filteredQuestions.shuffled()
        } else {
            filteredQuestions.shuffled().take(count)
        }
    }

    fun getQuestionById(id: String): Question? {
        return allQuestions.find { it.id == id }
    }
}