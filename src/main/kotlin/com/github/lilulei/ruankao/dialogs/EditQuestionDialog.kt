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
    private val formPanel = QuestionFormPanel(project, FormMode.EDIT, originalQuestion)

    init {
        title = "编辑试题 - ${originalQuestion.id}"
        init()
        populateQuestionData()
    }

    private fun populateQuestionData() {
        // 填充表单数据
        formPanel.titleField.text = originalQuestion.title
        // 考试级别和考试类型现在通过QuestionFormPanel的populateFormData方法自动填充
        formPanel.levelComboBox.selectedItem = originalQuestion.level.displayName
        formPanel.explanationArea.text = originalQuestion.explanation

        // 设置章节
        var chapterIndex: Int? = null
        val model = formPanel.chapterComboBox.model as DefaultComboBoxModel<String>
        for (i in 0 until model.size) {
            if (model.getElementAt(i) == originalQuestion.chapter) {
                chapterIndex = i
                break
            }
        }
        if (chapterIndex != null && chapterIndex >= 0) {
            formPanel.chapterComboBox.selectedIndex = chapterIndex
        }

        // 设置日期
        val calendar = java.util.Calendar.getInstance()
        calendar.set(originalQuestion.year.year, originalQuestion.year.monthValue - 1, originalQuestion.year.dayOfMonth)
        formPanel.dateSpinner.value = calendar.time

        // 填充选项和正确答案
        formPanel.populateOptions(originalQuestion.options, originalQuestion.correctAnswers)
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