package com.github.lilulei.ruankao.dialogs

import com.github.lilulei.ruankao.model.KnowledgeChapter
import com.github.lilulei.ruankao.services.KnowledgeChapterService
import com.github.lilulei.ruankao.services.QuestionService
import com.github.lilulei.ruankao.services.UserIdentityService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*

/**
 * 章节管理对话框
 * 提供章节的查看、编辑、删除功能
 */
class ChapterManagementDialog(private val project: Project) : DialogWrapper(true) {
    private val chapterService = project.getService(KnowledgeChapterService::class.java)
    private val questionService = project.getService(QuestionService::class.java)
    private val userIdentityService = project.getService(UserIdentityService::class.java)
    
    private val chapterListModel = DefaultListModel<String>()
    private val chapterList = JList(chapterListModel)
    private val addButton = JButton("添加章节")
    private val editButton = JButton("编辑章节")
    private val deleteButton = JButton("删除章节")
    private val refreshButton = JButton("刷新")

    init {
        title = "章节管理"
        initializeComponents()
        updateChapterList()
        init()
    }

    private fun initializeComponents() {
        chapterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        chapterList.layoutOrientation = JList.VERTICAL
        
        val listScrollPane = JScrollPane(chapterList)
        listScrollPane.preferredSize = Dimension(250, 200)

        // 添加按钮事件
        addButton.addActionListener {
            handleAddChapter()
        }

        editButton.addActionListener {
            handleEditChapter()
        }

        deleteButton.addActionListener {
            handleDeleteChapter()
        }

        refreshButton.addActionListener {
            updateChapterList()
        }

        // 禁用编辑和删除按钮，直到选中一个章节
        chapterList.addListSelectionListener {
            if (!chapterList.valueIsAdjusting) {
                val selected = chapterList.selectedValue != null
                editButton.isEnabled = selected
                deleteButton.isEnabled = selected
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        // 列表面板
        val listPanel = JPanel(BorderLayout())
        listPanel.border = BorderFactory.createTitledBorder("知识点章节列表")
        
        val listScrollPane = JScrollPane(chapterList)
        listScrollPane.preferredSize = Dimension(250, 200)
        listPanel.add(listScrollPane, BorderLayout.CENTER)

        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        buttonPanel.add(addButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(refreshButton)

        // 将组件添加到主面板
        panel.add(listPanel, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun updateChapterList() {
        chapterListModel.clear()
        val currentLevel = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel()
        } else {
            "软考高级"  // 默认级别
        }
        val currentExamType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType().displayName
        } else {
            "信息系统项目管理师"  // 默认考试类型
        }
        
        // 显示当前身份下的章节
        chapterService.getChapterNamesByIdentity(currentLevel, currentExamType).sorted().forEach { name ->
            chapterListModel.addElement(name)
        }
    }

    private fun handleAddChapter() {
        val currentLevel = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedLevel()
        } else {
            "软考高级"  // 默认级别
        }
        val currentExamType = if (userIdentityService.isIdentitySelected()) {
            userIdentityService.getSelectedExamType().displayName
        } else {
            "信息系统项目管理师"  // 默认考试类型
        }
        
        val newChapterNameObj = JOptionPane.showInputDialog(
            this.window,
            "请输入新章节名称:",
            "添加章节",
            JOptionPane.QUESTION_MESSAGE
        )

        if (newChapterNameObj != null) {
            val newChapterName = newChapterNameObj.toString()
            val trimmedName = newChapterName.trim { it <= ' ' }
            if (trimmedName.length > 0) {
                // 检查当前身份下章节名称是否已存在
                if (chapterService.chapterNameExists(trimmedName, currentLevel, currentExamType)) {
                    JOptionPane.showMessageDialog(
                        this.window,
                        "该身份下章节名称已存在，请使用不同的名称。",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return
                }

                val newChapter = KnowledgeChapter(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    level = currentLevel,  // 直接使用当前用户身份的级别
                    examType = currentExamType  // 直接使用当前用户身份的考试类型
                )
                chapterService.addChapter(newChapter)
                updateChapterList()
                JOptionPane.showMessageDialog(
                    this.window,
                    "章节 \"$trimmedName\" 已添加成功。",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    }

    private fun handleEditChapter() {
        val selectedChapter = chapterList.selectedValue as? String
        if (selectedChapter == null) {
            JOptionPane.showMessageDialog(
                this.window,
                "请先选择一个章节进行编辑。",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val chapter = chapterService.getChapterByName(selectedChapter)
        if (chapter == null) {
            JOptionPane.showMessageDialog(
                this.window,
                "找不到选定的章节。",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val levels = arrayOf("软考高级", "软考中级", "软考初级", "全部级别", "保持原有设置")
        val currentLevel = chapter.level ?: "全部级别"
        val currentExamType = chapter.examType ?: "全部类型"
        val levelChoice = JOptionPane.showInputDialog(
            this.window,
            "请选择章节适用级别:",
            "编辑章节 - 选择级别",
            JOptionPane.QUESTION_MESSAGE,
            null,
            levels,
            currentLevel
        ) as? String

        if (levelChoice != null) {
            val level = when (levelChoice) {
                "全部级别" -> null
                "保持原有设置" -> chapter.level
                else -> levelChoice
            }

            val examTypeChoice = JOptionPane.showInputDialog(
                this.window,
                "请输入考试类型（如信息系统项目管理师）:",
                "编辑章节 - 输入考试类型",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                currentExamType
            ) as? String

            val examType = when (examTypeChoice) {
                "全部类型" -> null
                "保持原有设置" -> chapter.examType
                else -> examTypeChoice
            }

            val newChapterNameObj = JOptionPane.showInputDialog(
                this.window,
                "请输入新的章节名称:",
                "编辑章节",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                selectedChapter
            )

            if (newChapterNameObj != null) {
                val newChapterName = newChapterNameObj.toString()
                val trimmedName = newChapterName.trim { it <= ' ' }
                
                if (trimmedName.length > 0 && newChapterName != selectedChapter) {
                    // 检查新名称在同身份下是否已存在
                    if (chapterService.chapterNameExists(trimmedName, level, examType)) {
                        JOptionPane.showMessageDialog(
                            this.window,
                            "该身份下章节名称已存在，请使用不同的名称。",
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                        )
                        return
                    }

                    if (chapter != null) {
                        val updatedChapter = chapter.copy(
                            name = trimmedName,
                            level = level ?: chapter.level,
                            examType = examType ?: chapter.examType,
                            updatedAt = System.currentTimeMillis()
                        )
                        chapterService.updateChapter(updatedChapter)
                        updateChapterList()
                        JOptionPane.showMessageDialog(
                            this.window,
                            "章节 \"$selectedChapter\" 已更新为 \"$newChapterName\"。",
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                } else if ((level != null && level != "保持原有设置" && level != chapter.level) ||
                          (examType != null && examType != "保持原有设置" && examType != chapter.examType)) {
                    // 如果只是更改级别或考试类型而不更改名称
                    val updatedChapter = chapter.copy(
                        level = level ?: chapter.level,
                        examType = examType ?: chapter.examType,
                        updatedAt = System.currentTimeMillis()
                    )
                    chapterService.updateChapter(updatedChapter)
                    updateChapterList()
                    JOptionPane.showMessageDialog(
                        this.window,
                        "章节 \"$selectedChapter\" 的适用级别或考试类型已更新。",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }
    }

    private fun handleDeleteChapter() {
        val selectedChapter = chapterList.selectedValue as? String
        if (selectedChapter == null) {
            JOptionPane.showMessageDialog(
                this.window,
                "请先选择一个章节进行删除。",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        // 检查该章节下是否已绑定试题
        val questionsInChapter = questionService.allQuestionsList.filter { 
            it.chapter != null && it.chapter.equals(selectedChapter, ignoreCase = true) 
        }
        
        if (questionsInChapter.isNotEmpty()) {
            JOptionPane.showMessageDialog(
                this.window,
                "章节 \"$selectedChapter\" 下存在 ${questionsInChapter.size} 道试题，无法删除。\n" +
                "请先将这些试题移至其他章节或删除这些试题后再尝试删除章节。",
                "删除失败",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val confirmed = JOptionPane.showConfirmDialog(
            this.window,
            "确定要删除章节 \"$selectedChapter\" 吗？\n此操作不可撤销。",
            "确认删除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirmed == JOptionPane.YES_OPTION) {
            val chapter = chapterService.getChapterByName(selectedChapter)
            if (chapter != null) {
                val deleted = chapterService.removeChapter(chapter.id, questionService)
                if (deleted) {
                    updateChapterList()
                    JOptionPane.showMessageDialog(
                        this.window,
                        "章节 \"$selectedChapter\" 已成功删除。",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        this.window,
                        "删除章节 \"$selectedChapter\" 时发生错误。",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }
}