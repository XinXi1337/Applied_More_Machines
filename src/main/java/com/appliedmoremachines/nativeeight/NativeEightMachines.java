package com.appliedmoremachines.nativeeight;
import com.appliedmoremachines.*;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;
import java.util.function.Supplier;
public final class NativeEightMachines {
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AppliedMoreMachines.MOD_ID);
    private static final Map<String,Supplier<? extends BlockEntityType<?>>> BY_ID=new HashMap<>();
    public static BlockEntityType<?> type(String id) { return BY_ID.get(id).get(); }
    public static void register(IEventBus bus) {
        if(MachineAvailability.enabled("crystal_assembler_x8")) ExtendedEightRegistration.register(bus);
        if(MachineAvailability.enabled("circuit_etcher_x8")) EtcherEightRegistration.register(bus);
        if(MachineAvailability.enabled("overload_processing_factory_x8")) OverloadEightRegistration.register(bus);
        TYPES.register(bus);
    }
    public static <T extends AEBaseBlockEntity,B extends AEBaseEntityBlock<T>> Supplier<BlockEntityType<T>> add(
            String id,Supplier<B> factory,Class<T> host,BlockEntityType.BlockEntitySupplier<T> constructor,
            BlockEntityTicker<T> clientTick,BlockEntityTicker<T> serverTick) {
        var block=ModBlocks.BLOCKS.register(id,factory);
        var item=ModBlocks.ITEMS.register(id,()->new appeng.block.AEBaseBlockItem(block.get(),new Item.Properties()));
        var type=TYPES.register(id,()-> {
            var value=BlockEntityType.Builder.of(constructor,block.get()).build(null);
            block.get().setBlockEntity(host,value,clientTick,serverTick);
            AEBaseBlockEntity.registerBlockEntityItem(value,item.get());
            return value;
        });
        BY_ID.put(id,type); return type;
    }
}
