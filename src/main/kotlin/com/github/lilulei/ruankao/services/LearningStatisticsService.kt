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

/**
 * 学习统计变更监听器接口
 */
interface LearningStatisticsChangeListener {
    /**
     * 当统计信息更新时调用此方法
     */
    fun onStatisticsUpdated()
}

/**
 * 学习统计服务类，负责管理用户的学习统计数据、每日练习记录和成就系统
 * 实现了持久化存储功能，数据保存在软考学习统计文件中
 *
 * @property logger 日志记录器
 * @property statistics 总体学习统计数据
 * @property dailyPracticeRecords 每日练习记录映射表，键为YYYY-MM-DD格式的日期字符串
 * @property listeners 统计信息变更监听器列表
 */
@State(name = "LearningStatisticsService", storages = [Storage("softexam_learning_statistics.xml")])
@Service(Service.Level.PROJECT)
class LearningStatisticsService(private val project: Project) : PersistentStateComponent<Element> {
    private val logger = logger<LearningStatisticsService>()

    private val userIdentityService = project.getService(UserIdentityService::class.java)
    private var statistics = LearningStatistics()
    private val dailyPracticeRecords = mutableMapOf<String, DailyPracticeRecord>() // key: date string in YYYY-MM-DD format
    private val listeners = mutableListOf<LearningStatisticsChangeListener>()
    
    init {
        logger.info("=== 初始化学习统计服务 ===")
        logger.info("注册身份变更监听器")
        // 注册身份变更监听器
        userIdentityService.addIdentityChangeListener(object : UserIdentityChangeListener {
            override fun onIdentityChanged(newLevel: ExamLevel, newExamType: ExamType) {
                logger.info("=== 学习统计服务收到身份变更通知 ===")
                logger.info("新身份: ${'$'}{newLevel.displayName} - ${'$'}{newExamType.displayName}")
                logger.info("切换前统计数据身份: ${'$'}{statistics.examLevel} - ${'$'}{statistics.examType}")
                switchToIdentity(newLevel.displayName, newExamType.displayName)
                logger.info("切换后统计数据身份: ${'$'}{statistics.examLevel} - ${'$'}{statistics.examType}")
                logger.info("=== 学习统计身份切换完成 ===")
            }
        })
        logger.info("学习统计服务初始化完成")
        logger.info("当前统计数据身份: ${'$'}{statistics.examLevel} - ${'$'}{statistics.examType}")
        logger.info("=========================")
    }

    /**
     * 添加学习统计变更监听器
     *
     * @param listener 要添加的监听器对象
     */
    fun addStatisticsListener(listener: LearningStatisticsChangeListener) {
        listeners.add(listener)
    }

    /**
     * 移除学习统计变更监听器
     *
     * @param listener 要移除的监听器对象
     */
    fun removeStatisticsListener(listener: LearningStatisticsChangeListener) {
        listeners.remove(listener)
    }

    /**
     * 通知所有注册的监听器统计信息已更新
     */
    private fun notifyListeners() {
        listeners.forEach { it.onStatisticsUpdated() }
    }

    /**
     * 获取当前状态元素，用于持久化存储学习统计数据
     *
     * @return 包含所有学习统计数据的XML元素
     */
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
        
        // 保存身份信息
        if (statistics.examLevel != null) {
            statsElement.setAttribute("examLevel", statistics.examLevel)
        }
        if (statistics.examType != null) {
            statsElement.setAttribute("examType", statistics.examType)
        }

