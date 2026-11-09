package com.appliedmoremachines.nativeeight;
import io.github.lounode.ae2cs.common.block.CircuitEtcherBlock;
import io.github.lounode.ae2cs.common.block.entity.CircuitEtcherBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
public final class EtcherEightRegistration {
    public static void register(IEventBus bus) {
        var type=NativeEightMachines.add("circuit_etcher_x8",()->new CircuitEtcherBlock(appeng.block.AEBaseBlock.metalProps()),CircuitEtcherBlockEntity.class,CircuitEtcherEight::new,
                (l,p,s,h)->h.clientTick(),(l,p,s,h)->h.serverTick());
        bus.addListener((RegisterCapabilitiesEvent e)-> {
            e.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,type.get(),(h,s)->h);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,type.get(),(h,s)-> {
                var inv=h.getMachineComponents().getService(io.github.lounode.ae2cs.common.machine.component.SideConfigComponent.class).appEngInvForSide(s);
                return inv==null ? null : inv.toItemHandler();
            });
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,type.get(),(h,s)->h.getMachineComponents()
                    .getService(io.github.lounode.ae2cs.common.machine.component.EnergyComponent.class).getForgeEnergyAdapter());
        });
    }
}
