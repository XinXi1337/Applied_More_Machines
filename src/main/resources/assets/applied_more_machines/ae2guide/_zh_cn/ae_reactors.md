---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE反应堆
  icon: applied_more_machines:ae_reactors_controller
  position: 319
categories:
- machines
item_ids:
- applied_more_machines:ae_reactors_controller
- applied_more_machines:ae_reactors_pattern
- applied_more_machines:ae_reactors_shell
- applied_more_machines:ae_reactors_fuelrod
---

# AE反应堆

<BlockImage id="applied_more_machines:ae_reactors_controller" scale="6" />

需要安装**极限反应堆（Extreme Reactors）**，控制器负责加工，样板总成存放样板，外壳和燃料棒用于组成结构。

## 使用

1. 在 JEI 中查看结构与材料，用控制器的**投影／放置**按钮搭建，结构为 **12×11×12，共1008个方块**，支持四个水平朝向。
2. 两个控制器和两个样板总成都要接入有可用频道的 ME 网络，每个方块支持六面连接占用1个频道，每个总成可放36张样板。
3. 样板总成只有结构规定的两个位置，**不能用外壳替换，也不能把其他外壳换成样板总成**，两个控制器共享同一座反应堆的订单、进度和升级效果。
4. 在样板总成中放入 **样板**，通过 AE 合成系统下单，控制器中的 AE 图案可点击查看 JEI 加工配方。

样板投入和产物必须与 **JEI显示的一次原生配方数量**一致，手动翻倍、缺少产物或材料错误会成为非法样板，其他合法样板仍可工作。
JEI“+”可将配方填写到支持的样板终端，仍需自行编码并放入总成。

## 支持的配方

- **反应堆（The Reactor）**：按极限反应堆当前燃料映射，将燃料转为对应废料，保留物品或流体形式及准确数量比例。
- **再处理器（Reprocessor）**：读取已加载的原生再处理配方。
- **流化器（Fluidizer）**：读取固体流化、固体混合和流体混合配方。

读取当前世界已加载的配方和燃料映射，包含数据包修改，**暂时不生产FE电力，等后续版本吧**

## 速度、升级与耗能

基础 **160 tick，8并行**，每轮只处理一种配方，其他订单排队，不足整批时只加工实际收到的份数。

每台控制器有 **1个升级槽**，可放4张加速卡或4张陨石超频卡。
两个控制器合计每种最多4张，同一种卡不能占用两个槽，满配需要两种卡时分别放入两台控制器。

- AE加速卡：每张 **+64并行／−10 tick**，最多4张。
- 陨石超频卡：每张 **+256并行／−20 tick**，最多4张，需要对应模组提供该卡。

| 升级组合 | 并行 | 每轮用时 |
| --- | --- | --- |
| 无升级 | 8 | 160 tick（8秒） |
| 4张加速卡 | 264 | 120 tick（6秒） |
| 4张陨石超频卡 | 1032 | 80 tick（4秒） |
| 4张加速卡＋4张陨石超频卡 | 1288 | 40 tick（2秒） |

每轮合计消耗 **8,000 AE**，优先使用 网络 存储中的 FE，其次网络 AE，费用不随并行份数增加。
CPU负责备料和派单，控制器决定实际加工速度与并行。
拆卸需同一玩家连续点击控制器的**拆卸**按钮8次，间隔不超过1秒。

## 倍增卡

安装数据能源后，在原有升级槽外增加一个单张倍增卡专槽。产量取整、并行、返还物及ME余量规则见[倍增卡](multiplication_card.md)。
