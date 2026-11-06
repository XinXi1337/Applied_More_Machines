# AE2:更多机器 / Applied:MoreMachines

为 Applied Energistics 2 添加机器与多方块加工结构。

- Minecraft 1.21.1
- NeoForge 21.1.169 及以上（1.21.1 分支；开发版本 21.1.264）
- Applied Energistics 2 19.2.18
- Java 21
- 作者：XinXi
- 许可证：[MIT](LICENSE)

## 功能

包含 AE 注魔祭坛、合成核心、附魔台、钓鱼机、通用工厂、胶乳提取加工机、溶解成型机、流体镭射钻和 AE 反应堆。部分机器及加工配方需要安装对应的第三方模组。
使用说明见游戏内 AE2 的 GuideME，配方和多方块结构可通过 JEI 查看。

## 开发依赖更新

本地编译 API 按模组名称自动匹配版本，规则集中在 `gradle/local-mod-apis.gradle`。更新时在对应的 `run/mods` 或 `.dependency-cache/compile-only` 目录替换旧版，然后在 IDEA 重新加载 Gradle，无需改版本文件名配置。同一模组保留一个版本；缺失或重复时会提示具体模组和目录。可运行 `gradlew.bat checkLocalModApis` 查看识别结果。

这些依赖在玩家运行模组时仍为可选；编译项目需要它们提供接口。自动识别解决文件名随版本变化的问题，接口本身发生变化仍需适配。AE2、NeoForge 和 Gradle 插件的开发版本继续由构建配置明确指定。
