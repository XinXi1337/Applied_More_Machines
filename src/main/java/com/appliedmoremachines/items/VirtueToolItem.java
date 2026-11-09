package com.appliedmoremachines.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** No damage component or durability callbacks: these tools never wear out. */
public final class VirtueToolItem extends Item {
    private final boolean blade;
    public VirtueToolItem(boolean blade) { super(new Properties().stacksTo(1)); this.blade = blade; }
    public boolean isBlade() { return blade; }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        return blade; // Suppress damage, knockback, fire and sweep before vanilla attack handling.
    }
    @Override public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return blade || VirtueTools.isCrystal(state);
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return blade ? 1 : VirtueTools.isCrystal(state) ? 1 : 0;
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return !blade && VirtueTools.isCrystal(state);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.applied_more_machines." +
                (blade ? "blade_of_benevolence" : "pickaxe_of_righteousness")).withStyle(ChatFormatting.GRAY));
    }
}
