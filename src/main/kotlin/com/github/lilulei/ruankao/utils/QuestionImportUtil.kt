package com.github.lilulei.ruankao.utils

import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.QuestionService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.diagnostic.logger
import java.io.File
import java.io.IOException

/**
 * 试题导入工具类
 * 提供从JSON、XML等格式导入试题的功能
 */
class QuestionImportUtil {
    private val logger = logger<QuestionImportUtil>()

    /**
     * 从JSON文件导入试题
     * @param filePath JSON文件路径
     * @param questionService 试题服务实例
     * @return 成功导入的试题数量
     */
    fun importQuestionsFromJson(filePath: String, questionService: QuestionService): Int {
        return try {
            val jsonContent = File(filePath).readText()
            val gson = Gson()
            val listType = object : TypeToken<List<Question>>() {}.type
            val questions: List<Question> = gson.fromJson(jsonContent, listType)

            var importedCount = 0
            questions.forEach { question ->
                // 检查是否已存在相同ID的题目
                if (!questionService.questionExists(question.id)) {
                    questionService.addQuestion(question)
                    importedCount++
                }
            }

            logger.info("成功从JSON文件导入 $importedCount 遮题目")
            importedCount
        } catch (e: IOException) {
            logger.error("读取JSON文件失败: $filePath", e)
            0
        } catch (e: Exception) {
            logger.error("解析JSON文件失败: $filePath", e)
            0
        }
    }

