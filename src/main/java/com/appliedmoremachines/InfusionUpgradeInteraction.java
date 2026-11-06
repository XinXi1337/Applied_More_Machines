package com.appliedmoremachines;

import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Handles sneaking before vanilla skips ordinary held-item block interaction. */
public final class InfusionUpgradeInteraction {
    private InfusionUpgradeInteraction() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var heldStack = event.getItemStack();
        if (!player.isShiftKeyDown() || player.isSpectator() || !player.getAbilities().mayBuild
                || !InfusionUpgradeCards.accepts(heldStack)
                || !level.mayInteract(player, event.getPos())
                || !player.mayUseItemAt(event.getPos(), event.getHitVec().getDirection(), heldStack)) {
            return;
        }
        var blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof InfusionPedestaBlockEntity)
                && !(blockEntity instanceof CraftingCoreBlockEntity)
                && !(blockEntity instanceof AEFishBlockEntity)) {
            return;
        }
        if (!level.isClientSide) {
            int installed = blockEntity instanceof InfusionPedestaBlockEntity altar
                    ? altar.installUpgradeCards(heldStack)
                    : blockEntity instanceof CraftingCoreBlockEntity core ? core.installUpgradeCards(heldStack)
                    : ((AEFishBlockEntity) blockEntity).installUpgradeCards(heldStack);
            if (!player.getAbilities().instabuild) {
                heldStack.shrink(installed);
            }
        }
        // Consume this interaction even when full, avoiding GUI/item use and offhand double handling.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
