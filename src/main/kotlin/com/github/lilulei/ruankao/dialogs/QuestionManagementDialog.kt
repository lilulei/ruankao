package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.ExamLevel
import com.github.lilulei.ruankao.model.ExamType
import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.model.QuestionType
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.UserIdentityService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import java.awt.BasicStroke
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn

/**
 * 试题管理对话框
 * 提供对已添加试题的查看、编辑、删除等管理功能，支持批量操作和筛选
 */
class QuestionManagementDialog(private val project: Project) : DialogWrapper(true) {
    private val questionService = project.getService(QuestionService::class.java)
    private val userIdentityService = project.getService(UserIdentityService::class.java)
    private lateinit var table: JTable
    private lateinit var tableModel: QuestionTableModel
    private val questionList = mutableListOf<Question>()
    private val filteredQuestionList = mutableListOf<Question>()
    private var selectedQuestions = mutableSetOf<String>()
    
    // 筛选组件
    private lateinit var searchField: JTextField
    private lateinit var identityLevelComboBox: JComboBox<ExamLevel>
    private lateinit var identityTypeComboBox: JComboBox<ExamType>

    init {
        title = "试题管理"
        init()
        refreshQuestionList()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(900, 600)

        // 创建筛选面板
        val filterPanel = createFilterPanel()
        
        // 创建工具栏
        val toolbarPanel = createToolbarPanel()

        // 创建表格
        tableModel = QuestionTableModel(filteredQuestionList, selectedQuestions)
        table = JTable(tableModel)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        
        // 使用自定义复选框渲染器，确保选中状态持久化显示
        val checkboxRenderer = CustomCheckboxRenderer(selectedQuestions)
        table.setDefaultRenderer(Boolean::class.java, checkboxRenderer)
        // 移除默认编辑器，避免与自定义点击处理冲突
        table.setDefaultEditor(Boolean::class.java, null)
        
        // 添加鼠标点击事件处理，实现真正的选中/取消选中功能
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                // 使用 mousePressed 而不是 mouseClicked，响应更快更稳定
                val point = e.point
                val row = table.rowAtPoint(point)
                val column = table.columnAtPoint(point)
                
                // 只处理第一列（复选框列）的点击
                if (column == 0 && row >= 0 && row < tableModel.rowCount) {
                    val questionId = tableModel.getQuestionIdAt(row)
                    if (questionId != null) {
                        // 阻止事件继续传播，避免与其他事件处理器冲突
                        e.consume()
                        
                        if (selectedQuestions.contains(questionId)) {
                            // 已选中，取消选中
                            selectedQuestions.remove(questionId)
                        } else {
                            // 未选中，选中
                            selectedQuestions.add(questionId)
                        }
                        // 刷新表格显示
                        tableModel.fireTableDataChanged()
                    }
                }
            }
        })
        
        // 设置列宽
        val columnModel = table.columnModel
        columnModel.getColumn(0).preferredWidth = 40   // 复选框（增加宽度让图标更明显）
        columnModel.getColumn(1).preferredWidth = 80   // ID
        columnModel.getColumn(2).preferredWidth = 100  // 考试级别
        columnModel.getColumn(3).preferredWidth = 150  // 考试类型
        columnModel.getColumn(4).preferredWidth = 250  // 题目标题
        columnModel.getColumn(5).preferredWidth = 120  // 知识点章节
        columnModel.getColumn(6).preferredWidth = 80   // 难度
        columnModel.getColumn(7).preferredWidth = 100  // 考试日期

        val scrollPane = JScrollPane(table)
        scrollPane.preferredSize = Dimension(880, 450)

        // 布局组件
        val topPanel = JPanel(BorderLayout())
        topPanel.add(filterPanel, BorderLayout.NORTH)
        topPanel.add(toolbarPanel, BorderLayout.CENTER)
        
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createFilterPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = BorderFactory.createTitledBorder("身份选择")
        
        // 搜索框
        panel.add(JLabel("题目搜索:"))
        searchField = JTextField(15)
        searchField.toolTipText = "输入题目标题关键词进行搜索"
        searchField.addActionListener {
            applyFilters()
        }
        panel.add(searchField)
        
        // 考试级别选择（一级）
        panel.add(Box.createHorizontalStrut(10))
        panel.add(JLabel("考试级别:"))
        identityLevelComboBox = JComboBox(ExamLevel.values())
        identityLevelComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = when (value) {
                    is ExamLevel -> value.displayName
                    else -> value.toString()
                }
                return component
            }
        }
        // 设置初始选中项为用户当前选择的级别
        identityLevelComboBox.selectedItem = userIdentityService.getSelectedExamLevel()
        identityLevelComboBox.addActionListener {
            updateExamTypeComboBox()
            // 只有在初始化完成后才应用筛选
            if (::tableModel.isInitialized) {
                applyFilters()
            }
        }
        panel.add(identityLevelComboBox)
        
        // 考试类型选择（二级联动）
        panel.add(Box.createHorizontalStrut(10))
        panel.add(JLabel("考试类型:"))
        identityTypeComboBox = JComboBox(arrayOf<ExamType>())
        identityTypeComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = when (value) {
                    is ExamType -> value.displayName
                    else -> value.toString()
                }
                return component
            }
        }
        identityTypeComboBox.addActionListener {
            syncLevelFromType()
            // 只有在初始化完成后才应用筛选
            if (::tableModel.isInitialized) {
                applyFilters()
            }
        }
        panel.add(identityTypeComboBox)
        
        // 初始化考试类型下拉框（延迟初始化，避免tableModel未准备好）
        SwingUtilities.invokeLater {
            updateExamTypeComboBox()
        }
        
        // 应用筛选按钮
        val applyButton = JButton("应用筛选")
        applyButton.addActionListener {
            applyFilters()
        }
        panel.add(applyButton)
        
        // 重置筛选按钮
        val resetButton = JButton("重置选择")
        resetButton.addActionListener {
            identityLevelComboBox.selectedItem = userIdentityService.getSelectedExamLevel()
            updateExamTypeComboBox()
            applyFilters()
        }
        panel.add(resetButton)

        return panel
    }

    private fun createToolbarPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        // 批量操作按钮
        val selectAllButton = JButton("全选")
        val deselectAllButton = JButton("取消全选")
        
        val refreshButton = JButton("刷新")
        val addButton = JButton("添加单个试题")
        val importButton = JButton("批量导入试题")
        val templateButton = JButton("查看模板示例")
        val chapterButton = JButton("管理知识点章节")
        val editButton = JButton("编辑")
        val deleteButton = JButton("删除")
        val batchDeleteButton = JButton("批量删除")
        val viewButton = JButton("查看详情")

        // 批量选择事件处理
        selectAllButton.addActionListener {
            selectedQuestions.addAll(filteredQuestionList.map { it.id })
            tableModel.fireTableDataChanged()
        }
        
        deselectAllButton.addActionListener {
            selectedQuestions.clear()
            tableModel.fireTableDataChanged()
        }

        refreshButton.addActionListener { refreshQuestionList() }
        addButton.addActionListener { addNewQuestion() }
        importButton.addActionListener { importQuestions() }
        templateButton.addActionListener { showTemplateExample() }
        chapterButton.addActionListener { manageChapters() }
        editButton.addActionListener { editSelectedQuestion() }
        deleteButton.addActionListener { deleteSelectedQuestion() }
        batchDeleteButton.addActionListener { batchDeleteQuestions() }
        viewButton.addActionListener { viewSelectedQuestion() }

        // 添加批量操作按钮
        panel.add(selectAllButton)
        panel.add(deselectAllButton)
        panel.add(Box.createHorizontalStrut(10))
        
        panel.add(refreshButton)
        panel.add(addButton)
        panel.add(importButton)
        panel.add(templateButton)
        panel.add(chapterButton)
        panel.add(Box.createHorizontalStrut(20))
        panel.add(editButton)
        panel.add(deleteButton)
        panel.add(batchDeleteButton)
        panel.add(viewButton)

        return panel
    }

    private fun applyFilters() {
        filteredQuestionList.clear()
        
        var tempList = questionList.toList()
        
        // 按题目标题搜索
        val searchText = searchField.text.trim()
        if (searchText.isNotEmpty()) {
            tempList = tempList.filter { it.title.contains(searchText, ignoreCase = true) }
        }
        
        // 按考试级别筛选（来自身份选择）
        val selectedLevel = identityLevelComboBox.selectedItem as? ExamLevel
        if (selectedLevel != null) {
            tempList = tempList.filter { it.examLevel == selectedLevel }
        }
        
        // 按考试类型筛选（来自身份选择）
        val selectedType = identityTypeComboBox.selectedItem as? ExamType
        if (selectedType != null) {
            tempList = tempList.filter { it.examType == selectedType }
        }
        
        filteredQuestionList.addAll(tempList)
        
        // 清理selectedQuestions中不在当前筛选结果中的项
        selectedQuestions.retainAll(filteredQuestionList.map { it.id })
        
        // 只有在tableModel已初始化的情况下才更新表格
        if (::tableModel.isInitialized) {
            tableModel.fireTableDataChanged()
        }
    }

    private fun resetFilters() {
        searchField.text = ""
        identityLevelComboBox.selectedItem = userIdentityService.getSelectedExamLevel()
        updateExamTypeComboBox()
        applyFilters()
    }

    private fun refreshQuestionList() {
        questionList.clear()
        questionList.addAll(questionService.allQuestionsList.sortedBy { it.id })
        // 刷新时清空选中状态
        selectedQuestions.clear()
        applyFilters() // 重新应用筛选
    }

    private fun editSelectedQuestion() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val question = filteredQuestionList[selectedRow]

            // 检查是否为内置试题
            if (question.questionType == QuestionType.BUILT_IN) {
                JOptionPane.showMessageDialog(
                    this.window,
                    "内置试题不允许修改",
                    "提示",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            val dialog = com.github.lilulei.ruankao.dialogs.EditQuestionDialog(project, question)
            if (dialog.showAndGet()) {
                val updatedQuestion = dialog.getUpdatedQuestion()
                if (updatedQuestion != null) {
                    val success = questionService.updateQuestion(updatedQuestion)
                    if (success) {
                        refreshQuestionList()
                        JOptionPane.showMessageDialog(
                            this.window,
                            "试题更新成功！",
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        JOptionPane.showMessageDialog(
                            this.window,
                            "更新试题失败",
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(
                this.window,
                "请先选择要编辑的试题",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    private fun deleteSelectedQuestion() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val question = filteredQuestionList[selectedRow]

            // 检查是否为内置试题
            if (question.questionType == QuestionType.BUILT_IN) {
                JOptionPane.showMessageDialog(
                    this.window,
                    "内置试题不允许删除",
                    "提示",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            val confirm = JOptionPane.showConfirmDialog(
                this.window,
                "确定要删除试题 \"${question.title}\" 吗？\n此操作不可撤销！",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )

            if (confirm == JOptionPane.YES_OPTION) {
                val success = questionService.removeQuestion(question.id)
                if (success) {
                    refreshQuestionList()
                    JOptionPane.showMessageDialog(
                        this.window,
                        "试题删除成功！",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        this.window,
                        "删除试题失败",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        } else {
            JOptionPane.showMessageDialog(
                this.window,
                "请先选择要删除的试题",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    private fun batchDeleteQuestions() {
        if (selectedQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "请先选择要删除的试题",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        // 过滤出非内置试题
        val questionsToDelete = filteredQuestionList.filter {
            selectedQuestions.contains(it.id) && it.questionType != QuestionType.BUILT_IN
        }
        val builtInCount = selectedQuestions.size - questionsToDelete.size

        if (questionsToDelete.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "选中的试题均为内置试题，无法删除",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        var message = "确定要删除选中的 ${questionsToDelete.size} 道试题吗？"
        if (builtInCount > 0) {
            message += "\n（其中 $builtInCount 道内置试题将被跳过）"
        }
        message += "\n此操作不可撤销！"

        val confirm = JOptionPane.showConfirmDialog(
            this.window,
            message,
            "确认批量删除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            var deletedCount = 0
            val questionIdsToDelete = questionsToDelete.map { it.id }.toList()
            questionIdsToDelete.forEach { questionId ->
                val success = questionService.removeQuestion(questionId)
                if (success) {
                    deletedCount++
                }
            }

            selectedQuestions.clear()
            refreshQuestionList()

            JOptionPane.showMessageDialog(
                this.window,
                "成功删除 $deletedCount 道试题！",
                "成功",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun viewSelectedQuestion() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val question = filteredQuestionList[selectedRow]
            val dialog = com.github.lilulei.ruankao.dialogs.ViewQuestionDialog(project, question)
            dialog.show()
        } else {
            JOptionPane.showMessageDialog(
                this.window,
                "请先选择要查看的试题",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    private fun addNewQuestion() {
        val dialog = AddQuestionDialog(project)
        if (dialog.showAndGet()) {
            JOptionPane.showMessageDialog(
                this.window,
                "试题添加成功！",
                "成功",
                JOptionPane.INFORMATION_MESSAGE
            )
            refreshQuestionList()
        }
    }

    private fun importQuestions() {
        val dialog = ImportQuestionsDialog(project)
        if (dialog.showAndGet()) {
            refreshQuestionList()
        }
    }

    private fun showTemplateExample() {
        try {
            val templatePath = "src/main/resources/templates/questions_template.json"
            
            val message = """
                |模板文件位置：$templatePath
                |
                |您可以：
                |1. 直接复制此文件作为导入模板
                |2. 参考其格式创建自己的试题文件
                |3. 使用相对路径导入：$templatePath
                |
                |模板包含5个示例题目，涵盖不同难度和考试类型。
                |""".trimMargin()
            
            JOptionPane.showMessageDialog(
                this.window,
                message,
                "模板示例说明",
                JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this.window,
                "无法显示模板信息：${e.message}",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun manageChapters() {
        val dialog = ChapterManagementDialog(project)
        dialog.show()
    }
    
    /**
     * 更新考试类型下拉框（根据选中的考试级别）
     */
    private fun updateExamTypeComboBox() {
        val selectedLevel = identityLevelComboBox.selectedItem as? ExamLevel
        if (selectedLevel != null) {
            val examTypes = userIdentityService.getExamTypesForLevel(selectedLevel)
            identityTypeComboBox.removeAllItems()
            examTypes.forEach { identityTypeComboBox.addItem(it) }
            
            // 设置默认选中项为该级别的默认考试类型
            val defaultType = userIdentityService.getDefaultExamTypeForLevel(selectedLevel)
            identityTypeComboBox.selectedItem = defaultType
        }
    }
    
    /**
     * 根据选中的考试类型同步更新考试级别
     */
    private fun syncLevelFromType() {
        val selectedType = identityTypeComboBox.selectedItem as? ExamType
        if (selectedType != null) {
            // 获取该考试类型对应的级别
            val correspondingLevel = when (selectedType) {
                // 软考高级
                ExamType.SYSTEM_ANALYST,
                ExamType.SYSTEM_ARCHITECT,
                ExamType.NETWORK_PLANNER,
                ExamType.PROJECT_MANAGER,
                ExamType.SYSTEM_PLANNING_MANAGER -> ExamLevel.SENIOR
                
                // 软考中级
                ExamType.SYSTEM_INTEGRATION_ENGINEER,
                ExamType.NETWORK_ENGINEER,
                ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER,
                ExamType.SOFTWARE_TESTER,
                ExamType.DATABASE_ENGINEER,
                ExamType.MULTIMEDIA_DESIGNER,
                ExamType.SOFTWARE_DESIGNER,
                ExamType.INFORMATION_SYSTEM_SUPERVISOR,
                ExamType.E_COMMERCE_DESIGNER,
                ExamType.INFORMATION_SECURITY_ENGINEER,
                ExamType.EMBEDDED_SYSTEM_DESIGNER,
                ExamType.SOFTWARE_PROCESS_EVALUATOR,
                ExamType.COMPUTER_AIDED_DESIGNER,
                ExamType.COMPUTER_HARDWARE_ENGINEER,
                ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER -> ExamLevel.INTERMEDIATE
                
                // 软考初级
                ExamType.PROGRAMMER,
                ExamType.NETWORK_ADMINISTRATOR,
                ExamType.INFORMATION_PROCESSING_TECHNICIAN,
                ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER,
                ExamType.MULTIMEDIA_APPLICATION_DESIGNER,
                ExamType.E_COMMERCE_TECHNICIAN,
                ExamType.WEB_DESIGNER -> ExamLevel.JUNIOR
            }
            
            // 更新级别选择框，避免触发循环事件
            val actionListeners = identityLevelComboBox.actionListeners.toList()
            actionListeners.forEach { identityLevelComboBox.removeActionListener(it) }
            identityLevelComboBox.selectedItem = correspondingLevel
            actionListeners.forEach { listener ->
                identityLevelComboBox.addActionListener(listener)
            }
        }
    }
    
    /**
     * 试题表格模型 - 支持复选框
     */
    class QuestionTableModel(
        private val questions: List<Question>,
        private val selectedQuestions: MutableSet<String>
    ) : AbstractTableModel() {
        private val columnNames = arrayOf("", "ID", "考试级别", "考试类型", "题目标题", "知识点章节", "难度", "考试日期")

        override fun getRowCount(): Int = questions.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> Boolean::class.javaPrimitiveType ?: Boolean::class.java
                else -> String::class.java
            }
        }
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0 // 只有复选框列可编辑
        }

        override fun getValueAt(row: Int, column: Int): Any? {
            if (row < 0 || row >= questions.size) return null
            val question = questions[row]
            return when (column) {
                0 -> null // 返回null，避免显示true/false文本
                1 -> question.id
                2 -> question.examLevel.displayName
                3 -> question.examType.displayName
                4 -> question.title
                5 -> question.chapter ?: "无"
                6 -> question.level.displayName
                7 -> question.year.toString()
                else -> null
            }
        }
        
        override fun setValueAt(value: Any?, row: Int, column: Int) {
            if (column == 0 && value is Boolean) {
                val question = questions[row]
                if (value) {
                    selectedQuestions.add(question.id)
                } else {
                    selectedQuestions.remove(question.id)
                }
                fireTableCellUpdated(row, column)
            }
        }
        
        /**
         * 获取指定行的题目ID
         */
        fun getQuestionIdAt(row: Int): String? {
            return if (row >= 0 && row < questions.size) {
                questions[row].id
            } else {
                null
            }
        }
    }
    
    /**
     * 自定义复选框渲染器：显示矩形中间加对号的图标样式
     */
    private class CustomCheckboxRenderer(
        private val selectedQuestions: MutableSet<String>
    ) : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            // 将组件转换为 JLabel 类型
            if (component is JLabel) {
                // 设置背景透明
                component.isOpaque = false
                
                // 关键修复：直接检查 selectedQuestions 集合获取真实选中状态
                val questionId = table?.model?.let { model ->
                    if (model is QuestionTableModel && row >= 0) {
                        model.getQuestionIdAt(row)
                    } else {
                        null
                    }
                }
                val isSelectedNow = questionId != null && selectedQuestions.contains(questionId)
                
                // 如果是复选框列（第0列）
                if (column == 0) {
                    component.text = ""
                    if (isSelectedNow) {
                        // 选中状态：显示对号图标
                        component.icon = CheckIcon(true)
                        // 添加浅蓝色背景
                        component.background = Color(230, 240, 255)
                        component.isOpaque = true
                    } else {
                        // 未选中状态：显示空矩形框
                        component.icon = CheckIcon(false)
                        // 添加浅灰色背景使其更可见
                        component.background = Color(245, 245, 245)
                        component.isOpaque = true
                    }
                } else {
                    // 其他列显示默认内容
                    component.text = value?.toString() ?: ""
                    component.icon = null
                    component.background = Color.WHITE
                    component.isOpaque = false
                }
            }
            
            return component
        }
    }

    /**
     * 对号图标类 - 支持选中和未选中两种状态
     */
    private class CheckIcon(private val isChecked: Boolean) : Icon {
        private val size = 16
        
        override fun getIconWidth(): Int = size
        
        override fun getIconHeight(): Int = size
        
        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
            g?.let { graphics ->
                // 绘制矩形边框 - 使用深灰色，更清晰可见
                graphics.color = Color(80, 80, 80)  // 深灰色边框
                graphics.drawRect(x, y, size - 1, size - 1)
                
                // 如果是选中状态，绘制对号
                if (isChecked) {
                    // 绘制对号 - 使用深蓝色，确保清晰
                    graphics.color = Color(0, 100, 200)  // 深蓝色对号
                    if (graphics is Graphics2D) {
                        graphics.stroke = BasicStroke(2.0f)
                    }
                    
                    // 对号的两条线
                    graphics.drawLine(x + 4, y + 8, x + 7, y + 12)
                    graphics.drawLine(x + 7, y + 12, x + 12, y + 4)
                }
                // 未选中状态只绘制矩形框，不绘制对号
            }
        }
    }
}