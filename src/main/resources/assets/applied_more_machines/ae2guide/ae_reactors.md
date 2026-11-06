---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE Reactor
  icon: applied_more_machines:ae_reactors_controller
  position: 319
categories:
- machines
item_ids:
- applied_more_machines:ae_reactors_controller
- applied_more_machines:ae_reactors_pattern
- applied_more_machines:ae_reactors_shell
- applied_more_machines:ae_reactors_fuelrod
---

# AE Reactor

<BlockImage id="applied_more_machines:ae_reactors_controller" scale="6" />

Requires **Extreme Reactors**. Controllers run processing, pattern assemblies hold patterns, and casings and fuel rods complete the structure.

## Use

1. Check the structure and materials in JEI, then use a controller's **Project / Place** buttons. The structure measures **12×11×12 and contains 1,008 blocks**, with four horizontal orientations.
2. Connect both controllers and both pattern assemblies to an active ME network with available channels. Each accepts cables on all six faces, requires one channel and uses no idle power. Each assembly holds 36 patterns.
3. Pattern assemblies belong in the two fixed structure positions. **Casings cannot replace them, and other casing positions cannot become assemblies.** Both controllers share orders, progress and upgrade effects for the same structure.
4. Put **AE processing patterns** in the assemblies and request crafting through AE. Click the controller's **AE silhouette** with an empty cursor to view the processing recipes in JEI.

Pattern inputs and outputs must match the **complete quantities for one native operation shown in JEI**. Manually multiplying quantities, omitting outputs or using incorrect materials makes that pattern invalid; other valid patterns keep working. JEI's transfer button fills supported pattern terminals, but you still need to encode the pattern and place it in an assembly.

## Supported recipes

- **The Reactor**: converts fuel to its corresponding waste using the current Extreme Reactors fuel mappings, preserving item or fluid form and exact quantity ratios.
- **Reprocessor**: uses loaded native reprocessing recipes.
- **Fluidizer**: uses solid fluidizing, solid mixing and fluid mixing recipes.

Recipes and fuel mappings come from the current world, including data-pack changes. **The machine generates no FE and does not process coolant vaporization or steam condensation.**

## Speed, upgrades and energy

Base: **160 ticks (8 seconds), 8 parallel operations**. Each round processes one recipe type; other orders wait. Partial batches process only the copies actually received.

Each controller has **one upgrade slot**, holding up to four AE speed cards or four Meteorite Overclock cards. Across both controllers, each type is limited to four cards and may occupy only one slot. To use both types, place one type in each controller.

- AE speed card: **+64 parallel / −10 ticks** per card, maximum 4.
- Meteorite Overclock card: **+256 parallel / −20 ticks** per card, maximum 4; requires the mod providing that card.

| Upgrades | Parallel operations | Round duration |
| --- | --- | --- |
| None | 8 | 160 ticks (8 seconds) |
| 4 speed cards | 264 | 120 ticks (6 seconds) |
| 4 Meteorite Overclock cards | 1032 | 80 ticks (4 seconds) |
| 4 speed + 4 Meteorite Overclock cards | 1288 | 40 ticks (2 seconds) |

Each round costs **8,000 AE in total**, taking stored FE from ME storage first, then network AE. The cost does not increase with parallel copies. The AE CPU prepares and dispatches materials; the controller determines processing speed and parallelism. **Fluid-only recipes receive no extra 1,000-fold bonus.** Always encode native recipe quantities.

## Pausing and dismantling

Power, channel or structure loss preserves materials and pauses work. The interrupted round restarts its timer on recovery. Cancelling returns unconsumed inputs. When storage is full, products remain pending until space becomes available.

To dismantle, the same player must click **Dismantle** eight times, with no more than one second between clicks. Empty the structure's Sky Stone Tanks first and leave enough room in ME storage or your inventory. Both controllers remain; other structure blocks and stored patterns return to ME storage first, then your inventory.
