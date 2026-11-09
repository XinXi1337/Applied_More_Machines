package com.appliedmoremachines.nativeeight;
import com.appliedmoremachines.production.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
public final class CircuitEtcherEight extends io.github.lounode.ae2cs.common.block.entity.CircuitEtcherBlockEntity implements NativeEightHost {
    private final MultiplicationUpgrade multiplication=new MultiplicationUpgrade(this);
    private appeng.api.upgrades.IUpgradeInventory nativeUpgrades;
    public CircuitEtcherEight(BlockPos pos,BlockState state) { super(pos,state); EightInventories.track(getInputInv(), getOutputInv()); }
    @Override public ItemStack getMainMenuIcon() { return getItemFromBlockEntity().getDefaultInstance(); }
    @Override public BlockEntityType<?> getType() { return NativeEightMachines.type("circuit_etcher_x8"); }
    @Override protected net.minecraft.world.item.Item getItemFromBlockEntity() { return getBlockState().getBlock().asItem(); }
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplication; }
    @Override public appeng.api.upgrades.IUpgradeInventory getUpgrades() {
        if(multiplication==null) return super.getUpgrades();
        if(nativeUpgrades==null) nativeUpgrades=new com.appliedmoremachines.inscriber.InscriberUpgradeInventory(super.getUpgrades());
        return nativeUpgrades;
    }
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup); multiplication.save(tag,lookup);
    }
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider lookup) {
        super.loadTag(tag,lookup); multiplication.load(tag,lookup);
    }
    @Override public void addAdditionalDrops(Level level,BlockPos pos,List<ItemStack> drops) {
        var contents=new ArrayList<ItemStack>(); super.addAdditionalDrops(level,pos,contents);
        contents.addAll(multiplication.takeDrops()); EightInventories.splitDrops(contents,drops);
    }
    @Override public void clearContent() { super.clearContent(); if(multiplication!=null) multiplication.clearContent(); }
}