        // 保存类别统计
        val categoryStatsElement = Element("categoryStats")
        statistics.categoryStats.forEach { (categoryName, stat) ->
            val statElement = Element("stat")
            statElement.setAttribute("categoryName", categoryName)
            statElement.setAttribute("totalQuestions", stat.totalQuestions.toString())
            statElement.setAttribute("correctAnswers", stat.correctAnswers.toString())
            statElement.setAttribute("mastered", stat.mastered.toString())
            if (stat.examLevel != null) {
                statElement.setAttribute("examLevel", stat.examLevel)
            }
            if (stat.examType != null) {
                statElement.setAttribute("examType", stat.examType)
            }
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

    /**
     * 加载持久化状态到当前实例
     *
     * @param state 要加载的状态元素
     */
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
                val examLevel = statsElement.getAttributeValue("examLevel")
                val examType = statsElement.getAttributeValue("examType")

                val categoryStats = mutableMapOf<String, CategoryStat>()
                val categoryStatsElement = statsElement.getChild("categoryStats")
                categoryStatsElement?.getChildren("stat")?.forEach { statElement ->
                    val categoryName = statElement.getAttributeValue("categoryName")
                    val totalQs = statElement.getAttributeValue("totalQuestions")?.toIntOrNull() ?: 0
                    val correctQs = statElement.getAttributeValue("correctAnswers")?.toIntOrNull() ?: 0
                    val mastered = statElement.getAttributeValue("mastered")?.toBooleanStrictOrNull() ?: false
                    val statExamLevel = statElement.getAttributeValue("examLevel")
                    val statExamType = statElement.getAttributeValue("examType")

                    categoryStats[categoryName] = CategoryStat(
                        categoryName = categoryName,
                        totalQuestions = totalQs,
                        correctAnswers = correctQs,
                        mastered = mastered,
                        examLevel = statExamLevel,
                        examType = statExamType
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
                    achievements = achievements,
                    examLevel = examLevel,
                    examType = examType
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

    /**
     * 记录一次练习会话的统计数据
     *
     * @param session 要记录的练习会话对象
     */
    fun recordPracticeSession(session: PracticeSession) {
        val today = getCurrentDateString()
        
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

        // 更新总统计数据
        statistics = statistics.copy(
            totalPractices = statistics.totalPractices + 1,
            totalQuestions = statistics.totalQuestions + session.answers.size,
            correctAnswers = statistics.correctAnswers + session.answers.count { it.value.isCorrect },
            studyTimeMinutes = statistics.studyTimeMinutes + ((session.endTime ?: System.currentTimeMillis()) - session.startTime).toInt() / (1000 * 60),
            lastStudyDate = System.currentTimeMillis(),
            examLevel = currentLevel,
            examType = currentExamType
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

                // 分类统计功能已移除
            }
        }

        // 检查成就解锁
        checkAchievements()

        // 通知监听器
        notifyListeners()
    }

    /**
     * 记录单个问题的回答结果（用于实时更新）
     *
     * @param questionId 问题ID
     * @param isCorrect 回答是否正确
     */
    fun recordQuestionAnswer(questionId: String, isCorrect: Boolean) {
        statistics = statistics.copy(
            totalQuestions = statistics.totalQuestions + 1,
            correctAnswers = if (isCorrect) statistics.correctAnswers + 1 else statistics.correctAnswers,
            lastStudyDate = System.currentTimeMillis()
        )

        // 通知监听器
        notifyListeners()
    }

    /**
     * 更新连续学习天数统计
     *
     * @param today 当前日期字符串（YYYY-MM-DD格式）
     */
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

    /**
     * 获取当前日期字符串
     *
     * @return 格式为YYYY-MM-DD的当前日期字符串
     */
    private fun getCurrentDateString(): String {
        return LocalDate.now().toString()
    }

    /**
     * 检查并解锁新的成就
     */
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

    /**
     * 获取总体学习统计数据
     *
     * @return LearningStatistics对象，包含所有总体学习统计数据
     */
    fun getOverallStatistics(): LearningStatistics {
        return statistics
    }

    /**
     * 获取今天的练习统计数据
     *
     * @return DailyPracticeRecord对象，包含今天的练习统计数据
     */
    fun getTodayStatistics(): DailyPracticeRecord {
        val today = getCurrentDateString()
        return dailyPracticeRecords.getOrDefault(today, DailyPracticeRecord())
    }



    /**
     * 获取已获得的成就列表
     *
     * @return Set<String>，包含所有已获得的成就名称
     */
    fun getAchievements(): Set<String> {
        return statistics.achievements.toSet()
    }

    /**
     * 获取所有每日练习记录
     *
     * @return Map<String, DailyPracticeRecord>，键为日期字符串，值为对应的每日练习记录
     */
    fun getDailyRecords(): Map<String, DailyPracticeRecord> {
        return dailyPracticeRecords.toMap()
    }

    /**
     * 清空所有学习统计数据
     * 重置为初始状态
     */
    fun clearAllData() {
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

        statistics = LearningStatistics(
            examLevel = currentLevel,
            examType = currentExamType
        )
        dailyPracticeRecords.clear()
        
        // 通知监听器
        notifyListeners()
        
        logger.info("学习统计数据已清空")
    }

    /**
     * 切换到指定身份的学习统计数据
     * 
     * @param examLevel 考试级别
     * @param examType 考试类型
     */
    fun switchToIdentity(examLevel: String, examType: String) {
        // 更新当前统计数据的身份信息
        statistics = statistics.copy(
            examLevel = examLevel,
            examType = examType
        )
        
        // 通知监听器
        notifyListeners()
        
        logger.info("学习统计已切换到身份: $examLevel - $examType")
    }

    /**
     * 获取当前身份标识符
     * 用于区分不同身份的数据
     * 
     * @return 身份标识符字符串，格式为 "examLevel_examType"
     */
    fun getCurrentIdentityKey(): String {
        val examLevel = statistics.examLevel ?: "unknown_examLevel"
        val examType = statistics.examType ?: "unknown_examType"
        return "${'$'}{examLevel}_${'$'}{examType}"
    }

    /**
     * 获取当前身份下的学习统计数据
     * 
     * @return 当前身份的学习统计数据
     */
    fun getStatisticsForCurrentIdentity(): LearningStatistics {
        logger.info("=== 获取当前身份学习统计数据 ===")
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
        logger.info("统计数据身份: ${statistics.examLevel} - ${statistics.examType}")
        
        // 如果当前统计数据的身份不匹配，则返回空统计数据
        if (statistics.examLevel != currentLevel || statistics.examType != currentExamType) {
            logger.info("身份不匹配，返回空统计数据")
            val emptyStats = LearningStatistics(
                examLevel = currentLevel,
                examType = currentExamType
            )
            logger.info("=== 学习统计数据获取完成 ===")
            return emptyStats
        }
        
        logger.info("身份匹配，返回现有统计数据")
        logger.info("=== 学习统计数据获取完成 ===")
        return statistics
    }
}

