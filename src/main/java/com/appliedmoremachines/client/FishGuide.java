package com.appliedmoremachines.client;

import appeng.items.tools.GuideItem;
import com.appliedmoremachines.AppliedMoreMachines;
import guideme.GuidesCommon;
import guideme.PageAnchor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Namespaced addon page discovered in AE2's existing GuideME content folder. */
public final class FishGuide {
    public static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "ae_fish.md");

    private FishGuide() {}

    public static void open(Player player) {
        if (player != null) GuidesCommon.openGuide(player, GuideItem.GUIDE_ID, PageAnchor.page(PAGE));
    }
}
