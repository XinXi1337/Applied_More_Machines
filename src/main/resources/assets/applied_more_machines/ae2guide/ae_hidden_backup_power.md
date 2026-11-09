---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 323
  title: AE Hidden Backup Power
  icon: applied_more_machines:ae_hidden_backup_power
item_ids:
  - applied_more_machines:ae_hidden_backup_power
---

# AE Hidden Backup Power

<BlockImage id="applied_more_machines:ae_hidden_backup_power" scale="6" />

Dedicated to resolving AE power outages. This block and its recipe are available only with AppliedFlux installed.

## Crafting and connection

Place one Flux Accessor (`appflux:flux_accessor`) in the center of a crafting table, surrounded by eight Dense Energy Cells (`ae2:dense_energy_cell`), to craft one AE Hidden Backup Power.

All six sides connect to ME networks. It requires **one channel** and consumes **0 AE/t** while idle. Its internal buffer holds **64,000,000 AE** and starts empty. It accepts network AE and external FE. With a channel, it also actively draws FE from network storage, then its own disk, to charge the internal buffer.

## Network power priority

While the block has a channel, network energy consumption uses these sources in order:

1. FE in ordinary network storage.
2. FE in hidden backup disks.
3. Internal AE in hidden backup power blocks.
4. Other AE energy storage on the network.

Channel availability alone determines the operating mode. A temporarily unpowered network can still receive backup energy while this block retains its channel.

## Dedicated backup disk

The center slot holds one disk capable of storing FE through the AE storage interface, without a mod or item whitelist. Ordinary batteries are not storage disks.

Its contents are hidden from terminals. The network can insert FE into the disk at the lowest priority. This is dedicated backup storage: players insert and remove disks manually, and item pipes cannot access the slot.

## Without a channel

The block outputs up to **1,000,000 FE per tick on each of its six sides**, using disk energy before internal energy. Each side has its own limit; automatic pushing and external extraction share that side's limit for the tick.

## Breaking and saving

Breaking the block preserves its internal energy in the dropped block item, which restores it when placed again. The disk drops separately and retains its contents and energy.
