package com.github.lilulei.ruankao.actions

import com.github.lilulei.ruankao.dialogs.ExportDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * 导出动作类
 * 提供导出练习记录、错题本和学习统计的功能
 */
class ExportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showExportDialog(project)
    }

    private fun showExportDialog(project: Project) {
        val dialog = ExportDialog(project)
        dialog.show()
    }
}