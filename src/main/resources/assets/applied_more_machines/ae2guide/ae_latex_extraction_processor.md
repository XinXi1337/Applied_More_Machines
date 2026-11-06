---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 316
  title: AE Latex Extraction Processor
  icon: applied_more_machines:ae_latex_extraction_processor
item_ids:
  - applied_more_machines:ae_latex_extraction_processor
---

# AE Latex Extraction Processor

<BlockImage id="applied_more_machines:ae_latex_extraction_processor" scale="6" />

Extracts whole logs into latex while independently processing latex and water into dry rubber. All six faces accept ME cables and FE. Work requires an active network and one channel. The private 8,000 AE buffer starts empty and refills from ME or FE. Each process costs 4,000 AE per batch, regardless of parallel count. Idle power is zero.

## Inventory and progress

The lower left slot accepts the minecraft:logs tag; the right slot is output only. Latex and water each have a dedicated 16 B tank. Click a tank with one carried fluid container to empty it. Unreserved latex can also be taken into a container except in Rubber mode; water is input only. The central AE silhouette shows extraction progress. The thin strip shows rubber progress; hovering it displays only the stored energy percentage.

Extraction reads the world's loaded Industrial Foregoing fluid extractor recipes, preferring specific recipes over defaults. Each whole log yields the source latex amount times 2,000 mB in 64 seconds: oak yields 4 B. There is no stripped log return. The installed latex processing unit uses 750 mB latex and 500 mB water for one dry rubber in 8 seconds.

## Modes

The three buttons are mutually exclusive and initially unselected. Click the selected button again to deselect it. With no mode, supply materials manually; both processes run, keep latex locally and send production overflow to ME.

- **Auto (passive):** Pull logs and water from ME. Prefer the highest yield, then the largest stock at that yield. Split each new latex batch once: half goes to ME, half stays for rubber. Local overflow also goes to ME. Existing tank contents are never repeatedly halved. Dry rubber is sent to ME automatically.
- **Latex:** Supply logs manually. Send all latex to ME and cancel the unpaid rubber batch to release its reserved latex. This mode never pulls materials from ME.
- **Rubber:** Supply materials manually. Both processes run, keeping latex for rubber. Stored latex cannot be extracted to ME, pipes or containers; only new production overflow goes to ME. This mode never pulls materials from ME.

## Upgrades

Five shared slots accept one slot per category. Stack up to four Speed Cards and four Meteorite Overclocking Cards in their respective slots. Each Speed Card removes 4 seconds from extraction and 0.5 seconds from rubber. Each Overclocking Card removes 8 seconds and 1 second respectively.

Industrial Foregoing Extras addons accept only Tier 3–6, one per category. Speed reduces remaining time by 20/30/40/50%; Efficiency by 30/40/50/60%. Subtract fixed card reductions first, then multiply both addon factors, rounding up to at least one tick. Processing Tier 3/4/5/6 gives total parallel counts of 4/5/6/7 for both processes. Smaller batches are allowed when materials are limited. Seven oak logs yield 28 B; materials scale, energy per batch does not. A full upgrade set takes 3.2 seconds for extraction and 0.4 seconds for rubber. Shift-click cards or sneak-use held cards to install them.

Batch parameters are captured at start; upgrade changes apply next batch. Working logs cannot be removed, and active rubber ingredients are reserved in the tanks. Power/channel loss pauses progress. Full output or ME refusal preserves ingredients and delays completion without repeated charges. Inventory, fluids, settings, work and already paid pending ME latex persist in saves. Breaking drops real items; fluids and energy are not retained by the block item.

## Input and output

All six sides expose item/fluid interfaces; upgrades are hidden from pipes. Log, water and latex inputs always accept all faces. Automatic adjacent output starts disabled. Enable it to select output faces using the same six-direction layout as the other machines. Latex and dry rubber share this setting, initially with no output faces selected. Automatic output pushes into selected adjacent containers. Turning it off preserves face settings, and pipes may extract from those selected faces. Rubber mode still blocks latex extraction, and reserved ingredients cannot be removed. The configuration page disables all inventory slots.
