package com.appliedmoremachines;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(AppliedMoreMachines.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AppliedMoreMachines.MOD_ID);

    // The registry ID intentionally follows the requested spelling: pedesta.
    public static final DeferredBlock<InfusionPedestaBlock> AE_INFUSION_PEDESTA = BLOCKS.register(
            "ae_infusion_pedesta",
            () -> new InfusionPedestaBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_INFUSION_PEDESTA_ITEM =
            ITEMS.registerSimpleBlockItem(AE_INFUSION_PEDESTA);

    public static final DeferredBlock<CraftingCoreBlock> AE_CRAFTING_CORE = BLOCKS.register(
            "ae_crafting_core",
            () -> new CraftingCoreBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_CRAFTING_CORE_ITEM =
            ITEMS.registerSimpleBlockItem(AE_CRAFTING_CORE);

    public static final DeferredBlock<AEEnchantingTableBlock> AE_ENCHANTING_TABLE = BLOCKS.register(
            "ae_enchanting_table",
            () -> new AEEnchantingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)));
    public static final DeferredItem<BlockItem> AE_ENCHANTING_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem(AE_ENCHANTING_TABLE);

    public static final DeferredBlock<AEFishBlock> AE_FISH = BLOCKS.register(
            "ae_fish",
            () -> new AEFishBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FISH_ITEM =
            ITEMS.registerSimpleBlockItem(AE_FISH);

    public static final DeferredBlock<FactoryPatternBlock> AE_FACTORY_PATTERN = BLOCKS.register(
            "ae_factory_pattern",
            () -> new FactoryPatternBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FACTORY_PATTERN_ITEM =
            ITEMS.registerSimpleBlockItem(AE_FACTORY_PATTERN);

    public static final DeferredBlock<FactoryControllerBlock> AE_FACTORY_CONTROLLER = BLOCKS.register(
            "ae_factory_controller",
            () -> new FactoryControllerBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FACTORY_CONTROLLER_ITEM =
            ITEMS.registerSimpleBlockItem(AE_FACTORY_CONTROLLER);

    public static final DeferredBlock<FactoryComponentBlock> AE_FACTORY_CASING = BLOCKS.register(
            "ae_factory_casing",
            () -> new FactoryComponentBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FACTORY_CASING_ITEM =
            ITEMS.registerSimpleBlockItem(AE_FACTORY_CASING);

    public static final DeferredBlock<LatexExtractionProcessorBlock> AE_LATEX_EXTRACTION_PROCESSOR = BLOCKS.register(
            "ae_latex_extraction_processor",
            () -> new LatexExtractionProcessorBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_LATEX_EXTRACTION_PROCESSOR_ITEM =
            ITEMS.registerSimpleBlockItem(AE_LATEX_EXTRACTION_PROCESSOR);

    public static final DeferredBlock<DissolutionChamberBlock> AE_DISSOLUTION_CHAMBER_FRONT = BLOCKS.register(
            "ae_dissolution_chamber_front",
            () -> new DissolutionChamberBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_DISSOLUTION_CHAMBER_FRONT_ITEM =
            ITEMS.registerSimpleBlockItem(AE_DISSOLUTION_CHAMBER_FRONT);

    public static final DeferredBlock<FluidLaserBaseBlock> AE_FLUID_LASER_BASE = com.appliedmoremachines.laser.FluidLaserAvailability.enabled() ? BLOCKS.register(
            "ae_fluid_laser_base",
            () -> new FluidLaserBaseBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL))) : null;
    public static final DeferredItem<BlockItem> AE_FLUID_LASER_BASE_ITEM =
            AE_FLUID_LASER_BASE == null ? null : ITEMS.registerSimpleBlockItem(AE_FLUID_LASER_BASE);

    public static final DeferredBlock<Block> AE_REACTORS_SHELL =
            ModList.get() != null && ModList.get().isLoaded("bigreactors") ? BLOCKS.register(
                    "ae_reactors_shell",
                    () -> new Block(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL))) : null;
    public static final DeferredItem<BlockItem> AE_REACTORS_SHELL_ITEM =
            AE_REACTORS_SHELL == null ? null : ITEMS.registerSimpleBlockItem(AE_REACTORS_SHELL);
    public static final DeferredBlock<ReactorControllerBlock> AE_REACTORS_CONTROLLER =
            ModList.get() != null && ModList.get().isLoaded("bigreactors") ? BLOCKS.register(
                    "ae_reactors_controller",
                    () -> new ReactorControllerBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL))) : null;
    public static final DeferredItem<BlockItem> AE_REACTORS_CONTROLLER_ITEM =
            AE_REACTORS_CONTROLLER == null ? null : ITEMS.registerSimpleBlockItem(AE_REACTORS_CONTROLLER);

    public static final DeferredBlock<ReactorPatternBlock> AE_REACTORS_PATTERN =
            ModList.get() != null && ModList.get().isLoaded("bigreactors") ? BLOCKS.register(
                    "ae_reactors_pattern",
                    () -> new ReactorPatternBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL))) : null;
    public static final DeferredItem<BlockItem> AE_REACTORS_PATTERN_ITEM =
            AE_REACTORS_PATTERN == null ? null : ITEMS.registerSimpleBlockItem(AE_REACTORS_PATTERN);

    public static final DeferredBlock<ReactorFuelRodBlock> AE_REACTORS_FUELROD =
            ModList.get() != null && ModList.get().isLoaded("bigreactors") ? BLOCKS.register(
                    "ae_reactors_fuelrod",
                    () -> new ReactorFuelRodBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)
                            .noOcclusion().isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false))) : null;
    public static final DeferredItem<BlockItem> AE_REACTORS_FUELROD_ITEM =
            AE_REACTORS_FUELROD == null ? null : ITEMS.registerSimpleBlockItem(AE_REACTORS_FUELROD);

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModBlocks::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(AE_INFUSION_PEDESTA_ITEM.get());
            event.accept(AE_CRAFTING_CORE_ITEM.get());
            event.accept(AE_ENCHANTING_TABLE_ITEM.get());
            event.accept(AE_FISH_ITEM.get());
            event.accept(AE_FACTORY_PATTERN_ITEM.get());
            event.accept(AE_FACTORY_CONTROLLER_ITEM.get());
            event.accept(AE_FACTORY_CASING_ITEM.get());
            event.accept(AE_LATEX_EXTRACTION_PROCESSOR_ITEM.get());
            event.accept(AE_DISSOLUTION_CHAMBER_FRONT_ITEM.get());
            if (AE_FLUID_LASER_BASE_ITEM != null) event.accept(AE_FLUID_LASER_BASE_ITEM.get());
            if (AE_REACTORS_SHELL_ITEM != null) event.accept(AE_REACTORS_SHELL_ITEM.get());
            if (AE_REACTORS_FUELROD_ITEM != null) event.accept(AE_REACTORS_FUELROD_ITEM.get());
            if (AE_REACTORS_PATTERN_ITEM != null) event.accept(AE_REACTORS_PATTERN_ITEM.get());
            if (AE_REACTORS_CONTROLLER_ITEM != null) event.accept(AE_REACTORS_CONTROLLER_ITEM.get());
        }
    }
}
