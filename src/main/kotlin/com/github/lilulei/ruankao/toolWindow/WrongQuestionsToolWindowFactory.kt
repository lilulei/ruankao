package com.github.lilulei.ruankao.toolWindow

import com.github.lilulei.ruankao.model.WrongQuestionInfo
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.WrongQuestionChangeListener
import com.github.lilulei.ruankao.services.WrongQuestionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * 错题本工具窗口工厂类，用于创建和管理错题本工具窗口
 */
class WrongQuestionsToolWindowFactory : ToolWindowFactory {

    /**
     * 创建工具窗口内容
     *
     * @param project 当前项目实例
     * @param toolWindow 工具窗口实例
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = WrongQuestionsToolWindow(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)

        // 当内容被选中/激活时刷新数据
        content.setDisposer {
            myToolWindow.cleanup()
        }
    }

    /**
     * 判断工具窗口是否应该可用
     *
     * @param project 当前项目实例
     * @return 始终返回true，表示工具窗口始终可用
     */
    override fun shouldBeAvailable(project: Project) = true

    /**
     * 错题本工具窗口内部类，负责管理错题本界面和功能
     *
     * @param toolWindow 工具窗口实例
     * @param project 当前项目实例
     */
    class WrongQuestionsToolWindow(toolWindow: ToolWindow, private val project: Project) {
        private val wrongQuestionService = project.getService(WrongQuestionService::class.java)
        private val questionService = project.getService(QuestionService::class.java)

        private lateinit var table: JTable
        private lateinit var tableModel: WrongQuestionsTableModel
        private val listener = object : WrongQuestionChangeListener {
            override fun onWrongQuestionUpdated() {
                SwingUtilities.invokeLater {
                    refreshWrongQuestions()
                }
            }
        }

        init {
            // 注册监听器
            wrongQuestionService.addWrongQuestionListener(listener)
        }

        /**
         * 清理资源，移除监听器避免内存泄漏
         */
        fun cleanup() {
            // 移除监听器避免内存泄漏
            wrongQuestionService.removeWrongQuestionListener(listener)
        }

        /**
         * 获取工具窗口的内容面板
         *
         * @return 包含错题本界面的面板组件
         */
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()

            val titleLabel = JLabel("错题本", SwingConstants.CENTER)
            titleLabel.font = titleLabel.font.deriveFont(16f)

            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
                val refreshButton = JButton("刷新")
                refreshButton.addActionListener {
                    refreshWrongQuestions()
                }
                add(refreshButton)
            }

            tableModel = WrongQuestionsTableModel(emptyList())
            table = JTable(tableModel)
            val scrollPane = JBScrollPane(table)

            add(titleLabel, BorderLayout.NORTH)
            add(buttonPanel, BorderLayout.CENTER)
            add(scrollPane, BorderLayout.SOUTH)

            refreshWrongQuestions()
        }

        /**
         * 刷新错题列表数据
         */
        fun refreshWrongQuestions() {
            val wrongQuestions = wrongQuestionService.allWrongQuestions.values.toList()
            tableModel.updateData(wrongQuestions)
        }
    }

    /**
     * 错题表格模型类，用于管理错题表格的数据展示
     *
     * @param wrongQuestions 错题信息列表
     */
    class WrongQuestionsTableModel(private var wrongQuestions: List<WrongQuestionInfo>) : AbstractTableModel() {
        private val columnNames = arrayOf("题目ID", "错误次数", "最后错误时间", "已掌握", "连续正确次数")

        /**
         * 获取表格行数
         *
         * @return 表格行数（错题数量）
         */
        override fun getRowCount(): Int = wrongQuestions.size

        /**
         * 获取表格列数
         *
         * @return 表格列数（5列）
         */
        override fun getColumnCount(): Int = columnNames.size

        /**
         * 获取列名
         *
         * @param column 列索引
         * @return 对应列的名称
         */
        override fun getColumnName(column: Int): String = columnNames[column]

        /**
         * 获取指定单元格的值
         *
         * @param row 行索引
         * @param column 列索引
         * @return 指定位置的单元格值
         */
        override fun getValueAt(row: Int, column: Int): Any? {
            val wrongQuestion = wrongQuestions[row]
            return when (column) {
                0 -> wrongQuestion.questionId
                1 -> wrongQuestion.errorCount
                2 -> java.util.Date(wrongQuestion.lastErrorTime).toString()
                3 -> wrongQuestion.mastered
                4 -> wrongQuestion.consecutiveCorrectCount
                else -> null
            }
        }

        /**
         * 更新表格数据
         *
         * @param newWrongQuestions 新的错题信息列表
         */
        fun updateData(newWrongQuestions: List<WrongQuestionInfo>) {
            wrongQuestions = newWrongQuestions
            fireTableDataChanged()
        }
    }
}
