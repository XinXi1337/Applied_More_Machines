---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE Universal Factory
  icon: applied_more_machines:ae_factory_controller
  position: 315
categories:
- machines
item_ids:
- applied_more_machines:ae_factory_controller
- applied_more_machines:ae_factory_pattern
- applied_more_machines:ae_factory_casing
---

# AE Universal Factory

<BlockImage id="applied_more_machines:ae_factory_controller" scale="6" />

The controller runs recipes, pattern assemblies hold patterns, and casings complete the structure. Requires Mekanism and MekMM's Ultimate Oxidizing Factory; chemical processing also requires Applied Mekanistics.

## Use

1. Follow the JEI structure preview, or use the controller's **Project / Place** buttons. Both controllers share processing; 26 casing positions may become pattern assemblies.
2. Connect controllers and pattern assemblies to an active ME network with channels. Put **AE processing patterns** in the assemblies and order through AE. Each assembly holds 36 patterns.
3. Click the controller's **AE silhouette** with an empty cursor to view all processing recipes in JEI: inputs on the left, products on the right.

Patterns must match the **complete quantities for one operation**: encoding 2→2 for a native 1→1 recipe is rejected, while other valid patterns keep working. Chemical amounts are the unupgraded machine's full-operation consumption; speed does not reduce materials. Chance byproducts become **100% guaranteed**.

## Supported machine recipes

Loaded recipes, including data-pack changes, are supported for these machines in the structure:

- Ultimate Smelting Factory
- Ultimate Infusing Factory (metallurgic infusing)
- Ultimate Enriching Factory
- Ultimate Crushing Factory
- Ultimate Sawing Factory
- Ultimate Purifying Factory
- Ultimate Injecting Factory
- Ultimate Compressing Factory
- Ultimate Oxidizing Factory (MekMM)
- Chemical Infuser
- Electrolytic Separator
- Chemical Dissolution Chamber
- Chemical Washer
- Nutritional Liquifier
- Pressurized Reaction Chamber
- Rotary Condensentrator (both directions)
- Thermal Evaporation Controller

Extra recipe: **1 B sodium → 1 B superheated sodium**. Encode 1000 mB of each chemical.

## Speed, upgrades and energy

Base: **160 ticks, 8 parallel operations**. The two controllers have four upgrade slots in total; each upgrade type occupies only one slot. Per card: AE speed **+64 parallel / −10 ticks** (max 4); Meteorite Overclock **+256 / −20 ticks** (max 4); Mekanism speed **+128 / −2 ticks** (max 16).

Without stack upgrades, each round runs one recipe type. Each stack upgrade allows one more type, up to 8 upgrades. Spare capacity boosts copies per recipe: 8 upgrades with one type give **8 times the copies**; eight or nine simultaneous types each use base parallelism. This boost does not shorten the round.

Recipes whose inputs and outputs are entirely fluids or chemicals process **1,000 times as many copies per round**, in addition to the stack bonus. At 8 parallel operations, a 1 mB water to 1 mB steam recipe can process 8,000 copies per round. Encode the original recipe amounts; recipes containing items do not receive this bonus.

Each round costs **8,000 AE in total**, taking stored FE from ME storage first, then network AE. The AE CPU prepares and dispatches materials; the controller determines processing speed and parallelism.
Power, channel or structure loss preserves materials and pauses work; the interrupted round restarts its timer on recovery. Cancelling returns unconsumed inputs. Without Applied Mekanistics, only chemical recipes pause.

## Upgrade the whole factory

After all orders finish, hold enough Mekanism Extras **tier installers** and right-click either controller. Every structure machine with a registered factory at that tier is replaced; other machines stay in place. Inventories and settings use native upgrade data migration. Failed upgrades consume no installers.

| Target tier | Installers | Parallel bonus | Recipe duration |
| --- | --- | --- | --- |
| Absolute | 16 | +256 | Card-adjusted duration |
| Supreme | 16 | +512 | Card-adjusted duration |
| Cosmic | 32 | +2048 | Half the card-adjusted duration, rounded up |
| Infinite | 64 | (Card-adjusted parallelism +2048) ×2 | 1 tick |

Only the current machine tier contributes a bonus. Without cards, parallelism is **264 / 520 / 2056 / 4112** respectively. Absolute and Supreme can skip lower tiers. Cosmic requires Supreme machines; Infinite requires Cosmic machines. Running, queued and pending storage-return orders all block upgrades.
