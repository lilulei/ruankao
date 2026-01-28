package com.github.lilulei.ruankao.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.lilulei.ruankao.MyBundle

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().info("软考刷题插件已启动")
    }

    fun getRandomNumber() = (1..100).random()
}