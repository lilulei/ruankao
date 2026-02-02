package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.DifficultyLevel
import com.github.lilulei.ruankao.model.ExamType
import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.KnowledgeChapterService
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.UserIdentityService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.DefaultComboBoxModel
import kotlin.jvm.java

/**
 * 添加试题对话框
 * 提供手动添加单个试题的界面
 */
class AddQuestionDialog(private val project: Project) : DialogWrapper(true) {
    private val questionService = project.getService(QuestionService::class.java)
    private val userIdentityService = project.getService(UserIdentityService::class.java)

    // UI组件
    private val titleField = JTextField(30).apply {
        toolTipText = "请输入题目内容"
    }
    private val levelLabel = JLabel().apply {
        // 如果用户已选择身份，则使用当前身份的级别作为默认值
        text = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel()
        } else {
            "软考高级"  // 默认考试级别
        }
        toolTipText = "当前考试级别"
    }
    private val examTypeLabel = JLabel().apply {
        text = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType().displayName
        } else {
            ExamType.PROJECT_MANAGER.displayName // 默认考试类型
        }
        toolTipText = "当前考试类型"
    }
    private val chapterComboBox = ComboBox<String>(emptyArray()).apply {
        renderer = object : ListCellRenderer<String> {
            override fun getListCellRendererComponent(
                list: JList<out String>?,
                value: String?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): JComponent {
                val label = JLabel(value ?: "")
                if (isSelected) {
                    label.background = list?.selectionBackground
                    label.foreground = list?.selectionForeground
                } else {
                    label.background = list?.background
                    label.foreground = list?.foreground
                }
                label.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                return label
            }
        }
        toolTipText = "请选择当前身份维护的知识点章节"
    }
    private val difficultyComboBox = ComboBox<DifficultyLevel>(DifficultyLevel.values()).apply {
        selectedItem = DifficultyLevel.MEDIUM
        renderer = object : ListCellRenderer<DifficultyLevel> {
            override fun getListCellRendererComponent(
                list: JList<out DifficultyLevel>?,
                value: DifficultyLevel?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): JComponent {
                val label = JLabel(value?.displayName ?: "")
                if (isSelected) {
                    label.background = list?.selectionBackground
                    label.foreground = list?.selectionForeground
                } else {
                    label.background = list?.background
                    label.foreground = list?.foreground
                }
                label.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                return label
            }
        }
        toolTipText = "选择题目难度"
    }
    private val explanationArea = JTextArea(5, 30).apply {
        text = "在此处输入题目解析..."
        toolTipText = "请输入题目解析，帮助理解题目"
    }
    private val optionsPanel = JPanel(GridBagLayout())
    private val optionsMap = mutableMapOf<JTextField, JTextField>() // 选项键值对

    init {
        title = "添加新试题"
        initializeComponents()
        init()
    }
    
    private fun initializeComponents() {
        // 初始化章节下拉框
        updateChapterComboBox()
    }
    
    private fun updateChapterComboBox() {
        val currentLevel = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel()
        } else {
            "软考高级"  // 默认级别
        }
        val currentExamType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType().displayName
        } else {
            "信息系统项目管理师"  // 默认考试类型
        }
        
        // 获取当前身份下的章节列表
        val chapterService = project.getService(KnowledgeChapterService::class.java)
        val chapters = chapterService.getChapterNamesByIdentity(currentLevel, currentExamType)
            .sorted()
        
        // 更新下拉框选项
        chapterComboBox.model = DefaultComboBoxModel(chapters.toTypedArray())
        
        // 设置默认选中项
        if (chapters.isNotEmpty()) {
            chapterComboBox.selectedItem = chapters.first()
        } else {
            // 如果没有章节，显示提示文本
            chapterComboBox.model = DefaultComboBoxModel(arrayOf("请先添加章节..."))
            chapterComboBox.isEnabled = false
        }
        
        // 更新工具提示
        chapterComboBox.toolTipText = if (userIdentityService.isIdentitySelected()) {
            "当前身份维护的章节（共${chapters.size}个）"
        } else {
            "请先选择考试身份以查看可用章节"
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
        }

        // 考试级别
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        val levelLabelStatic = JLabel("  考试级别:")
        val requiredLevelLabel = JLabel("*")
        requiredLevelLabel.foreground = java.awt.Color.RED

        val levelPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
        levelPanel.add(levelLabelStatic)
        levelPanel.add(requiredLevelLabel)
        levelPanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)

        panel.add(levelPanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        // 使用标签替代下拉框，显示当前考试级别
        panel.add(levelLabel, gbc)

        // 考试类型
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        val typeLabelStatic = JLabel("  考试类型:")
        val requiredTypeLabel = JLabel("*")
        requiredTypeLabel.foreground = java.awt.Color.RED

        val typePanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
        typePanel.add(typeLabelStatic)
        typePanel.add(requiredTypeLabel)
        typePanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)

        panel.add(typePanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        // 使用标签替代下拉框，显示当前考试类型
        panel.add(examTypeLabel, gbc)

        // 题目标题
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.WEST
        val titleLabel = JLabel("   题目标题:")
        val requiredTitleLabel = JLabel("*")
        requiredTitleLabel.foreground = java.awt.Color.RED

        val titlePanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
        titlePanel.add(titleLabel)
        titlePanel.add(requiredTitleLabel)
        titlePanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)

        panel.add(titlePanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(titleField, gbc)

        // 章节
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        val chapterLabel = JLabel("知识点章节:")
        val requiredChapterLabel = JLabel("*")
        requiredChapterLabel.foreground = java.awt.Color.RED
        
        val chapterPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
        chapterPanel.add(chapterLabel)
        chapterPanel.add(requiredChapterLabel)
        chapterPanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)
        
        panel.add(chapterPanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(chapterComboBox, gbc)



        // 难度等级
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.fill = GridBagConstraints.NONE
        val difficultyLabel = JLabel("          难度:")
        val requiredDifficultyLabel = JLabel("*")
        requiredDifficultyLabel.foreground = java.awt.Color.RED
        
        val difficultyPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
        difficultyPanel.add(difficultyLabel)
        difficultyPanel.add(requiredDifficultyLabel)
        difficultyPanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)
        
        panel.add(difficultyPanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.NONE
        panel.add(difficultyComboBox, gbc)

        // 选项输入区域
        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 1.0
        panel.add(createOptionsPanel(), gbc)

        // 解析输入区域
        gbc.gridx = 0
        gbc.gridy = 6
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weighty = 0.0
        val explanationLabel = JLabel("解析:")
        val requiredExplanationLabel = JLabel("*")
        requiredExplanationLabel.foreground = java.awt.Color.RED
        
        val explanationPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
        explanationPanel.add(explanationLabel)
        explanationPanel.add(requiredExplanationLabel)
        explanationPanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)
        
        panel.add(explanationPanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 1.0
        val scrollPane = JScrollPane(explanationArea)
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        panel.add(scrollPane, gbc)

        return panel
    }

    private fun createOptionsPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.border = BorderFactory.createTitledBorder("选项输入")
        val mainGbc = GridBagConstraints().apply {
            insets = Insets(2, 2, 2, 2)
        }

        val panel = JPanel(GridBagLayout())
        // 移除原有的复用gbc，改为在循环内创建独立约束

        // 添加选项输入行
        for (i in 0 until 6) { // 最多支持6个选项
            val keyField = JTextField(3).apply {
                text = ""
                toolTipText = when(i) {
                    0 -> "选项A标识，如：A"
                    1 -> "选项B标识，如：B"
                    2 -> "选项C标识，如：C"
                    3 -> "选项D标识，如：D"
                    4 -> "选项E标识，如：E"
                    5 -> "选项F标识，如：F"
                    else -> "选项标识，如：A、B、C、D"
                }
                // 添加占位符效果
                foreground = java.awt.Color.GRAY
                text = when(i) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    3 -> "D"
                    4 -> "E"
                    5 -> "F"
                    else -> ""
                }
                addFocusListener(object : java.awt.event.FocusAdapter() {
                    override fun focusGained(e: java.awt.event.FocusEvent?) {
                        if (text == when(i) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                3 -> "D"
                                4 -> "E"
                                5 -> "F"
                                else -> ""
                            }) {
                            text = ""
                            foreground = java.awt.Color.BLACK
                        }
                    }

                    override fun focusLost(e: java.awt.event.FocusEvent?) {
                        if (text.isEmpty()) {
                            text = when(i) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                3 -> "D"
                                4 -> "E"
                                5 -> "F"
                                else -> ""
                            }
                            foreground = java.awt.Color.GRAY
                        }
                    }
                })
            }
            val valueField = JTextField(20).apply {
                text = ""
                toolTipText = when(i) {
                    0 -> "选项A的具体内容"
                    1 -> "选项B的具体内容"
                    2 -> "选项C的具体内容"
                    3 -> "选项D的具体内容"
                    4 -> "选项E的具体内容"
                    5 -> "选项F的具体内容"
                    else -> "选项具体内容"
                }
                // 添加占位符效果
                foreground = java.awt.Color.GRAY
                text = when(i) {
                    0 -> "选项A内容"
                    1 -> "选项B内容"
                    2 -> "选项C内容"
                    3 -> "选项D内容"
                    4 -> "选项E内容"
                    5 -> "选项F内容"
                    else -> ""
                }
                addFocusListener(object : java.awt.event.FocusAdapter() {
                    override fun focusGained(e: java.awt.event.FocusEvent?) {
                        if (text == when(i) {
                                0 -> "选项A内容"
                                1 -> "选项B内容"
                                2 -> "选项C内容"
                                3 -> "选项D内容"
                                4 -> "选项E内容"
                                5 -> "选项F内容"
                                else -> ""
                            }) {
                            text = ""
                            foreground = java.awt.Color.BLACK
                        }
                    }

                    override fun focusLost(e: java.awt.event.FocusEvent?) {
                        if (text.isEmpty()) {
                            text = when(i) {
                                0 -> "选项A内容"
                                1 -> "选项B内容"
                                2 -> "选项C内容"
                                3 -> "选项D内容"
                                4 -> "选项E内容"
                                5 -> "选项F内容"
                                else -> ""
                            }
                            foreground = java.awt.Color.GRAY
                        }
                    }
                })
            }
            optionsMap[keyField] = valueField

            // ========== 关键修改1：为每个keyField创建独立的约束 ==========
            val keyGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = i
                insets = Insets(2, 2, 2, 2)
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.NONE // 明确禁止拉伸（核心）
                weightx = 0.0 // 无横向权重，避免被拉伸
            }
            panel.add(keyField, keyGbc)

            // ========== 关键修改2：为每个valueField创建独立的约束 ==========
            val valueGbc = GridBagConstraints().apply {
                gridx = 1
                gridy = i
                insets = Insets(2, 2, 2, 2)
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL // 仅valueField横向拉伸
                weightx = 1.0 // 仅valueField占用剩余横向空间
            }
            panel.add(valueField, valueGbc)
        }

        mainGbc.gridx = 0
        mainGbc.gridy = 0
        mainGbc.gridwidth = 1
        mainGbc.fill = GridBagConstraints.BOTH
        mainGbc.weightx = 1.0
        mainGbc.weighty = 1.0
        mainPanel.add(panel, mainGbc)

        // 添加说明标签
        val instructionLabel = JLabel("<html><font color='gray'>提示：在下方勾选正确答案</font></html>")
        mainGbc.gridx = 0
        mainGbc.gridy = 1
        mainGbc.fill = GridBagConstraints.HORIZONTAL
        mainGbc.weightx = 1.0
        mainGbc.weighty = 0.0
        mainPanel.add(instructionLabel, mainGbc)

        // 添加正确答案选择复选框
        mainGbc.gridx = 0
        mainGbc.gridy = 2
        mainGbc.fill = GridBagConstraints.HORIZONTAL
        mainGbc.weightx = 1.0
        mainGbc.weighty = 0.0
        mainPanel.add(createAnswerSelectionPanel(), mainGbc)

        return mainPanel
    }

    private fun createAnswerSelectionPanel(): JPanel {
        val panel = JPanel()
        panel.border = BorderFactory.createTitledBorder("选择正确答案*") // 添加必填星号

        // 动态创建复选框
        val buttonGroup = ButtonGroup()
        val answerCheckBoxes = mutableListOf<JCheckBox>()

        for (i in 0 until 6) {
            val keyField = optionsMap.keys.elementAtOrNull(i) ?: continue
            val keyText = if (keyField.text.trim().isEmpty()) {
                when (i) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    3 -> "D"
                    4 -> "E"
                    5 -> "F"
                    else -> ""
                }
            } else {
                keyField.text
            }
            val checkBox = JCheckBox(keyText)
            // 设置默认提示文本
            checkBox.toolTipText = "选择选项 \"$keyText\" 作为正确答案"
            checkBox.addActionListener {
                if (checkBox.isSelected) {
                    correctAnswers.add(keyText)
                } else {
                    correctAnswers.remove(keyText)
                }
            }
            answerCheckBoxes.add(checkBox)
            panel.add(checkBox)
        }

        return panel
    }

    private var correctAnswers = mutableSetOf<String>()

    override fun doOKAction() {
        val title = titleField.text.trim()
        val level = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel()
        } else {
            "软考高级"  // 默认考试级别
        }
        val examType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType()
        } else {
            ExamType.PROJECT_MANAGER // 默认考试类型
        }
        val difficulty = difficultyComboBox.selectedItem as DifficultyLevel
        val explanation = if (explanationArea.text == "在此处输入题目解析...") "" else explanationArea.text.trim()

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "题目标题不能为空",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        // 获取章节：从下拉框获取选中的章节
        val selectedChapter = chapterComboBox.selectedItem as? String
        val chapter = if (selectedChapter != null && selectedChapter != "请先添加章节...") {
            selectedChapter
        } else {
            ""
        }

        // 收集有效的选项
        val options = mutableMapOf<String, String>()

        optionsMap.entries.forEach { (keyField, valueField) ->
            val key = keyField.text.trim()
            val value = valueField.text.trim()
            
            if (key.isNotEmpty() && value.isNotEmpty()) {
                options[key] = value
            }
        }

        if (options.size < 2) {
            JOptionPane.showMessageDialog(
                this.window,
                "至少需要2个有效选项",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (correctAnswers.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "请选择正确答案",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (explanation.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "题目解析不能为空",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (chapter.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "知识点章节不能为空",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        // 创建并添加新题目
        val newQuestion = Question(
            id = generateQuestionId(),
            title = title,
            category = level, // 使用用户选择的级别作为分类
            examType = examType,
            level = difficulty,
            chapter = chapter, // 使用绑定的章节
            options = options,
            correctAnswers = correctAnswers,
            explanation = explanation
        )

        questionService.addQuestion(newQuestion)
        super.doOKAction()
    }

    private fun generateQuestionId(): String {
        return "custom_${System.currentTimeMillis()}"
    }

    fun getAddedQuestion(): Question? {
        val title = titleField.text.trim()
        if (title.isEmpty()) {
            return null
        }

        val explanation = if (explanationArea.text == "在此处输入题目解析...") "" else explanationArea.text.trim()
        if (explanation.isEmpty()) {
            return null
        }

        val selectedChapter = chapterComboBox.selectedItem as? String
        val chapter = if (selectedChapter != null && selectedChapter != "请先添加章节...") {
            selectedChapter
        } else {
            return null
        }

        val options = mutableMapOf<String, String>()
        optionsMap.entries.forEach { (keyField, valueField) ->
            val key = keyField.text.trim()
            val value = valueField.text.trim()
            
            if (key.isNotEmpty() && value.isNotEmpty()) {
                options[key] = value
            }
        }

        if (options.size < 2 || correctAnswers.isEmpty()) {
            return null
        }

        val level = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel()
        } else {
            "软考高级"  // 默认考试级别
        }
        val examType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType()
        } else {
            ExamType.PROJECT_MANAGER // 默认考试类型
        }
        val difficulty = difficultyComboBox.selectedItem as DifficultyLevel

        return Question(
            id = generateQuestionId(),
            title = title,
            category = level, // 使用用户选择的级别作为分类
            examType = examType,
            level = difficulty,
            chapter = if (chapter.isNotEmpty()) chapter else null, // 添加章节信息
            options = options,
            correctAnswers = correctAnswers,
            explanation = explanation
        )
    }
}
