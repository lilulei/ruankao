package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.Question
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * 查看试题详情对话框
 * 提供只读模式查看试题完整信息
 */
class ViewQuestionDialog(private val project: Project, private val question: Question) : DialogWrapper(true) {
    private val formPanel = QuestionFormPanel(project, FormMode.VIEW, question)

    init {
        title = "试题详情 - ${question.id}"
        init()
        populateQuestionData()
    }

    private fun populateQuestionData() {
        // 填充表单数据
        formPanel.titleField.text = question.title
        // category字段已移除
        formPanel.examTypeLabel.text = question.examType.displayName
        formPanel.levelComboBox.selectedItem = question.level.displayName
        formPanel.explanationArea.text = question.explanation
        
        // 设置章节
        var chapterIndex: Int? = null
        val model = formPanel.chapterComboBox.model as DefaultComboBoxModel<String>
        for (i in 0 until model.size) {
            if (model.getElementAt(i) == question.chapter) {
                chapterIndex = i
                break
            }
        }
        if (chapterIndex != null && chapterIndex >= 0) {
            formPanel.chapterComboBox.selectedIndex = chapterIndex
        }
        
        // 设置日期
        val calendar = java.util.Calendar.getInstance()
        calendar.set(question.year.year, question.year.monthValue - 1, question.year.dayOfMonth)
        formPanel.dateSpinner.value = calendar.time
        
        // 填充选项和正确答案
        formPanel.populateOptions(question.options, question.correctAnswers)
        
        // 设置为只读模式
        formPanel.setViewMode()
    }

    override fun createCenterPanel(): JComponent {
        // 修改：直接返回表单面板，与编辑界面保持一致
        // 不再使用JScrollPane包装，避免布局变化
        return formPanel.createFormPanel()
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }
}