package com.appliedmoremachines.client;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModMenus;
import com.appliedmoremachines.ModBlockEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AppliedMoreMachines.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.AE_ENCHANTING_TABLE.get(), AEEnchantingTableRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.AE_INFUSION_PEDESTA.get(), InfusionPedestaScreen::new);
        event.register(ModMenus.AE_CRAFTING_CORE.get(), CraftingCoreScreen::new);
        event.register(ModMenus.AE_ENCHANTING_TABLE.get(), AEEnchantingTableScreen::new);
        event.register(ModMenus.AE_FISH.get(), AEFishScreen::new);
        if (ModMenus.AE_FLUID_LASER_BASE != null)
            event.register(ModMenus.AE_FLUID_LASER_BASE.get(), FluidLaserBaseScreen::new);
        event.register(ModMenus.AE_DISSOLUTION_CHAMBER_FRONT.get(), DissolutionChamberScreen::new);
        event.register(ModMenus.AE_LATEX_EXTRACTION_PROCESSOR.get(), LatexExtractionProcessorScreen::new);
        event.register(ModMenus.AE_FACTORY_PATTERN.get(), FactoryPatternScreen::new);
        event.register(ModMenus.AE_FACTORY_CONTROLLER.get(), FactoryControllerScreen::new);
        if (ModMenus.AE_REACTORS_CONTROLLER != null) {
            event.register(ModMenus.AE_REACTORS_CONTROLLER.get(), ReactorControllerScreen::new);
            event.register(ModMenus.AE_REACTORS_PATTERN.get(), ReactorPatternScreen::new);
        }
    }
}
