package com.github.lilulei.ruankao.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.lilulei.ruankao.MyBundle
import com.github.lilulei.ruankao.services.MyProjectService
import javax.swing.JButton


/**
 * 软考刷题工具窗口工厂类，用于创建和管理自定义工具窗口
 */
class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().info("软考刷题工具窗口初始化")
    }

    /**
     * 创建工具窗口内容
     *
     * @param project 当前项目实例
     * @param toolWindow 工具窗口实例
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    /**
     * 判断工具窗口是否应该可用
     *
     * @param project 当前项目实例
     * @return 始终返回false，表示不再显示旧窗口
     */
    override fun shouldBeAvailable(project: Project) = false // 不再显示旧窗口

    /**
     * 软考刷题工具窗口内部类，负责构建窗口UI组件
     *
     * @param toolWindow 工具窗口实例
     */
    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        /**
         * 获取工具窗口的内容面板
         *
         * @return 返回包含标签和按钮的JBPanel组件
         */
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            // 添加随机数按钮，点击时更新标签显示的随机数
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
