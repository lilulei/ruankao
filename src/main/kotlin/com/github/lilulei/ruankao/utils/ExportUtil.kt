package com.github.lilulei.ruankao.utils

import com.github.lilulei.ruankao.model.PracticeSession
import com.github.lilulei.ruankao.services.LearningStatisticsService
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.WrongQuestionService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导出工具类
 * 提供练习记录导出为CSV的功能
 */
class ExportUtil {
    private val logger = logger<ExportUtil>()

    /**
     * 导出练习记录为CSV格式
     * @param project 当前项目实例
     * @param exportPath 导出文件路径
     * @return 是否导出成功
     */
    fun exportPracticeRecordsToCsv(project: Project, exportPath: String): Boolean {
        return try {
            val statisticsService = project.getService(LearningStatisticsService::class.java)
            val questionService = project.getService(QuestionService::class.java)
            val wrongQuestionService = project.getService(WrongQuestionService::class.java)

            val writer = File(exportPath).printWriter()

            // 写入CSV头部
            writer.println("统计项,数值")

            // 导出总体统计数据
            val overallStats = statisticsService.getOverallStatistics()
            writer.println("总练习次数,${overallStats.totalPractices}")
            writer.println("答题总数,${overallStats.totalQuestions}")
            writer.println("正确数,${overallStats.correctAnswers}")
            writer.println("正确率,${if (overallStats.totalQuestions > 0) "${String.format("%.2f", overallStats.correctAnswers.toDouble() / overallStats.totalQuestions * 100)}%" else "0%"}")
            writer.println("学习时长(分钟),${overallStats.studyTimeMinutes}")
            writer.println("连续学习天数,${overallStats.dailyStreak}")

            // 导出分类统计
            writer.println("")
            writer.println("分类统计:")
            writer.println("分类,总题数,答对题数,掌握状态")
            val categoryStats = statisticsService.getCategoryStatistics()
            categoryStats.forEach { (category, stat) ->
                writer.println("$category,${stat.totalQuestions},${stat.correctAnswers},${if (stat.mastered) "已掌握" else "未掌握"}")
            }

            // 导出错题本数据
            writer.println("")
            writer.println("错题本数据:")
            writer.println("题目ID,错误次数,最后错误时间,掌握状态,连续正确次数")
            val wrongQuestions = wrongQuestionService.allWrongQuestions
            wrongQuestions.forEach { (_, info) ->
                val lastErrorTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(info.lastErrorTime))
                writer.println("${info.questionId},${info.errorCount},$lastErrorTime,${if (info.mastered) "已掌握" else "未掌握"},${info.consecutiveCorrectCount}")
            }

            writer.close()
            logger.info("练习记录已导出到: $exportPath")
            true
        } catch (e: Exception) {
            logger.error("导出练习记录失败", e)
            false
        }
    }

    /**
     * 导出错题本为CSV格式
     * @param project 当前项目实例
     * @param exportPath 导出文件路径
     * @return 是否导出成功
     */
    fun exportWrongQuestionsToCsv(project: Project, exportPath: String): Boolean {
        return try {
            val wrongQuestionService = project.getService(WrongQuestionService::class.java)

            val writer = File(exportPath).printWriter()

            // 写入CSV头部
            writer.println("题目ID,错误次数,最后错误时间,掌握状态,连续正确次数")

            // 导出错题本数据
            val wrongQuestions = wrongQuestionService.allWrongQuestions
            wrongQuestions.forEach { (_, info) ->
                val lastErrorTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(info.lastErrorTime))
                writer.println("${info.questionId},${info.errorCount},$lastErrorTime,${if (info.mastered) "已掌握" else "未掌握"},${info.consecutiveCorrectCount}")
            }

            writer.close()
            logger.info("错题本已导出到: $exportPath")
            true
        } catch (e: Exception) {
            logger.error("导出错题本失败", e)
            false
        }
    }

    /**
     * 导出学习统计为CSV格式
     * @param project 当前项目实例
     * @param exportPath 导出文件路径
     * @return 是否导出成功
     */
    fun exportLearningStatisticsToCsv(project: Project, exportPath: String): Boolean {
        return try {
            val statisticsService = project.getService(LearningStatisticsService::class.java)

            val writer = File(exportPath).printWriter()

            // 写入CSV头部
            writer.println("统计项,数值")

            // 导出总体统计数据
            val overallStats = statisticsService.getOverallStatistics()
            writer.println("总练习次数,${overallStats.totalPractices}")
            writer.println("答题总数,${overallStats.totalQuestions}")
            writer.println("正确数,${overallStats.correctAnswers}")
            writer.println("正确率,${if (overallStats.totalQuestions > 0) "${String.format("%.2f", overallStats.correctAnswers.toDouble() / overallStats.totalQuestions * 100)}%" else "0%"}")
            writer.println("学习时长(分钟),${overallStats.studyTimeMinutes}")
            writer.println("连续学习天数,${overallStats.dailyStreak}")

            // 导出分类统计
            writer.println("")
            writer.println("分类统计:")
            writer.println("分类,总题数,答对题数,掌握状态")
            val categoryStats = statisticsService.getCategoryStatistics()
            categoryStats.forEach { (category, stat) ->
                writer.println("$category,${stat.totalQuestions},${stat.correctAnswers},${if (stat.mastered) "已掌握" else "未掌握"}")
            }

            // 导出成就
            writer.println("")
            writer.println("获得成就:")
            val achievements = statisticsService.getAchievements()
            achievements.forEach { achievement ->
                writer.println(achievement)
            }

            writer.close()
            logger.info("学习统计已导出到: $exportPath")
            true
        } catch (e: Exception) {
            logger.error("导出学习统计失败", e)
            false
        }
    }
}