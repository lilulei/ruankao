package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.DifficultyLevel
import com.github.lilulei.ruankao.model.ExamLevel
import com.github.lilulei.ruankao.model.ExamType
import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.KnowledgeChapterService
import com.github.lilulei.ruankao.services.UserIdentityService
import com.intellij.openapi.project.Project
import java.awt.*
import java.time.LocalDate
import javax.swing.*
import javax.swing.DefaultComboBoxModel

/**
 * 试题表单面板
 * 提供可复用的试题输入界面，支持添加、编辑模式
 */
class QuestionFormPanel(
    private val project: Project,
    private val mode: FormMode = FormMode.ADD,
    private val existingQuestion: Question? = null
) {
    private val userIdentityService = project.getService(UserIdentityService::class.java)
    
    // UI组件
    val titleField = JTextField(30).apply {
        toolTipText = "请输入题目内容"
        foreground = Color.GRAY
        text = "请输入题目内容..."
        addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent?) {
                if (text == "请输入题目内容...") {
                    text = ""
                    foreground = Color.BLACK
                }
            }

            override fun focusLost(e: java.awt.event.FocusEvent?) {
                if (text.isEmpty()) {
                    text = "请输入题目内容..."
                    foreground = Color.GRAY
                }
            }
        })
    }

    // 当前身份显示组件
    private lateinit var identityDisplayField: JTextField
    private lateinit var identityDropDownButton: JButton
    
    // 存储当前选中的身份信息 - 始终从全局服务获取
    private var currentExamType: ExamType = userIdentityService.getSelectedExamType()
    private var currentExamLevel: String = userIdentityService.getSelectedExamLevel().displayName
    
    // 身份选择面板
    private val identityPanel = JPanel(BorderLayout(5, 0)).apply {
        border = BorderFactory.createTitledBorder("当前身份")
    }

    val chapterComboBox = JComboBox<String>(emptyArray()).apply {
        renderer = createListCellRenderer()
        toolTipText = "请选择当前身份维护的知识点章节"
    }

    val levelComboBox = JComboBox<String>(arrayOf("简单", "中等", "困难")).apply {
        selectedItem = "中等"
        toolTipText = "选择题目难度"
    }
    
    val explanationArea = JTextArea(5, 30).apply {
        toolTipText = "请输入题目解析，帮助理解题目"
        foreground = Color.GRAY
        text = "在此处输入题目解析..."
        addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent?) {
                if (text == "在此处输入题目解析...") {
                    text = ""
                    foreground = Color.BLACK
                }
            }

            override fun focusLost(e: java.awt.event.FocusEvent?) {
                if (text.isEmpty()) {
                    text = "在此处输入题目解析..."
                    foreground = Color.GRAY
                }
            }
        })
    }
    
    val dateSpinner = JSpinner(SpinnerDateModel()).apply {
        editor = JSpinner.DateEditor(this, "yyyy-MM-dd")
        toolTipText = "选择考试年份和日期"
    }
    
    val optionsPanel = JPanel(GridBagLayout())
    val optionsMap = mutableMapOf<JTextField, JTextField>()
    val correctAnswers = mutableSetOf<String>()
    
    private val answerCheckBoxes = mutableListOf<JCheckBox>()
    
    init {
        initializeComponents()
        if (mode == FormMode.EDIT && existingQuestion != null) {
            populateFormData(existingQuestion)
        } else {
            // 添加模式的默认值
            explanationArea.text = "在此处输入题目解析..."
            dateSpinner.value = java.util.Date()
        }
    }
    
    private fun initializeComponents() {
        setupIdentityPanel()
        updateChapterComboBox()
        // 添加身份变更监听器
        userIdentityService.addIdentityChangeListener(object : com.github.lilulei.ruankao.services.UserIdentityChangeListener {
            override fun onIdentityChanged(newLevel: com.github.lilulei.ruankao.model.ExamLevel, newExamType: com.github.lilulei.ruankao.model.ExamType) {
                currentExamLevel = newLevel.displayName
                currentExamType = newExamType
                updateIdentityDisplay()
                updateChapterComboBox()
            }
        })
    }
    
    /**
     * 设置身份选择面板
     */
    private fun setupIdentityPanel() {
        // 创建显示文本框
        identityDisplayField = JTextField().apply {
            isEditable = false
            text = "$currentExamLevel - ${currentExamType.displayName}"
            toolTipText = "点击选择考试身份"
        }
        
        // 创建下拉按钮
        identityDropDownButton = JButton().apply {
            icon = UIManager.getIcon("ComboBox.buttonArrowIcon")
            isBorderPainted = false
            isContentAreaFilled = false
            toolTipText = "点击选择考试身份"
        }
        
        // 组装面板
        identityPanel.apply {
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                "当前身份", 
                0, 0, Font(null, Font.BOLD, 12)
            )
            add(identityDisplayField, BorderLayout.CENTER)
            add(identityDropDownButton, BorderLayout.EAST)
        }
        
        // 添加点击事件
        val clickListener = object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                showIdentitySelectionDialog()
            }
        }
        identityDisplayField.addMouseListener(clickListener)
        identityDropDownButton.addActionListener { showIdentitySelectionDialog() }
    }
    
    /**
     * 显示身份选择对话框
     */
    private fun showIdentitySelectionDialog() {
        val dialog = UserIdentityDialog()
        if (dialog.showAndGet()) {
            val selectedExamType = dialog.getSelectedExamType()
            val selectedLevel = dialog.getSelectedExamLevel()
            
            if (selectedExamType != null && selectedLevel != null) {
                currentExamType = selectedExamType
                currentExamLevel = selectedLevel
                updateIdentityDisplay()
                userIdentityService.setSelectedExamType(selectedExamType)
                updateChapterComboBox()
            }
        }
    }
    
    /**
     * 更新身份显示
     */
    private fun updateIdentityDisplay() {
        identityDisplayField.text = "$currentExamLevel - ${currentExamType.displayName}"
    }
    
    /**
     * 更新章节下拉框，根据当前身份获取对应的章节
     */
    private fun updateChapterComboBox() {
        val chapterService = project.getService(KnowledgeChapterService::class.java)
        val chapters = chapterService.getChapterNamesByIdentity(currentExamLevel, currentExamType.displayName).sorted()
        
        // 始终启用章节下拉框，让用户可以看到状态
        chapterComboBox.isEnabled = true
        chapterComboBox.model = DefaultComboBoxModel(chapters.toTypedArray())
        
        if (chapters.isNotEmpty()) {
            chapterComboBox.selectedItem = chapters.first()
        } else {
            chapterComboBox.model = DefaultComboBoxModel(arrayOf("请先添加章节..."))
        }
        
        chapterComboBox.toolTipText = "当前身份维护的章节（共${chapters.size}个）"
    }
    
    /**
     * 填充表单数据（编辑模式）
     */
    private fun populateFormData(question: Question) {
        titleField.text = question.title
        
        // 设置当前身份为题目对应的身份
        currentExamLevel = question.examLevel.displayName
        currentExamType = question.examType
        updateIdentityDisplay()
        updateChapterComboBox()
        
        // 设置章节
        var chapterIndex: Int? = null
        val model = chapterComboBox.model as DefaultComboBoxModel<String>
        for (i in 0 until model.size) {
            if (model.getElementAt(i) == question.chapter) {
                chapterIndex = i
                break
            }
        }
        if (chapterIndex != null && chapterIndex >= 0) {
            chapterComboBox.selectedIndex = chapterIndex
        }
        
        // 设置难度
        levelComboBox.selectedItem = question.level.displayName
        
        // 设置解析
        explanationArea.text = question.explanation
        
        // 设置日期
        val calendar = java.util.Calendar.getInstance()
        calendar.set(question.year.year, question.year.monthValue - 1, question.year.dayOfMonth)
        dateSpinner.value = calendar.time
    }
    
    fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 2, 2, 2)
        }

        // 统一标签宽度
        val labelWidth = 65
        
        // 当前身份
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(identityPanel, gbc)
        
        // 重置gridwidth和gridy用于后续组件
        gbc.gridwidth = 1
        gbc.gridy = 1

        // 题目标题
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.WEST
        val titlePanel = JLabel("题目标题").apply {
            foreground = Color.WHITE
            text = "题目标题*"
            // 单独给星号设置红色
            val styledText = "<html>题目标题<span style='color:red;'>*</span></html>"
            text = styledText
        }
        panel.add(titlePanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(titleField, gbc)

        // 章节
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST
        val chapterPanel = JLabel("知识点章节").apply {
            foreground = Color.WHITE
            text = "知识点章节"
        }
        panel.add(chapterPanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(chapterComboBox, gbc)

        // 难度等级
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST
        val difficultyPanel = JLabel("难度").apply {
            foreground = Color.WHITE
            text = "难度*"
            // 单独给星号设置红色
            val styledText = "<html>难度<span style='color:red;'>*</span></html>"
            text = styledText
        }
        panel.add(difficultyPanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(levelComboBox, gbc)

        // 日期选择
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST
        val datePanel = JLabel("考试日期").apply {
            foreground = Color.WHITE
            text = "考试日期*"
            // 单独给星号设置红色
            val styledText = "<html>考试日期<span style='color:red;'>*</span></html>"
            text = styledText
        }
        panel.add(datePanel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(dateSpinner, gbc)

        // 选项输入区域
        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 0.3
        panel.add(createOptionsPanel(), gbc)

        // 解析输入区域
        gbc.gridx = 0
        gbc.gridy = 6
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weighty = 0.0
        gbc.anchor = GridBagConstraints.WEST
        val explanationLabel = JLabel("解析").apply {
            foreground = Color.WHITE
            text = "解析*"
            // 单独给星号设置红色
            val styledText = "<html>解析<span style='color:red;'>*</span></html>"
            text = styledText
        }
        panel.add(explanationLabel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 0.4
        val scrollPane = JScrollPane(explanationArea)
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        panel.add(scrollPane, gbc)

        return panel
    }
    
    private fun createOptionsPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.border = BorderFactory.createTitledBorder("选项输入")
        val mainGbc = GridBagConstraints().apply {
            insets = Insets(1, 1, 1, 1)
        }

        val panel = JPanel(GridBagLayout())

        // 添加选项输入行
        for (i in 0 until 4) {
            val keyField = createKeyField(i)
            val valueField = createValueField(i)
            optionsMap[keyField] = valueField

            val keyGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = i
                insets = Insets(0, 0, 0, 0)
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.NONE
                weightx = 0.0
            }
            panel.add(keyField, keyGbc)

            val valueGbc = GridBagConstraints().apply {
                gridx = 1
                gridy = i
                insets = Insets(0, 0, 0, 0)
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
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
    
    private fun createKeyField(index: Int): JTextField {
        return JTextField(3).apply {
            toolTipText = when(index) {
                0 -> "选项A标识，如：A"
                1 -> "选项B标识，如：B"
                2 -> "选项C标识，如：C"
                3 -> "选项D标识，如：D"
                else -> "选项标识，如：A、B、C、D"
            }
            foreground = Color.GRAY
            text = when(index) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                3 -> "D"
                else -> ""
            }
            addFocusListener(object : java.awt.event.FocusAdapter() {
                override fun focusGained(e: java.awt.event.FocusEvent?) {
                    if (text == when(index) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> ""
                        }) {
                        text = ""
                        foreground = Color.BLACK
                    }
                }

                override fun focusLost(e: java.awt.event.FocusEvent?) {
                    if (text.isEmpty()) {
                        text = when(index) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> ""
                        }
                        foreground = Color.GRAY
                    }
                }
            })
        }
    }
    
    private fun createValueField(index: Int): JTextField {
        return JTextField(20).apply {
            toolTipText = when(index) {
                0 -> "选项A的具体内容"
                1 -> "选项B的具体内容"
                2 -> "选项C的具体内容"
                3 -> "选项D的具体内容"
                else -> "选项具体内容"
            }
            foreground = Color.GRAY
            text = when(index) {
                0 -> "选项A内容"
                1 -> "选项B内容"
                2 -> "选项C内容"
                3 -> "选项D内容"
                else -> ""
            }
            addFocusListener(object : java.awt.event.FocusAdapter() {
                override fun focusGained(e: java.awt.event.FocusEvent?) {
                    if (text == when(index) {
                            0 -> "选项A内容"
                            1 -> "选项B内容"
                            2 -> "选项C内容"
                            3 -> "选项D内容"
                            else -> ""
                        }) {
                        text = ""
                        foreground = Color.BLACK
                    }
                }

                override fun focusLost(e: java.awt.event.FocusEvent?) {
                    if (text.isEmpty()) {
                        text = when(index) {
                            0 -> "选项A内容"
                            1 -> "选项B内容"
                            2 -> "选项C内容"
                            3 -> "选项D内容"
                            else -> ""
                        }
                        foreground = Color.GRAY
                    }
                }
            })
        }
    }
    
    private fun createAnswerSelectionPanel(): JPanel {
        val panel = JPanel()
        panel.border = BorderFactory.createTitledBorder("选择正确答案*")

        answerCheckBoxes.clear()
        
        for (i in 0 until 4) {
            val keyField = optionsMap.keys.elementAtOrNull(i) ?: continue
            val keyText = if (keyField.text.trim().isEmpty()) {
                when (i) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    3 -> "D"
                    else -> ""
                }
            } else {
                keyField.text
            }
            val checkBox = JCheckBox(keyText)
            checkBox.toolTipText = "选择选项 \"$keyText\" 作为正确答案"
            
            if (mode == FormMode.VIEW) {
                checkBox.isEnabled = false
            } else {
                checkBox.addActionListener {
                    if (checkBox.isSelected) {
                        correctAnswers.add(keyText)
                    } else {
                        correctAnswers.remove(keyText)
                    }
                }
            }
            
            answerCheckBoxes.add(checkBox)
            panel.add(checkBox)
        }

        return panel
    }
    
    private fun createListCellRenderer(): ListCellRenderer<Any> {
        return object : ListCellRenderer<Any> {
            override fun getListCellRendererComponent(
                list: JList<out Any>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val label = JLabel(value?.toString() ?: "")
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
    }
    
    fun populateOptions(options: Map<String, String>, correctAns: Set<String>) {
        optionsMap.entries.forEachIndexed { index, entry ->
            val (keyField, valueField) = entry
            val optionKey = options.keys.elementAtOrNull(index)
            val optionValue = options.values.elementAtOrNull(index)
            
            if (optionKey != null && optionValue != null) {
                keyField.text = optionKey
                keyField.foreground = Color.BLACK
                valueField.text = optionValue
                valueField.foreground = Color.BLACK
            }
        }
        
        // 设置正确答案
        correctAnswers.clear()
        correctAnswers.addAll(correctAns)
        
        // 更新复选框状态
        answerCheckBoxes.forEach { checkBox ->
            checkBox.isSelected = correctAns.contains(checkBox.text)
        }
    }
    
    fun getFormData(): QuestionFormData? {
        val title = titleField.text.trim()
        val explanation = explanationArea.text.trim()
        
        // 检查是否为提示文字
        val isTitlePlaceholder = title == "请输入题目内容..." && titleField.foreground == Color.GRAY
        val isExplanationPlaceholder = explanation == "在此处输入题目解析..." && explanationArea.foreground == Color.GRAY
        
        if (title.isEmpty() || explanation.isEmpty() || isTitlePlaceholder || isExplanationPlaceholder) {
            return null
        }
        
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
            return null
        }
        
        if (correctAnswers.isEmpty()) {
            return null
        }
        
        // 获取日期
        val dateValue = dateSpinner.value as? java.util.Date
        val localDate = if (dateValue != null) {
            dateValue.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        } else {
            LocalDate.now()
        }
        
        val difficulty = when (levelComboBox.selectedItem as String) {
            "简单" -> DifficultyLevel.EASY
            "中等" -> DifficultyLevel.MEDIUM
            "困难" -> DifficultyLevel.HARD
            else -> DifficultyLevel.MEDIUM
        }
        
        // 根据当前身份获取ExamLevel枚举
        val examLevel = when (currentExamLevel) {
            "软考高级" -> ExamLevel.SENIOR
            "软考中级" -> ExamLevel.INTERMEDIATE
            "软考初级" -> ExamLevel.JUNIOR
            else -> ExamLevel.SENIOR
        }
        
        return QuestionFormData(
            title = title,
            examLevel = examLevel,
            examType = currentExamType,
            level = difficulty,
            chapter = chapter,
            options = options,
            correctAnswers = correctAnswers,
            explanation = explanation,
            year = localDate
        )
    }
    
    /**
     * 设置为查看模式，禁用所有可编辑组件
     */
    fun setViewMode() {
        titleField.isEditable = false
        chapterComboBox.isEnabled = false
        levelComboBox.isEnabled = false
        explanationArea.isEditable = false
        dateSpinner.isEnabled = false
        identityPanel.isEnabled = false
        identityDisplayField.isEditable = false
        identityDropDownButton.isEnabled = false

        optionsMap.keys.forEach { it.isEditable = false }
        optionsMap.values.forEach { it.isEditable = false }

        answerCheckBoxes.forEach { it.isEnabled = false }
    }
}

/**
 * 表单模式枚举
 */
enum class FormMode {
    ADD,    // 添加模式
    EDIT,   // 编辑模式
    VIEW    // 查看模式
}

/**
 * 试题表单数据类
 */
data class QuestionFormData(
    val title: String,

    val examLevel: ExamLevel,
    val examType: ExamType,
    val level: DifficultyLevel,
    val chapter: String,
    val options: Map<String, String>,
    val correctAnswers: Set<String>,
    val explanation: String,
    val year: LocalDate
)