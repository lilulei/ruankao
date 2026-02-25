package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.ExamType
import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 用户身份选择对话框
 * 使用级联下拉框选择考试级别和考试类型
 */
class UserIdentityDialog : DialogWrapper(true) {

    private lateinit var mainComboBox: JComboBox<String>
    private lateinit var displayField: JTextField

    // 存储当前选中状态
    private var currentSelection: ExamType? = null
    private var selectedExamLevel: String? = null

    init {
        title = "选择考试身份"
        init()
        // 默认选中软考高级-信息系统项目管理师
        setDefaultSelection("软考高级", ExamType.PROJECT_MANAGER)
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
        }

        // 当前身份标签
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        panel.add(JLabel("当前身份:"), gbc)

        // 创建级联下拉框
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(createCascadeComboBox(), gbc)

        return panel
    }

    /**
     * 创建级联下拉框
     */
    private fun createCascadeComboBox(): JComponent {
        // 创建显示文本框
        displayField = JTextField().apply {
            isEditable = false
            text = "软考高级 - 信息系统项目管理师"
        }

        // 创建下拉按钮
        val dropDownButton = JButton().apply {
            icon = UIManager.getIcon("ComboBox.buttonArrowIcon")
            isBorderPainted = false
            isContentAreaFilled = false
        }

        // 组装成组合框样式
        val comboBoxPanel = JPanel(BorderLayout(5, 0)).apply {
            border = UIManager.getBorder("ComboBox.border")
            add(displayField, BorderLayout.CENTER)
            add(dropDownButton, BorderLayout.EAST)
        }

        // 点击按钮显示级联弹出面板
        dropDownButton.addActionListener {
            showCascadePopup(comboBoxPanel)
        }
        displayField.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                showCascadePopup(comboBoxPanel)
            }
        })

        return comboBoxPanel
    }

    /**
     * 显示级联弹出面板
     */
    private fun showCascadePopup(owner: JComponent) {
        val popup = JPopupMenu()
        popup.isFocusable = true
        popup.border = BorderFactory.createLineBorder(Color.GRAY)

        // 创建主面板：左侧级别列表 + 右侧类型列表
        val mainPanel = JPanel(GridLayout(1, 2, 0, 0))

        // 左侧级别列表
        val levelList = listOf("软考高级", "软考中级", "软考初级")
        val levelPanel = JPanel(BorderLayout())
        levelPanel.border = EmptyBorder(5, 5, 5, 5)

        val levelListModel = DefaultListModel<String>().apply {
            levelList.forEach { addElement(it) }
        }
        val levelJList = JList(levelListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectedIndex = levelList.indexOf(selectedExamLevel ?: "软考高级")
            cellRenderer = DefaultListCellRenderer().apply {
                horizontalAlignment = SwingConstants.LEFT
            }
        }
        levelPanel.add(JScrollPane(levelJList), BorderLayout.CENTER)

        // 右侧类型列表
        val typePanel = JPanel(BorderLayout())
        typePanel.border = EmptyBorder(5, 5, 5, 5)

        val typeListModel = DefaultListModel<ExamType>()
        val typeJList = JList(typeListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is ExamType) {
                        text = value.displayName
                    }
                    return label
                }
            }
        }
        typePanel.add(JScrollPane(typeJList), BorderLayout.CENTER)

        // 组装左右面板
        mainPanel.add(levelPanel)
        mainPanel.add(typePanel)
        mainPanel.preferredSize = Dimension(450, 250)

        popup.add(mainPanel)
        popup.show(owner, 0, owner.height)

        // 更新右侧类型列表
        fun updateTypeList(level: String) {
            typeListModel.clear()
            val examTypes = getExamTypesByLevel(level)
            examTypes.forEach { typeListModel.addElement(it) }

            // 如果当前有选中类型，选中它；否则选第一个
            val currentType = currentSelection
            if (currentType != null && currentType in examTypes) {
                typeJList.setSelectedValue(currentType, true)
            } else if (examTypes.isNotEmpty()) {
                typeJList.selectedIndex = 0
            }
        }

        // 初始显示
        updateTypeList(levelJList.selectedValue)

        // 左侧级别选择监听 - 联动右侧类型
        levelJList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedLevel = levelJList.selectedValue
                if (selectedLevel != null) {
                    updateTypeList(selectedLevel)
                }
            }
        }

        // 右侧类型选择监听 - 选中后更新显示并关闭
        typeJList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val selectedType = typeJList.selectedValue
                    val selectedLevel = levelJList.selectedValue
                    if (selectedType != null && selectedLevel != null) {
                        currentSelection = selectedType
                        selectedExamLevel = selectedLevel
                        displayField.text = "$selectedLevel - ${selectedType.displayName}"
                        popup.isVisible = false
                    }
                }
            }
        })
    }

    /**
     * 根据级别获取对应的考试类型列表
     */
    private fun getExamTypesByLevel(level: String): List<ExamType> {
        return when (level) {
            "软考高级" -> listOf(
                ExamType.PROJECT_MANAGER,
                ExamType.SYSTEM_ANALYST,
                ExamType.SYSTEM_ARCHITECT,
                ExamType.NETWORK_PLANNER,
                ExamType.SYSTEM_PLANNING_MANAGER
            )
            "软考中级" -> listOf(
                ExamType.SYSTEM_INTEGRATION_ENGINEER,
                ExamType.NETWORK_ENGINEER,
                ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER,
                ExamType.SOFTWARE_TESTER,
                ExamType.DATABASE_ENGINEER,
                ExamType.MULTIMEDIA_DESIGNER,
                ExamType.SOFTWARE_DESIGNER,
                ExamType.INFORMATION_SYSTEM_SUPERVISOR,
                ExamType.E_COMMERCE_DESIGNER,
                ExamType.INFORMATION_SECURITY_ENGINEER,
                ExamType.EMBEDDED_SYSTEM_DESIGNER,
                ExamType.SOFTWARE_PROCESS_EVALUATOR,
                ExamType.COMPUTER_AIDED_DESIGNER,
                ExamType.COMPUTER_HARDWARE_ENGINEER,
                ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER
            )
            "软考初级" -> listOf(
                ExamType.PROGRAMMER,
                ExamType.NETWORK_ADMINISTRATOR,
                ExamType.INFORMATION_PROCESSING_TECHNICIAN,
                ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER,
                ExamType.MULTIMEDIA_APPLICATION_DESIGNER,
                ExamType.E_COMMERCE_TECHNICIAN,
                ExamType.WEB_DESIGNER
            )
            else -> emptyList()
        }
    }

    /**
     * 设置默认选中
     */
    private fun setDefaultSelection(level: String, examType: ExamType) {
        currentSelection = examType
        selectedExamLevel = level
        displayField.text = "$level - ${examType.displayName}"
    }

    /**
     * 获取用户选择的考试类型
     */
    fun getSelectedExamType(): ExamType? {
        return currentSelection
    }

    /**
     * 获取用户选择的级别
     */
    fun getSelectedExamLevel(): String? {
        return selectedExamLevel
    }
}
