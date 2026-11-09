package com.appliedmoremachines.client;
import com.appliedmoremachines.nativeeight.NativeEightMachines;
import com.glodblock.github.extendedae.common.tileentities.TileCircuitCutter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
public final class NativeEightClient {
    @SuppressWarnings("unchecked")
    public static void extendedRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType<TileCircuitCutter>)NativeEightMachines.type("circuit_cutter_x8"),com.glodblock.github.extendedae.client.render.tesr.CircuitCutterTESR::new);
    }
}
