# 只是根棍子
> `JustARod` by @CSNeko

这是[More_end_rod](https://github.com/CSneko/More_end_rod/)模组的续作，但是需要toNeko作为前置，你可以将其理解为toNeko的附属模组喵

你需要在 `Minecraft 1.21.1` 运行本模组，支持以下加载器：

- **Fabric**：需要安装 [Fabric API](https://modrinth.com/mod/fabric-api)、[Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) 以及 [toNeko](https://modrinth.com/mod/tonekomod) 作为前置哦
- **NeoForge**：需要安装 [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api)、[Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) 以及 toNeko 的 NeoForge 版本作为前置哦（构建说明见下方）

喜欢这个模组的话记得把star点上(｢・ω・)｢

可以来[爱发电](https://afdian.com/a/cccry)支持作者获取整合包哦

# 构建

本项目使用 [Architectury](https://github.com/architectury) 多加载器结构：

```
common/    平台无关代码（编译时面向 Fabric API）
fabric/    Fabric 加载器入口
neoforge/  NeoForge 加载器入口（运行时由 Forgified Fabric API 提供 Fabric API）
```

```bash
./gradlew build          # 构建全部
./gradlew :fabric:build      # 只构建 Fabric
./gradlew :neoforge:build    # 只构建 NeoForge
./gradlew runClient      # Fabric 开发客户端
./gradlew :neoforge:runClient # NeoForge 开发客户端（如已配置）
```

产物位于 `fabric/build/libs/JustARod-fabric-<版本>.jar` 与 `neoforge/build/libs/JustARod-neoforge-<版本>.jar`。

> 注意：`libs/toneko-neoforge-*-awfix.jar` 是 toNeko NeoForge 版的本地修补版
> （移除了其 `architectury.common.json` 中与 Loom 冲突的 accessWidener 键），
> 仅用于 NeoForge 开发运行时，详见 `libs/README.md`。


可以来[爱发电](https://afdian.com/a/cccry)支持作者获取整合包哦

# 如何使用

# 已经替大家把猫猫草服了喵~

## 恰个小广子
如果你也想自己开服务器的话呢，这边有家低价稳定易上手的服务商喵，他们家的服务器带宽还是很高的喵，而且对于内存也不吝啬，对于MC这种很吃单核心CPU的游戏也非常友好喵。无论是小服务器还是大服务器，都可以来试试哦。点击这里 -> [木桶面板](https://pm.mutong1.com/minecraft)，使用优惠码csneko可以享受超级折扣喵。
