package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.utils.ExportUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * 导出对话框
 * 提供导出练习记录、错题本和学习统计的功能
 */
class ExportDialog(private val project: Project) : DialogWrapper(true) {
    private val exportUtil = ExportUtil()

    // UI组件
    private val fileField = JTextField(30)
    private val browseButton = JButton("浏览")
    private val exportTypeComboBox = JComboBox<String>(arrayOf("练习记录", "错题本", "学习统计"))
    private val exportResultLabel = JLabel(" ")

    init {
        title = "导出数据"
        init()
        
        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "选择导出文件路径"
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            
            val extension = when (exportTypeComboBox.selectedItem as String) {
                "练习记录" -> ".practice.csv"
                "错题本" -> ".wrong.csv"
                "学习统计" -> ".stats.csv"
                else -> ".csv"
            }
            
            val result = fileChooser.showSaveDialog(null)
            
            if (result == JFileChooser.APPROVE_OPTION) {
                var selectedFilePath = fileChooser.selectedFile.absolutePath
                
                // 确保文件有.csv扩展名
                if (!selectedFilePath.lowercase().endsWith(".csv")) {
                    selectedFilePath += ".csv"
                }
                
                fileField.text = selectedFilePath
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
        panel.add(JLabel("导出路径:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(fileField, gbc)

        gbc.gridx = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(browseButton, gbc)

        // 导出类型
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JLabel("导出类型:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.NONE
        panel.add(exportTypeComboBox, gbc)

        // 导出结果
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("导出结果:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(exportResultLabel, gbc)

        return panel
    }

    override fun doOKAction() {
        val filePath = fileField.text.trim()
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "请选择导出文件路径",
                "输入验证错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val selectedType = exportTypeComboBox.selectedItem as String
        val success = when (selectedType) {
            "练习记录" -> exportUtil.exportPracticeRecordsToCsv(project, filePath)
            "错题本" -> exportUtil.exportWrongQuestionsToCsv(project, filePath)
            "学习统计" -> exportUtil.exportLearningStatisticsToCsv(project, filePath)
            else -> false
        }

        if (success) {
            exportResultLabel.text = "导出成功: $filePath"
            exportResultLabel.foreground = java.awt.Color.GREEN
        } else {
            exportResultLabel.text = "导出失败"
            exportResultLabel.foreground = java.awt.Color.RED
        }
    }
}