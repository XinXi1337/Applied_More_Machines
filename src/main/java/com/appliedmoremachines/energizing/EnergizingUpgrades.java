package com.appliedmoremachines.energizing;
import com.appliedmoremachines.InfusionUpgradeCards;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
public final class EnergizingUpgrades {
 public static boolean isCard(ItemStack s) { return InfusionUpgradeCards.accepts(s); }
 public static boolean accepts(Container c,int slot,ItemStack s) { return slot==26 && isCard(s); }
 public static double multiplier(Container c) {
  ItemStack s=c.getItem(26); return isCard(s) ? 1+Math.min(4,s.getCount())*(InfusionUpgradeCards.isOverload(s)?0.2:0.1):1;
 }
 private EnergizingUpgrades() {}
}
