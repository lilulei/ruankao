package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import org.jdom.Element
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

// 定义学习统计变更监听器接口
interface LearningStatisticsChangeListener {
    fun onStatisticsUpdated()
}

@State(name = "LearningStatisticsService", storages = [Storage("softexam_learning_statistics.xml")])
@Service(Service.Level.PROJECT)
class LearningStatisticsService : PersistentStateComponent<Element> {
    private val logger = logger<LearningStatisticsService>()
    
    private var statistics = LearningStatistics()
    private val dailyPracticeRecords = mutableMapOf<String, DailyPracticeRecord>() // key: date string in YYYY-MM-DD format
    private val listeners = mutableListOf<LearningStatisticsChangeListener>()
    
    // 添加监听器
    fun addStatisticsListener(listener: LearningStatisticsChangeListener) {
        listeners.add(listener)
    }

    // 移除监听器
    fun removeStatisticsListener(listener: LearningStatisticsChangeListener) {
        listeners.remove(listener)
    }

    // 触发监听器
    private fun notifyListeners() {
        listeners.forEach { it.onStatisticsUpdated() }
    }

    override fun getState(): Element {
        val element = Element("LearningStatisticsService")
        
        // 保存统计数据
        val statsElement = Element("statistics")
        statsElement.setAttribute("totalPractices", statistics.totalPractices.toString())
        statsElement.setAttribute("totalQuestions", statistics.totalQuestions.toString())
        statsElement.setAttribute("correctAnswers", statistics.correctAnswers.toString())
        statsElement.setAttribute("studyTimeMinutes", statistics.studyTimeMinutes.toString())
        statsElement.setAttribute("dailyStreak", statistics.dailyStreak.toString())
        if (statistics.lastStudyDate != null) {
            statsElement.setAttribute("lastStudyDate", statistics.lastStudyDate.toString())
        }
        
        // 保存类别统计
        val categoryStatsElement = Element("categoryStats")
        statistics.categoryStats.forEach { (categoryName, stat) ->
            val statElement = Element("stat")
            statElement.setAttribute("categoryName", categoryName)
            statElement.setAttribute("totalQuestions", stat.totalQuestions.toString())
            statElement.setAttribute("correctAnswers", stat.correctAnswers.toString())
            statElement.setAttribute("mastered", stat.mastered.toString())
            categoryStatsElement.addContent(statElement)
        }
        statsElement.addContent(categoryStatsElement)
        
        // 保存成就
        val achievementsElement = Element("achievements")
        statistics.achievements.forEach { achievement ->
            val achievementElement = Element("achievement")
            achievementElement.text = achievement
            achievementsElement.addContent(achievementElement)
        }
        statsElement.addContent(achievementsElement)
        
        element.addContent(statsElement)
        
        // 保存每日练习记录
        val dailyRecordsElement = Element("dailyRecords")
        dailyPracticeRecords.forEach { (date, record) ->
            val recordElement = Element("record")
            recordElement.setAttribute("date", date)
            recordElement.setAttribute("practices", record.practices.toString())
            recordElement.setAttribute("questions", record.questionsAnswered.toString())
            recordElement.setAttribute("correct", record.correctlyAnswered.toString())
            recordElement.setAttribute("timeSpent", record.timeSpentMinutes.toString())
            dailyRecordsElement.addContent(recordElement)
        }
        element.addContent(dailyRecordsElement)
        
        return element
    }

