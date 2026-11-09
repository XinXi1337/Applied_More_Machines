---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE Crafting Core
  icon: applied_more_machines:ae_crafting_core
  position: 312
categories:
- machines
item_ids:
- applied_more_machines:ae_crafting_core
---

# AE Crafting Core

<BlockImage id="applied_more_machines:ae_crafting_core" scale="6" />

The AE Crafting Core runs the world's loaded Draconic Evolution fusion recipes, including data-pack changes.
It preserves each recipe's central catalyst, ingredient quantities, minimum injector tier, original FE cost and data-aware product assembly.
Draconic Evolution is optional: without it, the machine remains registered, but its crafting and fusion recipes are unavailable.

## Quick start

1. Put fusion injectors in the **top center slot**. Each injector unlocks one of the sixteen side slots. Equipment stacks contain one tier only, up to **16 injectors**; they are not consumed.
2. Put the recipe's central catalyst in the **bottom center slot**. Put the other ingredients in unlocked side slots, in any order.
3. Supply AE through an active ME connection or FE through any face. The shared buffer starts empty and holds **256,000,000 AE (256 M AE)**.
4. When complete, the catalyst is replaced by the product in the same bottom center slot. Remove that product before starting another recipe.

## Injectors and material slots

Supported tiers, from lowest to highest, are **Basic, Wyvern, Draconic and Chaotic**.
Higher-tier equipment supports lower-tier recipes. Injector count controls the number of usable side slots.
Identical stackable ingredients may share a slot: if eight recipe entries fit into four stacks, four installed injectors are enough.
The machine still checks every required item, quantity and component within those unlocked slots. Unstackable materials need separate slots.
Slots unlock in the left panel first, then the right panel, row by row. Locked slots cannot accept materials.

Injectors cannot be removed or replaced once processing has paid energy or made progress, including a pause for power.
When idle, removing injectors returns materials from newly locked slots to the operating player's inventory.
If the inventory cannot hold all of them, the removal is cancelled; items are not discarded.
Take equipment into your inventory rather than using the drop key on its slot.

The central catalyst and side ingredients may be stacked up to their native item limits. Matching requires enough of each ingredient; each operation consumes only one recipe's required quantities.
The center still becomes the product. Unused catalyst is returned to unlocked side slots, preserving its components. If there is insufficient space, crafting waits without consuming materials or discarding the surplus.
Side ingredients and products retain their native stack limits and components. Non-consumed ingredients remain; crafting container remainders need free unlocked space.

## Energy and progress

The capacity is **256 M AE**, not 256 M FE. Recipe energy is the original **FE** value, converted using AE2's configured FE/AE ratio.
Energy is drawn progressively and saved as paid work. Recipes costing more than the buffer, including one-tick recipes, accumulate payment across refills.
Insufficient power pauses processing without consuming ingredients; materials and the product are committed only when processing and payment are complete.
There is no extra flat recipe charge. The center cube shows processing progress; hover over it for a percentage, not an energy reading.
With JEI installed, click the cube with an empty cursor to open the **AE Crafting Core** recipe category.
It shows the machine's available loaded recipes, their materials and counts, minimum injector tier and product.
Hover over a recipe's cube for its original FE cost and base duration. This entry opens recipe lookup; it does not fill ingredients automatically.

The machine consumes **one ME channel** and has zero idle usage. ME charging draws actual network energy only while its node is active.
All six faces accept FE into the same private AE buffer, independently of ME charging. The FE interface only receives energy.
Jade, when installed, shows real stored AE, maximum capacity and network state.

Inventory, settings, energy and unfinished paid work are saved. Reloading does not charge the saved work again.
Changing recipe inputs cancels the current job; already spent work energy is not refunded. Changing speed resets elapsed time but preserves payment for the same job.
Breaking the machine drops its items, but the dropped block does not retain its energy or unfinished work.

## Upgrades

There are four one-card upgrade slots on the right. AE2 speed cards and optional AE2 Crystal Science Meteorite Overclock Cards can be mixed.
Processing time is **max(1, 160 − 20 × speed cards − 40 × overclock cards) ticks**.
At 20 TPS the base is eight seconds. Four speed cards give 80 ticks; four overclock cards give one tick. Cards do not reduce recipe energy.
Shift-click cards from your inventory or hold them and Shift + right-click any face to install them in empty slots.

## Item transport and automatic output

All six faces accept recipe materials from item pipes and pattern providers. Automation tries the recognized central catalyst first, then unlocked side slots.
It does not install or extract equipment or upgrade cards, and can extract only completed products.
Shift-clicking an injector prioritizes the equipment slot; to use an injector as a recipe catalyst, place it manually in the bottom center slot.
Manual placement does not rearrange your materials. If an automatic batch initially reserves the center for another recipe's catalyst,
the completed set of ingredients is checked against real recipes and the correct catalyst is moved into place without losing the other materials.
Use processing patterns and send one job at a time; retrieve products through suitable ME item transport.

Enable automatic output in the left toolbar, then open the output configuration button that appears.
All six output faces default to disabled. Enable the desired adjacent containers; front/back/left/right follow machine orientation, while top/bottom remain vertical.
Only accepted products move; full, missing or rejecting containers leave the rest in the center slot.
Return or Escape leaves configuration. The guide button opens this page.

## Crafting and troubleshooting

<RecipeFor id="applied_more_machines:ae_crafting_core" />

- **No progress:** verify the central catalyst and sufficient quantity, space for unused catalyst, enough unlocked slots, the required injector tier, complete materials and available energy.
- **Injectors will not come out:** processing is active or paused for energy, or your inventory cannot hold the materials that would be returned.
- **Pipes reject materials:** install injectors first, check the loaded recipe, central-slot occupancy and native stack limits. Automation cannot insert into locked slots.
- **No automatic output:** enable both the master switch and a side, and place an accepting container directly beside it.
- **No recipe without Draconic Evolution:** this is expected; AE2 is the only directly required content mod.

## Multiplication Card

With Data Energistics installed, one dedicated single-card slot is added beside the existing upgrade slots. See [Multiplication Card](multiplication_card.md) for rounding, parallel copies, container returns and ME overflow.
