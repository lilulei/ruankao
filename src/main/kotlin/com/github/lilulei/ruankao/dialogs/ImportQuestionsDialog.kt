package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.utils.QuestionImportUtil
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * 导入试题对话框
 * 提供从JSON、XML文件批量导入试题的功能
 */
class ImportQuestionsDialog(private val project: Project) : DialogWrapper(true) {
    private val questionService = project.getService(QuestionService::class.java)
    private val importUtil = QuestionImportUtil()

    // UI组件
    private val fileField = JTextField(30)
    private val browseButton = JButton("浏览")
    private val formatComboBox = JComboBox<String>(arrayOf("JSON", "XML"))
    private val importResultLabel = JLabel(" ")

    init {
        title = "导入试题"
        init()
        
        browseButton.addActionListener {
            val chooser = FileChooserFactory.getInstance().createFileChooser(
                com.intellij.openapi.fileChooser.FileChooserDescriptor(true, false, false, false, false, false),
                null, null
            )
            val fileChooser = chooser.choose(project, null)
            if (fileChooser.isNotEmpty()) {
                val selectedFile = fileChooser[0]
                fileField.text = selectedFile.path
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
        }

        // 文件路径
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        panel.add(JLabel("文件路径:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(fileField, gbc)

        gbc.gridx = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(browseButton, gbc)

        // 文件格式
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JLabel("文件格式:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.NONE
        panel.add(formatComboBox, gbc)

        // 导入结果
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("导入结果:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(importResultLabel, gbc)

        return panel
    }

    override fun doOKAction() {
        val filePath = fileField.text.trim()
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "请选择要导入的文件",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val selectedFormat = formatComboBox.selectedItem as String
        val importedCount = when (selectedFormat.uppercase()) {
            "JSON" -> importUtil.importQuestionsFromJson(filePath, questionService)
            "XML" -> importUtil.importQuestionsFromXml(filePath, questionService)
            else -> 0
        }

        if (importedCount > 0) {
            importResultLabel.text = "成功导入 $importedCount 遶题目"
            importResultLabel.foreground = java.awt.Color.GREEN
            // 刷新服务中的试题列表
            questionService.refreshQuestions()
        } else {
            importResultLabel.text = "导入失败或没有新题目被添加"
            importResultLabel.foreground = java.awt.Color.RED
        }
    }

    fun getImportedCount(): Int {
        val filePath = fileField.text.trim()
        if (filePath.isEmpty()) return 0

        val selectedFormat = formatComboBox.selectedItem as String
        return when (selectedFormat.uppercase()) {
            "JSON" -> importUtil.importQuestionsFromJson(filePath, questionService)
            "XML" -> importUtil.importQuestionsFromXml(filePath, questionService)
            else -> 0
        }
    }
}