    override fun loadState(state: Element) {
        try {
            val statsElement = state.getChild("statistics")
            if (statsElement != null) {
                val totalPractices = statsElement.getAttributeValue("totalPractices")?.toIntOrNull() ?: 0
                val totalQuestions = statsElement.getAttributeValue("totalQuestions")?.toIntOrNull() ?: 0
                val correctAnswers = statsElement.getAttributeValue("correctAnswers")?.toIntOrNull() ?: 0
                val studyTimeMinutes = statsElement.getAttributeValue("studyTimeMinutes")?.toIntOrNull() ?: 0
                val dailyStreak = statsElement.getAttributeValue("dailyStreak")?.toIntOrNull() ?: 0
                val lastStudyDate = statsElement.getAttributeValue("lastStudyDate")?.toLongOrNull()
                
                val categoryStats = mutableMapOf<String, CategoryStat>()
                val categoryStatsElement = statsElement.getChild("categoryStats")
                categoryStatsElement?.getChildren("stat")?.forEach { statElement ->
                    val categoryName = statElement.getAttributeValue("categoryName")
                    val totalQs = statElement.getAttributeValue("totalQuestions")?.toIntOrNull() ?: 0
                    val correctQs = statElement.getAttributeValue("correctAnswers")?.toIntOrNull() ?: 0
                    val mastered = statElement.getAttributeValue("mastered")?.toBooleanStrictOrNull() ?: false
                    
                    categoryStats[categoryName] = CategoryStat(
                        categoryName = categoryName,
                        totalQuestions = totalQs,
                        correctAnswers = correctQs,
                        mastered = mastered
                    )
                }
                
                val achievements = mutableSetOf<String>()
                val achievementsElement = statsElement.getChild("achievements")
                achievementsElement?.getChildren("achievement")?.forEach { achievementElement ->
                    achievements.add(achievementElement.text)
                }
                
                statistics = LearningStatistics(
                    totalPractices = totalPractices,
                    totalQuestions = totalQuestions,
                    correctAnswers = correctAnswers,
                    studyTimeMinutes = studyTimeMinutes,
                    dailyStreak = dailyStreak,
                    lastStudyDate = lastStudyDate,
                    categoryStats = categoryStats,
                    achievements = achievements
                )
            }
            
            // 加载每日练习记录
            val dailyRecordsElement = state.getChild("dailyRecords")
            if (dailyRecordsElement != null) {
                dailyPracticeRecords.clear()
                dailyRecordsElement.getChildren("record").forEach { recordElement ->
                    val date = recordElement.getAttributeValue("date")
                    val practices = recordElement.getAttributeValue("practices")?.toIntOrNull() ?: 0
                    val questions = recordElement.getAttributeValue("questions")?.toIntOrNull() ?: 0
                    val correct = recordElement.getAttributeValue("correct")?.toIntOrNull() ?: 0
                    val timeSpent = recordElement.getAttributeValue("timeSpent")?.toIntOrNull() ?: 0
                    
                    dailyPracticeRecords[date] = DailyPracticeRecord(
                        practices = practices,
                        questionsAnswered = questions,
                        correctlyAnswered = correct,
                        timeSpentMinutes = timeSpent
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error loading learning statistics", e)
        }
    }

    fun recordPracticeSession(session: PracticeSession) {
        val today = getCurrentDateString()
        
        // 更新总统计数据
        statistics = statistics.copy(
            totalPractices = statistics.totalPractices + 1,
            totalQuestions = statistics.totalQuestions + session.answers.size,
            correctAnswers = statistics.correctAnswers + session.answers.count { it.value.isCorrect },
            studyTimeMinutes = statistics.studyTimeMinutes + ((session.endTime ?: System.currentTimeMillis()) - session.startTime).toInt() / (1000 * 60),
            lastStudyDate = System.currentTimeMillis()
        )
        
        // 更新或创建今日记录
        val todayRecord = dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord()).copy(
            practices = dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord()).practices + 1,
            questionsAnswered = dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord()).questionsAnswered + session.answers.size,
            correctlyAnswered = dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord()).correctlyAnswered + session.answers.count { it.value.isCorrect },
            timeSpentMinutes = dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord()).timeSpentMinutes + ((session.endTime ?: System.currentTimeMillis()) - session.startTime).toInt() / (1000 * 60)
        )
        dailyPracticeRecords[today] = todayRecord
        
        // 更新连续天数
        updateDailyStreak(today)
        
        // 更新分类统计
        session.answers.forEach { (questionId, answerRecord) ->
            // 获取问题信息以确定分类
            val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val questionService = project.getService(QuestionService::class.java)
                val question = questionService.getQuestionById(questionId)
                
                if (question != null) {
                    val category = question.category
                    val existingStat = statistics.categoryStats.getOrDefault(category, CategoryStat(category))
                    
                    statistics.categoryStats[category] = CategoryStat(
                        categoryName = category,
                        totalQuestions = existingStat.totalQuestions + 1,
                        correctAnswers = if (answerRecord.isCorrect) existingStat.correctAnswers + 1 else existingStat.correctAnswers,
                        mastered = (existingStat.correctAnswers + if (answerRecord.isCorrect) 1 else 0).toDouble() / (existingStat.totalQuestions + 1) >= 0.8
                    )
                }
            }
        }
        
        // 检查成就解锁
        checkAchievements()
        
        // 通知监听器
        notifyListeners()
    }
    
    // 新增方法：记录单个问题的回答（用于实时更新）
    fun recordQuestionAnswer(questionId: String, isCorrect: Boolean) {
        statistics = statistics.copy(
            totalQuestions = statistics.totalQuestions + 1,
            correctAnswers = if (isCorrect) statistics.correctAnswers + 1 else statistics.correctAnswers,
            lastStudyDate = System.currentTimeMillis()
        )
        
        // 通知监听器
        notifyListeners()
    }

    private fun updateDailyStreak(today: String) {
        val lastStudyDate = statistics.lastStudyDate
        if (lastStudyDate != null) {
            val lastDate = LocalDate.ofEpochDay(lastStudyDate / (24 * 60 * 60 * 1000))
            val currentDate = LocalDate.parse(today)
            val diffDays = java.time.temporal.ChronoUnit.DAYS.between(lastDate, currentDate)
            
            if (diffDays == 1L) {
                // 连续学习
                statistics = statistics.copy(dailyStreak = statistics.dailyStreak + 1)
            } else if (diffDays > 1L) {
                // 中断了，重置连续天数
                statistics = statistics.copy(dailyStreak = 1)
            } else if (diffDays == 0L) {
                // 同一天多次学习，保持不变
            } else {
                // 其他情况，如日期倒退，也重置
                statistics = statistics.copy(dailyStreak = 1)
            }
        } else {
            // 第一次学习
            statistics = statistics.copy(dailyStreak = 1)
        }
    }

    private fun getCurrentDateString(): String {
        return LocalDate.now().toString()
    }

    private fun checkAchievements() {
        val newAchievements = mutableSetOf<String>()
        
        // 检查连续打卡成就
        if (statistics.dailyStreak >= 7 && "连续7天学习" !in statistics.achievements) {
            newAchievements.add("连续7天学习")
        }
        if (statistics.dailyStreak >= 30 && "连续30天学习" !in statistics.achievements) {
            newAchievements.add("连续30天学习")
        }
        
        // 检查练习总数成就
        if (statistics.totalPractices >= 10 && "完成10次练习" !in statistics.achievements) {
            newAchievements.add("完成10次练习")
        }
        if (statistics.totalPractices >= 50 && "完成50次练习" !in statistics.achievements) {
            newAchievements.add("完成50次练习")
        }
        
        // 检查题目总数成就
        if (statistics.totalQuestions >= 100 && "完成100道题目" !in statistics.achievements) {
            newAchievements.add("完成100道题目")
        }
        if (statistics.totalQuestions >= 500 && "完成500道题目" !in statistics.achievements) {
            newAchievements.add("完成500道题目")
        }
        
        // 检查正确率成就
        val accuracyRate = if (statistics.totalQuestions > 0) {
            statistics.correctAnswers.toDouble() / statistics.totalQuestions
        } else 0.0
        
        if (accuracyRate >= 0.8 && "总体正确率超过80%" !in statistics.achievements) {
            newAchievements.add("总体正确率超过80%")
        }
        
        // 添加新成就
        statistics.achievements.addAll(newAchievements)
    }

    fun getOverallStatistics(): LearningStatistics {
        return statistics
    }

    fun getTodayStatistics(): DailyPracticeRecord {
        val today = getCurrentDateString()
        return dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord())
    }

    fun getCategoryStatistics(): Map<String, CategoryStat> {
        return statistics.categoryStats.toMap()
    }

    fun getAchievements(): Set<String> {
        return statistics.achievements.toSet()
    }

    fun getDailyRecords(): Map<String, DailyPracticeRecord> {
        return dailyPracticeRecords.toMap()
    }
}