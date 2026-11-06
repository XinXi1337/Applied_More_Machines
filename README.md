# AE2:更多机器 / Applied:MoreMachines

为 Applied Energistics 2 添加机器与多方块加工结构。

- Minecraft 1.21.1
- NeoForge 21.1.264
- Applied Energistics 2 19.2.18
- Java 21
- 作者：XinXi
- 许可证：[MIT](LICENSE)

## 功能

包含 AE 注魔祭坛、合成核心、附魔台、钓鱼机、通用工厂、胶乳提取加工机、溶解成型机、流体镭射钻和 AE 反应堆。部分机器及加工配方需要安装对应的第三方模组。

使用说明见游戏内 AE2 的 GuideME，配方和多方块结构可通过 JEI 查看。

## 开发环境

1. 安装 JDK 21，设置 JAVA_HOME，或在 IDE 中将 Gradle JVM 选择为 JDK 21。
2. 将 gradle.properties.example 复制为 gradle.properties，再按本机需要调整配置。该本地文件不会提交到 Git。
3. 用 IDE 打开项目根目录，通过项目自带的 Gradle Wrapper 导入。
4. 构建脚本引用的可选集成 API 来自 .dependency-cache/compile-only/ 与 run/mods/；按 build.gradle 中的文件名和版本自行提供。这些第三方 JAR 不随仓库分发。

仅编译源码并处理资源：

    .\gradlew.bat compileJava processResources

正式源码和运行资源位于 src/。游戏存档、日志、依赖缓存、本机配置、AI 笔记及美术草稿不纳入版本控制；正式贴图、模型、配方和指南保留在 src/main/resources/。
