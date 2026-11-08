package com.appliedmoremachines.client;

import appeng.client.gui.Icon;
import com.appliedmoremachines.production.MultiplicationSlot;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MultiplicationScreen {
    private MultiplicationScreen() {}
    /** Extend the existing frame by one row, retaining its own colors and padding. */
    public static int panelHeight(net.minecraft.world.inventory.AbstractContainerMenu menu, int original) {
        for (Slot slot : menu.slots) {
            if (slot instanceof MultiplicationSlot && slot.isActive()) return original + 18;
        }
        return original;
    }
    public static void background(GuiGraphics graphics, Slot slot) {
        int x = slot.x, y = slot.y;
        Icon.SLOT_BACKGROUND.getBlitter().dest(x - 1, y - 1).blit(graphics);
        if (!slot.hasItem()) Icon.BACKGROUND_UPGRADE.getBlitter().dest(x, y).blit(graphics);
    }
    public static boolean tooltip(GuiGraphics graphics, Font font, Slot hovered, ItemStack carried,
            int x, int y, Function<ItemStack, List<Component>> itemTooltip) {
        if (!(hovered instanceof MultiplicationSlot) || !hovered.isActive() || !carried.isEmpty()) return false;
        List<Component> lines = new ArrayList<>();
        if (hovered.hasItem()) { lines.addAll(itemTooltip.apply(hovered.getItem())); lines.add(Component.empty()); }
        lines.add(Component.translatable("gui.applied_more_machines.available_upgrades"));
        lines.add(Component.translatable("gui.applied_more_machines.upgrade_limit",
                Component.translatable("item.data_energistics.multiplication_card"), 1).withStyle(net.minecraft.ChatFormatting.GRAY));
        graphics.renderComponentTooltip(font, lines, x, y);
        return true;
    }
}
