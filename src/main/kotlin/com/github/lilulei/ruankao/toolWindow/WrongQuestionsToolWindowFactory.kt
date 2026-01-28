package com.github.lilulei.ruankao.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.util.Key
import com.intellij.ui.content.ContentFactory
import com.github.lilulei.ruankao.services.WrongQuestionService
import com.github.lilulei.ruankao.services.QuestionService
import javax.swing.*
import java.awt.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBPanel
import com.github.lilulei.ruankao.model.WrongQuestionInfo
import com.github.lilulei.ruankao.model.Question
import javax.swing.table.AbstractTableModel
import com.github.lilulei.ruankao.services.WrongQuestionChangeListener

class WrongQuestionsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = WrongQuestionsToolWindow(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
        
        // 当内容被选中/激活时刷新数据
        content.setDisposer {
            myToolWindow.cleanup()
        }
    }

    override fun shouldBeAvailable(project: Project) = true

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

        fun cleanup() {
            // 移除监听器避免内存泄漏
            wrongQuestionService.removeWrongQuestionListener(listener)
        }

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
        
        fun refreshWrongQuestions() {
            val wrongQuestions = wrongQuestionService.allWrongQuestions.values.toList()
            tableModel.updateData(wrongQuestions)
        }
    }
    
    class WrongQuestionsTableModel(private var wrongQuestions: List<WrongQuestionInfo>) : AbstractTableModel() {
        private val columnNames = arrayOf("题目ID", "错误次数", "最后错误时间", "已掌握", "连续正确次数")
        
        override fun getRowCount(): Int = wrongQuestions.size
        
        override fun getColumnCount(): Int = columnNames.size
        
        override fun getColumnName(column: Int): String = columnNames[column]
        
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
        
        // 添加更新数据的方法
        fun updateData(newWrongQuestions: List<WrongQuestionInfo>) {
            wrongQuestions = newWrongQuestions
            fireTableDataChanged()
        }
    }
}