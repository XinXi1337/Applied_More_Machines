package com.appliedmoremachines.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class VirtueTools {
    private VirtueTools() {}
    public static boolean isPick(ItemStack stack) {
        return stack.getItem() instanceof VirtueToolItem item && !item.isBlade();
    }
    public static boolean isBlade(ItemStack stack) {
        return stack.getItem() instanceof VirtueToolItem item && item.isBlade();
    }
    public static boolean isCrystal(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("draconicevolution") &&
                (id.getPath().equals("chaos_crystal") || id.getPath().equals("chaos_crystal_part"));
    }
    public static void attack(AttackEntityEvent event) {
        var player = event.getEntity();
        if (!isBlade(player.getMainHandItem())) return;
        event.setCanceled(true);
        if (!player.level().isClientSide && !player.isSpectator())
            com.appliedmoremachines.integration.DraconicVirtueTools.killGuardian(event.getTarget());
    }
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (isPick(event.getEntity().getMainHandItem()) && !isCrystal(event.getLevel().getBlockState(event.getPos())))
            event.setCanceled(true);
    }
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (isPick(event.getPlayer().getMainHandItem()) && !isCrystal(event.getState())) event.setCanceled(true);
    }
}
