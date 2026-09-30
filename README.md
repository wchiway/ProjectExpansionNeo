[ProjectExpansionNeo](README.md) › **English** / [简体中文](README.zh-CN.md)

# ProjectExpansionNeo

**ProjectExpansionNeo** is an expansion for **ProjectEF Neo** on **Minecraft 1.21.1 / Fabric**.
Ported from the NeoForge version of Project Expansion, it adds more options for EMC generation, storage, and automation.

## Features

- **EMC generation:** Higher-tier energy collectors, relays, and power flowers.
- **EMC storage and automation:** Large EMC stars, EMC links, transmutation interfaces, and the Energy Condenser MK3.
- **Storage and utilities:** Advanced alchemical chests, alchemical books, the Arcane Transmutation Tablet, and infinite fuel.

## Installation

Use Minecraft 1.21.1, Java 21, and Fabric Loader 0.16.9 or later.
Place the following in your instance's `mods/` directory:

- Fabric API for Minecraft 1.21.1.
- The full Minecraft 1.21.1 / Fabric JAR from the [latest ProjectEF Neo release](https://github.com/wchiway/ProjectEF/releases/latest).
- `ProjectExpansionNeo-1.21.1-1.1.1.jar`.

Choose the regular ProjectEF JAR, such as `ProjectEF-1.21.1-PE1.3.1.jar`, **not** the `-api.jar` or `-sources.jar` asset.
ProjectEF Neo bundles Forge Config API Port and the permissions library; you do not need to install them separately.

## Optional integrations

Supported integrations include JEI, EMI, Jade, WTHIT, and Trinkets.
Trinkets provides a dedicated transmutation tablet slot in place of the former Curios integration.
TOP integration is not included in this Fabric version.

## Configuration and compatibility

Server and client configuration files continue to use TOML. The mod ID remains `projectexpansion`.
Migrating existing NeoForge worlds across loaders has not been verified.

See the [development guide (Chinese)](DEV.md) for building, development setup, resource generation, and testing.

## Project origins

- [Upstream Project Expansion](https://github.com/DonovanDMC/ProjectExpansion)
- [ProjectEF](https://github.com/wchiway/ProjectEF)

This project retains upstream code, assets, and author attribution and is licensed under the [MIT License](LICENSE).
It is an unofficial expansion and is not supported by the original ProjectE project.
