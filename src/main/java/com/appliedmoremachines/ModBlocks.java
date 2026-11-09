package com.appliedmoremachines;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import java.util.function.Supplier;
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
    public static final DeferredBlock<InfusionPedestaBlock> AE_INFUSION_PEDESTA = registerMachine(
            "ae_infusion_pedesta",
            () -> new InfusionPedestaBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_INFUSION_PEDESTA_ITEM =
            registerMachineItem(AE_INFUSION_PEDESTA);

    public static final DeferredBlock<CraftingCoreBlock> AE_CRAFTING_CORE = registerMachine(
            "ae_crafting_core",
            () -> new CraftingCoreBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_CRAFTING_CORE_ITEM =
            registerMachineItem(AE_CRAFTING_CORE);

    public static final DeferredBlock<AEEnchantingTableBlock> AE_ENCHANTING_TABLE = registerMachine(
            "ae_enchanting_table",
            () -> new AEEnchantingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)));
    public static final DeferredItem<BlockItem> AE_ENCHANTING_TABLE_ITEM =
            registerMachineItem(AE_ENCHANTING_TABLE);

    public static final DeferredBlock<AEFishBlock> AE_FISH = registerMachine(
            "ae_fish",
            () -> new AEFishBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FISH_ITEM =
            registerMachineItem(AE_FISH);

    public static final DeferredBlock<FactoryPatternBlock> AE_FACTORY_PATTERN = registerMachine(
            "ae_factory_pattern",
            () -> new FactoryPatternBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FACTORY_PATTERN_ITEM =
            registerMachineItem(AE_FACTORY_PATTERN);

    public static final DeferredBlock<FactoryControllerBlock> AE_FACTORY_CONTROLLER = registerMachine(
            "ae_factory_controller",
            () -> new FactoryControllerBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FACTORY_CONTROLLER_ITEM =
            registerMachineItem(AE_FACTORY_CONTROLLER);

    public static final DeferredBlock<FactoryCasingBlock> AE_FACTORY_CASING = registerMachine(
            "ae_factory_casing",
            () -> new FactoryCasingBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FACTORY_CASING_ITEM =
            registerMachineItem(AE_FACTORY_CASING);

    public static final DeferredBlock<LatexExtractionProcessorBlock> AE_LATEX_EXTRACTION_PROCESSOR = registerMachine(
            "ae_latex_extraction_processor",
            () -> new LatexExtractionProcessorBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_LATEX_EXTRACTION_PROCESSOR_ITEM =
            registerMachineItem(AE_LATEX_EXTRACTION_PROCESSOR);

    public static final DeferredBlock<DissolutionChamberBlock> AE_DISSOLUTION_CHAMBER_FRONT = registerMachine(
            "ae_dissolution_chamber_front",
            () -> new DissolutionChamberBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_DISSOLUTION_CHAMBER_FRONT_ITEM =
            registerMachineItem(AE_DISSOLUTION_CHAMBER_FRONT);

    public static final DeferredBlock<FluidLaserBaseBlock> AE_FLUID_LASER_BASE = registerMachine(
            "ae_fluid_laser_base",
            () -> new FluidLaserBaseBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_FLUID_LASER_BASE_ITEM =
            registerMachineItem(AE_FLUID_LASER_BASE);

    public static final DeferredBlock<Block> AE_REACTORS_SHELL =
            registerMachine(
                    "ae_reactors_shell",
                    () -> new Block(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_REACTORS_SHELL_ITEM =
            registerMachineItem(AE_REACTORS_SHELL);
    public static final DeferredBlock<ReactorControllerBlock> AE_REACTORS_CONTROLLER =
            registerMachine(
                    "ae_reactors_controller",
                    () -> new ReactorControllerBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_REACTORS_CONTROLLER_ITEM =
            registerMachineItem(AE_REACTORS_CONTROLLER);

    public static final DeferredBlock<ReactorPatternBlock> AE_REACTORS_PATTERN =
            registerMachine(
                    "ae_reactors_pattern",
                    () -> new ReactorPatternBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_REACTORS_PATTERN_ITEM =
            registerMachineItem(AE_REACTORS_PATTERN);

    public static final DeferredBlock<ReactorFuelRodBlock> AE_REACTORS_FUELROD =
            registerMachine(
                    "ae_reactors_fuelrod",
                    () -> new ReactorFuelRodBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)
                            .noOcclusion().isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredItem<BlockItem> AE_REACTORS_FUELROD_ITEM =
            registerMachineItem(AE_REACTORS_FUELROD);

    public static final DeferredBlock<AEBrewingBlock> AE_BREWING = registerMachine(
            "ae_brewing", () -> new AEBrewingBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_BREWING_ITEM = registerMachineItem(AE_BREWING);

    public static final DeferredBlock<AEEnergizingBlock> AE_ENERGIZING_ORB = registerMachine(
            "ae_energizing_orb", () -> new AEEnergizingBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> AE_ENERGIZING_ORB_ITEM = registerMachineItem(AE_ENERGIZING_ORB);
    public static final DeferredBlock<appeng.block.misc.InscriberBlock> INSCRIBER_X8 = registerMachine(
            "inscriber_x8", () -> new appeng.block.misc.InscriberBlock(appeng.block.AEBaseBlock.metalProps().noOcclusion()));
    public static final DeferredItem<BlockItem> INSCRIBER_X8_ITEM = ITEMS.register("inscriber_x8", () ->
            new appeng.block.AEBaseBlockItem(INSCRIBER_X8.get(), new net.minecraft.world.item.Item.Properties()));

    public static final DeferredBlock<appeng.block.misc.InscriberBlock> INSCRIBER_X64 = registerMachine(
            "inscriber_x64", () -> new appeng.block.misc.InscriberBlock(appeng.block.AEBaseBlock.metalProps().noOcclusion()));
    public static final DeferredItem<BlockItem> INSCRIBER_X64_ITEM = ITEMS.register("inscriber_x64", () ->
            new appeng.block.AEBaseBlockItem(INSCRIBER_X64.get(), new net.minecraft.world.item.Item.Properties()));

    public static final DeferredBlock<AEHiddenBackupPowerBlock> AE_HIDDEN_BACKUP_POWER = registerMachine(
            "ae_hidden_backup_power", () -> new AEHiddenBackupPowerBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> AE_HIDDEN_BACKUP_POWER_ITEM = registerMachineItem(AE_HIDDEN_BACKUP_POWER);

    public static final DeferredItem<com.appliedmoremachines.items.VirtueToolItem> BLADE_OF_BENEVOLENCE =
            MachineAvailability.enabled("blade_of_benevolence") ? ITEMS.register("blade_of_benevolence", () -> new com.appliedmoremachines.items.VirtueToolItem(true)) : null;
    public static final DeferredItem<com.appliedmoremachines.items.VirtueToolItem> PICKAXE_OF_RIGHTEOUSNESS =
            MachineAvailability.enabled("pickaxe_of_righteousness") ? ITEMS.register("pickaxe_of_righteousness", () -> new com.appliedmoremachines.items.VirtueToolItem(false)) : null;

    private ModBlocks() {}

    private static <T extends Block> DeferredBlock<T> registerMachine(String id, Supplier<T> factory) {
        return MachineAvailability.enabled(id) ? BLOCKS.register(id, factory) : null;
    }

    private static DeferredItem<BlockItem> registerMachineItem(DeferredBlock<?> block) {
        return block == null ? null : ITEMS.registerSimpleBlockItem(block);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModBlocks::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            if (AE_INFUSION_PEDESTA_ITEM != null) event.accept(AE_INFUSION_PEDESTA_ITEM.get());
            if (AE_CRAFTING_CORE_ITEM != null) event.accept(AE_CRAFTING_CORE_ITEM.get());
            event.accept(AE_ENCHANTING_TABLE_ITEM.get());
            event.accept(AE_FISH_ITEM.get());
            event.accept(AE_BREWING_ITEM.get());
            if (AE_HIDDEN_BACKUP_POWER_ITEM != null) event.accept(AE_HIDDEN_BACKUP_POWER_ITEM.get());
            event.accept(INSCRIBER_X8_ITEM.get());
            event.accept(INSCRIBER_X64_ITEM.get());
            if (AE_ENERGIZING_ORB_ITEM != null) event.accept(AE_ENERGIZING_ORB_ITEM.get());
            if (AE_FACTORY_PATTERN_ITEM != null) event.accept(AE_FACTORY_PATTERN_ITEM.get());
            if (AE_FACTORY_CONTROLLER_ITEM != null) event.accept(AE_FACTORY_CONTROLLER_ITEM.get());
            if (AE_FACTORY_CASING_ITEM != null) event.accept(AE_FACTORY_CASING_ITEM.get());
            if (AE_LATEX_EXTRACTION_PROCESSOR_ITEM != null) event.accept(AE_LATEX_EXTRACTION_PROCESSOR_ITEM.get());
            if (AE_DISSOLUTION_CHAMBER_FRONT_ITEM != null) event.accept(AE_DISSOLUTION_CHAMBER_FRONT_ITEM.get());
            if (AE_FLUID_LASER_BASE_ITEM != null) event.accept(AE_FLUID_LASER_BASE_ITEM.get());
            if (AE_REACTORS_SHELL_ITEM != null) event.accept(AE_REACTORS_SHELL_ITEM.get());
            if (AE_REACTORS_FUELROD_ITEM != null) event.accept(AE_REACTORS_FUELROD_ITEM.get());
            if (AE_REACTORS_PATTERN_ITEM != null) event.accept(AE_REACTORS_PATTERN_ITEM.get());
            if (AE_REACTORS_CONTROLLER_ITEM != null) event.accept(AE_REACTORS_CONTROLLER_ITEM.get());
        }
    }
}
