# 试题JSON模板说明

## 概述
此模板用于软考刷题插件的试题批量导入功能。您可以按照此模板格式创建JSON文件，然后通过插件的"试题管理"功能导入试题。

## JSON结构说明

每个试题对象包含以下字段：

- `id`: 试题唯一标识符（字符串）
- `title`: 试题标题或内容（字符串）
- `options`: 选项集合（对象），键为选项标识（如"A"、"B"），值为选项内容
- `correctAnswers`: 正确答案集合（数组），支持多选题的多个答案
- `explanation`: 试题解析说明（字符串）
- `level`: 难度等级（字符串）- "EASY", "MEDIUM", "HARD"
- `category`: 试题分类（字符串）
- `year`: 考试年份（数字）
- `examType`: 考试类型（字符串）- "SOFTWARE_DESIGNER", "NETWORK_ENGINEER", "DATABASE_ENGINEER", "SYSTEM_ANALYST"

## 使用方法

1. 按照模板格式创建您的试题JSON文件
2. 在IDE中使用快捷键 `Ctrl+Alt+Q` 打开试题管理功能
3. 选择"批量导入试题"
4. 选择您创建的JSON文件
5. 点击"导入"完成操作

## 示例

请参见 `questions_template.json` 文件，其中包含了不同难度、不同分类和不同考试类型的试题示例。

## 注意事项

- 试题ID必须唯一，重复ID的试题将被忽略
- 选项键和正确答案必须匹配
- 支持单选题和多选题
- 难度等级和考试类型必须使用指定的枚举值