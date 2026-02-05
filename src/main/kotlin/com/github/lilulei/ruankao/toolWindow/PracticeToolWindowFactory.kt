package com.github.lilulei.ruankao.toolWindow

import com.github.lilulei.ruankao.dialogs.ChapterManagementDialog
import com.github.lilulei.ruankao.dialogs.UserIdentityDialog
import com.github.lilulei.ruankao.model.*
import com.github.lilulei.ruankao.services.LearningStatisticsChangeListener
import com.github.lilulei.ruankao.services.LearningStatisticsService
import com.github.lilulei.ruankao.services.PracticeService
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.UserIdentityService
import com.github.lilulei.ruankao.services.KnowledgeChapterService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import java.util.concurrent.TimeUnit
import javax.swing.*

/**
 * 软考刷题练习工具窗口工厂类
 * 负责创建和管理练习工具窗口的内容
 */
class PracticeToolWindowFactory : ToolWindowFactory {

    /**
     * 创建工具窗口内容
     * @param project 当前项目实例
     * @param toolWindow 工具窗口实例
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = PracticeToolWindow(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    /**
     * 判断工具窗口是否应该可用
     * @param project 当前项目实例
     * @return 始终返回true，表示工具窗口始终可用
     */
    override fun shouldBeAvailable(project: Project) = true

    /**
     * 练习工具窗口类
     * 管理练习界面的主要功能和UI组件
     * @param toolWindow 工具窗口实例
     * @param project 当前项目实例
     */
    class PracticeToolWindow(toolWindow: ToolWindow, private val project: Project) {
        private val practiceService = project.getService(PracticeService::class.java)
        private val questionService = project.getService(QuestionService::class.java)
        private val statsService = project.getService(LearningStatisticsService::class.java)
        private val userIdentityService = project.getService(UserIdentityService::class.java)

        init {
            // 检查用户是否已选择身份，如果没有则提示选择
            ensureUserIdentitySelected()
        }

        /**
         * 确保用户已选择身份
         */
        private fun ensureUserIdentitySelected() {
            if (!userIdentityService.isIdentitySelected()) {
                val dialog = UserIdentityDialog()
                if (dialog.showAndGet()) {
                    val selectedExamType = dialog.getSelectedExamType()
                    if (selectedExamType != null) {
                        userIdentityService.setSelectedExamType(selectedExamType)
                    }
                }
            }
        }

        /**
         * 获取工具窗口的主要内容面板
         * @return 返回包含练习界面的面板组件
         */
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()

            // 标题
            val titleLabel = JLabel("软考刷题练习", SwingConstants.CENTER)
            titleLabel.font = titleLabel.font.deriveFont(18f)

            // 身份信息面板
            val identityPanel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("当前身份")
                
