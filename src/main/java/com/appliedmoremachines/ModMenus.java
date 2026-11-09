package com.appliedmoremachines;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AppliedMoreMachines.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InfusionPedestaMenu>> AE_INFUSION_PEDESTA =
            ModBlocks.AE_INFUSION_PEDESTA == null ? null : MENUS.register("ae_infusion_pedesta", () ->
                    new MenuType<>(InfusionPedestaMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingCoreMenu>> AE_CRAFTING_CORE =
            ModBlocks.AE_CRAFTING_CORE == null ? null : MENUS.register("ae_crafting_core", () ->
                    new MenuType<>(CraftingCoreMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AEEnchantingTableMenu>> AE_ENCHANTING_TABLE =
            MENUS.register("ae_enchanting_table", () ->
                    new MenuType<>(AEEnchantingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AEFishMenu>> AE_FISH =
            MENUS.register("ae_fish", () -> new MenuType<>(AEFishMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FactoryPatternMenu>> AE_FACTORY_PATTERN =
            ModBlocks.AE_FACTORY_PATTERN == null ? null : MENUS.register("ae_factory_pattern", () -> new MenuType<>(FactoryPatternMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FactoryControllerMenu>> AE_FACTORY_CONTROLLER =
            ModBlocks.AE_FACTORY_CONTROLLER == null ? null : MENUS.register("ae_factory_controller", () ->
                    new MenuType<>(FactoryControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<LatexExtractionProcessorMenu>> AE_LATEX_EXTRACTION_PROCESSOR =
            ModBlocks.AE_LATEX_EXTRACTION_PROCESSOR == null ? null : MENUS.register("ae_latex_extraction_processor", () ->
                    new MenuType<>(LatexExtractionProcessorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<DissolutionChamberMenu>> AE_DISSOLUTION_CHAMBER_FRONT =
            ModBlocks.AE_DISSOLUTION_CHAMBER_FRONT == null ? null : MENUS.register("ae_dissolution_chamber_front", () ->
                    new MenuType<>(DissolutionChamberMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidLaserBaseMenu>> AE_FLUID_LASER_BASE =
            ModBlocks.AE_FLUID_LASER_BASE == null ? null : MENUS.register("ae_fluid_laser_base", () ->
                    new MenuType<>(FluidLaserBaseMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControllerMenu>> AE_REACTORS_CONTROLLER =
            ModBlocks.AE_REACTORS_CONTROLLER == null ? null : MENUS.register("ae_reactors_controller", () ->
                    new MenuType<>(ReactorControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorPatternMenu>> AE_REACTORS_PATTERN =
            ModBlocks.AE_REACTORS_PATTERN == null ? null : MENUS.register("ae_reactors_pattern", () ->
                    new MenuType<>(ReactorPatternMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AEBrewingMenu>> AE_BREWING =
            MENUS.register("ae_brewing", () -> new MenuType<>(AEBrewingMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AEEnergizingMenu>> AE_ENERGIZING_ORB = ModBlocks.AE_ENERGIZING_ORB == null ? null :
            MENUS.register("ae_energizing_orb", () -> new MenuType<>(AEEnergizingMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<AEHiddenBackupPowerMenu>> AE_HIDDEN_BACKUP_POWER =
            ModBlocks.AE_HIDDEN_BACKUP_POWER == null ? null : MENUS.register("ae_hidden_backup_power", () ->
                    new MenuType<>(AEHiddenBackupPowerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
