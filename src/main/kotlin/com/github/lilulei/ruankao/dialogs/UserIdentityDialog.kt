package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.ExamType
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * 用户身份选择对话框
 * 让用户选择软考级别和具体的考试类型
 */
class UserIdentityDialog : DialogWrapper(true) {
    private val levelComboBox = ComboBox<String>( arrayOf("软考高级", "软考中级", "软考初级")).apply {
        selectedItem = "软考高级"
    }
    
    private lateinit var examTypeComboBox: ComboBox<ExamType>
    
    init {
        title = "选择考试身份"
        initializeComponents()
        init()
    }
    
    private fun initializeComponents() {
        examTypeComboBox = ComboBox<ExamType>()
        updateExamTypes("软考高级")
        examTypeComboBox.selectedItem = ExamType.PROJECT_MANAGER // 默认选择软考高级的第一个
        examTypeComboBox.renderer = object : ListCellRenderer<ExamType> {
            override fun getListCellRendererComponent(
                list: JList<out ExamType>?,
                value: ExamType?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val label = JLabel(value?.displayName ?: "")
                if (isSelected) {
                    label.background = UIManager.getColor("ComboBox.selectionBackground")
                    label.foreground = UIManager.getColor("ComboBox.selectionForeground")
                } else {
                    label.background = UIManager.getColor("ComboBox.background")
                    label.foreground = UIManager.getColor("ComboBox.foreground")
                }
                label.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                return label
            }
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
        }

        // 级别选择
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        panel.add(JLabel("考试级别:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(levelComboBox, gbc)

        // 考试类型选择
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JLabel("考试类型:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.NONE
        panel.add(examTypeComboBox, gbc)

        // 添加事件监听器，实现二级联动
        levelComboBox.addActionListener {
            val selectedLevel = levelComboBox.selectedItem as String
            updateExamTypes(selectedLevel)
        }

        return panel
    }
    
    /**
     * 根据选择的级别更新考试类型下拉框
     */
    private fun updateExamTypes(level: String) {
        val examTypes = when (level) {
            "软考高级" -> arrayOf(
                ExamType.PROJECT_MANAGER,        // 信息系统项目管理师
                ExamType.SYSTEM_ANALYST,         // 系统分析师
                ExamType.SYSTEM_ARCHITECT,       // 系统架构设计师
                ExamType.NETWORK_PLANNER,        // 网络规划设计师
                ExamType.SYSTEM_PLANNING_MANAGER // 系统规划与管理师
            )
            "软考中级" -> arrayOf(
                ExamType.SYSTEM_INTEGRATION_ENGINEER,          // 系统集成项目管理工程师
                ExamType.NETWORK_ENGINEER,                     // 网络工程师
                ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER, // 信息系统管理工程师
                ExamType.SOFTWARE_TESTER,                      // 软件评测师
                ExamType.DATABASE_ENGINEER,                    // 数据库系统工程师
                ExamType.MULTIMEDIA_DESIGNER,                  // 多媒体应用设计师
                ExamType.SOFTWARE_DESIGNER,                    // 软件设计师
                ExamType.INFORMATION_SYSTEM_SUPERVISOR,        // 信息系统监理师
                ExamType.E_COMMERCE_DESIGNER,                  // 电子商务设计师
                ExamType.INFORMATION_SECURITY_ENGINEER,        // 信息安全工程师
                ExamType.EMBEDDED_SYSTEM_DESIGNER,             // 嵌入式系统设计师
                ExamType.SOFTWARE_PROCESS_EVALUATOR,           // 软件过程能力评估师
                ExamType.COMPUTER_AIDED_DESIGNER,              // 计算机辅助设计师
                ExamType.COMPUTER_HARDWARE_ENGINEER,           // 计算机硬件工程师
                ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER // 信息技术支持工程师
            )
            "软考初级" -> arrayOf(
                ExamType.PROGRAMMER,                          // 程序员
                ExamType.NETWORK_ADMINISTRATOR,               // 网络管理员
                ExamType.INFORMATION_PROCESSING_TECHNICIAN,   // 信息处理技术员
                ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER, // 信息系统运行管理员
                ExamType.MULTIMEDIA_APPLICATION_DESIGNER,     // 多媒体应用制作技术员
                ExamType.E_COMMERCE_TECHNICIAN,               // 电子商务技术员
                ExamType.WEB_DESIGNER                         // 网页制作员
            )
            else -> arrayOf(ExamType.SOFTWARE_DESIGNER) // 默认
        }
        
        examTypeComboBox.removeAllItems()
        examTypes.forEach { examType ->
            examTypeComboBox.addItem(examType)
        }
        
        // 设置默认选中第一个
        if (examTypes.isNotEmpty()) {
            examTypeComboBox.selectedItem = examTypes[0]
        }
    }
    
    /**
     * 获取用户选择的考试类型
     */
    fun getSelectedExamType(): ExamType? {
        return examTypeComboBox.selectedItem as? ExamType
    }
    
    /**
     * 获取用户选择的级别
     */
    fun getSelectedLevel(): String {
        return levelComboBox.selectedItem as String
    }
}