                val currentIdentityLabel = JLabel(getCurrentIdentityText(), SwingConstants.CENTER)
                currentIdentityLabel.foreground = Color.WHITE
                currentIdentityLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                
                // 添加点击事件，允许用户修改身份
                currentIdentityLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                        val dialog = UserIdentityDialog()
                        if (dialog.showAndGet()) {
                            val selectedExamType = dialog.getSelectedExamType()
                            if (selectedExamType != null) {
                                userIdentityService.setSelectedExamType(selectedExamType)
                                // 更新标签文本
                                currentIdentityLabel.text = getCurrentIdentityText()
                            }
                        }
                    }
                })
                
                add(currentIdentityLabel, BorderLayout.CENTER)
            }

            // 练习模式选择面板
            val modeSelectionPanel = JPanel(GridBagLayout()).apply {
                border = BorderFactory.createTitledBorder("练习模式")

                val gbc = GridBagConstraints().apply {
                    insets = Insets(5, 5, 5, 5)
                }

                val dailyPracticeBtn = JButton("每日一练")
                val specialTopicBtn = JButton("知识点章节练习")
                val mockExamBtn = JButton("模拟考试")
                val randomPracticeBtn = JButton("难度分级练习")

                add(dailyPracticeBtn, gbc.apply { gridx = 0; gridy = 0 })
                add(specialTopicBtn, gbc.apply { gridx = 1; gridy = 0 })
                add(mockExamBtn, gbc.apply { gridx = 2; gridy = 0 })
                add(randomPracticeBtn, gbc.apply { gridx = 3; gridy = 0 })

                // 每日一练按钮事件
                dailyPracticeBtn.addActionListener {
                    // 确保用户已选择身份
                    ensureUserIdentitySelected()
                    showDailyPracticeDialog()
                }

                // 专项练习按钮事件
                specialTopicBtn.addActionListener {
                    // 确保用户已选择身份
                    ensureUserIdentitySelected()
                    showSpecialTopicDialog()
                }

                // 模拟考试按钮事件
                mockExamBtn.addActionListener {
                    // 确保用户已选择身份
                    ensureUserIdentitySelected()
                    showMockExamDialog()
                }

                // 难度分级练习按钮事件
                randomPracticeBtn.addActionListener {
                    // 确保用户已选择身份
                    ensureUserIdentitySelected()
                    showRandomPracticeDialog()
                }
            }

            // 统计信息面板
            val statsLabel = JLabel()
            val statsPanel = JPanel(GridBagLayout()).apply {
                border = BorderFactory.createTitledBorder("学习统计")
                updateStatsLabel(statsLabel)
                add(statsLabel)
            }

            // 创建顶部工具栏面板（包含标题和设置按钮）
            val topToolbarPanel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createLineBorder(Color.GRAY, 1) // 添加边框以便可视化
                
                // 添加设置按钮到右上角
                val settingsButton = JButton("⚙️ 设置")
                settingsButton.preferredSize = Dimension(100, 35)
                settingsButton.toolTipText = "练习模式设置"
                settingsButton.font = settingsButton.font.deriveFont(Font.BOLD, 14f)
                settingsButton.background = Color(70, 130, 180) // Steel blue color
                settingsButton.foreground = Color.WHITE
                
                // 添加鼠标悬停效果
                settingsButton.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                        settingsButton.background = Color(100, 149, 237) // 更亮的蓝色
                    }
                    
                    override fun mouseExited(e: java.awt.event.MouseEvent?) {
                        settingsButton.background = Color(70, 130, 180) // 恢复原色
                    }
                })
                
                add(titleLabel, BorderLayout.CENTER)
                add(settingsButton, BorderLayout.EAST)
                
                // 设置按钮事件
                settingsButton.addActionListener {
                    showSettingsDialog()
                }
            }

            // 创建包含顶部工具栏和身份信息的整体顶部面板
            val headerPanel = JPanel(BorderLayout()).apply {
                add(topToolbarPanel, BorderLayout.NORTH)
                add(identityPanel, BorderLayout.SOUTH)
            }

            // 将组件添加到主面板
            add(headerPanel, BorderLayout.NORTH)
            add(modeSelectionPanel, BorderLayout.CENTER)
            add(statsPanel, BorderLayout.SOUTH)

            // 添加统计监听器
            val statsListener = object : LearningStatisticsChangeListener {
                override fun onStatisticsUpdated() {
                    SwingUtilities.invokeLater {
                        updateStatsLabel(statsLabel)
                    }
                }
            }
            statsService.addStatisticsListener(statsListener)

            // 在窗口关闭时移除监听器（概念上，实际实现可能需要其他方式）
            (this as? JBPanel<*>)?.putClientProperty("statsListener", statsListener)
        }

        /**
         * 获取当前身份文本
         */
        private fun getCurrentIdentityText(): String {
            return if (userIdentityService.isIdentitySelected()) {
                val examType = userIdentityService.getSelectedExamType()
                val level = userIdentityService.getSelectedLevel()
                "${level.displayName} - ${examType.displayName} (点击修改)"
            } else {
                "未选择身份 (点击设置)"
            }
        }
        
        /**
         * 显示设置对话框
         */
        private fun showSettingsDialog() {
            println("设置按钮被点击了！") // 调试输出
            val settingsDialog = SettingsDialog()
            settingsDialog.show()
        }
        
        /**
         * 显示每日练习对话框
         */
        private fun showDailyPracticeDialog() {
            val settingsDialog = SettingsDialog()
            val dailyCount = settingsDialog.getDailyPracticeQuestionsCount()
            val questions = questionService.getQuestionsByLevel(userIdentityService.getSelectedLevel().displayName).take(dailyCount)
            startPractice(PracticeType.DAILY_PRACTICE, questions)
        }

        /**
         * 更新统计标签显示的学习统计数据
         * @param label 要更新的JLabel组件
         */
        private fun updateStatsLabel(label: JLabel) {
            val stats = statsService.getOverallStatistics()
            val statsText = """
                总练习次数: ${stats.totalPractices}
                答题总数: ${stats.totalQuestions}
                正确数: ${stats.correctAnswers}
                正确率: ${if (stats.totalQuestions > 0) "%.2f".format(stats.correctAnswers.toDouble() / stats.totalQuestions * 100) else "0.00"}%
                连续学习天数: ${stats.dailyStreak}
            """.trimIndent()

            label.text = "<html>$statsText</html>"
        }

        /**
         * 开始练习会话
         * @param practiceType 练习类型枚举
         * @param questions 要练习的题目列表
         */
        private fun startPractice(practiceType: PracticeType, questions: List<Question>) {
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(null, "没有找到符合条件的题目，请先添加题目！", "提示", JOptionPane.INFORMATION_MESSAGE)
                return
            }

            val session = practiceService.startNewSession(practiceType, questions)
            showPracticeInterface(session, practiceType)
        }

        /**
         * 显示练习界面
         * @param session 练习会话对象
         * @param practiceType 练习类型，用于确定是否需要倒计时等特性
         */
        private fun showPracticeInterface(session: PracticeSession, practiceType: PracticeType = PracticeType.RANDOM_PRACTICE) {
            val frame = JFrame("答题界面")
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            frame.setSize(800, 600)
            frame.setLocationRelativeTo(null)

            val practicePanel = if (practiceType == PracticeType.MOCK_EXAM) {
                // 模拟考试模式，启用倒计时
                val settingsDialog = SettingsDialog()
                val examTime = settingsDialog.getMockExamTime()
                PracticeQuestionPanelWithTimer(project, session, examTime) {
                    frame.dispose()
                }
            } else {
                PracticeQuestionPanel(project, session) {
                    frame.dispose()
                }
            }

            frame.add(practicePanel)
            frame.isVisible = true
        }

        /**
         * 显示专项练习对话框，让用户选择练习专题
         */
        private fun showSpecialTopicDialog() {
            val questionService = project.getService(QuestionService::class.java)
            val chapterService = project.getService(KnowledgeChapterService::class.java)
            val userIdentityService = project.getService(UserIdentityService::class.java)
            
            // 从用户选择的级别获取对应的题目
            val selectedLevel = userIdentityService.getSelectedLevel()
            val selectedExamType = userIdentityService.getSelectedExamType().displayName
            val questionsForLevel = questionService.getQuestionsByLevel(selectedLevel.displayName)

            // 获取当前身份下的所有章节（用户自己添加的知识点章节）
            val chaptersForIdentity = chapterService.getChapterNamesByIdentity(selectedLevel.displayName, selectedExamType)

            if (chaptersForIdentity.isEmpty()) {
                JOptionPane.showMessageDialog(null, "当前身份暂无自定义章节，请先添加章节！", "提示", JOptionPane.INFORMATION_MESSAGE)
                val chapterManagementDialog = ChapterManagementDialog(project)
                chapterManagementDialog.show()
                return
            }

            val topicArray = chaptersForIdentity.toTypedArray()
            val options = arrayOf("选择知识点章节", "管理章节")
            val choice = JOptionPane.showOptionDialog(
                null,
                "请选择操作:",
                "知识点章节练习",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            )

            when (choice) {
                0 -> {
                    val selectedTopic = JOptionPane.showInputDialog(
                        null,
                        "请选择知识点章节:",
                        "知识点章节练习",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        topicArray,
                        topicArray.firstOrNull()
                    ) as? String

                    if (selectedTopic != null) {
                        // 获取该级别和章节下的题目，使用设置中的题目数量
                        val settingsDialog = SettingsDialog()
                        val questionsCount = settingsDialog.getSpecialTopicQuestionsCount()
                        val questions = questionService.getQuestionsByLevelAndChapter(selectedLevel.displayName, selectedTopic).take(questionsCount)
                        
                        startPractice(PracticeType.SPECIAL_TOPIC, questions)
                    }
                }
                1 -> {
                    // 打开章节管理对话框
                    val chapterManagementDialog = ChapterManagementDialog(project)
                    chapterManagementDialog.show()
                }
            }
        }

        /**
         * 显示模拟考试对话框
         * 直接使用当前用户身份的考试类型，无需用户选择
         */
        private fun showMockExamDialog() {
            // 直接使用当前用户身份的考试类型
            val currentExamType = userIdentityService.getSelectedExamType()
            
            // 获取设置中的参数
            val settingsDialog = SettingsDialog()
            val examTime = settingsDialog.getMockExamTime() // 考试时间（分钟）
            val examQuestionsCount = settingsDialog.getMockExamQuestionsCount() // 试题数量
            
            // 根据当前考试类型获取题目
            val questions = questionService.getQuestionsByExamType(currentExamType).take(examQuestionsCount)
            
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(null, "当前考试类型下没有题目，请先添加题目！", "提示", JOptionPane.INFORMATION_MESSAGE)
                return
            }
            
            // 开始模拟考试（带倒计时）
            startPractice(PracticeType.MOCK_EXAM, questions)
        }

        /**
         * 显示随机练习对话框，让用户选择难度级别
         */
        private fun showRandomPracticeDialog() {
            // 基于用户选择的级别获取题目
            val selectedLevel = userIdentityService.getSelectedLevel()
            val questionsForLevel = questionService.getQuestionsByLevel(selectedLevel.displayName)
            
            val options = arrayOf("简单", "中等", "困难", "所有难度")
            val selectedOption = JOptionPane.showInputDialog(
                null,
                "请选择难度:",
                "随机练习",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
            ) as? String

            if (selectedOption != null) {
                val difficulty = when (selectedOption) {
                    "简单" -> DifficultyLevel.EASY
                    "中等" -> DifficultyLevel.MEDIUM
                    "困难" -> DifficultyLevel.HARD
                    else -> null
                }

                val settingsDialog = SettingsDialog()
                val randomQuestionsCount = settingsDialog.getRandomPracticeQuestionsCount()

                val questions = if (difficulty != null) {
                    // 从当前级别中筛选特定难度的题目
                    questionsForLevel.filter { it.level == difficulty }.take(randomQuestionsCount)
                } else {
                    // 从当前级别中随机获取题目
                    questionsForLevel.shuffled().take(randomQuestionsCount)
                }

                startPractice(PracticeType.RANDOM_PRACTICE, questions)
            }
        }
    }
    
    /**
     * 设置对话框类
     * 管理各种练习模式的设置选项
     */
    class SettingsDialog : DialogWrapper(true) {
        private val dailyPracticeQuestionsField = JTextField("10")
        private val specialTopicQuestionsField = JTextField("10")  // 知识点章节练习题目数量
        private val mockExamTimeField = JTextField("120")
        private val mockExamQuestionsField = JTextField("75")
        private val randomPracticeQuestionsField = JTextField("15")

        init {
            title = "练习模式设置"
            super.init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints().apply {
                insets = Insets(5, 5, 5, 5)
                anchor = GridBagConstraints.WEST
            }

            // 每日练习设置
            gbc.gridx = 0
            gbc.gridy = 0
            panel.add(JLabel("每日练习题目数量:"), gbc)
            
            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            panel.add(dailyPracticeQuestionsField, gbc)

            // 知识点章节练习设置
            gbc.gridx = 0
            gbc.gridy = 1
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            panel.add(JLabel("知识点章节练习题目数量:"), gbc)
            
            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            panel.add(specialTopicQuestionsField, gbc)

            // 模拟考试设置
            gbc.gridx = 0
            gbc.gridy = 2
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            panel.add(JLabel("模拟考试时间(分钟):"), gbc)
            
            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            panel.add(mockExamTimeField, gbc)
            
            gbc.gridx = 0
            gbc.gridy = 3
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            panel.add(JLabel("模拟考试题目数量:"), gbc)
            
            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            panel.add(mockExamQuestionsField, gbc)

            // 随机练习设置
            gbc.gridx = 0
            gbc.gridy = 4
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            panel.add(JLabel("随机练习题目数量:"), gbc)
            
            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            panel.add(randomPracticeQuestionsField, gbc)

            return panel
        }
        
        fun getDailyPracticeQuestionsCount(): Int {
            return try {
                dailyPracticeQuestionsField.text.toInt()
            } catch (e: NumberFormatException) {
                10 // 默认值
            }
        }
        
        fun getSpecialTopicQuestionsCount(): Int {
            return try {
                specialTopicQuestionsField.text.toInt()
            } catch (e: NumberFormatException) {
                10 // 默认值
            }
        }
        
        fun getMockExamTime(): Int {
            return try {
                mockExamTimeField.text.toInt()
            } catch (e: NumberFormatException) {
                120 // 默认值
            }
        }
        
        fun getMockExamQuestionsCount(): Int {
            return try {
                mockExamQuestionsField.text.toInt()
            } catch (e: NumberFormatException) {
                75 // 默认值
            }
        }
        
        fun getRandomPracticeQuestionsCount(): Int {
            return try {
                randomPracticeQuestionsField.text.toInt()
            } catch (e: NumberFormatException) {
                15 // 默认值
            }
        }
    }

    /**
     * 练习题目面板类（带倒计时）
     * 负责显示单个练习题目的界面和处理用户交互，支持考试倒计时
     * @param project 当前项目实例
     * @param session 练习会话对象
     * @param examTime 考试时间（分钟）
     * @param onPracticeComplete 练习完成时的回调函数
     */
    class PracticeQuestionPanelWithTimer(
        private val project: Project,
        private val session: PracticeSession,
        private val examTime: Int, // 考试时间（分钟）
        private val onPracticeComplete: () -> Unit
    ) : JPanel() {
        private var currentQuestionIndex = 0
        private val questionService = project.getService(QuestionService::class.java)
        private val practiceService = project.getService(PracticeService::class.java)
        private val selectedOptions = mutableSetOf<String>()
        private var timer: javax.swing.Timer? = null
        private var remainingTime = examTime * 60 // 转换为秒

        init {
            layout = BorderLayout()
            startTimer()
            updateQuestionDisplay()
        }

        private fun startTimer() {
            val timeLabel = JLabel(formatTime(remainingTime), SwingConstants.CENTER)
            timeLabel.font = timeLabel.font.deriveFont(14f)
            timeLabel.foreground = Color.RED
            
            val timerPanel = JPanel(BorderLayout())
            timerPanel.border = BorderFactory.createTitledBorder("考试剩余时间")
            timerPanel.add(timeLabel, BorderLayout.CENTER)
            
            add(timerPanel, BorderLayout.NORTH)

            // 创建每秒更新一次的定时器
            timer = javax.swing.Timer(1000) { 
                remainingTime--
                timeLabel.text = formatTime(remainingTime)
                
                if (remainingTime <= 0) {
                    timer?.stop()
                    // 时间到了，自动提交考试
                    endExam()
                }
            }
            timer?.start()
        }

        private fun formatTime(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", mins, secs)
        }

        private fun endExam() {
            // 结束考试
            practiceService.endCurrentSession()
            JOptionPane.showMessageDialog(this, "考试时间到！", "考试结束", JOptionPane.INFORMATION_MESSAGE)
            onPracticeComplete()
        }

        /**
         * 更新当前题目显示
         * 如果已到达最后一题则显示结果，否则显示当前题目
         */
        private fun updateQuestionDisplay() {
            // 移除之前的内容（除了计时器面板）
            val components = this.components
            for (i in components.indices) {
                val component = components[i]
                if (component is JPanel) {
                    val border = component.border
                    if (border != null && border.toString().contains("考试剩余时间")) {
                        // 保留包含"考试剩余时间"的边框组件
                    } else {
                        remove(component)
                    }
                } else {
                    remove(component)
                }
            }

            if (currentQuestionIndex >= session.questions.size) {
                showResults()
                return
            }

            val question = session.questions[currentQuestionIndex]
            val questionPanel = createQuestionPanel(question)

            add(questionPanel, BorderLayout.CENTER)
            revalidate()
            repaint()
        }

        /**
         * 创建单个题目的显示面板
         * @param question 要显示的题目对象
         * @return 包含题目信息的JPanel组件
         */
        private fun createQuestionPanel(question: Question) : JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

            // 题目标题
            val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val titleLabel = JLabel("<html><b>${currentQuestionIndex + 1}. ${question.title}</b></html>")
            titleLabel.alignmentX = Component.LEFT_ALIGNMENT
            titlePanel.add(titleLabel)

            // 选项面板
            val optionsPanel = JPanel()
            optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)

            selectedOptions.clear()
            question.options.forEach { (key, value) ->
                val checkBox = JCheckBox("<html>$key. $value</html>")
                checkBox.addActionListener {
                    if (checkBox.isSelected) {
                        selectedOptions.add(key)
                    } else {
                        selectedOptions.remove(key)
                    }
                }
                optionsPanel.add(checkBox)
                optionsPanel.add(Box.createVerticalStrut(5)) // 添加间距
            }

            // 导航按钮
            val navPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            val prevButton = JButton("上一题")
            val nextButton = JButton("下一题")
            val submitButton = JButton("提交答案")

            prevButton.isEnabled = currentQuestionIndex > 0
            nextButton.isEnabled = currentQuestionIndex < session.questions.size - 1

            prevButton.addActionListener {
                if (currentQuestionIndex > 0) {
                    currentQuestionIndex--
                    updateQuestionDisplay()
                }
            }

            nextButton.addActionListener {
                if (currentQuestionIndex < session.questions.size - 1) {
                    currentQuestionIndex++
                    updateQuestionDisplay()
                }
            }

            submitButton.addActionListener {
                submitCurrentAnswer(question)
            }

            navPanel.add(prevButton)
            navPanel.add(nextButton)
            navPanel.add(submitButton)

            panel.add(titlePanel, BorderLayout.NORTH)
            panel.add(optionsPanel, BorderLayout.CENTER)
            panel.add(navPanel, BorderLayout.SOUTH)

            return panel
        }

        /**
         * 提交当前题目的答案并处理结果
         * @param question 当前题目对象
         */
        private fun submitCurrentAnswer(question: Question) {
            val isCorrect = selectedOptions == question.correctAnswers
            practiceService.submitAnswer(question.id, selectedOptions.toSet(), isCorrect)

            val message = if (isCorrect) "回答正确！" else "回答错误！正确答案是: ${question.correctAnswers.joinToString(", ")}"
            JOptionPane.showMessageDialog(this, message, "答题结果", if (isCorrect) JOptionPane.INFORMATION_MESSAGE else JOptionPane.WARNING_MESSAGE)

            // 即时更新错题本
            if (isCorrect) {
                practiceService.updateCorrectAnswer(question.id)
            } else {
                practiceService.updateWrongAnswer(question.id)
            }

            // 即时更新学习统计
            val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val statsService = project.getService(LearningStatisticsService::class.java)
                statsService.recordQuestionAnswer(question.id, isCorrect)
            }

            if (!isCorrect) {
                // 显示解释
                if (question.explanation.isNotEmpty()) {
                    JOptionPane.showMessageDialog(this, "<html><body><b>解析:</b><br>${question.explanation}</body></html>", "题目解析", JOptionPane.INFORMATION_MESSAGE)
                }
            }

            if (currentQuestionIndex < session.questions.size - 1) {
                currentQuestionIndex++
                updateQuestionDisplay()
            } else {
                // 练习完成
                endExam()
            }
        }

        /**
         * 显示练习结果统计信息
         */
        private fun showResults() {
            val stats = session.answers.values
            val correctCount = stats.count { it.isCorrect }
            val totalCount = stats.size
            val accuracy = if (totalCount > 0) (correctCount.toDouble() / totalCount * 100).toInt() else 0

            val resultMessage = """
                练习完成！
                答题总数: $totalCount
                正确数: $correctCount
                正确率: $accuracy%
            """.trimIndent()

            JOptionPane.showMessageDialog(this, resultMessage, "练习结果", JOptionPane.INFORMATION_MESSAGE)
            onPracticeComplete()
        }

        override fun removeNotify() {
            super.removeNotify()
            timer?.stop()
        }
    }

    /**
     * 练习题目面板类
     * 负责显示单个练习题目的界面和处理用户交互
     * @param project 当前项目实例
     * @param session 练习会话对象
     * @param onPracticeComplete 练习完成时的回调函数
     */
    class PracticeQuestionPanel(
        private val project: Project,
        private val session: PracticeSession,
        private val onPracticeComplete: () -> Unit
    ) : JPanel() {
        private var currentQuestionIndex = 0
        private val questionService = project.getService(QuestionService::class.java)
        private val practiceService = project.getService(PracticeService::class.java)
        private val selectedOptions = mutableSetOf<String>()

        init {
            layout = BorderLayout()
            updateQuestionDisplay()
        }

        /**
         * 更新当前题目显示
         * 如果已到达最后一题则显示结果，否则显示当前题目
         */
        private fun updateQuestionDisplay() {
            removeAll()

            if (currentQuestionIndex >= session.questions.size) {
                showResults()
                return
            }

            val question = session.questions[currentQuestionIndex]
            val questionPanel = createQuestionPanel(question)

            add(questionPanel, BorderLayout.CENTER)
            revalidate()
            repaint()
        }

        /**
         * 创建单个题目的显示面板
         * @param question 要显示的题目对象
         * @return 包含题目信息的JPanel组件
         */
        private fun createQuestionPanel(question: Question) : JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

            // 题目标题
            val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val titleLabel = JLabel("<html><b>${currentQuestionIndex + 1}. ${question.title}</b></html>")
            titleLabel.alignmentX = Component.LEFT_ALIGNMENT
            titlePanel.add(titleLabel)

            // 选项面板
            val optionsPanel = JPanel()
            optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)

            selectedOptions.clear()
            question.options.forEach { (key, value) ->
                val checkBox = JCheckBox("<html>$key. $value</html>")
                checkBox.addActionListener {
                    if (checkBox.isSelected) {
                        selectedOptions.add(key)
                    } else {
                        selectedOptions.remove(key)
                    }
                }
                optionsPanel.add(checkBox)
                optionsPanel.add(Box.createVerticalStrut(5)) // 添加间距
            }

            // 导航按钮
            val navPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            val prevButton = JButton("上一题")
            val nextButton = JButton("下一题")
            val submitButton = JButton("提交答案")

            prevButton.isEnabled = currentQuestionIndex > 0
            nextButton.isEnabled = currentQuestionIndex < session.questions.size - 1

            prevButton.addActionListener {
                if (currentQuestionIndex > 0) {
                    currentQuestionIndex--
                    updateQuestionDisplay()
                }
            }

            nextButton.addActionListener {
                if (currentQuestionIndex < session.questions.size - 1) {
                    currentQuestionIndex++
                    updateQuestionDisplay()
                }
            }

            submitButton.addActionListener {
                submitCurrentAnswer(question)
            }

            navPanel.add(prevButton)
            navPanel.add(nextButton)
            navPanel.add(submitButton)

            panel.add(titlePanel, BorderLayout.NORTH)
            panel.add(optionsPanel, BorderLayout.CENTER)
            panel.add(navPanel, BorderLayout.SOUTH)

            return panel
        }

        /**
         * 提交当前题目的答案并处理结果
         * @param question 当前题目对象
         */
        private fun submitCurrentAnswer(question: Question) {
            val isCorrect = selectedOptions == question.correctAnswers
            practiceService.submitAnswer(question.id, selectedOptions.toSet(), isCorrect)

            val message = if (isCorrect) "回答正确！" else "回答错误！正确答案是: ${question.correctAnswers.joinToString(", ")}"
            JOptionPane.showMessageDialog(this, message, "答题结果", if (isCorrect) JOptionPane.INFORMATION_MESSAGE else JOptionPane.WARNING_MESSAGE)

            // 即时更新错题本
            if (isCorrect) {
                practiceService.updateCorrectAnswer(question.id)
            } else {
                practiceService.updateWrongAnswer(question.id)
            }

            // 即时更新学习统计
            val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val statsService = project.getService(LearningStatisticsService::class.java)
                statsService.recordQuestionAnswer(question.id, isCorrect)
            }

            if (!isCorrect) {
                // 显示解释
                if (question.explanation.isNotEmpty()) {
                    JOptionPane.showMessageDialog(this, "<html><body><b>解析:</b><br>${question.explanation}</body></html>", "题目解析", JOptionPane.INFORMATION_MESSAGE)
                }
            }

            if (currentQuestionIndex < session.questions.size - 1) {
                currentQuestionIndex++
                updateQuestionDisplay()
            } else {
                // 练习完成
                practiceService.endCurrentSession()
                JOptionPane.showMessageDialog(this, "练习已完成！", "提示", JOptionPane.INFORMATION_MESSAGE)
                onPracticeComplete()
            }
        }

        /**
         * 显示练习结果统计信息
         */
        private fun showResults() {
            val stats = session.answers.values
            val correctCount = stats.count { it.isCorrect }
            val totalCount = stats.size
            val accuracy = if (totalCount > 0) (correctCount.toDouble() / totalCount * 100).toInt() else 0

            val resultMessage = """
                练习完成！
                答题总数: $totalCount
                正确数: $correctCount
                正确率: $accuracy%
            """.trimIndent()

            JOptionPane.showMessageDialog(this, resultMessage, "练习结果", JOptionPane.INFORMATION_MESSAGE)
            onPracticeComplete()
        }
    }
}