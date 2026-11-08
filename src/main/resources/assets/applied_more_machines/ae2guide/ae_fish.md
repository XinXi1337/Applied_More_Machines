---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE Fishing Machine
  icon: applied_more_machines:ae_fish
  position: 314
categories:
- machines
item_ids:
- applied_more_machines:ae_fish
---

# AE Fishing Machine

<BlockImage id="applied_more_machines:ae_fish" scale="6" />

Insert a vanilla fishing rod into the lower-left slot. Enchantments have no effect and the rod loses no durability. The upper 27 slots hold catches. No water structure is required; location-dependent loot conditions use the machine's position.

The machine stores **256,000 AE** and needs **one active ME channel**. All six faces accept ME/FE power. Item interfaces are **extraction only**; insert the rod and upgrade cards by hand.

A cycle takes **200 ticks** and performs one draw, costing **8,000 AE per successful draw**. Empty catches cost no energy. Each speed card subtracts 30 ticks and adds one draw; each overload card subtracts 40 ticks and adds two draws. Four shared upgrade slots accept either card. Shift-right-click the machine with cards to install them. Card and category changes apply to the next cycle.

The three buttons select **fish**, **treasure**, or **modded catches**. Select one; click it again to return to normal fishing. Modded catches are items outside the `minecraft` namespace, from loaded fishing loot. Fishing-table weights, conditions and item data are retained. The fourth button independently toggles **Block unstackable items**, combined with any category. It applies to unpaid draws and keeps existing catches. Each cycle performs its set number of attempts, including misses; it never waits to fill a successful batch.

The whole cycle's energy must be available before sampling. Missing power, rod or channel pauses progress. Full output slots retain the exact paid batch until space is available; work survives closing the screen and world reloads. Breaking the machine drops its inventory and any paid batch.

Productive Bees fishing recipes also yield the corresponding **bee spawn eggs** in normal or modded-only mode. Their original chance rules apply, without biome restrictions; disabled bee recipes remain unavailable.

Enable automatic output on the left, then select the adjacent output faces. External pipes may extract catches from any face; they cannot insert items or access the rod/upgrades.

## Multiplication Card

With Data Energistics installed, one dedicated single-card slot is added beside the existing upgrade slots. See [Multiplication Card](multiplication_card.md) for rounding, parallel copies, container returns and ME overflow.
