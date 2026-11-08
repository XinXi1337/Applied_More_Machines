package com.appliedmoremachines;

import net.minecraft.core.registries.Registries;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedMoreMachines.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.applied_more_machines"))
                    .icon(() -> ModBlocks.AE_ENCHANTING_TABLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) ->
                            ModBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .withTabFactory(RotatingMachineTab::new)
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    private static final class RotatingMachineTab extends CreativeModeTab {
        private final long animationStart = System.nanoTime();
        private ItemStack[] icons;

        private RotatingMachineTab(CreativeModeTab.Builder builder) {
            super(builder);
        }

        @Override
        public ItemStack getIconItem() {
            // Vanilla caches the icon supplier; read the current frame here instead.
            if (icons == null) {
                var available = new ArrayList<ItemStack>();
                if (ModBlocks.AE_INFUSION_PEDESTA_ITEM != null)
                    available.add(ModBlocks.AE_INFUSION_PEDESTA_ITEM.get().getDefaultInstance());
                if (ModBlocks.AE_CRAFTING_CORE_ITEM != null)
                    available.add(ModBlocks.AE_CRAFTING_CORE_ITEM.get().getDefaultInstance());
                available.add(ModBlocks.AE_ENCHANTING_TABLE_ITEM.get().getDefaultInstance());
                icons = available.toArray(ItemStack[]::new);
            }
            int index = (int) (((System.nanoTime() - animationStart) / 1_000_000_000L) % icons.length);
            return icons[index];
        }
    }
}
