# libs/ — 本地修补的第三方依赖

## toneko-neoforge-1.9.2-awfix.jar

来源：Modrinth `tonekomod` 版本 `ytAtEo73`（toNeko 1.9.2 的 NeoForge 构建）。

修改内容：仅从 `architectury.common.json` 中移除了 `"accessWidener": "toneko.accesswidener"` 这一个键，
其余内容（包括 `META-INF/jars/` 里的 JarJar 嵌套依赖）完全保留。

原因：该 NeoForge 构建内的 `toneko.accesswidener` 使用 **mojang（官方）命名空间**，
而 Architectury Loom 的 access-widener jar 处理器要求类路径上模组声明的访问宽化器是
**intermediary** 命名空间。若把原版 jar 作为 `modImplementation` 加入 neoforge 子项目，
配置阶段会报错：

    Cannot remap access widener from namespace 'mojang'. Expected: 'intermediary'

移除该键即可（此键只参与构建期处理，运行时无用；NeoForge 生产环境使用
META-INF/accesstransformer.cfg）。编译期用普通 `compileOnly` 引用原版坐标，
开发运行时用本修补 jar 作为 `modLocalRuntime`。

升级 toNeko 版本时需重新生成此文件：
解压新 jar → 删除 architectury.common.json 的 accessWidener 键 → 重新打包。

## teamreborn-energy（构建期自动重映射，不再需要手工 jar）

TeamReborn Energy 由 `neoforge/build.gradle` 里的
`remapEnergyJar` / `packageEnergyDev` 任务在构建时自动处理。

原因：原版 jar 是 Fabric 构建，类与方法引用全部是 intermediary 名
（如 `class_1799`、`method_60655），而 NeoForge 生产环境运行于 mojang 官方名。
直接打包会因描述符不匹配抛 NoSuchMethodError（例如悬停电动根棍时）。
Loom 自带的 RemapJarTask 在本工程的 layered officialMojangMappings 下
只重映射类名、方法引用不动，因此 `remapEnergyJar 用 ASM + 项目 tiny 映射
自行完成 intermediary 到 named 的完整重映射（方法可能声明于接口却经实现类调用，
靠 intermediary 名全局唯一这一性质做裸名回退）。

两个产物：
- `build/energy/teamreborn-energy-4.1.0-named.jar：重映射后的纯类库，
  由 shadowBundle 打包进生产模组（fabric.mod.json 会被 shadowJar 排除）；
- `build/energy/teamreborn-energy-4.1.0-neoforge.jar：开发环境包装版——
  删除 fabric.mod.json 并添加 lowcodefml 的 mods.toml（模板见本目录
  energy-neoforge.mods.toml），使 FML 能加载它。纯 Fabric 模组会被 FML 拒绝
  （"is a Fabric mod and cannot be loaded"），其类对其他模组的类加载器不可见。
