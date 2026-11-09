package com.appliedmoremachines.nativeeight;
import com.glodblock.github.extendedae.common.blocks.*;
import com.glodblock.github.extendedae.common.tileentities.*;
import appeng.api.AECapabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
public final class ExtendedEightRegistration {
    public static void register(IEventBus bus) {
        var assembler=NativeEightMachines.add("crystal_assembler_x8",BlockCrystalAssembler::new,TileCrystalAssembler.class,CrystalAssemblerEight::new,null,null);
        var cutter=NativeEightMachines.add("circuit_cutter_x8",BlockCircuitCutter::new,TileCircuitCutter.class,CircuitCutterEight::new,null,null);
        bus.addListener((RegisterCapabilitiesEvent e)-> {
            e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,assembler.get(),(h,s)->h);
            e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,cutter.get(),(h,s)->h);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,assembler.get(),(h,s)->h.getExposedItemHandler(s));
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,cutter.get(),(h,s)->h.getExposedItemHandler(s));
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,assembler.get(),(h,s)->h.getEnergyStorage(s));
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,cutter.get(),(h,s)->h.getEnergyStorage(s));
            e.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV,assembler.get(),(h,s)->h.getGenericInv(s));
            e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,assembler.get(),(h,s)->new appeng.helpers.externalstorage.GenericStackFluidStorage(h.getGenericInv(s)));
        });
    }
}
