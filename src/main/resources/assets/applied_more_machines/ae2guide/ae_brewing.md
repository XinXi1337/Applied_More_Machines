---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 320
  title: AE Brewing Machine
  icon: applied_more_machines:ae_brewing
item_ids:
  - applied_more_machines:ae_brewing
---

# AE Brewing Machine

<BlockImage id="applied_more_machines:ae_brewing" scale="6" />

Brews exactly three bottles per batch using the world's vanilla brewing service. Requires one active ME channel and no blaze powder fuel. Blaze powder can still be used as a Strength potion ingredient.

## Inputs and outputs

The first three top slots each hold one identical potion, including its item type and components. Regular, splash and lingering potions are supported. The fourth slot holds one valid brewing ingredient and rejects potions. All four inputs are limited to one item each. The three bottom slots are outputs only.

A batch consumes three bottles and one ingredient, producing three bottles. Container remainders, such as the bottle returned by dragon's breath, go directly into ME storage without occupying input or output slots. This ignores the adjacent auto-output switch and direction settings. If ME cannot accept them, the batch waits without consuming ingredients or energy. Any remainder rejected during actual insertion is saved and retried before another batch starts; breaking drops only the undelivered amount.

Alternatively, put one glass bottle in each of the first three slots and leave the fourth empty. With an active channel, three water bottles are produced in one tick without consuming water or processing energy.

## Time and power

Normal brewing takes 20 seconds and costs 16,000 AE per batch. Two upgrade slots accept one type per slot: up to four Speed Cards and four Meteorite Overclocking Cards. Each Speed Card removes one second and each Overclocking Card removes four seconds, with a minimum of one tick. Four of each reach one tick. Shift-click or sneak-use held cards to install them.

The private 256,000 AE buffer starts empty and has no idle drain. All six sides accept FE and ME cables. Charging prioritizes AppliedFlux FE stored in ME, then network AE; without AppliedFlux it uses AE. External FE can charge the buffer, but processing still needs an active channel.

Ingredients remain until the atomic completion payment. Lost power or channel pauses work; full outputs wait without consuming ingredients or repeating charges. Changes to inputs, recipe results or upgrade duration reset progress. Saved progress revalidates real inventory on load. Breaking drops actual items and does not retain energy.

## Water and automation

The left tank is always full of 8 B of vanilla water, including immediately after placement. Water is never consumed. The sealed tank rejects fluid insertion, extraction and player buckets.

Pipes and hoppers on all six sides can insert valid inputs and extract only bottom outputs. Materials and upgrades cannot be extracted automatically. Automatic adjacent output and all selected directions start off. Enable automatic output to configure its six relative directions. Turning it off does not prevent pipes from extracting products. The configuration page locks all inventory slots.

## JEI and patterns

Click the brewing-stand progress silhouette to open JEI. Each page shows three input bottles, one ingredient and three output bottles. The water display is not an input, and no brewing step count is shown.

The JEI transfer button fills processing patterns in AE pattern terminals, WTLib wireless terminals and Tianshu wired/wireless terminals: three input bottles, one ingredient and a single primary output with amount three. Shift also fills exactly one batch. Players still click Encode in the terminal.

<RecipeFor id="applied_more_machines:ae_brewing" />

## Multiplication Card

With Data Energistics installed, one dedicated single-card slot is added beside the existing upgrade slots. See [Multiplication Card](multiplication_card.md) for rounding, parallel copies, container returns and ME overflow.
