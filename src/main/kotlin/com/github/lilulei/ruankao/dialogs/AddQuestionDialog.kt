package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.QuestionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

/**
 * 添加试题对话框
 * 提供手动添加单个试题的界面
 */
class AddQuestionDialog(private val project: Project) : DialogWrapper(true) {
    private val questionService = project.getService(QuestionService::class.java)
    private val formPanel = QuestionFormPanel(project, FormMode.ADD)

    init {
        title = "添加新试题"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return formPanel.createFormPanel()
    }

    override fun doOKAction() {
        val formData = formPanel.getFormData()
        if (formData == null) {
            JOptionPane.showMessageDialog(
                this.window,
                "请填写所有必填项并确保至少有2个有效选项",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        // 创建并添加新题目
        val newQuestion = Question(
            id = generateQuestionId(),
            title = formData.title,
            category = formData.category,
            examType = formData.examType,
            examLevel = formData.examLevel,
            level = formData.level,
            chapter = formData.chapter,
            options = formData.options,
            correctAnswers = formData.correctAnswers,
            explanation = formData.explanation,
            year = formData.year
        )

        questionService.addQuestion(newQuestion)
        super.doOKAction()
    }

    private fun generateQuestionId(): String {
        return "custom_${System.currentTimeMillis()}"
    }

    fun getAddedQuestion(): Question? {
        val formData = formPanel.getFormData()
        if (formData == null) {
            return null
        }

        return Question(
            id = generateQuestionId(),
            title = formData.title,
            category = formData.category,
            examType = formData.examType,
            examLevel = formData.examLevel,
            level = formData.level,
            chapter = formData.chapter,
            options = formData.options,
            correctAnswers = formData.correctAnswers,
            explanation = formData.explanation,
            year = formData.year
        )
    }
}
