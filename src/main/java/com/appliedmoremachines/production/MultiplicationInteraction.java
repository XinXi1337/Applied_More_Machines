package com.appliedmoremachines.production;

import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class MultiplicationInteraction {
    private MultiplicationInteraction() {}
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity(); var level = event.getLevel(); var stack = event.getItemStack();
        if (!player.isShiftKeyDown() || player.isSpectator() || !player.getAbilities().mayBuild
                || !MultiplicationUpgrade.isCard(stack) || !level.mayInteract(player, event.getPos())
                || !player.mayUseItemAt(event.getPos(), event.getHitVec().getDirection(), stack)
                || !(level.getBlockEntity(event.getPos()) instanceof MultiplicationHost host)) return;
        if (!level.isClientSide) {
            var inventory = host.multiplicationUpgrade();
            if (inventory.isEmpty() && inventory.canPlaceItem(0, stack)) {
                inventory.setItem(0, stack.copyWithCount(1));
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
