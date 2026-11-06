package com.appliedmoremachines;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AppliedMoreMachines.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionPedestaBlockEntity>>
            AE_INFUSION_PEDESTA = BLOCK_ENTITIES.register("ae_infusion_pedesta", () ->
                    BlockEntityType.Builder.of(
                            InfusionPedestaBlockEntity::new, ModBlocks.AE_INFUSION_PEDESTA.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingCoreBlockEntity>>
            AE_CRAFTING_CORE = BLOCK_ENTITIES.register("ae_crafting_core", () ->
                    BlockEntityType.Builder.of(
                            CraftingCoreBlockEntity::new, ModBlocks.AE_CRAFTING_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AEEnchantingTableBlockEntity>>
            AE_ENCHANTING_TABLE = BLOCK_ENTITIES.register("ae_enchanting_table", () ->
                    BlockEntityType.Builder.of(
                            AEEnchantingTableBlockEntity::new, ModBlocks.AE_ENCHANTING_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AEFishBlockEntity>> AE_FISH =
            BLOCK_ENTITIES.register("ae_fish", () -> BlockEntityType.Builder.of(AEFishBlockEntity::new, ModBlocks.AE_FISH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryControllerBlockEntity>> AE_FACTORY_CONTROLLER =
            BLOCK_ENTITIES.register("ae_factory_controller", () -> BlockEntityType.Builder.of(
                    FactoryControllerBlockEntity::new, ModBlocks.AE_FACTORY_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryPatternBlockEntity>> AE_FACTORY_PATTERN =
            BLOCK_ENTITIES.register("ae_factory_pattern", () -> BlockEntityType.Builder.of(
                    FactoryPatternBlockEntity::new, ModBlocks.AE_FACTORY_PATTERN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LatexExtractionProcessorBlockEntity>>
            AE_LATEX_EXTRACTION_PROCESSOR = BLOCK_ENTITIES.register("ae_latex_extraction_processor", () ->
                    BlockEntityType.Builder.of(LatexExtractionProcessorBlockEntity::new,
                            ModBlocks.AE_LATEX_EXTRACTION_PROCESSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DissolutionChamberBlockEntity>>
            AE_DISSOLUTION_CHAMBER_FRONT = BLOCK_ENTITIES.register("ae_dissolution_chamber_front", () ->
                    BlockEntityType.Builder.of(DissolutionChamberBlockEntity::new,
                            ModBlocks.AE_DISSOLUTION_CHAMBER_FRONT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidLaserBaseBlockEntity>>
            AE_FLUID_LASER_BASE = ModBlocks.AE_FLUID_LASER_BASE == null ? null : BLOCK_ENTITIES.register("ae_fluid_laser_base", () ->
                    BlockEntityType.Builder.of(FluidLaserBaseBlockEntity::new,
                            ModBlocks.AE_FLUID_LASER_BASE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorControllerBlockEntity>> AE_REACTORS_CONTROLLER =
            ModBlocks.AE_REACTORS_CONTROLLER == null ? null : BLOCK_ENTITIES.register("ae_reactors_controller", () ->
                    BlockEntityType.Builder.of(ReactorControllerBlockEntity::new, ModBlocks.AE_REACTORS_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorPatternBlockEntity>> AE_REACTORS_PATTERN =
            ModBlocks.AE_REACTORS_PATTERN == null ? null : BLOCK_ENTITIES.register("ae_reactors_pattern", () ->
                    BlockEntityType.Builder.of(ReactorPatternBlockEntity::new, ModBlocks.AE_REACTORS_PATTERN.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
