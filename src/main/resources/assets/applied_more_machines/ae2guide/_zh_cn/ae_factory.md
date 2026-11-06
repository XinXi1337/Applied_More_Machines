---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE通用工厂
  icon: applied_more_machines:ae_factory_controller
  position: 315
categories:
- machines
item_ids:
- applied_more_machines:ae_factory_controller
- applied_more_machines:ae_factory_pattern
- applied_more_machines:ae_factory_casing
---

# AE通用工厂

<BlockImage id="applied_more_machines:ae_factory_controller" scale="6" />

强兼部分mek的机器，不需要你再堆量摆那一排了。

## 使用

jei查看结构所需材料，搭建使用AE通用工厂控制器的**投影／放置**按钮。
控制器和样板总成需要接入 ME 网络，每个总成可放 36 张样板。
样板必须按照jei那样写，手动翻倍会被识别成非法样板。

## 支持的机器配方

仅开放结构内以下机器对应的已加载配方，包含数据包修改：

- 熔炼
- 灌注
- 富集
- 粉碎
- 锯木
- 提纯
- 压射
- 压缩
- 氧化
- 化学灌注器
- 电解分离器
- 化学溶解室
- 化学清洗机
- 营养液化器
- 加压反应室
- 回旋式气液转换机（双向）
- 热力蒸馏控制器

额外配方：**1 B 钠 → 1 B 过热钠**，是不是很合理。

## 速度、升级与耗能

基础 **160 tick、8 并行**，两个控制器共四个升级槽，同一种升级只能占一个槽。
AE 加速卡每张 **+64 并行／−10 tick**（最多 4 张）。
陨石超频卡 **+256并行／−20 tick**（最多 4 张）。
Mek 速度升级 **+128并行／−2 tick**（最多 16 个）。

无堆叠升级时，一轮只做一种配方，剩下的配方需要排队，每个堆叠升级多容纳一种，最多 8 个。
空闲额度可以让效率翻倍。
输入和输出全部为流体或化学品的配方，会额外享受1000倍效率。

每轮合计消耗 **8,000 AE**，优先使用 AE 存储中的 FE，其次网络 AE。
CPU 负责备料和派单，控制器决定实际加工速度与并行。
断电、失去频道或结构破坏会保料暂停，恢复后本轮从零计时，取消订单退还未消耗原料。

## 整座工厂升级

完成全部订单后，手持足量的 Mekanism Extras **工厂安装器**右键任一控制器，将结构中有对应工厂的机器统一替换；没有对应版本的机器保留。机器库存和设置沿用原生升级接口迁移，失败不消耗安装器。

| 目标等级 | 安装器数量 | 并行奖励 | 配方用时 |
| --- | --- |-----|--|
| 绝对 | 16 | +256 | 无|
| 至尊 | 16 | +512 | 无|
| 寰宇支配 | 32 | +2048 | 减半|
| 悖论无限 | 64 | 并行翻倍| 锁定 1 tick |


