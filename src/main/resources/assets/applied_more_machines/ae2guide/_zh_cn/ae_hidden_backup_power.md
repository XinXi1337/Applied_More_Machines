---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 323
  title: AE后备隐藏能源
  icon: applied_more_machines:ae_hidden_backup_power
item_ids:
  - applied_more_machines:ae_hidden_backup_power
---

# AE后备隐藏能源

<BlockImage id="applied_more_machines:ae_hidden_backup_power" scale="6" />

专用解决AE断电问题，仅安装应用通量（AppliedFlux）时注册。

## 使用

六面均可连接 ME 网络，需要 **1 个频道**，内置容量 **64,000,000 AE**。
只要本机拥有频道，网络耗电就按以下顺序使用能量：

1. 网络存储中的 FE。
2. 后备隐藏能源内磁盘中的 FE。
3. 后备隐藏能源的内置 AE。
4. 网络其他 AE 储能。

联网状态仅由频道决定，网络暂时缺电但本机仍有频道时，仍可提供后备能源。

## 专用后备磁盘

界面中央只有一个磁盘槽，支持通过 AE 存储接口存储 FE 的磁盘，不限定模组或磁盘型号。
盘内能量在终端中不可见，磁盘为专用后备，由玩家手动装卸。

## 失去频道

没有频道时，每 tick 向六个面分别输出最多 **1,000,000 FE**，优先消耗磁盘，再消耗内置能量。

