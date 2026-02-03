package com.github.lilulei.ruankao.actions

import com.github.lilulei.ruankao.dialogs.AddQuestionDialog
import com.github.lilulei.ruankao.dialogs.ChapterManagementDialog
import com.github.lilulei.ruankao.dialogs.ImportQuestionsDialog
import com.github.lilulei.ruankao.dialogs.QuestionManagementDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * 试题管理动作类
 * 提供添加和导入试题的菜单选项
 */
class ManageQuestionsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showExistingQuestionManagementDialog(project)
    }



    private fun showExistingQuestionManagementDialog(project: Project) {
        val dialog = QuestionManagementDialog(project)
        dialog.show()
    }

    private fun showTemplateExample(project: Project) {
        try {
            // 获取模板文件路径
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
            
            javax.swing.JOptionPane.showMessageDialog(
                null,
                message,
                "模板示例说明",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "无法显示模板信息：${e.message}",
                "错误",
                javax.swing.JOptionPane.ERROR_MESSAGE
            )
        }
    }
}