package com.github.lilulei.ruankao.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.lilulei.ruankao.MyBundle

/**
 * 项目级服务类，用于提供软考刷题插件的核心功能
 * 该服务在项目级别运行，负责初始化和管理项目相关的业务逻辑
 *
 * @param project 当前IDE项目实例
 */
@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
        // 记录项目服务初始化日志和插件启动信息
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().info("软考刷题插件已启动")
    }

    /**
     * 获取一个1到100之间的随机整数
     * 用于生成随机题目编号或其他需要随机数值的场景
     *
     * @return 返回1到100之间的随机整数（包含边界值）
     */
    fun getRandomNumber() = (1..100).random()
}
