package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.ExamLevel
import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.UserIdentityService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.AbstractTableModel
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
    private lateinit var levelFilterComboBox: JComboBox<ExamLevel?>
    private lateinit var identityFilterCheckBox: JCheckBox

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
        
        // 设置列宽
        val columnModel = table.columnModel
        columnModel.getColumn(0).preferredWidth = 30   // 复选框
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
        panel.border = BorderFactory.createTitledBorder("筛选条件")
        
        // 搜索框
        panel.add(JLabel("题目搜索:"))
        searchField = JTextField(15)
        searchField.toolTipText = "输入题目标题关键词进行搜索"
        searchField.addActionListener {
            applyFilters()
        }
        panel.add(searchField)
        
        // 考试级别筛选
        panel.add(Box.createHorizontalStrut(10))
        panel.add(JLabel("考试级别:"))
        levelFilterComboBox = JComboBox(arrayOf<ExamLevel?>(null, ExamLevel.SENIOR, ExamLevel.INTERMEDIATE, ExamLevel.JUNIOR))
        levelFilterComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = when (value) {
                    null -> "全部级别"
                    is ExamLevel -> value.displayName
                    else -> value.toString()
                }
                return component
            }
        }
        levelFilterComboBox.addActionListener {
            applyFilters()
        }
        panel.add(levelFilterComboBox)
        
        // 身份筛选
        panel.add(Box.createHorizontalStrut(10))
        identityFilterCheckBox = JCheckBox("按当前身份筛选")
        identityFilterCheckBox.addActionListener {
            applyFilters()
        }
        panel.add(identityFilterCheckBox)
        
        // 应用筛选按钮
        val applyButton = JButton("应用筛选")
        applyButton.addActionListener {
            applyFilters()
        }
        panel.add(applyButton)
        
        // 重置筛选按钮
        val resetButton = JButton("重置筛选")
        resetButton.addActionListener {
            resetFilters()
        }
        panel.add(resetButton)

        return panel
    }

    private fun createToolbarPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        val refreshButton = JButton("刷新")
        val addButton = JButton("添加单个试题")
        val importButton = JButton("批量导入试题")
        val templateButton = JButton("查看模板示例")
        val chapterButton = JButton("管理知识点章节")
        val editButton = JButton("编辑")
        val deleteButton = JButton("删除")
        val batchDeleteButton = JButton("批量删除")
        val viewButton = JButton("查看详情")

        refreshButton.addActionListener { refreshQuestionList() }
        addButton.addActionListener { addNewQuestion() }
        importButton.addActionListener { importQuestions() }
        templateButton.addActionListener { showTemplateExample() }
        chapterButton.addActionListener { manageChapters() }
        editButton.addActionListener { editSelectedQuestion() }
        deleteButton.addActionListener { deleteSelectedQuestion() }
        batchDeleteButton.addActionListener { batchDeleteQuestions() }
        viewButton.addActionListener { viewSelectedQuestion() }

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
        
        // 按考试级别筛选
        val selectedLevel = levelFilterComboBox.selectedItem as? ExamLevel
        if (selectedLevel != null) {
            tempList = tempList.filter { it.examLevel == selectedLevel }
        }
        
        // 按当前身份筛选
        if (identityFilterCheckBox.isSelected && userIdentityService.isIdentitySelected()) {
            val currentLevel = userIdentityService.getSelectedLevel()
            val currentExamType = userIdentityService.getSelectedExamType()
            tempList = tempList.filter { 
                it.examLevel == currentLevel && it.examType == currentExamType 
            }
        }
        
        filteredQuestionList.addAll(tempList)
        tableModel.fireTableDataChanged()
    }

    private fun resetFilters() {
        searchField.text = ""
        levelFilterComboBox.selectedIndex = 0
        identityFilterCheckBox.isSelected = false
        applyFilters()
    }

    private fun refreshQuestionList() {
        questionList.clear()
        questionList.addAll(questionService.allQuestionsList.sortedBy { it.id })
        applyFilters() // 重新应用筛选
    }

    private fun editSelectedQuestion() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val question = filteredQuestionList[selectedRow]
            val dialog = com.github.lilulei.ruankao.dialogs.EditQuestionDialog(project, question)
            if (dialog.showAndGet()) {
                val updatedQuestion = dialog.getUpdatedQuestion()
                if (updatedQuestion != null) {
                    questionService.updateQuestion(updatedQuestion)
                    refreshQuestionList()
                    JOptionPane.showMessageDialog(
                        this.window,
                        "试题更新成功！",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
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
            val confirm = JOptionPane.showConfirmDialog(
                this.window,
                "确定要删除试题 \"${question.title}\" 吗？\n此操作不可撤销！",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )

            if (confirm == JOptionPane.YES_OPTION) {
                questionService.removeQuestion(question.id)
                refreshQuestionList()
                JOptionPane.showMessageDialog(
                    this.window,
                    "试题删除成功！",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE
                )
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

        val confirm = JOptionPane.showConfirmDialog(
            this.window,
            "确定要删除选中的 ${selectedQuestions.size} 道试题吗？\n此操作不可撤销！",
            "确认批量删除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            var deletedCount = 0
            val questionsToDelete = selectedQuestions.toList() // 创建副本避免并发修改
            questionsToDelete.forEach { questionId ->
                questionService.removeQuestion(questionId)
                deletedCount++
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
                0 -> Boolean::class.java // 复选框列
                else -> String::class.java
            }
        }
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0 // 只有复选框列可编辑
        }

        override fun getValueAt(row: Int, column: Int): Any? {
            val question = questions[row]
            return when (column) {
                0 -> selectedQuestions.contains(question.id) // 复选框状态
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
    }
}