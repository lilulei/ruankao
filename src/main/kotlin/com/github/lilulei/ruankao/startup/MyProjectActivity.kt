package com.github.lilulei.ruankao.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * 项目活动监听器类，用于在项目启动时执行相关操作
 */
class MyProjectActivity : ProjectActivity {

    /**
     * 在项目启动时执行的方法
     *
     * @param project 当前启动的IntelliJ项目实例
     * @return 无返回值（Unit）
     */
    override suspend fun execute(project: Project) {
        // 记录软考刷题插件启动日志
        thisLogger().info("软考刷题插件启动")
    }
}
