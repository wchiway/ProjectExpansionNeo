# ProjectExpansionNeo 开发指南

本文件介绍构建、开发与验证流程。模组功能和安装说明见 [README](README.md)。

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
| ProjectEF Neo | 1.3.0（本地 JAR） |

普通构建使用已跟踪的生成资源，不需要 Node.js；重新生成资源时才需要 Bash、Node.js 和 npm。

## 构建

默认读取同级 ProjectEF Neo 项目的构建产物：

```text
MC_Mod/
├── ProjectE/build/libs/ProjectEF-1.21.1-PE1.3.0.jar
└── ProjectExpansionNeo/
```

先构建 ProjectEF Neo，再在本项目根目录执行：

```sh
./gradlew build
```

也可以指定其他位置的前置 JAR：

```sh
./gradlew build -Pprojectef_jar=/path/to/ProjectEF.jar
```

本指南中的命令均从项目根目录执行；Windows 将 `./gradlew` 替换为 `gradlew.bat`。
可安装的产物位于 `build/libs/ProjectExpansionNeo-1.21.1-1.0.6.jar`，
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
  流体单位及额度恢复、无限燃料仅扣费一次。报告为 `build/gametest-results.xml`。
- `runClientSmoke` 使用独立的 `build/client-smoke/` 目录，
  检查客户端初始化、按键绑定和本模组全部已注册物品的模型，然后自动退出。

### 已有验证记录

此前已通过上述 4 项 GameTest，以及 JEI 开发配置下的客户端启动和 174 个物品模型检查。
可选集成的完整游戏内交互仍需结合实际整合包验证；
此前测试环境没有可用的语音朗读库和音频设备，声音与朗读未验证。

Java 修改至少执行编译；涉及事务、燃料、注册或网络的修改按需运行 GameTest。
编译通过不能代替游戏内行为验证。纯文档修改无需启动游戏。

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

- 对外名称和发布 JAR 名称使用 `ProjectExpansionNeo`；模组 ID 保持 `projectexpansion`，
  前置 ProjectEF Neo 的模组 ID 为 `projecte`。
- 修改注册 ID、数据组件、持久化数据或网络格式时，同时检查写入和读取位置。
- Fabric Transfer API 的模拟与嵌套事务必须能回滚；EMC 保留 `BigInteger` 精度，
  流体使用 Fabric droplets（每桶 81,000）。
- 可选集成需要检查模组加载条件，并保持客户端与服务端的类加载隔离。
- 修改 Gradle 任务时保持配置缓存可用：提前捕获所需值，避免在任务执行闭包中访问 `project`。
- 不提交 `.gradle_home/`、`.gradle/`、`build/`、`run/` 和 `node_modules/`。
- 旧 `buildSrc/` 与发布脚本未接入 Fabric 构建，不作为本项目的常规构建入口。
