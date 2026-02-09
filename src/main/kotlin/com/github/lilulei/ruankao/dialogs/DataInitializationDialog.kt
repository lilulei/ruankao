package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.services.LearningStatisticsService
import com.github.lilulei.ruankao.services.WrongQuestionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

/**
 * 数据初始化对话框
 * 用于清空学习统计和错题本数据
 */
class DataInitializationDialog(private val project: Project) : DialogWrapper(true) {
    
    private val learningStatisticsService = project.getService(LearningStatisticsService::class.java)
    private val wrongQuestionService = project.getService(WrongQuestionService::class.java)
    
    private lateinit var learningStatsCheckBox: JCheckBox
    private lateinit var wrongQuestionsCheckBox: JCheckBox
    private lateinit var warningLabel: JLabel

    init {
        title = "数据初始化"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        }

        // 标题
        val titleLabel = JLabel("选择要初始化的数据").apply {
            font = font.deriveFont(16f)
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
        panel.add(titleLabel)
        panel.add(Box.createVerticalStrut(15))

        // 警告信息
        warningLabel = JLabel("<html><font color='red'>警告：此操作不可撤销，将永久删除所选数据！</font></html>").apply {
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
        panel.add(warningLabel)
        panel.add(Box.createVerticalStrut(15))

        // 学习统计数据选项
        learningStatsCheckBox = JCheckBox("清空学习统计数据（练习记录、成就等）").apply {
            isSelected = true
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
        panel.add(learningStatsCheckBox)
        panel.add(Box.createVerticalStrut(10))

        // 错题本数据选项
        wrongQuestionsCheckBox = JCheckBox("清空错题本数据（错题记录、掌握状态等）").apply {
            isSelected = true
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
        panel.add(wrongQuestionsCheckBox)
        panel.add(Box.createVerticalStrut(20))

        // 当前身份信息
        val currentIdentity = getCurrentIdentityText()
        val identityLabel = JLabel("<html>当前身份：<b>$currentIdentity</b><br>初始化操作仅影响当前身份的数据</html>").apply {
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
        panel.add(identityLabel)

        return panel
    }

    override fun createActions(): Array<Action> {
        val initializeAction = object : DialogWrapperAction("初始化") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                performInitialization()
            }
        }
        
        return arrayOf(initializeAction, cancelAction)
    }

    /**
     * 执行数据初始化操作
     */
    private fun performInitialization() {
        val selectedOptions = mutableListOf<String>()
        
        if (learningStatsCheckBox.isSelected) {
            selectedOptions.add("学习统计数据")
        }
        
        if (wrongQuestionsCheckBox.isSelected) {
            selectedOptions.add("错题本数据")
        }
        
        if (selectedOptions.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "请至少选择一项要初始化的数据",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        // 确认对话框
        val confirmMessage = """
            确定要初始化以下数据吗？
            
            ${selectedOptions.joinToString("\n")}
            
            此操作不可撤销，请谨慎操作！
        """.trimIndent()

        val result = JOptionPane.showConfirmDialog(
            contentPanel,
            confirmMessage,
            "确认初始化",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (result == JOptionPane.YES_OPTION) {
            // 执行初始化
            if (learningStatsCheckBox.isSelected) {
                learningStatisticsService.clearAllData()
            }
            
            if (wrongQuestionsCheckBox.isSelected) {
                wrongQuestionService.clearAllData()
            }

            // 显示成功消息
            JOptionPane.showMessageDialog(
                contentPanel,
                "数据初始化完成！\n已清空：${selectedOptions.joinToString("、")}",
                "初始化成功",
                JOptionPane.INFORMATION_MESSAGE
            )
            
            close(OK_EXIT_CODE)
        }
    }

    /**
     * 获取当前身份文本
     */
    private fun getCurrentIdentityText(): String {
        val userIdentityService = project.getService(com.github.lilulei.ruankao.services.UserIdentityService::class.java)
        return if (userIdentityService.isIdentitySelected()) {
            val examType = userIdentityService.getSelectedExamType()
            val level = userIdentityService.getSelectedExamLevel()
            "${level.displayName} - ${examType.displayName}"
        } else {
            "未选择身份"
        }
    }
}