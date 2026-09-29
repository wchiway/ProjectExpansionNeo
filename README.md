# ProjectExpansionNeo

**ProjectExpansionNeo** 是面向 **Minecraft 1.21.1 / Fabric** 的 ProjectEF Neo 扩展，
由 Project Expansion 的 NeoForge 版本迁移而来。模组 ID 保留为 `projectexpansion`。

提供更多等级的 EMC 收集器、能量中继器、能量之花、EMC 链接、巨型之星，
以及转换接口、高级炼金箱、凝聚器 MK3、炼金书、奥术转换终端、无限燃料等内容。

## 安装

使用 Java 21，并在 Fabric 实例的 `mods/` 目录中放入：

- Fabric API。
- ProjectEF Neo（本项目使用同级 `ProjectE` 的 1.3.0 版本进行验证）。
- `ProjectExpansionNeo-1.21.1-1.0.6.jar`。

当前构建使用 Fabric Loader 0.16.14、Fabric API 0.116.14+1.21.1。
ProjectEF Neo 内嵌提供 Forge Config API Port 和权限库；本扩展使用 Fabric 加载器。

可选集成包括 JEI、EMI、Jade、WTHIT 和 Trinkets。Trinkets 提供专用转换终端槽位，Z
替代原 Curios 集成；本 Fabric 版本不包含 TOP 集成。

## 构建

需要 JDK 21。默认读取同级项目的构建产物：

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

Windows 使用 `gradlew.bat`。可安装的产物位于
`build/libs/ProjectExpansionNeo-1.21.1-1.0.6.jar`，`-sources.jar` 为源码包。

两个启动脚本都将 Gradle 用户目录设为本项目的 **`.gradle_home/`**。
如果 IDE 绕过启动脚本，也应在 IDE 中设置相同目录。

## 开发与验证

```sh
./gradlew runClient
./gradlew runServer
./gradlew -Pgametest -Precipe_viewer=none runGameTestServer
./gradlew -Pgametest runClientSmoke
```

`recipe_viewer` 可选 `jei`（默认）、`emi` 或 `none`，例如
`./gradlew runClient -Precipe_viewer=emi`。

测试模组位于 `src/testmod/`，不会打入发布 JAR：

- GameTest 验证收集器库存的嵌套事务、EMC 链接的大整数结算与回滚、
  流体单位及额度恢复、无限燃料仅扣费一次。报告为 `build/gametest-results.xml`。
- `runClientSmoke` 使用独立的 `build/client-smoke/` 目录，检查客户端初始化和全部
  174 个物品模型，然后自动退出。

已通过上述 4 项 GameTest，以及 JEI 开发配置下的客户端启动和模型检查。
可选集成的完整游戏内交互仍需结合实际整合包验证。
测试环境没有可用的语音朗读库和音频设备，声音与朗读未验证。

## 资源与配置

普通构建使用已跟踪的 `src/generated/resources/`，不要求安装 Node.js。
资源生成器和模板位于 `src/main/generation/`；修改后可显式执行：

```sh
./gradlew data
```

此任务需要 Bash、Node.js 和 npm。它使用锁文件安装项目依赖，并将 npm 下载缓存保存在
`.gradle_home/npm/`。服务器和客户端配置沿用 TOML 格式。

注册 ID 保留不变；这不代表 NeoForge 存档可直接迁移。旧存档跨加载器迁移未验证。

## 项目来源

- [上游 Project Expansion](https://github.com/DonovanDMC/ProjectExpansion)
- [ProjectEF](https://github.com/wchiway/ProjectEF)

本项目保留上游代码、资源与作者署名，采用 [MIT 许可证](LICENSE)。
这是非官方扩展，不由 ProjectE 原项目提供支持。
