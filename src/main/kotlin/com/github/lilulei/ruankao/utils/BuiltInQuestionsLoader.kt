package com.github.lilulei.ruankao.utils

import com.github.lilulei.ruankao.model.*
import com.github.lilulei.ruankao.services.KnowledgeChapterService
import com.github.lilulei.ruankao.services.QuestionService
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.InputStreamReader

/**
 * 内置试题管理工具类
 * 负责加载和管理各个考试类型的内置试题模板
 */
class BuiltInQuestionsLoader {
    private val logger = logger<BuiltInQuestionsLoader>()
    private val gson = Gson()

    companion object {
        private val loadedExamTypes = mutableSetOf<ExamType>()
    }

    /**
     * 加载指定考试类型的内置试题
     * @param project 项目实例
     * @param examType 考试类型
     * @return 加载的试题数量
     */
    fun loadBuiltInQuestions(project: Project, examType: ExamType): Int {
        // 检查是否已经加载过该考试类型的试题
        if (examType in loadedExamTypes) {
            logger.info("考试类型 ${examType.displayName} 的内置试题已加载过，跳过本次加载")
            return 0
        }

        val questionService = project.getService(QuestionService::class.java)
        val knowledgeChapterService = project.getService(KnowledgeChapterService::class.java)
        val fileName = "${examType.name}.json"
        var loadedCount = 0

        try {
            // 从resources目录加载试题文件
            val resourcePath = "/built_in_questions/$fileName"
            logger.info("尝试加载内置试题文件: $resourcePath")

            val resourceStream = this::class.java.getResourceAsStream(resourcePath)

            if (resourceStream != null) {
                val jsonString = InputStreamReader(resourceStream, "UTF-8").readText()
                logger.debug("读取到JSON内容，长度: ${jsonString.length}")

                val questions = gson.fromJson(jsonString, Array<BuiltInQuestionTemplate>::class.java).toList()
                logger.info("解析得到 ${questions.size} 道试题模板")

                // 收集所有章节
                val chaptersToAdd = mutableSetOf<String>()

                questions.forEach { template ->
                    try {
                        // 检查试题是否已存在
                        if (!questionService.questionExists(template.id)) {
                            val question = convertToQuestion(template)
                            questionService.addQuestion(question)
                            loadedCount++
                            logger.info("成功加载内置试题: ${template.id} - ${template.title}")

                            // 收集章节信息
                            template.chapter?.let { chapter ->
                                if (chapter.isNotBlank()) {
                                    chaptersToAdd.add(chapter)
                                }
                            }
                        } else {
                            logger.info("内置试题已存在，跳过: ${template.id}")
                        }
                    } catch (e: Exception) {
                        logger.error("处理单个试题模板时出错: ${template.id}", e)
                    }
                }

                // 同步章节到KnowledgeChapterService
                if (chaptersToAdd.isNotEmpty()) {
                    val examLevel = ExamLevel.valueOf(questions.firstOrNull()?.examLevel ?: "SENIOR")
                    chaptersToAdd.forEach { chapterName ->
                        val chapterId = "${examType.name}_${chapterName}"
                        // 检查章节是否已存在
                        if (knowledgeChapterService.getChapterById(chapterId) == null) {
                            val chapter = KnowledgeChapter(
                                id = chapterId,
                                name = chapterName,
                                level = examLevel.displayName,
                                examType = examType.displayName
                            )
                            knowledgeChapterService.addChapter(chapter)
                            logger.info("同步添加内置章节: $chapterName for ${examType.displayName}")
                        }
                    }
                }

                // 标记该考试类型已加载
                loadedExamTypes.add(examType)
                logger.info("成功加载 ${examType.displayName} 的内置试题，共 $loadedCount 道")
            } else {
                logger.warn("未找到内置试题文件: $resourcePath")
            }
        } catch (e: Exception) {
            logger.error("加载内置试题失败: $fileName", e)
        }

        return loadedCount
    }

    /**
     * 将内置试题模板转换为Question对象
     */
    private fun convertToQuestion(template: BuiltInQuestionTemplate): Question {
        return Question(
            id = template.id,
            title = template.title,
            options = template.options,
            correctAnswers = template.correctAnswers,
            explanation = template.explanation,
            level = DifficultyLevel.valueOf(template.level),
            chapter = template.chapter,
            year = java.time.LocalDate.parse(template.year),
            examType = ExamType.valueOf(template.examType),
            examLevel = ExamLevel.valueOf(template.examLevel),
            questionType = QuestionType.BUILT_IN
        )
    }

    /**
     * 检查指定考试类型的内置试题是否已加载
     */
    fun isLoaded(examType: ExamType): Boolean {
        return examType in loadedExamTypes
    }

    /**
     * 重置加载状态（用于测试或特殊场景）
     */
    fun resetLoadStatus() {
        loadedExamTypes.clear()
        logger.info("内置试题加载状态已重置")
    }
}

/**
 * 内置试题模板数据类（用于JSON序列化）
 */
data class BuiltInQuestionTemplate(
    val id: String,
    val title: String,
    val options: Map<String, String>,
    val correctAnswers: Set<String>,
    val explanation: String,
    val level: String,
    val chapter: String? = null,
    val year: String,
    val examType: String,
    val examLevel: String,
    val questionType: String
)