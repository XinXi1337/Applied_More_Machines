package com.appliedmoremachines;

import appeng.api.stacks.AEKey;
import appeng.api.AECapabilities;
import com.appliedmoremachines.processing.SeedRecipes;
import com.appliedmoremachines.fusion.CoreRecipes;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AppliedMoreMachines.MOD_ID)
public final class AppliedMoreMachines {
    public static final String MOD_ID = "applied_more_machines";
    public static final Logger LOGGER = LogUtils.getLogger();
    public AppliedMoreMachines(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(AppliedMoreMachines::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, com.appliedmoremachines.production.MultiplicationInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, InfusionUpgradeInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DissolutionUpgradeInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BrewingUpgradeInteraction::onRightClickBlock);
        if (ModBlocks.AE_ENERGIZING_ORB != null) NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EnergizingUpgradeInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FluidLaserUpgradeInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LatexUpgradeInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FactoryTierUpgradeInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent event) -> {
            SeedRecipes.invalidate();
            com.appliedmoremachines.brewing.BrewingRecipes.invalidate();
            com.appliedmoremachines.laser.FluidLaserRecipes.invalidate();
            com.appliedmoremachines.dissolution.DissolutionRecipes.invalidate();
            com.appliedmoremachines.latex.LatexRecipes.invalidate();
            CoreRecipes.invalidate();
            com.appliedmoremachines.fishing.FishingLoot.invalidate();
            com.appliedmoremachines.factory.FactoryRecipes.invalidate();
            com.appliedmoremachines.reactor.ReactorRecipes.invalidate();
        });
        LOGGER.info("Applied:MoreMachines loaded; AE2 API: {}", AEKey.class.getName());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var type : java.util.List.of(ModBlockEntities.INSCRIBER_X8.get(), ModBlockEntities.INSCRIBER_X64.get())) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type, (machine, side) -> machine);
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, appeng.blockentity.powersink.AEBasePoweredBlockEntity::getEnergyStorage);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, appeng.blockentity.AEBaseInvBlockEntity::getExposedItemHandler);
            event.registerBlockEntity(AECapabilities.CRANKABLE, type, appeng.blockentity.misc.InscriberBlockEntity::getCrankable);
        }
        if (ModBlockEntities.AE_ENERGIZING_ORB != null) {
            event.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.AE_ENERGIZING_ORB.get(), (machine, side) -> machine);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.AE_ENERGIZING_ORB.get(), (machine, side) -> machine.getForgeEnergyStorage());
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.AE_ENERGIZING_ORB.get(), (machine, side) -> machine.getAutomationInventory());
        }
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.AE_BREWING.get(), (machine, side) -> machine);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.AE_BREWING.get(), (machine, side) -> machine.getForgeEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AE_BREWING.get(), (machine, side) -> machine.getAutomationInventory());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.AE_BREWING.get(), (machine, side) -> machine.getWaterDisplay());
        if (ModBlockEntities.AE_REACTORS_CONTROLLER != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_REACTORS_CONTROLLER.get(), (machine, side) -> machine);
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_REACTORS_PATTERN.get(), (machine, side) -> machine);
        }
        if (ModBlockEntities.AE_FLUID_LASER_BASE != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_FLUID_LASER_BASE.get(), (machine, side) -> machine);
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.AE_FLUID_LASER_BASE.get(), (machine, side) -> machine.getForgeEnergyStorage());
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.AE_FLUID_LASER_BASE.get(), (machine, side) -> machine.getFluidHandler(side));
        }
        if (ModBlockEntities.AE_DISSOLUTION_CHAMBER_FRONT != null) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.AE_DISSOLUTION_CHAMBER_FRONT.get(), (machine, side) -> machine.getAutomationInventory(side));
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.AE_DISSOLUTION_CHAMBER_FRONT.get(), (machine, side) -> machine.getFluidHandler(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_DISSOLUTION_CHAMBER_FRONT.get(), (machine, side) -> machine);
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.AE_DISSOLUTION_CHAMBER_FRONT.get(), (machine, side) -> machine.getForgeEnergyStorage());
        }
        if (ModBlockEntities.AE_LATEX_EXTRACTION_PROCESSOR != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_LATEX_EXTRACTION_PROCESSOR.get(), (machine, side) -> machine);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.AE_LATEX_EXTRACTION_PROCESSOR.get(), (machine, side) -> machine.getAutomationInventory(side));
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.AE_LATEX_EXTRACTION_PROCESSOR.get(), (machine, side) -> machine.getFluidHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.AE_LATEX_EXTRACTION_PROCESSOR.get(), (machine, side) -> machine.getForgeEnergyStorage());
        }
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.AE_FISH.get(), (fish, side) -> fish);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AE_FISH.get(), (fish, side) -> fish.getAutomationInventory());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.AE_FISH.get(), (fish, side) -> fish.getForgeEnergyStorage());
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.AE_ENCHANTING_TABLE.get(), (table, context) -> table);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.AE_ENCHANTING_TABLE.get(), (table, side) -> table.getForgeEnergyStorage());
        if (ModBlockEntities.AE_INFUSION_PEDESTA != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_INFUSION_PEDESTA.get(), (altar, context) -> altar);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.AE_INFUSION_PEDESTA.get(), (altar, side) -> altar.getAutomationInventory());
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.AE_INFUSION_PEDESTA.get(), (altar, side) -> altar.getForgeEnergyStorage());
        }
        if (ModBlockEntities.AE_CRAFTING_CORE != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_CRAFTING_CORE.get(), (core, context) -> core);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.AE_CRAFTING_CORE.get(), (core, side) -> core.getAutomationInventory());
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.AE_CRAFTING_CORE.get(), (core, side) -> core.getForgeEnergyStorage());
        }
        if (ModBlockEntities.AE_FACTORY_CONTROLLER != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_FACTORY_CONTROLLER.get(), (controller, side) -> controller);
        }
        if (ModBlockEntities.AE_FACTORY_PATTERN != null) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    ModBlockEntities.AE_FACTORY_PATTERN.get(), (pattern, side) -> pattern.isStructureFormed() ? pattern : null);
        }
    }
}
