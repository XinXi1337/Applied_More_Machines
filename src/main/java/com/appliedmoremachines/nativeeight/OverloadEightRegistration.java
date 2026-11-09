package com.appliedmoremachines.nativeeight;
import com.moakiee.ae2lt.block.OverloadProcessingFactoryBlock;
import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
public final class OverloadEightRegistration {
    public static void register(IEventBus bus) {
        var type=NativeEightMachines.add("overload_processing_factory_x8",OverloadProcessingFactoryBlock::new,OverloadProcessingFactoryBlockEntity.class,OverloadFactoryEight::new,null,OverloadProcessingFactoryBlockEntity::serverTick);
        bus.addListener((RegisterCapabilitiesEvent e)-> {
            e.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,type.get(),(h,s)->h);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,type.get(),(h,s)->h.getAutomationInventory());
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,type.get(),(h,s)->h.getEnergyStorageCapability(s));
            e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,type.get(),(h,s)->h.getFluidHandlerCapability(s));
        });
    }
}
