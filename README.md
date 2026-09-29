# 等价扩展:Neo（ProjectExpansionNeo）

**等价扩展:Neo**（英文名 **ProjectExpansionNeo**）是面向 **Minecraft 1.21.1 / Fabric** 的 ProjectEF Neo 扩展，
由 Project Expansion 的 NeoForge 版本迁移而来，为 EMC 的生产、存储与自动化提供更多选择。

## 主要内容

- **EMC 生产**：更多等级的 EMC 收集器、能量中继器和能量之花。
- **EMC 存储与自动化**：巨型之星、EMC 链接、转换接口和能量凝聚器 MK3。
- **收纳与实用工具**：高级炼金箱、炼金书、奥术转换终端和无限燃料。

## 安装

使用 Minecraft 1.21.1、Java 21 和 Fabric Loader 0.16.9 或更高版本，
并在实例的 `mods/` 目录中放入：

- Fabric API。
- ProjectEF Neo 1.3.0 或更高版本。
- `ProjectExpansionNeo-1.21.1-1.0.6.jar`。

Fabric API 需要选择适用于 Minecraft 1.21.1 的版本。
ProjectEF Neo 内嵌提供 Forge Config API Port 和权限库，无需单独安装。

## 可选集成

支持 JEI、EMI、Jade、WTHIT 和 Trinkets。
Trinkets 提供专用转换终端槽位，替代原 Curios 集成；本 Fabric 版本不包含 TOP 集成。

## 配置与兼容性

服务器和客户端配置沿用 TOML 格式。模组 ID 保持为 `projectexpansion`。
旧版 NeoForge 存档跨加载器迁移尚未验证。

构建、开发环境、资源生成和测试说明见 [开发指南](DEV.md)。

## 项目来源

- [上游 Project Expansion](https://github.com/DonovanDMC/ProjectExpansion)
- [ProjectEF](https://github.com/wchiway/ProjectEF)

本项目保留上游代码、资源与作者署名，采用 [MIT 许可证](LICENSE)。
这是非官方扩展，不由 ProjectE 原项目提供支持。
