package com.github.lilulei.ruankao.actions

import com.github.lilulei.ruankao.dialogs.UserIdentityDialog
import com.github.lilulei.ruankao.services.UserIdentityService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * 更改用户身份的动作类
 * 允许用户更改他们选择的考试级别和类型
 */
class ChangeIdentityAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val dialog = UserIdentityDialog()
        if (dialog.showAndGet()) {
            val selectedExamType = dialog.getSelectedExamType()
            if (selectedExamType != null) {
                val userIdentityService = project.getService(UserIdentityService::class.java)
                userIdentityService.setSelectedExamType(selectedExamType)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
    }
}