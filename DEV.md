# 等价升华:Neo 开发指南

本文件介绍构建、开发与验证流程。模组功能和安装说明见 [English README](README.md) / [中文 README](README.zh-CN.md)。

## 开发环境

需要 JDK 21。项目使用 Fabric Loom、Mojang 官方映射和 Parchment，
具体版本与依赖配置以 [gradle.properties](gradle.properties) 和 [build.gradle](build.gradle) 为准。

当前构建配置：

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| Fabric Loader | 0.16.14 |
| Fabric API | 0.116.14+1.21.1 |
| Fabric Loom | 1.10.5 |
| ProjectEF Neo | 1.3.1（GitHub Release） |

普通构建使用已跟踪的生成资源，不需要 Node.js；重新生成资源时才需要 Bash、Node.js 和 npm。

## 构建

默认由 Gradle 从 [ProjectEF 的 GitHub Releases](https://github.com/wchiway/ProjectEF/releases/latest)
下载前置模组，不需要检出或构建同级 `ProjectE` 项目。
当前使用最新 Release **1.3.1** 的完整包 `ProjectEF-1.21.1-PE1.3.1.jar`，不是 `-api.jar` 或 `-sources.jar`。

直接在本项目根目录执行：

```sh
./gradlew build
```

Release 版本由 `gradle.properties` 中的 `projectef_version` 固定，避免同一份源码的依赖随时间变化。
后续升级时，先检查最新 Release 是否仍适用于 Minecraft 1.21.1 / Fabric，再更新该属性。
下载和重映射缓存均保存在项目内；首次构建需要联网，依赖缓存完整后可使用 `./gradlew build --offline`。

仅在本地调试前置模组时，可显式覆盖默认 Release 依赖：

```sh
./gradlew build -Pprojectef_jar=/path/to/ProjectEF.jar
```

本指南中的命令均从项目根目录执行；Windows 将 `./gradlew` 替换为 `gradlew.bat`。
可安装的产物位于 `build/libs/ProjectExpansionNeo-1.21.1-1.1.0.jar`，
`-sources.jar` 为源码包。文件名中的版本由 `gradle.properties` 决定。

两个 Wrapper 启动脚本都将 `GRADLE_USER_HOME` 固定为项目内的 **`.gradle_home/`**。
IDE 或直接调用 Wrapper 主类时，也应指定相同的 Gradle 用户目录。

ProjectEF Neo 的发布 JAR 内嵌提供 Forge Config API Port 和权限库。
Loom 的开发环境重映射无法完整暴露前置 JAR 的内嵌库，因此本项目在构建脚本中单独声明相应开发依赖。

## 运行与验证

| 命令 | 用途 |
| --- | --- |
| `./gradlew compileJava` | 编译 Java 源码 |
| `./gradlew build` | 构建发布 JAR 和源码包 |
| `./gradlew runClient` | 启动开发客户端 |
| `./gradlew runServer` | 启动开发服务端 |
| `./gradlew -Pgametest -Precipe_viewer=none runGameTestServer` | 运行无界面 GameTest |
| `./gradlew -Pgametest runClientSmoke` | 运行客户端初始化和物品模型检查 |

`recipe_viewer` 可选 `jei`（默认）、`emi` 或 `none`。例如：

```sh
./gradlew runClient -Precipe_viewer=emi
```

测试模组位于 `src/testmod/`，仅在指定 `-Pgametest` 时启用，不进入发布 JAR。

- GameTest 覆盖收集器库存的嵌套事务、EMC 链接的大整数结算与回滚、
  流体单位及额度恢复、无限燃料仅扣费一次，以及奥术转换终端的主手、副手和无手持打开。
  报告为 `build/gametest-results.xml`。
- `runClientSmoke` 使用独立的 `build/client-smoke/` 目录，
  检查客户端初始化、按键绑定和本模组全部已注册物品的模型，然后自动退出。

### 已有验证记录

2026-09-29：修复奥术转换终端打开时使用未绑定菜单引用的问题后，编译、构建及全部 6 项 GameTest 通过，
包含主手、副手和无手持打开的回归验证。此次未手动验证客户端界面或 Trinkets 槽位的完整交互。
此前已通过 JEI 开发配置下的客户端启动和 174 个物品模型检查。
可选集成的完整游戏内交互仍需结合实际整合包验证；
此前测试环境没有可用的语音朗读库和音频设备，声音与朗读未验证。

Java 修改至少执行编译；涉及事务、燃料、注册或网络的修改按需运行 GameTest。
编译通过不能代替游戏内行为验证。纯文档修改无需启动游戏。

## GitHub Release 发布

发布工作流位于 [`.github/workflows/release.yml`](.github/workflows/release.yml)，
从同级 ProjectE 的 Release 工作流迁移，保留 DeepSeek 更新日志生成功能。
发布辅助逻辑在 [`.github/scripts/release.mjs`](.github/scripts/release.mjs)，使用 Node.js 24 内置 API，不需要安装 npm 依赖。

### 配置与触发

- 在仓库 **Settings → Secrets and variables → Actions** 中设置 Secret `DEEPSEEK_API_KEY`，启用英文 AI 更新日志。
  可选的 Repository Variable `DEEPSEEK_MODEL` 覆盖模型名，默认沿用源工作流的 `deepseek-v4-flash`。
  设置密钥后，工作流会将发布范围内的提交标题发送到 DeepSeek；不会发送源代码、差异或密钥内容。
- 推荐使用 `<minecraft_version>-<mod_version>` 格式的 tag，例如 `1.21.1-1.1.0`，与发布 JAR 和 `updates.json` 的格式一致。
  同时兼容 `1.1.0` 和 `v1.1.0`，不支持预发布后缀；每次版本只选择一种 tag 格式。
- 推送匹配格式的 tag 会自动触发。也可以在 Actions 的 **Release → Run workflow** 中，
  将 `version` 填为**已存在**的 tag。工作流不会创建 tag；目标 tag 必须包含本工作流和辅助脚本。
- 发布前先更新 `gradle.properties` 的 `mod_version`，同步维护 README 中的安装示例及 `updates.json`。
  工作流会核对 tag、Minecraft 版本、源码提交及 JAR 内的 `fabric.mod.json`，不一致时停止发布。

### 发布流程与保护

1. 只读构建作业使用 Java 21、项目 Gradle Wrapper 和 `.gradle_home/` 缓存。
   Gradle Action 校验 Wrapper，并管理依赖缓存；配置缓存不会在未配置加密密钥时上传。
   tag 缓存只能供同一 tag 的重跑使用，不能在不同 tag 之间共享。
2. 执行完整 `build` 和无界面 GameTest；任一步失败都不会发布。测试报告保留 14 天。
3. 从最近的祖先发布 tag 收集提交；首次发布使用完整历史。忽略无关 tag 和其他 Minecraft 版本的复合 tag。
   DeepSeek 提示词与 Release 的安装说明、文件说明统一使用英文。
4. 未设置密钥、输入过大、请求超时、HTTP 错误或响应无效时，使用英文兜底说明及提交历史链接，
   **不直接复制可能为中文的提交消息**。超时为 90 秒；空内容、截断响应和含中日韩文字的结果不会作为 AI 摘要发布。
5. 仅上传精确匹配当前版本的正式 JAR、源码 JAR 和 `SHA256SUMS`，不使用宽泛的 JAR 通配符。
   Release 安装说明明确要求 ProjectEF Neo 和 Fabric API，不将前置内嵌依赖误写成本扩展内嵌。
6. 发布作业才获得 `contents: write`；它校验产物校验和及远端 tag 指向，且不执行项目构建或调用 AI。
   已存在的 Release 不会被覆盖。同一 tag 的发布串行执行，不中断正在发布的运行。

原始提交来源和最终摘要作为 Actions artifact 保留 14 天，发布包保留 7 天；不会保存 API 请求、响应或密钥。
如果同一发布已经成功，重跑会因 Release 已存在而失败；修改已发布内容需单独人工处理。

仅检查发布逻辑、不调用 AI 或创建 Release：

```sh
node --test .github/scripts/release.test.mjs
actionlint .github/workflows/release.yml
```

本地修改工作流不会自动推送 tag、触发远端 Actions 或创建 Release。

## 资源生成

生成器和模板位于 `src/main/generation/`，生成结果位于 `src/generated/resources/`。
修改生成器或模板后执行：

```sh
./gradlew data
```

该任务调用 `scripts/generate-assets.sh`，使用 `npm ci` 按锁文件安装依赖，
并将 npm 下载缓存保存在 `.gradle_home/npm/`。
修改生成资源时应同步提交生成器或模板与生成结果；普通构建不会自动运行生成器。

## 代码与资源布局

Java 包根目录为 `src/main/java/cool/furry/mc/neoforge/projectexpansion/`。
包名中的 `neoforge` 是历史命名，本项目使用 Fabric 加载器。

| 路径 | 用途 |
| --- | --- |
| Java 包下的 `registries/` | 注册表、数据组件与能力 |
| Java 包下的 `block/`、`item/` | 方块、方块实体与物品 |
| Java 包下的 `platform/` | Fabric 库存、流体和 EMC 事务等平台适配 |
| Java 包下的 `events/`、`commands/`、`net/` | 回调、命令与网络 |
| Java 包下的 `client/`、`gui/`、`rendering/` | 客户端界面与渲染 |
| Java 包下的 `integrations/`、`mixin/` | 可选模组集成与运行时钩子 |
| Java 包下的 `config/`、`capability/`、`util/` | 配置、传送书数据与公共逻辑 |
| `src/main/resources/` | Fabric 元数据、访问权限与静态资源 |
| `src/main/generation/` | TypeScript 资源生成器与模板 |
| `src/generated/resources/` | 已跟踪的生成资源 |
| `src/testmod/` | 独立测试模组 |

## 维护约定

- 中文名使用“等价升华:Neo”，英文名和发布 JAR 名称使用 `ProjectExpansionNeo`；模组 ID 保持 `projectexpansion`，
  前置 ProjectEF Neo 的模组 ID 为 `projecte`。
- 修改注册 ID、数据组件、持久化数据或网络格式时，同时检查写入和读取位置。
- Fabric Transfer API 的模拟与嵌套事务必须能回滚；EMC 保留 `BigInteger` 精度，
  流体使用 Fabric droplets（每桶 81,000）。
- 可选集成需要检查模组加载条件，并保持客户端与服务端的类加载隔离。
- 修改 Gradle 任务时保持配置缓存可用：提前捕获所需值，避免在任务执行闭包中访问 `project`。
- 不提交 `.gradle_home/`、`.gradle/`、`build/`、`run/` 和 `node_modules/`。
- 旧 `buildSrc/` 与发布脚本未接入 Fabric 构建，不作为本项目的常规构建入口。
