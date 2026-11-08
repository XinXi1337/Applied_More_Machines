---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 321
  title: AE Energizing Orb
  icon: applied_more_machines:ae_energizing_orb
item_ids:
  - applied_more_machines:ae_energizing_orb
---

# AE Energizing Orb

<BlockImage id="applied_more_machines:ae_energizing_orb" scale="6" />

Processes one current-world Powah energizing recipe at a time and requires one active ME channel.

## Inventory

The outer sixteen slots each accept one rod of any tier, installed and removed manually. The inner nine slots accept stacked recipe materials. Matching allocates exactly one recipe; surplus quantities and unrelated items remain. The right output slot rejects all insertion. Rods and upgrade cards are inaccessible to automation.

Capacity starts at zero and equals the sum of the installed rods' configured FE capacities. Rod items retain their own stored energy; it is neither transferred nor charged. Removing a rod discards machine energy exceeding the new capacity.

## Power and speed

Configured rod transfer rates add together. One upgrade slot accepts up to four Speed Cards or four Meteorite Overclocking Cards, adding 10% or 20% per card to total throughput. Energy cost is unchanged. Shift-click or sneak-use held cards to install upgrades.

At full supply, ticks equal recipe energy divided by upgraded throughput, rounded up, minimum one tick. At most one recipe completes per tick. Powah's configured recipe energy multiplier applies. Actual energy is paid each tick, so recipes may cost more than capacity. At least one working rod is required.

All six sides accept FE and ME cables. Charging prioritizes AppliedFlux FE in ME storage, then network AE. The private buffer starts empty and has no idle drain. Missing power/channel or full output pauses work. Paid progress persists across saves. Adding surplus material does not reset a matching recipe; removing required ingredients invalidates work without refunding paid energy. Ingredients are consumed only on completion. Breaking drops actual inventory, not machine energy or work progress.

## Automation and JEI

Pipes and hoppers insert only materials and extract only products on all six sides. Adjacent automatic export and its direction flags start disabled; the left configuration controls active pushing, while ordinary pipes remain usable.

Click the AE progress silhouette for JEI. Rods are display-only catalysts. The transfer button fills one batch of materials and outputs in AE, WTLib and Tianshu pattern terminals, excluding rods. Players still click Encode.

<RecipeFor id="applied_more_machines:ae_energizing_orb" />

## Multiplication Card

With Data Energistics installed, one dedicated single-card slot is added beside the existing upgrade slots. See [Multiplication Card](multiplication_card.md) for rounding, parallel copies, container returns and ME overflow.
