---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 322
  title: Multiplication Card
  icon: ae2:inscriber
---

# Multiplication Card

With Data Energistics installed, this mod's processing machines with upgrade slots gain one extra dedicated slot for `data_energistics:multiplication_card`. It holds one card. The Universal Factory and AE Reactor share one effect across both controllers, even if cards were installed separately before formation. The enchanting table keeps its existing behavior.

Each native operation produces **50% more, rounded up before parallel copies**: a native 1→1 recipe becomes 1→2; eight copies produce 16, and a native output of 3 becomes 5. This applies to formal items, fluids, chemicals and byproducts. Container returns keep their original amounts. Brewing's three-bottle operation becomes five bottles; the free glass-bottle-to-water-bottle operation stays at three.

Material consumption, parallel limits, processing time and energy costs keep their existing rules. Encode processing patterns with the **original native recipe quantities**; bonus outputs do not change pattern admission or ingredient requests.

Outputs first fill compatible local output slots or tanks. All excess, including any base production that does not fit, goes to ME storage independently of the adjacent auto-export toggle and selected faces. If ME is offline, full or accepts only part, the machine saves the remaining output and pauses further processing until it is delivered. Removing the card does not discard already owned output.

The x8/x64 inscribers keep their 512/1024 output capacities and apply the bonus to every actual parallel copy. An old card in an ordinary inscriber upgrade slot moves into the dedicated slot on the next machine tick. Install cards through the dedicated slot, Shift transfer, or sneak-right-click while holding one.
