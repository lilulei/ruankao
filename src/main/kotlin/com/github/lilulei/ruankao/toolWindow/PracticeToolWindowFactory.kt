package com.github.lilulei.ruankao.toolWindow

import com.github.lilulei.ruankao.model.*
import com.github.lilulei.ruankao.services.LearningStatisticsChangeListener
import com.github.lilulei.ruankao.services.LearningStatisticsService
import com.github.lilulei.ruankao.services.PracticeService
import com.github.lilulei.ruankao.services.QuestionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.*
import javax.swing.*

class PracticeToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = PracticeToolWindow(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class PracticeToolWindow(toolWindow: ToolWindow, private val project: Project) {
        private val practiceService = project.getService(PracticeService::class.java)
        private val questionService = project.getService(QuestionService::class.java)
        private val statsService = project.getService(LearningStatisticsService::class.java)
        
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            
            // 标题
            val titleLabel = JLabel("软考刷题练习", SwingConstants.CENTER)
            titleLabel.font = titleLabel.font.deriveFont(18f)
            
            // 练习模式选择面板
            val modeSelectionPanel = JPanel(GridBagLayout()).apply {
                border = BorderFactory.createTitledBorder("练习模式")
                
                val gbc = GridBagConstraints().apply {
                    insets = Insets(5, 5, 5, 5)
                }
                
                val dailyPracticeBtn = JButton("每日一练")
                val specialTopicBtn = JButton("专项练习")
                val mockExamBtn = JButton("模拟考试")
                val randomPracticeBtn = JButton("随机练习")
                
                add(dailyPracticeBtn, gbc.apply { gridx = 0; gridy = 0 })
                add(specialTopicBtn, gbc.apply { gridx = 1; gridy = 0 })
                add(mockExamBtn, gbc.apply { gridx = 2; gridy = 0 })
                add(randomPracticeBtn, gbc.apply { gridx = 3; gridy = 0 })
                
                // 每日一练按钮事件
                dailyPracticeBtn.addActionListener {
                    val questions = questionService.getRandomQuestions(10) // 每日10题
                    startPractice(PracticeType.DAILY_PRACTICE, questions)
                }
                
                // 专项练习按钮事件
                specialTopicBtn.addActionListener {
                    showSpecialTopicDialog()
                }
                
                // 模拟考试按钮事件
                mockExamBtn.addActionListener {
                    showMockExamDialog()
                }
                
                // 随机练习按钮事件
                randomPracticeBtn.addActionListener {
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
            
            // 将组件添加到主面板
            add(titleLabel, BorderLayout.NORTH)
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
        
        private fun startPractice(practiceType: PracticeType, questions: List<Question>) {
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(null, "没有找到符合条件的题目，请先添加题目！", "提示", JOptionPane.INFORMATION_MESSAGE)
                return
            }
            
            val session = practiceService.startNewSession(practiceType, questions)
            showPracticeInterface(session)
        }
        
        private fun showPracticeInterface(session: PracticeSession) {
            val frame = JFrame("答题界面")
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            frame.setSize(800, 600)
            frame.setLocationRelativeTo(null)
            
            val practicePanel = PracticeQuestionPanel(project, session) { 
                // 练习完成后回调
                frame.dispose()
                // 刷新主窗口统计
                // 注意：这里可能需要通过其他方式更新主窗口统计
            }
            
            frame.add(practicePanel)
            frame.isVisible = true
        }
        
        private fun showSpecialTopicDialog() {
            val categories = questionService.allQuestions.map { it.category }.distinct()
            if (categories.isEmpty()) {
                JOptionPane.showMessageDialog(null, "暂无题目可供练习！", "提示", JOptionPane.INFORMATION_MESSAGE)
                return
            }
            
            val categoryArray = categories.toTypedArray()
            val selectedCategory = JOptionPane.showInputDialog(
                null, 
                "请选择练习专题:", 
                "专项练习", 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                categoryArray, 
                categoryArray.firstOrNull()
            ) as? String
            
            if (selectedCategory != null) {
                val questions = questionService.getQuestionsByCategory(selectedCategory).take(10)
                startPractice(PracticeType.SPECIAL_TOPIC, questions)
            }
        }
        
        private fun showMockExamDialog() {
            val examTypes = ExamType.values()
            val examTypeArray = examTypes.map { it.name }.toTypedArray()
            val selectedExamType = JOptionPane.showInputDialog(
                null, 
                "请选择考试类型:", 
                "模拟考试", 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                examTypeArray, 
                examTypeArray.firstOrNull()
            ) as? String
            
            if (selectedExamType != null) {
                val examType = ExamType.valueOf(selectedExamType)
                val questions = questionService.getQuestionsByExamType(examType).take(50) // 模拟考试通常题目较多
                startPractice(PracticeType.MOCK_EXAM, questions)
            }
        }
        
        private fun showRandomPracticeDialog() {
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
                
                val questions = if (difficulty != null) {
                    questionService.getQuestionsByDifficulty(difficulty).take(15)
                } else {
                    questionService.getRandomQuestions(15)
                }
                
                startPractice(PracticeType.RANDOM_PRACTICE, questions)
            }
        }
    }
    
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
        
        private fun createQuestionPanel(question: Question): JPanel {
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