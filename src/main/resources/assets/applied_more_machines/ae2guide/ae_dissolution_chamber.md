---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 317
  title: AE Dissolution Chamber
  icon: applied_more_machines:ae_dissolution_chamber_front
item_ids:
  - applied_more_machines:ae_dissolution_chamber_front
---

# AE Dissolution Chamber

<BlockImage id="applied_more_machines:ae_dissolution_chamber_front" scale="6" />

Uses the world's loaded Industrial Foregoing dissolution chamber recipes and IFEU fluid crafting table, precision crafting table, and infuser recipes. Precision recipes always succeed; repeated materials in recipes with more than eight entries are merged and shown with stack counts in JEI. The eight input slots match total quantities in any arrangement. Repeated materials may share a stack. Base duration is 8 seconds and cost is 4,000 AE per batch, including parallel batches. Processing needs an active ME network and one channel.

## Inventory and fluids

The eight surrounding slots accept native item stacks. The center tank holds 64 B of input fluid; the three right slots hold item outputs and the tall right tank holds 64 B of output fluid. Outputs reject insertion. Inputs accept all six faces; pipes and hoppers cannot extract inputs, but players may retrieve them. Click a tank with one carried fluid container to exchange fluid. The input tank allows retrieval; the output tank only allows extraction. Fluids use native animation and tint. The AE silhouette shows progress. With JEI installed, hovering shows its recipe hint and clicking opens this machine's recipes.

Ingredients stay in the inventory until completion. Removing required materials or changing the active recipe on reload resets progress. Missing energy or channel pauses work. Full outputs wait without consumption or repeated charges. Crafting container remainders need output space too. Inventory, fluids, upgrades, settings and progress persist. Breaking drops real items; the block item does not retain fluids or energy.

## Upgrades

Five shared slots accept one slot per category: up to four Speed Cards, four Meteorite Overclocking Cards, and one each of Industrial Foregoing Extras Speed, Efficiency and Processing addons at Tier 3–6. Shift-click or sneak-use held cards to install them.

Each Speed Card removes 0.5 seconds; each Overclocking Card removes 1 second. Fixed reductions apply first, then multiply by Speed factors 80/70/60/50% and Efficiency factors 70/60/50/40%, rounding up to at least one tick. Processing tiers give total parallel counts 4/5/6/7. Materials and outputs scale; energy stays at 4,000 AE per batch. Smaller batches are allowed when supplies or output space are limited. Full upgrades take 0.4 seconds. Upgrade effects are captured at batch start and changes apply next batch.

## Output and power

Automatic adjacent output starts off. Enable it to configure six relative output faces, initially all off. Items and fluids share the selection. Only accepted quantities move into adjacent containers. Turning automatic output off keeps directions; pipes may still extract products from selected faces. The configuration page disables all inventory slots.

The private 256,000 AE buffer starts empty with zero idle use. All six sides accept ME cables and FE. With an active channel, charging prioritizes AppliedFlux FE stored in ME, then network AE. Missing AppliedFlux safely falls back to AE. JEI lists the recipes; click the AE progress silhouette on the main page with an empty cursor to open the category.

## Multiplication Card

With Data Energistics installed, one dedicated single-card slot is added beside the existing upgrade slots. See [Multiplication Card](multiplication_card.md) for rounding, parallel copies, container returns and ME overflow.
