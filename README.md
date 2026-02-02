# ruankao

![Build](https://github.com/lilulei/ruankao/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
<h1>软考刷题助手</h1>

<p>这是一个专为准备软件水平考试（软考）的开发者设计的IntelliJ IDEA插件。它提供了丰富的刷题练习功能，帮助您在日常开发工作中随时进行软考备考。</p>

<h2>主要功能</h2>
<ul>
<li><strong>试题管理</strong>: 内置软考历年真题，支持按科目、知识点分类浏览</li>
<li><strong>刷题练习</strong>: 提供每日一练、专项练习、模拟考试、随机练习等多种模式</li>
<li><strong>错题本</strong>: 自动记录错题，支持智能复习策略</li>
<li><strong>学习统计</strong>: 提供详细的学习数据统计和可视化图表</li>
<li><strong>成就系统</strong>: 设置学习里程碑和成就徽章，激励持续学习</li>
</ul>
<!-- Plugin description end -->

## 项目功能列表

### 已完成的功能
- [x] 试题管理模块 - QuestionService
- [x] 练习模式 - PracticeService
- [x] 错题本功能 - WrongQuestionService
- [x] 学习统计 - LearningStatisticsService
- [x] 成就系统 - LearningStatisticsService
- [x] 多种练习模式（每日一练、专项练习、模拟考试、随机练习）
- [x] 实时UI更新 - 通过监听器模式实现
- [x] 数据持久化 - 使用PersistentStateComponent
- [x] 练习界面 - PracticeToolWindowFactory
- [x] 错题本界面 - WrongQuestionsToolWindowFactory
- [x] 项目启动活动 - MyProjectActivity

### 对应主要类文件

#### 模型层 (model/)
- **Question.kt** - 题目数据模型，包含题目标题、选项、正确答案等信息
- **WrongQuestionInfo.kt** - 错题信息数据模型，记录错误次数、掌握状态等
- **PracticeSession.kt** - 练习会话数据模型，跟踪当前练习进度
- **DailyPracticeRecord.kt** - 每日练习记录数据模型
- **LearningStatistics.kt** - 学习统计数据模型

#### 服务层 (services/)
- **QuestionService.kt** - 试题管理服务，负责题目的增删改查
- **PracticeService.kt** - 练习服务，管理练习会话和答题逻辑
- **WrongQuestionService.kt** - 错题本服务，管理错题信息和掌握状态
- **LearningStatisticsService.kt** - 学习统计服务，提供数据统计和成就系统
- **MyProjectService.kt** - 项目级服务基础类

#### 工具窗口 (toolWindow/)
- **PracticeToolWindowFactory.kt** - 练习工具窗口，提供多种练习模式入口
- **WrongQuestionsToolWindowFactory.kt** - 错题本工具窗口，展示错题列表和掌握情况
- **MyToolWindowFactory.kt** - 主工具窗口基础类

#### 启动项 (startup/)
- **MyProjectActivity.kt** - 项目启动活动，初始化插件组件

## 技术架构

### 技术栈
- **编程语言**: Kotlin
- **平台**: IntelliJ Platform
- **构建工具**: Gradle
- **Java版本**: Java 21
- **框架**: IntelliJ Platform Plugin SDK

### 核心架构特点

1. **分层架构**:
   - 模型层: 定义数据结构
   - 服务层: 处理业务逻辑和数据持久化
   - 界面层: 提供用户交互界面

2. **监听器模式**:
   - 实现了`WrongQuestionChangeListener`和`LearningStatisticsChangeListener`
   - 支持UI界面实时响应数据变化

3. **数据持久化**:
   - 使用IntelliJ平台的PersistentStateComponent接口
   - 数据存储在XML文件中

4. **插件集成**:
   - 通过plugin.xml注册工具窗口
   - 集成到IDE的工具窗口系统

### 设计模式
- **服务模式**: 使用@Service注解实现依赖注入
- **监听器模式**: 实现数据变更的实时响应
- **工厂模式**: ToolWindowFactory创建工具窗口
- **数据传输对象(DTO)**: 使用data class作为模型

## 项目结构分析

```
ruankao/
├── .github/                 # GitHub配置文件
│   └── workflows/           # CI/CD工作流配置
├── .run/                    # IDE运行配置
├── gradle/                  # Gradle构建脚本
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/github/lilulei/ruankao/
│   │   │       ├── model/           # 数据模型层
│   │   │       │   ├── DailyPracticeRecord.kt    # 每日练习记录
│   │   │       │   ├── LearningStatistics.kt     # 学习统计
│   │   │       │   ├── PracticeSession.kt        # 练习会话
│   │   │       │   ├── Question.kt               # 题目信息
│   │   │       │   └── WrongQuestionInfo.kt      # 错题信息
│   │   │       ├── services/        # 业务逻辑服务层
│   │   │       │   ├── LearningStatisticsService.kt  # 学习统计服务
│   │   │       │   ├── MyProjectService.kt           # 项目服务
│   │   │       │   ├── PracticeService.kt            # 练习服务
│   │   │       │   ├── QuestionService.kt            # 试题服务
│   │   │       │   └── WrongQuestionService.kt       # 错题服务
│   │   │       ├── startup/         # 启动配置
│   │   │       │   └── MyProjectActivity.kt          # 项目启动活动
│   │   │       └── toolWindow/      # UI界面层
│   │   │           ├── MyToolWindowFactory.kt        # 基础工具窗口
│   │   │           ├── PracticeToolWindowFactory.kt  # 练习工具窗口
│   │   │           └── WrongQuestionsToolWindowFactory.kt # 错题本工具窗口
│   │   └── resources/
│   │       ├── icons/               # 图标资源
│   │       │   └── exam.svg         # 插件图标
│   │       ├── messages/            # 国际化消息
│   │       │   └── MyBundle.properties
│   │       └── META-INF/            # 插件配置
│   │           └── plugin.xml       # 插件定义文件
│   ├── test/                        # 测试代码
│   │   ├── kotlin/                  # Kotlin测试代码
│   │   │   └── com/github/lilulei/ruankao/
│   │   │       └── MyPluginTest.kt  # 插件测试类
│   │   └── testData/                # 测试数据
│   │       └── rename/              # 重命名测试数据
└── testData_after.xml               # 测试数据示例
├── build.gradle.kts                 # 构建配置
├── gradle.properties                # Gradle属性配置
├── settings.gradle.kts              # 项目设置
├── CHANGELOG.md                     # 版本变更日志
└── README.md                       # 项目说明文档
```

## 开发指南

### 环境要求
- Java 21 或更高版本
- IntelliJ IDEA 2025.2.5 或更高版本
- Kotlin 插件

### 构建命令
```bash
# 编译项目
./gradlew build

# 运行IDE进行测试
./gradlew runIde

# 执行测试
./gradlew test

# 发布插件
./gradlew publishPlugin
```

### 测试策略
项目包含单元测试，位于`src/test/kotlin`目录下，主要测试：
- XML文件处理功能
- 项目服务功能
- 重命名功能

## 安装

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ruankao"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/lilulei/ruankao/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation