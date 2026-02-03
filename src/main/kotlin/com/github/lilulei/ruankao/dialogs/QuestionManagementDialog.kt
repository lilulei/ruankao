package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.QuestionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import java.awt.*
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * 试题管理对话框
 * 提供对已添加试题的查看、编辑、删除等管理功能
 */
class QuestionManagementDialog(private val project: Project) : DialogWrapper(true) {
    private val questionService = project.getService(QuestionService::class.java)
    private lateinit var table: JTable
    private lateinit var tableModel: QuestionTableModel
    private val questionList = mutableListOf<Question>()

    init {
        title = "试题管理"
        init()
        refreshQuestionList()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(800, 500)

        // 创建工具栏
        val toolbarPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val refreshButton = JButton("刷新")
        val addButton = JButton("添加单个试题")
        val importButton = JButton("批量导入试题")
        val templateButton = JButton("查看模板示例")
        val chapterButton = JButton("管理知识点章节")
        val editButton = JButton("编辑")
        val deleteButton = JButton("删除")
        val viewButton = JButton("查看详情")

        refreshButton.addActionListener { refreshQuestionList() }
        addButton.addActionListener { addNewQuestion() }
        importButton.addActionListener { importQuestions() }
        templateButton.addActionListener { showTemplateExample() }
        chapterButton.addActionListener { manageChapters() }
        editButton.addActionListener { editSelectedQuestion() }
        deleteButton.addActionListener { deleteSelectedQuestion() }
        viewButton.addActionListener { viewSelectedQuestion() }

        toolbarPanel.add(refreshButton)
        toolbarPanel.add(addButton)
        toolbarPanel.add(importButton)
        toolbarPanel.add(templateButton)
        toolbarPanel.add(chapterButton)
        toolbarPanel.add(Box.createHorizontalStrut(20)) // 添加间隔
        toolbarPanel.add(editButton)
        toolbarPanel.add(deleteButton)
        toolbarPanel.add(viewButton)

        // 创建表格
        tableModel = QuestionTableModel(questionList)
        table = JTable(tableModel)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        
        // 设置列宽
        table.columnModel.getColumn(0).preferredWidth = 80  // ID
        table.columnModel.getColumn(1).preferredWidth = 200 // 标题
        table.columnModel.getColumn(2).preferredWidth = 100 // 分类
        table.columnModel.getColumn(3).preferredWidth = 80  // 难度
        table.columnModel.getColumn(4).preferredWidth = 120 // 考试类型
        table.columnModel.getColumn(5).preferredWidth = 100 // 章节

        val scrollPane = JScrollPane(table)
        scrollPane.preferredSize = Dimension(780, 400)

        panel.add(toolbarPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun refreshQuestionList() {
        questionList.clear()
        questionList.addAll(questionService.allQuestionsList.sortedBy { it.id })
        tableModel.fireTableDataChanged()
    }

    private fun editSelectedQuestion() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val question = questionList[selectedRow]
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
            val question = questionList[selectedRow]
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

    private fun viewSelectedQuestion() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val question = questionList[selectedRow]
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
     * 试题表格模型
     */
    class QuestionTableModel(private val questions: List<Question>) : AbstractTableModel() {
        private val columnNames = arrayOf("ID", "题目标题", "分类", "难度", "考试类型", "章节")

        override fun getRowCount(): Int = questions.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(row: Int, column: Int): Any? {
            val question = questions[row]
            return when (column) {
                0 -> question.id
                1 -> question.title
                2 -> question.category
                3 -> question.level.displayName
                4 -> question.examType.displayName
                5 -> question.chapter ?: "无"
                else -> null
            }
        }
    }
}

