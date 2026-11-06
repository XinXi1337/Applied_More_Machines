package com.appliedmoremachines;

import appeng.core.definitions.AEItems;
import com.appliedmoremachines.factory.FactoryStructure;
import com.appliedmoremachines.factory.FactoryMachineTiers;
import com.appliedmoremachines.factory.FactoryStructures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Upgrade admission and combined metrics shared by both factory controllers. */
public final class FactoryUpgradeCards {
    private static final ResourceLocation OVERLOAD_CARD = id("ae2cs:overload_card");
    private static final ResourceLocation MEKANISM_SPEED = id("mekanism:upgrade_speed");
    private static final ResourceLocation MEKANISM_SPEED_LEGACY = id("mekanism:speed_upgrade");
    private static final ResourceLocation MEKANISM_STACK = id("mekanism_extras:upgrade_stack");
    private static final ResourceLocation MEKANISM_STACK_LEGACY = id("mekanism:upgrade_stack");
    private static final ResourceLocation MEKANISM_STACK_LEGACY_2 = id("mekanism:stack_upgrade");

    public enum Kind {
        AE_SPEED(4, "item.ae2.speed_card", 10, 64, 0.5D),
        OVERLOAD(4, "gui.applied_more_machines.overload_card", 20, 256, 1.0D),
        MEKANISM_SPEED(16, "item.mekanism.upgrade_speed", 2, 128, 0.1D),
        MEKANISM_STACK(8, "item.mekanism_extras.upgrade_stack", 0, 0, 0.0D);

        private final int limit;
        private final String translationKey;
        private final int ticksReduction;
        private final int parallel;
        private final double secondsReduction;

        Kind(int limit, String translationKey, int ticksReduction, int parallel, double secondsReduction) {
            this.limit = limit;
            this.translationKey = translationKey;
            this.ticksReduction = ticksReduction;
            this.parallel = parallel;
            this.secondsReduction = secondsReduction;
        }

        public int limit() { return limit; }
        public int ticksReduction() { return ticksReduction; }
        public int parallel() { return parallel; }
        public double secondsReduction() { return secondsReduction; }
        public Component label() { return Component.translatable(translationKey); }
    }

    private FactoryUpgradeCards() {}

    public static Kind type(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(AEItems.SPEED_CARD.asItem())) return Kind.AE_SPEED;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (OVERLOAD_CARD.equals(id)) return Kind.OVERLOAD;
        if (MEKANISM_SPEED.equals(id) || MEKANISM_SPEED_LEGACY.equals(id)) return Kind.MEKANISM_SPEED;
        if (MEKANISM_STACK.equals(id) || MEKANISM_STACK_LEGACY.equals(id)
                || MEKANISM_STACK_LEGACY_2.equals(id)) return Kind.MEKANISM_STACK;
        return null;
    }

    public static boolean canAccept(FactoryControllerBlockEntity owner, int slot, ItemStack stack) {
        Kind kind = type(stack);
        if (kind == null || slot < 0 || slot >= FactoryControllerBlockEntity.UPGRADE_COUNT) return false;

        ItemStack current = owner.getUpgrade(slot);
        if (!current.isEmpty() && type(current) != kind) return false;

        for (FactoryControllerBlockEntity controller : controllers(owner)) {
            for (int localSlot = 0; localSlot < FactoryControllerBlockEntity.UPGRADE_COUNT; localSlot++) {
                ItemStack existing = controller.getUpgrade(localSlot);
                if (type(existing) != kind) continue;
                if (!(controller == owner && localSlot == slot) && !existing.isEmpty()) return false;
            }
        }
        // Admission is partial-stack friendly. Slot.getMaxStackSize and setItem
        // clamp the inserted amount to the per-type limit.
        return true;
    }

    public static int count(FactoryControllerBlockEntity owner, Kind kind) {
        int total = 0;
        for (FactoryControllerBlockEntity controller : controllers(owner)) {
            for (int slot = 0; slot < FactoryControllerBlockEntity.UPGRADE_COUNT; slot++) {
                ItemStack stack = controller.getUpgrade(slot);
                if (type(stack) == kind) total += stack.getCount();
            }
        }
        return total;
    }

    public static int parallel(FactoryControllerBlockEntity owner) {
        return metrics(owner).parallel();
    }

    public static int durationTicks(FactoryControllerBlockEntity owner) {
        return metrics(owner).durationTicks();
    }

    private static FactoryUpgradeMath.Metrics metrics(FactoryControllerBlockEntity owner) {
        var base = FactoryUpgradeMath.calculate(count(owner, Kind.AE_SPEED), count(owner, Kind.OVERLOAD),
                count(owner, Kind.MEKANISM_SPEED), count(owner, Kind.MEKANISM_STACK));
        return owner.getLevel() == null ? base : FactoryMachineTiers.installed(
                FactoryStructures.view(owner.getLevel()), owner.formation()).apply(base);
    }

    /**
     * Returns both controllers of a formed factory. An unformed controller only
     * exposes its own two slots, avoiding accidental merging with nearby builds.
     */
    public static List<FactoryControllerBlockEntity> controllers(FactoryControllerBlockEntity owner) {
        if (owner.getLevel() == null || owner.formation() == null) return List.of(owner);
        List<FactoryControllerBlockEntity> result = new ArrayList<>(2);
        for (var offset : FactoryStructure.definition().controllerOffsets()) {
            var pos = owner.formation().position(offset);
            if (!owner.getLevel().hasChunkAt(pos)) continue;
            if (owner.getLevel().getBlockEntity(pos) instanceof FactoryControllerBlockEntity controller
                    && !result.contains(controller)) {
                result.add(controller);
            }
        }
        return result.isEmpty() ? List.of(owner) : List.copyOf(result);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }
}
