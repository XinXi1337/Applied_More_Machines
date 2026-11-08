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

编译 API 与运行整合包分开管理。首次导入时，`gradle/local-mod-apis.gradle` 从现有本地模组 JAR 只读提取 `.class`，固定到 `.dependency-cache/compile-api/`。IDEA 与 Gradle 都使用这套缓存；以后替换、改名或删除 `run/mods` 中的模组，不会改变编译 API 或使旧 JAR 路径失效。普通 Gradle `clean` 不删除该缓存；换包时保留 `.dependency-cache`。

首次初始化和显式刷新均优先使用各 API 原有来源目录，缺失时再查 `.dependency-cache/compile-only` 与 `run/mods`；名称匹配不区分大小写并允许整合包名称前缀，同一来源目录必须只有一个版本。已有固定缓存时不再扫描运行 JAR。运行 `gradlew.bat --offline checkLocalModApis` 可查看固定版本与路径。缓存只有 class 和本地记录，不构建、复制或修改任何 JAR，不进入模组产物或运行依赖。

只有主动升级开发 API 时才刷新，例如 `gradlew.bat --offline refreshLocalModApis -PammModApi=ae2lt` 仅更新天枢；省略 `-PammModApi` 更新全部本地 API。可选 ID：`ma/jei/de/bc/pb/mek/flux/omni/eco/thunder/ae2lt/trinity/wtlib`。刷新后重新加载 IDEA 的 Gradle 项目并编译验证。普通换运行整合包不需要刷新 API。

固定 API 解决换包导致的编译依赖丢失，不保证任意运行版本的二进制兼容；运行模组发生破坏性接口变化时仍需适配。AE2、NeoForge、GuideME 和 Gradle 插件的 Maven 开发版本继续由构建配置明确指定。新的开发环境首次初始化仍需本地 API 来源，缓存已被 Git 忽略。

普通开发运行默认跳过第三方 GameTest 自动注册，避免测试类加载未安装的可选模组接口。需要运行这些测试时，使用 `gradlew.bat runClient -PammEnableGameTests=true` 显式开启；正式环境不受此开发开关影响。
