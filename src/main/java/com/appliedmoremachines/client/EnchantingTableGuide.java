package com.appliedmoremachines.client;

import appeng.items.tools.GuideItem;
import com.appliedmoremachines.AppliedMoreMachines;
import guideme.GuidesCommon;
import guideme.PageAnchor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Opens the concise enchanting-table page in AE2's existing GuideME guide. */
public final class EnchantingTableGuide {
    public static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "ae_enchanting_table.md");

    private EnchantingTableGuide() {}

    public static void open(Player player) {
        if (player != null) GuidesCommon.openGuide(player, GuideItem.GUIDE_ID, PageAnchor.page(PAGE));
    }
}
