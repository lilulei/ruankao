# 软考刷题插件 - 考试级别功能实现说明

## 功能概述

本次更新为软考刷题插件添加了完整的考试级别支持，实现了考试级别与考试类型的联动功能。

## 主要变更

### 1. 数据模型增强

**文件：** `src/main/kotlin/com/github/lilulei/ruankao/model/Question.kt`

- 新增 `ExamLevel` 枚举，支持三个考试级别：
  - `SENIOR` ("软考高级")
  - `INTERMEDIATE` ("软考中级") 
  - `JUNIOR` ("软考初级")
- 在 `Question` 数据类中添加 `examLevel: ExamLevel` 字段

### 2. 用户身份服务优化

**文件：** `src/main/kotlin/com/github/lilulei/ruankao/services/UserIdentityService.kt`

- 修改存储结构，使用 `ExamLevel` 枚举替代字符串
- 新增以下公共方法：
  - `getDefaultExamTypeForLevel(level: ExamLevel): ExamType` - 获取指定级别的默认考试类型
  - `getExamTypesForLevel(level: ExamLevel): List<ExamType>` - 获取指定级别的所有考试类型
- 完善考试类型与考试级别的双向映射关系

### 3. 试题表单界面升级

**文件：** `src/main/kotlin/com/github/lilulei/ruankao/dialogs/QuestionFormPanel.kt`

- 替换原有的标签显示为下拉选择框
- 实现考试级别与考试类型的联动逻辑：
  - 选择考试级别时，自动更新考试类型下拉框内容
  - 自动选择该级别下的默认考试类型
  - 考试类型变化时自动更新章节列表
- 添加事件监听器处理联动逻辑

### 4. 章节管理功能增强

**文件：** 
- `src/main/kotlin/com/github/lilulei/ruankao/services/KnowledgeChapterService.kt`
- `src/main/kotlin/com/github/lilulei/ruankao/dialogs/ChapterManagementDialog.kt`

- 章节管理对话框添加考试级别筛选功能
- 支持按考试级别查看和管理章节
- 完善章节名称重复检查逻辑

### 5. 导入导出功能扩展

**文件：**
- `src/main/kotlin/com/github/lilulei/ruankao/utils/QuestionImportUtil.kt`
- `src/main/kotlin/com/github/lilulei/ruankao/utils/ExportUtil.kt`

- 导入功能支持解析考试级别字段
- 导出功能支持包含考试级别信息
- 完善XML和JSON格式的数据处理

## 功能特性

### 考试级别联动机制

1. **级别选择驱动类型更新**：当用户选择不同考试级别时，考试类型下拉框会自动更新为该级别对应的所有考试类型
2. **默认值智能设置**：每个级别都有预设的默认考试类型
3. **章节动态关联**：考试类型变化时，相关的知识点章节会自动更新

### 级别与类型映射关系

**软考高级 (SENIOR)**：
- 信息系统项目管理师 (PROJECT_MANAGER) ← 默认
- 系统分析师 (SYSTEM_ANALYST)
- 系统架构设计师 (SYSTEM_ARCHITECT)
- 网络规划设计师 (NETWORK_PLANNER)
- 系统规划与管理师 (SYSTEM_PLANNING_MANAGER)

**软考中级 (INTERMEDIATE)**：
- 软件设计师 (SOFTWARE_DESIGNER) ← 默认
- 网络工程师 (NETWORK_ENGINEER)
- 数据库系统工程师 (DATABASE_ENGINEER)
- 系统集成项目管理工程师 (SYSTEM_INTEGRATION_ENGINEER)
- 以及其他12个中级考试类型

**软考初级 (JUNIOR)**：
- 程序员 (PROGRAMMER) ← 默认
- 网络管理员 (NETWORK_ADMINISTRATOR)
- 信息处理技术员 (INFORMATION_PROCESSING_TECHNICIAN)
- 以及其他4个初级考试类型

## 使用说明

### 添加试题时的操作流程

1. 选择考试级别（软考高级/中级/初级）
2. 系统自动更新考试类型下拉框内容
3. 系统自动选择该级别的默认考试类型
4. 系统根据选择的考试类型更新可用的章节列表
5. 填写其他试题信息并保存

### 章节管理操作

1. 在章节管理对话框中选择目标考试级别
2. 查看该级别下的所有章节
3. 可以添加、编辑、删除相应级别的章节
4. 系统会防止跨级别重名章节的创建

## 技术实现要点

### 数据一致性保证
- 所有考试类型都严格映射到对应的考试级别
- 章节管理系统支持按级别和类型双重过滤
- 导入导出功能保持数据完整性

### 用户体验优化
- 界面布局遵循考试级别优先的原则
- 联动操作流畅自然，减少用户操作步骤
- 提供清晰的视觉反馈和提示信息

### 向后兼容性
- 保留现有数据结构的兼容性
- 旧版本数据可以平滑升级到新版本
- 提供合理的默认值处理机制

## 测试验证

创建了专门的测试类 `ExamLevelTest.kt` 来验证：
- 枚举值的正确性
- 级别与考试类型的映射关系
- 联动逻辑的准确性
- 边界条件的处理

## 注意事项

1. 系统默认考试级别为"软考高级"
2. 每个级别都有预设的默认考试类型
3. 章节名称在同一级别内必须唯一
4. 考试级别的改变会影响相关联的考试类型和章节选择