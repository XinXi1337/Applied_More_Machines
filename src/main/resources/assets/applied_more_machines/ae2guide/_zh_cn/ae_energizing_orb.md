---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 321
  title: AE充能台
  icon: applied_more_machines:ae_energizing_orb
item_ids:
  - applied_more_machines:ae_energizing_orb
---

# AE充能台

<BlockImage id="applied_more_machines:ae_energizing_orb" scale="6" />

读取当前世界 Powah 充能台配方，需要一个有效 ME 频道。

## 充能棒与材料

外围十六格每格一根充能棒，任意等级可混装，只能玩家手动装卸。
中央九格放配方材料，可正常堆叠，匹配只检查一份配方所需数量，多余材料与无关材料保留。
充能棒不消耗；机器容量等于所有已装充能棒的配置容量之和。
充能棒物品本身保存的电量不转入机器，也不会被机器充电，取出充能棒时，超出新容量的电量直接清除。

## 能量与升级

每 tick 最大加工能量为已装充能棒配置输出量之和，乘以升级倍率。
一个升级槽最多四张加速卡或四张陨石超频卡，每张分别增加 10% 或 20% 输出，同类加法叠加。
满供电时耗时为配方能耗除以每 tick 输出，向上取整，最低 1tick 配方完成时间。
六面接受 FE 和 ME 线缆，联网充电优先 ME 存储中的 FE，再用网络 AE。

## 倍增卡

安装数据能源后，在原有升级槽外增加一个单张倍增卡专槽。产量取整、并行、返还物及ME余量规则见[倍增卡](multiplication_card.md)。
