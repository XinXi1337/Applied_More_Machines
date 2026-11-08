---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE注魔祭坛
  icon: applied_more_machines:ae_infusion_pedesta
  position: 311
categories:
- machines
item_ids:
- applied_more_machines:ae_infusion_pedesta
---

# AE注魔祭坛

<BlockImage id="applied_more_machines:ae_infusion_pedesta" scale="6" />

AE注魔祭坛将神秘农业的注魔和觉醒配方集中到一台机器中，不需要封包和子网了。

## 快速使用

1. 机器内部最多储存 **8,000 AE**，把配方材料放入 **九个输入槽**，不要求摆放位置，只核对材料、数量及组件数据。
2. 每完成一次配方消耗 **4,000 AE**。
3. 手动取走产物、物品管道抽取，或开启自动输出送入相邻容器。

## 供能与频道

接入 ME 网络时，机器需要 **1 个频道**，没有待机耗能；网络供能需要节点正常联网并获得频道。
机器六个面都可以连接 FE 能量导管，FE 按 AE2 配置的换算比例充入同一个 **8,000 AE** 缓冲。
FE 接口只接收能量，不向外输出，缓冲已满时停止接收；FE 供能可以独立于 ME 连接为缓冲充电。

## 升级卡

右侧共有四个升级卡槽，每槽一张，支持 AE2 加速卡，以及水晶科技的陨石超频卡，可混放，**两种卡合计最多四张**。
陨石超频卡仅在安装对应可选模组后可用。

基础耗时为 **80 tick**，每张加速卡减少 16 tick，每张陨石超频卡减少 20 tick，最低配方完成时间为 1 tick。

<RecipeFor id="applied_more_machines:ae_infusion_pedesta" />

## 倍增卡

安装数据能源后，在原有升级槽外增加一个单张倍增卡专槽。产量取整、并行、返还物及ME余量规则见[倍增卡](multiplication_card.md)。
