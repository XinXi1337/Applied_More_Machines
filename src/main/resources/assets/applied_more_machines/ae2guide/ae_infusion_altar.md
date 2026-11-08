---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  title: AE Infusion Altar
  icon: applied_more_machines:ae_infusion_pedesta
  position: 311
categories:
- machines
item_ids:
- applied_more_machines:ae_infusion_pedesta
---

# AE Infusion Altar

<BlockImage id="applied_more_machines:ae_infusion_pedesta" scale="6" />

The AE Infusion Altar processes Mystical Agriculture infusion and awakening recipes in a single machine powered by an ME network.
It also supports crafting-only seed recipes for enabled tier 1–6 crops from Mystical Agriculture and Mystical Agradditions.
Available recipes follow the world's loaded recipes and data packs, including non-seed infusion and awakening products.

## Quick start

1. Connect any face of the machine to a powered ME network with an available channel, or supply FE through an energy pipe on any face. Its shared internal buffer stores up to **8,000 AE** and starts empty.
2. Put the required materials into the **nine input slots** at the top left. Their positions do not matter; the altar checks material types, quantities and components.
3. Leave space in the output slot on the right. Each completed recipe costs **4,000 AE**, regardless of the number of products.
4. Take the result manually, extract it with item transport, or enable automatic output to an adjacent container.

The machine requires **one channel** on its ME network and has no idle energy draw. All six faces accept FE into the same AE buffer, using AE2's configured FE/AE conversion rate. The FE interface only receives energy; it cannot power other machines. A full buffer rejects further input. FE can charge the buffer independently of the ME connection; drawing AE from the network requires an active node with its channel available.

## Inventory and progress

The nine input slots accept only materials used by currently loaded altar recipes. Items keep their normal stack limits:
four unstackable soul jars must occupy four slots. Soul jars must contain the correct creature; different souls or components are not interchangeable.
Stackable ingredients may be combined or spread over several input slots. Awakening recipes still require their full essence quantities.

The center crystal fills as a recipe progresses. With JEI installed, hover over it to see **Show Recipes** and click to open the altar's recipe category.
The narrow bar to the right shows the same processing progress; hovering over that bar shows its percentage. Progress is separate from stored energy.
The bottom slots are your backpack and hotbar. The output slot only allows extraction.

Insufficient power or a blocked output pauses processing without consuming materials. Materials and all 4,000 AE are paid together at completion.
Changing inputs or the processing speed resets progress. Inventory, energy, settings and unfinished progress are saved with the world.
Breaking the altar drops its items; the dropped block does not retain its AE buffer.

## Upgrades

The four slots on the right each accept one card. AE2 speed cards and Meteorite Overclock Cards from AE2 Crystal Science may be mixed,
with a shared maximum of **four cards**. Overclock cards are available only when that optional mod is installed.

Base processing time is **80 ticks (4 seconds at 20 TPS)**. Each speed card reduces that by 16 ticks; each overclock card reduces it by 20 ticks.
The minimum is one tick. Four speed cards give 16 ticks (0.8 seconds); four overclock cards give one tick. Energy remains 4,000 AE per recipe.

Shift-click a card from your inventory, or hold cards and **Shift + right-click any face** of the altar to install them into empty upgrade slots.
Full slots do not consume cards. In creative mode direct installation does not consume held cards.

## Pattern providers and item transport

Any face accepts recipe materials from an AE2 pattern provider or a standard item pipe. Use a **processing pattern** with the exact required inputs and outputs.
Input slot positions do not need to match the pattern. Automation can extract finished products; it cannot extract raw input materials or access upgrade cards.

For ME autocrafting, return products to the network through a pattern provider that accepts returned items, an import bus, or other suitable item transport.
Use blocking mode or send one recipe at a time if several jobs would compete for the same nine input slots.
The JEI view is a recipe browser; it does not provide automatic ingredient transfer with a “+” button.

## Automatic output

Enable the automatic output button in the left toolbar. The output configuration button appears only while automatic output is enabled.
Open it and activate the desired faces: **front, back, left, right, top or bottom**. All six faces start disabled; selections are saved.
Front/back/left/right rotate with the machine's front; top and bottom always mean up and down.

Each server tick, the altar tries to insert products into adjacent containers on the selected faces, in front/back/left/right/top/bottom order.
It moves only what the destination accepts. If a container is full, rejects the product, or is absent, remaining products stay in the output slot.
Automatic output does not throw items into the world and has no extra AE charge. Return or Escape leaves the configuration page.

## Recipe and optional integrations

<RecipeFor id="applied_more_machines:ae_infusion_pedesta" />

The machine's crafting recipe requires Mystical Agriculture. Its crafting grid uses two crafting units, one molecular assembler,
two infusion pedestals, one infusion altar, two crafting accelerators and one infusion crystal.
Without Mystical Agriculture the block remains registered, but its crafting and processing recipes are unavailable.

JEI is optional and displays nine material cells, actual quantities, and the altar's recipes. Jade is optional and displays stored AE / 8 kAE and network state.
Only AE2 is a direct required content mod; GuideME is already provided by AE2's required dependencies.

## Troubleshooting

- **No progress:** check recipe quantities/components, stored energy, and room in the output slot. When using ME power, also check network power and channel availability; FE can be supplied on any face. Nine inputs do not remove essence quantity requirements.
- **Pattern provider will not deliver:** check its orientation, processing pattern, blocking state and available input space. Invalid recipe materials are rejected.
- **No automatic output:** enable the master switch, activate a face, and place a container that accepts the product directly beside that face.
- **No recipe shortcut:** install JEI. The crystal shortcut is available on the main page with an empty cursor, not on the output configuration page.
- **Only a name in Jade:** check that the altar's AE storage and network state options are enabled in Jade's plugin settings.

## Multiplication Card

With Data Energistics installed, one dedicated single-card slot is added beside the existing upgrade slots. See [Multiplication Card](multiplication_card.md) for rounding, parallel copies, container returns and ME overflow.
