---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 318
  title: AE Fluid Laser Drill
  icon: applied_more_machines:ae_fluid_laser_base
item_ids:
  - applied_more_machines:ae_fluid_laser_base
---

# AE Fluid Laser Drill

<BlockImage id="applied_more_machines:ae_fluid_laser_base" scale="6" />

Requires Industrial Foregoing and Industrial Foregoing: Extra Upgrades (ifeu). The machine is not registered without ifeu. Imports the world's loaded fluid-drill recipes for the purple, sculk and dragon lenses. The catalyst structures replace the original entity, biome, dimension and depth requirements.

## Equipment

The lower-left top row holds one purple, sculk and dragon lens, left to right. The bottom row holds the matching lane's Industrial Foregoing laser drills, up to 36 per slot. All equipment is permanent and may only be inserted or removed by players. Shift-click routes items to their permitted fixed slots; pipes and hoppers cannot access any installed items.

The upper-left group requires three wither skeleton skulls in its top row, three soul sand blocks in its second row and one soul sand block in the bottom centre. The middle column requires a sculk shrieker, sculk and deepslate, from top to bottom. The right grid requires end crystals in all four corners, obsidian at the four edge centres and a dragon egg in the centre. Each catalyst slot holds one item and the positions must match.

Each lane needs its entire catalyst group, matching lens and at least one drill. Missing equipment in one lane does not disable the other lanes. Processing requires an active ME channel and 4,000 AE.

## Production and limits

The machine shares one progress cycle. The base cycle is 8 seconds and costs 4,000 AE total, regardless of the number of lanes or processing parallel. Lanes due during that cycle commit together. Each lane produces (10 + 5 × its drill count) mB, multiplied by its processing parallel. At 36 drills and seven parallel this is 1,330 mB per lane per completed batch. Equipment is never consumed.

Three output tanks hold 64 B each and correspond to the three lenses, left to right. Fluids cannot be inserted. Click a tank while carrying one empty fluid container to extract fluid. Tanks use native fluid animation and the AE glyph shows cycle progress.

Without a Healing addon each fluid is limited to an average of 80 mB/second. Healing Tiers 1–6 raise this to 160, 320, 640, 1,280, 2,560 mB/second and unlimited. Each lane has its own rate clock. Limited lanes wait longer rather than losing any batch output. Any full tank, or insufficient room for any ready lane's full batch, pauses the whole machine without charging energy. Freeing space resumes work.

## Upgrades

The first five slots use the AE Dissolution Chamber rules: up to four Acceleration Cards, four Meteorite Overclock Cards and one each of Speed, Efficiency and Processing addons, Tiers 3–6. Each kind occupies a single slot. The sixth slot is dedicated to one Healing addon, Tier 1–6. Shift-click installation and sneak-right-click installation are supported.

The base duration is 160 ticks. Each Acceleration Card subtracts 10 ticks; each Overclock Card subtracts 20. Speed Tiers 3–6 multiply duration by 80%/70%/60%/50%, and Efficiency by 70%/60%/50%/40%, rounded up to at least one tick. Processing Tiers 3–6 provide total parallel 4/5/6/7. Maximum speed is 8 ticks before rate limiting. Equipment, catalyst, upgrade or recipe changes reset unpaid work.

## Power and output

The private buffer holds 256,000 AE, starts empty and has no idle cost. All six faces accept ME connections and FE input. With an active channel, charging follows the Dissolution Chamber: stored AppliedFlux FE first, then network AE; without AppliedFlux it safely uses AE.

The left toolbar opens GuideME and controls automatic output and its six relative directions. Automatic output and all directions start disabled. The machine pushes only what adjacent pipes or tanks accept, and never automatically deposits fluids into ME storage. Selected directions still allow pipe extraction when automatic pushing is off. The configuration page disables all inventory slots.

Equipment, tanks, upgrades, output settings, rate clocks and progress persist. No power, no channel or full tanks pause work. Breaking the machine drops its actual items; fluids and energy are not saved onto the dropped block.

## JEI recipe display

With JEI installed, open recipes from the machine item or the central AE progress glyph on the main screen. The click area is disabled on the configuration page or while carrying an item. Each page shows the matching lens lane's fixed catalysts, lens, one drill and output tank; all equipment is retained. One drill without upgrades produces 15 mB per round in 8 seconds for 4,000 AE total. More drills and upgrades change output and speed according to the machine rules. Hover the AE glyph on the JEI page for the base parameters.