    /**
     * 从XML文件导入试题
     * @param filePath XML文件路径
     * @param questionService 试题服务实例
     * @return 成功导入的试题数量
     */
    fun importQuestionsFromXml(filePath: String, questionService: QuestionService): Int {
        return try {
            val xmlContent = File(filePath).readText()
            val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(java.io.ByteArrayInputStream(xmlContent.toByteArray()))
            
            val nodeList = document.getElementsByTagName("question")
            var importedCount = 0

            for (i in 0 until nodeList.length) {
                val questionNode = nodeList.item(i)
                if (questionNode.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val element = questionNode as org.w3c.dom.Element
                    
                    val id = element.getAttribute("id")
                    val title = getElementText(element, "title")
                    val category = getElementText(element, "category")
                    val difficulty = getElementText(element, "difficulty")
                    val chapter = getElementText(element, "chapter") // 新增章节信息
                    
                    // 获取选项
                    val options = mutableMapOf<String, String>()
                    val optionsNode = element.getElementsByTagName("options").item(0) as? org.w3c.dom.Element
                    if (optionsNode != null) {
                        val optionNodes = optionsNode.getElementsByTagName("option")
                        for (j in 0 until optionNodes.length) {
                            val optionNode = optionNodes.item(j) as org.w3c.dom.Element
                            val key = optionNode.getAttribute("key")
                            val value = optionNode.textContent
                            options[key] = value
                        }
                    }
                    
                    // 获取正确答案
                    val correctAnswers = mutableSetOf<String>()
                    val answersNode = element.getElementsByTagName("correctAnswers").item(0) as? org.w3c.dom.Element
                    if (answersNode != null) {
                        val answerNodes = answersNode.getElementsByTagName("answer")
                        for (j in 0 until answerNodes.length) {
                            val answerNode = answerNodes.item(j) as org.w3c.dom.Element
                            correctAnswers.add(answerNode.textContent)
                        }
                    }
                    
                    val explanation = getElementText(element, "explanation")
                    
                    val question = Question(
                        id = id,
                        title = title,
                        category = category,
                        chapter = if (chapter.isNotEmpty()) chapter else null, // 添加章节信息
                        level = when (difficulty.lowercase()) {
                            "easy", "简单" -> com.github.lilulei.ruankao.model.DifficultyLevel.EASY
                            "hard", "困难" -> com.github.lilulei.ruankao.model.DifficultyLevel.HARD
                            "medium", "中等" -> com.github.lilulei.ruankao.model.DifficultyLevel.MEDIUM
                            else -> com.github.lilulei.ruankao.model.DifficultyLevel.MEDIUM
                        },
                        examType = try {
                            val examTypeText = element.getElementsByTagName("examType").item(0)?.textContent ?: "SOFTWARE_DESIGNER"
                            // 先尝试匹配中文显示名称
                            com.github.lilulei.ruankao.model.ExamType.values().find { it.displayName == examTypeText } ?: 
                            // 如果没找到，再尝试匹配枚举名称
                            when (examTypeText) {
                                "SOFTWARE_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.SOFTWARE_DESIGNER
                                "NETWORK_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.NETWORK_ENGINEER
                                "DATABASE_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.DATABASE_ENGINEER
                                "SYSTEM_ANALYST" -> com.github.lilulei.ruankao.model.ExamType.SYSTEM_ANALYST
                                "SYSTEM_ARCHITECT" -> com.github.lilulei.ruankao.model.ExamType.SYSTEM_ARCHITECT
                                "NETWORK_PLANNER" -> com.github.lilulei.ruankao.model.ExamType.NETWORK_PLANNER
                                "PROJECT_MANAGER" -> com.github.lilulei.ruankao.model.ExamType.PROJECT_MANAGER
                                "SYSTEM_PLANNING_MANAGER" -> com.github.lilulei.ruankao.model.ExamType.SYSTEM_PLANNING_MANAGER
                                "SYSTEM_INTEGRATION_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.SYSTEM_INTEGRATION_ENGINEER
                                "INFORMATION_SYSTEM_MANAGEMENT_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER
                                "SOFTWARE_TESTER" -> com.github.lilulei.ruankao.model.ExamType.SOFTWARE_TESTER
                                "MULTIMEDIA_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.MULTIMEDIA_DESIGNER
                                "INFORMATION_SYSTEM_SUPERVISOR" -> com.github.lilulei.ruankao.model.ExamType.INFORMATION_SYSTEM_SUPERVISOR
                                "E_COMMERCE_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.E_COMMERCE_DESIGNER
                                "INFORMATION_SECURITY_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.INFORMATION_SECURITY_ENGINEER
                                "EMBEDDED_SYSTEM_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.EMBEDDED_SYSTEM_DESIGNER
                                "SOFTWARE_PROCESS_EVALUATOR" -> com.github.lilulei.ruankao.model.ExamType.SOFTWARE_PROCESS_EVALUATOR
                                "COMPUTER_AIDED_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.COMPUTER_AIDED_DESIGNER
                                "COMPUTER_HARDWARE_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.COMPUTER_HARDWARE_ENGINEER
                                "INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER" -> com.github.lilulei.ruankao.model.ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER
                                "PROGRAMMER" -> com.github.lilulei.ruankao.model.ExamType.PROGRAMMER
                                "NETWORK_ADMINISTRATOR" -> com.github.lilulei.ruankao.model.ExamType.NETWORK_ADMINISTRATOR
                                "INFORMATION_PROCESSING_TECHNICIAN" -> com.github.lilulei.ruankao.model.ExamType.INFORMATION_PROCESSING_TECHNICIAN
                                "INFORMATION_SYSTEM_OPERATION_MANAGER" -> com.github.lilulei.ruankao.model.ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER
                                "MULTIMEDIA_APPLICATION_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.MULTIMEDIA_APPLICATION_DESIGNER
                                "E_COMMERCE_TECHNICIAN" -> com.github.lilulei.ruankao.model.ExamType.E_COMMERCE_TECHNICIAN
                                "WEB_DESIGNER" -> com.github.lilulei.ruankao.model.ExamType.WEB_DESIGNER
                                else -> com.github.lilulei.ruankao.model.ExamType.SOFTWARE_DESIGNER // 默认考试类型
                            }
                        } catch (e: Exception) {
                            com.github.lilulei.ruankao.model.ExamType.SOFTWARE_DESIGNER
                        },
                        options = options,
                        correctAnswers = correctAnswers,
                        explanation = explanation
                    )
                    
                    // 检查是否已存在相同ID的题目
                    if (!questionService.questionExists(question.id)) {
                        questionService.addQuestion(question)
                        importedCount++
                    }
                }
            }

            logger.info("成功从XML文件导入 $importedCount 遮题目")
            importedCount
        } catch (e: IOException) {
            logger.error("读取XML文件失败: $filePath", e)
            0
        } catch (e: Exception) {
            logger.error("解析XML文件失败: $filePath", e)
            0
        }
    }

    /**
     * 获取XML元素的文本内容
     */
    private fun getElementText(element: org.w3c.dom.Element, tagName: String): String {
        val nodeList = element.getElementsByTagName(tagName)
        return if (nodeList.length > 0) nodeList.item(0).textContent else ""
    }
}