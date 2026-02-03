package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.Question
import com.github.lilulei.ruankao.services.QuestionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.*

/**
 * 编辑试题对话框
 * 提供编辑现有试题的功能
 */
class EditQuestionDialog(private val project: Project, private val originalQuestion: Question) : DialogWrapper(true) {
    private val questionService = project.getService(QuestionService::class.java)
    private val formPanel = QuestionFormPanel(project, FormMode.EDIT, originalQuestion)

    init {
        title = "编辑试题 - ${originalQuestion.id}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return formPanel.createFormPanel()
    }

    override fun doValidate(): ValidationInfo? {
        val formData = formPanel.getFormData()
        if (formData == null) {
            return ValidationInfo("请填写所有必填项并确保至少有2个有效选项")
        }
        return super.doValidate()
    }

    fun getUpdatedQuestion(): Question? {
        val formData = formPanel.getFormData()
        if (formData == null) {
            return null
        }

        return originalQuestion.copy(
            title = formData.title,
            category = formData.category,
            examType = formData.examType,
            level = formData.level,
            chapter = formData.chapter,
            options = formData.options,
            correctAnswers = formData.correctAnswers,
            explanation = formData.explanation,
            year = formData.year
        )
    }
}