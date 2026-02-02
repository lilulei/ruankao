package com.github.lilulei.ruankao.actions

import com.github.lilulei.ruankao.dialogs.AddQuestionDialog
import com.github.lilulei.ruankao.dialogs.ChapterManagementDialog
import com.github.lilulei.ruankao.dialogs.ImportQuestionsDialog
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
        showQuestionManagementDialog(project)
    }

    private fun showQuestionManagementDialog(project: Project) {
        val options = arrayOf("添加单个试题", "批量导入试题", "管理知识点章节", "取消")
        val choice = javax.swing.JOptionPane.showOptionDialog(
            null,
            "请选择试题管理操作：",
            "试题管理",
            javax.swing.JOptionPane.DEFAULT_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )

        when (choice) {
            0 -> showAddQuestionDialog(project)
            1 -> showImportQuestionsDialog(project)
            2 -> showChapterManagementDialog(project)
            else -> return
        }
    }

    private fun showAddQuestionDialog(project: Project) {
        val dialog = AddQuestionDialog(project)
        if (dialog.showAndGet()) {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "试题添加成功！",
                "成功",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun showImportQuestionsDialog(project: Project) {
        val dialog = ImportQuestionsDialog(project)
        if (dialog.showAndGet()) {
            // 导入结果已经在对话框中显示
        }
    }

    private fun showChapterManagementDialog(project: Project) {
        val dialog = ChapterManagementDialog(project)
        dialog.show()
    }
}