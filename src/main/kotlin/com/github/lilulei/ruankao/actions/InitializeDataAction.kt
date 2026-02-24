package com.github.lilulei.ruankao.actions

import com.github.lilulei.ruankao.dialogs.DataInitializationDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * 数据初始化动作类
 * 提供清空学习统计和错题本数据的功能
 */
class InitializeDataAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showInitializeDialog(project)
    }

    private fun showInitializeDialog(project: Project) {
        val dialog = DataInitializationDialog(project)
        dialog.show()
    }
}