package com.github.lilulei.ruankao.services

import com.github.lilulei.ruankao.model.ExamType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.jdom.Element

/**
 * 用户身份服务类
 * 用于保存和管理用户选择的考试身份
 */
@State(name = "UserIdentityService", storages = [Storage("user_identity.xml")])
@Service(Service.Level.PROJECT)
class UserIdentityService : PersistentStateComponent<Element> {
    private var selectedExamType: ExamType = ExamType.SOFTWARE_DESIGNER
    private var selectedLevel: String = "软考高级"
    private var hasUserMadeSelection: Boolean = false
    private var defaultChapter: String = "默认章节"  // 新增：默认章节字段
    
    companion object {
        fun getInstance(project: Project): UserIdentityService {
            return project.getService(UserIdentityService::class.java)
        }
    }
    
    override fun getState(): Element {
        val element = Element("UserIdentityService")
        element.setAttribute("selectedExamType", selectedExamType.name)
        element.setAttribute("selectedLevel", selectedLevel)
        element.setAttribute("hasUserMadeSelection", hasUserMadeSelection.toString())
        element.setAttribute("defaultChapter", defaultChapter)  // 新增：保存默认章节
        return element
    }
    
    override fun loadState(state: Element) {
        val examTypeStr = state.getAttributeValue("selectedExamType") ?: ExamType.SOFTWARE_DESIGNER.name
        val levelStr = state.getAttributeValue("selectedLevel") ?: "软考高级"
        val hasUserMadeSelectionStr = state.getAttributeValue("hasUserMadeSelection") ?: "false"
        val defaultChapterStr = state.getAttributeValue("defaultChapter") ?: "默认章节"  // 新增：加载默认章节
        
        // 尝试通过名称匹配考试类型
        selectedExamType = try {
            ExamType.valueOf(examTypeStr)
        } catch (e: IllegalArgumentException) {
            // 如果找不到对应的枚举值，使用默认值
            ExamType.SOFTWARE_DESIGNER
        }
        
        selectedLevel = levelStr
        defaultChapter = defaultChapterStr  // 新增：设置默认章节
        
        // 恢复用户选择状态
        hasUserMadeSelection = hasUserMadeSelectionStr.toBoolean()
    }
    
    /**
     * 设置用户选择的考试类型
     */
    fun setSelectedExamType(examType: ExamType) {
        this.selectedExamType = examType
        // 同时更新级别
        this.selectedLevel = when (examType) {
            ExamType.SYSTEM_ANALYST -> "软考高级"
            ExamType.SYSTEM_ARCHITECT -> "软考高级"
            ExamType.NETWORK_PLANNER -> "软考高级"
            ExamType.PROJECT_MANAGER -> "软考高级"
            ExamType.SYSTEM_PLANNING_MANAGER -> "软考高级"
            
            ExamType.SYSTEM_INTEGRATION_ENGINEER -> "软考中级"
            ExamType.NETWORK_ENGINEER -> "软考中级"
            ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER -> "软考中级"
            ExamType.SOFTWARE_TESTER -> "软考中级"
            ExamType.DATABASE_ENGINEER -> "软考中级"
            ExamType.MULTIMEDIA_DESIGNER -> "软考中级"
            ExamType.SOFTWARE_DESIGNER -> "软考中级"
            ExamType.INFORMATION_SYSTEM_SUPERVISOR -> "软考中级"
            ExamType.E_COMMERCE_DESIGNER -> "软考中级"
            ExamType.INFORMATION_SECURITY_ENGINEER -> "软考中级"
            ExamType.EMBEDDED_SYSTEM_DESIGNER -> "软考中级"
            ExamType.SOFTWARE_PROCESS_EVALUATOR -> "软考中级"
            ExamType.COMPUTER_AIDED_DESIGNER -> "软考中级"
            ExamType.COMPUTER_HARDWARE_ENGINEER -> "软考中级"
            ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER -> "软考中级"
            
            ExamType.PROGRAMMER -> "软考初级"
            ExamType.NETWORK_ADMINISTRATOR -> "软考初级"
            ExamType.INFORMATION_PROCESSING_TECHNICIAN -> "软考初级"
            ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER -> "软考初级"
            ExamType.MULTIMEDIA_APPLICATION_DESIGNER -> "软考初级"
            ExamType.E_COMMERCE_TECHNICIAN -> "软考初级"
            ExamType.WEB_DESIGNER -> "软考初级"
        }
        // 根据考试类型设置默认章节
        this.defaultChapter = when (examType) {
            ExamType.SYSTEM_ANALYST -> "系统分析知识域"
            ExamType.SYSTEM_ARCHITECT -> "系统架构知识域"
            ExamType.NETWORK_PLANNER -> "网络规划知识域"
            ExamType.PROJECT_MANAGER -> "项目管理知识域"
            ExamType.SYSTEM_PLANNING_MANAGER -> "系统规划知识域"
            
            ExamType.SYSTEM_INTEGRATION_ENGINEER -> "系统集成知识域"
            ExamType.NETWORK_ENGINEER -> "网络工程知识域"
            ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER -> "信息系统管理知识域"
            ExamType.SOFTWARE_TESTER -> "软件测试知识域"
            ExamType.DATABASE_ENGINEER -> "数据库知识域"
            ExamType.MULTIMEDIA_DESIGNER -> "多媒体设计知识域"
            ExamType.SOFTWARE_DESIGNER -> "软件设计知识域"
            ExamType.INFORMATION_SYSTEM_SUPERVISOR -> "信息系统监督知识域"
            ExamType.E_COMMERCE_DESIGNER -> "电子商务知识域"
            ExamType.INFORMATION_SECURITY_ENGINEER -> "信息安全知识域"
            ExamType.EMBEDDED_SYSTEM_DESIGNER -> "嵌入式系统知识域"
            ExamType.SOFTWARE_PROCESS_EVALUATOR -> "软件过程评估知识域"
            ExamType.COMPUTER_AIDED_DESIGNER -> "计算机辅助设计知识域"
            ExamType.COMPUTER_HARDWARE_ENGINEER -> "计算机硬件知识域"
            ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER -> "信息技术支持知识域"
            
            ExamType.PROGRAMMER -> "程序员知识域"
            ExamType.NETWORK_ADMINISTRATOR -> "网络管理员知识域"
            ExamType.INFORMATION_PROCESSING_TECHNICIAN -> "信息处理技术员知识域"
            ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER -> "信息系统运维管理知识域"
            ExamType.MULTIMEDIA_APPLICATION_DESIGNER -> "多媒体应用设计知识域"
            ExamType.E_COMMERCE_TECHNICIAN -> "电子商务技术员知识域"
            ExamType.WEB_DESIGNER -> "网页设计知识域"
        }
        // 标记用户已做出选择
        this.hasUserMadeSelection = true
    }
    
    /**
     * 获取用户选择的考试类型
     */
    fun getSelectedExamType(): ExamType {
        return selectedExamType
    }
    
    /**
     * 获取用户选择的级别
     */
    fun getSelectedLevel(): String {
        return selectedLevel
    }
    
    /**
     * 获取默认章节
     */
    fun getDefaultChapter(): String {
        return defaultChapter
    }
    
    /**
     * 检查是否已选择身份
     */
    fun isIdentitySelected(): Boolean {
        return hasUserMadeSelection
    }
}