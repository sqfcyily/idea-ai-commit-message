# AI Commit Message - IntelliJ IDEA Plugin

[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/platform-IntelliJ%20IDEA%202026.1+-blue.svg)]()
[![JDK](https://img.shields.io/badge/JDK-17%20%2F%2021-orange.svg)]()

一个支持多模型、可高度自定义的 IntelliJ IDEA Git 自动提交信息生成插件。通过接入主流大语言模型，根据你在提交面板中勾选的文件差异（Diff）自动生成简洁、规范、可定制风格的 Commit Message。

---

## 🚀 主要功能

1. **变动 Diff 自动分析**
   自动提取当前 Commit 面板中勾选的文件，剔除二进制文件，利用 IntelliJ 平台内存安全的 `ComparisonManager` 生成统一差异文本（Unified Diff）发送给大模型。
2. **多大语言模型（LLM）支持**
   * **Gemini** (默认提供 `gemini-1.5-flash`)
   * **OpenAI** (默认提供 `gpt-4o-mini`)
   * **Ollama** (支持本地离线运行的模型，如 `llama3` 等)
   * **Custom** (支持任何兼容 OpenAI 接口格式的自定义大模型终点，例如 **DeepSeek**、各种中转分发 API 等)
3. **支持自定义 API Base URL**
   支持中转接口及本地离线服务，解决特殊网络环境下的接口连接和分发需求。
4. **安全密钥管理**
   使用 IntelliJ 官方的安全凭证库 `PasswordSafe` 存储敏感的 API Key，不会以明文保存在项目的配置文件中。
5. **高度自定义的 Prompt 模板**
   默认生成符合 **Conventional Commits** 规范（如 `feat: add login feature`）的提交信息。你可以自由定制 Prompt 模板，在模板中通过 `{diff}` 和 `{language}` 占位符控制生成的上下文和语言。
6. **多语言一键切换**
   支持输出中文、英文、日文、西班牙文、法文或德文的提交信息。
7. **防重复点击与加载动画**
   点击生成后，按钮会自动置灰禁用并显示加载动画，待大模型生成结束后自动恢复，避免等待过程中的重复操作。

---

## 🛠️ 安装与配置说明

### 1. 从磁盘安装插件
1. 下载或本地构建生成的安装包：📂 `build/distributions/ai-commit-message-1.0-SNAPSHOT.zip`。
2. 打开 IntelliJ IDEA 2026.1.3，依次进入 `Settings -> Plugins`。
3. 点击插件页面右上角的 ⚙️ (齿轮按钮)，选择 **Install Plugin from Disk...**。
4. 选择上述 `zip` 包并确认，根据提示重启 IDE。

### 2. 配置大模型信息
1. 依次进入 `Settings -> Tools -> AI Commit Message`。
2. 选择大模型服务商（LLM Provider），设置 API Base URL、API Key 以及 Model Name。
   * *注意：若使用 One-API 等中转服务，API Base URL 请务必填写到 `/v1` 目录级别（例如 `http://8.220.205.185:8080/v1`）*。
3. 选择 Commit Message 默认输出的语言，并可在此微调 Prompt 模板。

### 3. 一键生成 Message
1. 修改文件后，打开 Commit 面板。
2. 勾选需要提交的文件，并在提交消息输入框的上方工具栏中，点击 ⚡ **Lightning (闪电) 图标**。
3. 等待生成动画结束，AI 生成的提交信息将自动填入输入框中。

---

## 💻 本地开发与构建指南

本插件采用 **Java** 编写，构建环境适配 **Gradle 8.13** 和 **JDK 21**，支持基于本地安装的 IDEA 进行“零外网下载”的编译和调试。

### 1. 编译代码
```bash
gradle compileJava -PlocalIdeaPath="D:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3"
```

### 2. 启动沙盒调试
该命令会基于你本地的 IDEA 安装启动一个独立的测试沙盒，用于直接测试和预览插件：
```bash
gradle runIde -PlocalIdeaPath="D:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3"
```

### 3. 打包插件
运行此命令生成可以直接分发和安装的 `.zip` 插件包：
```bash
gradle buildPlugin -PlocalIdeaPath="D:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3"
```
打包成功后，产物位于 `build/distributions/ai-commit-message-1.0-SNAPSHOT.zip`。

---

## 📦 项目结构说明

```text
├── build.gradle              # Gradle 构建配置文件（支持 localIdeaPath 动态关联与跳过插桩）
├── settings.gradle           # Gradle settings 文件
├── gradle.properties         # 本地构建代理和属性配置
└── src
    └── main
        ├── java
        │   └── com
        │       └── github
        │           └── aicommit
        │               ├── actions
        │               │   └── GenerateCommitMessageAction.java  # 闪电按钮 Action 逻辑
        │               ├── service
        │               │   └── LLMService.java                    # API 网络调用与 JSON 解析
        │               └── settings
        │                   ├── AICommitConfigurable.java          # 插件 Settings UI 配置界面
        │                   ├── AICommitPasswordSafe.java          # API Key 安全存储
        │                   └── AICommitSettings.java              # 插件持久化配置 State
        └── resources
            └── META-INF
                └── plugin.xml                                     # 插件功能模块与扩展点声明
```

## 🤖 GitHub Actions 自动构建与发布

本项目已配置 GitHub Actions 工作流。当你将代码推送到 GitHub 并推送版本 Tag（例如 `v1.0.0`）时，GitHub Actions 会自动编译插件并创建 Release，同时将生成的 `.zip` 插件包作为附件上传。

### 触发步骤：
1. **推送代码与 Tag**：
   ```bash
   git tag v1.0.0
   git push origin main --tags
   ```
2. **自动构建**：
   GitHub Actions 检测到以 `v` 开头的 Tag 之后，会自动执行以下流程：
   * 拉取代码并设置 JDK 21 及 Gradle。
   * 清除本地开发环境独有的代理和本地 IDEA 路径配置（确保在 CI 云端下载 SDK 并编译）。
   * 编译并构建插件包。
   * 创建一个名为 `v1.0.0` 的 GitHub Release，并将构建好的 `ai-commit-message-1.0-SNAPSHOT.zip` 上传到 Release 附件中。

## 📄 开源协议
本项目采用 [MIT License](LICENSE) 开源协议。
