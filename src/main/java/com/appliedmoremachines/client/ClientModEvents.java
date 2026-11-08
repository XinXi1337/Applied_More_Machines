package com.appliedmoremachines.client;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModMenus;
import com.appliedmoremachines.ModBlockEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AppliedMoreMachines.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "factory_casing"),
                FactoryCasingModel.LOADER);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.INSCRIBER_X8.get(), appeng.client.render.tesr.InscriberTESR::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INSCRIBER_X64.get(), appeng.client.render.tesr.InscriberTESR::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AE_ENCHANTING_TABLE.get(), AEEnchantingTableRenderer::new);
        if (ModBlockEntities.AE_ENERGIZING_ORB != null) event.registerBlockEntityRenderer(ModBlockEntities.AE_ENERGIZING_ORB.get(), AEEnergizingRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        if (ModMenus.AE_INFUSION_PEDESTA != null)
            event.register(ModMenus.AE_INFUSION_PEDESTA.get(), InfusionPedestaScreen::new);
        if (ModMenus.AE_CRAFTING_CORE != null)
            event.register(ModMenus.AE_CRAFTING_CORE.get(), CraftingCoreScreen::new);
        event.register(ModMenus.AE_ENCHANTING_TABLE.get(), AEEnchantingTableScreen::new);
        event.register(ModMenus.AE_FISH.get(), AEFishScreen::new);
        event.register(ModMenus.AE_BREWING.get(), AEBrewingScreen::new);
        if (ModMenus.AE_ENERGIZING_ORB != null) event.register(ModMenus.AE_ENERGIZING_ORB.get(), AEEnergizingScreen::new);
        if (ModMenus.AE_FLUID_LASER_BASE != null)
            event.register(ModMenus.AE_FLUID_LASER_BASE.get(), FluidLaserBaseScreen::new);
        if (ModMenus.AE_DISSOLUTION_CHAMBER_FRONT != null)
            event.register(ModMenus.AE_DISSOLUTION_CHAMBER_FRONT.get(), DissolutionChamberScreen::new);
        if (ModMenus.AE_LATEX_EXTRACTION_PROCESSOR != null)
            event.register(ModMenus.AE_LATEX_EXTRACTION_PROCESSOR.get(), LatexExtractionProcessorScreen::new);
        if (ModMenus.AE_FACTORY_PATTERN != null)
            event.register(ModMenus.AE_FACTORY_PATTERN.get(), FactoryPatternScreen::new);
        if (ModMenus.AE_FACTORY_CONTROLLER != null)
            event.register(ModMenus.AE_FACTORY_CONTROLLER.get(), FactoryControllerScreen::new);
        if (ModMenus.AE_REACTORS_CONTROLLER != null) {
            event.register(ModMenus.AE_REACTORS_CONTROLLER.get(), ReactorControllerScreen::new);
            event.register(ModMenus.AE_REACTORS_PATTERN.get(), ReactorPatternScreen::new);
        }
    }
}
