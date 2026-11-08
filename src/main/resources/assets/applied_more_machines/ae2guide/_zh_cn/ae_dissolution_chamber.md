---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 317
  title: AE溶解成型机
  icon: applied_more_machines:ae_dissolution_chamber_front
item_ids:
  - applied_more_machines:ae_dissolution_chamber_front
---

# AE溶解成型机

<BlockImage id="applied_more_machines:ae_dissolution_chamber_front" scale="6" />

读取当前世界实际加载的工业先锋溶解成型机，以及 IFEU 流体工作台、精密工作台和灌注器配方。
精密工作台配方按 100% 成功处理，不保留源配方概率；超过八条材料的配方会在 JEI 中合并同种材料并显示堆叠数量，加工仍按完整材料数量匹配。
八个输入格按材料总数匹配，不要求摆放位置，同种材料可以堆在一格。
配方完成基础用时8秒，每批消耗4,000 AE，并行不会增加每批电费，加工需要活跃ME网络及一个频道。
八个环形格为物品输入，遵守原生堆叠上限，中央为 64B输入流体槽，右侧三个格为物品产物。

## 升级

右侧五个共享槽，每种类型只能占一槽：加速卡最多4张、陨石超频卡最多4张，工业先锋扩展的速度、效率、处理插件各一张，只接受Tier 3–6。
每张加速卡缩短0.5秒，每张陨石超频卡缩短1秒。
先扣固定时间，再乘速度Tier 3/4/5/6的80%/70%/60%/50%及效率的70%/60%/50%/40%，向上取整至少1 tick。
处理插件提供总并行4/5/6/7，耗材和产物同时翻倍，电费仍为每批4,000 AE。材料或产物空间不足时可降低并行。

## 倍增卡

安装数据能源后，在原有升级槽外增加一个单张倍增卡专槽。产量取整、并行、返还物及ME余量规则见[倍增卡](multiplication_card.md)。
