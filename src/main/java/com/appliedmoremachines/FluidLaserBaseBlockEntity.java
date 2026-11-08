package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.me.helpers.*;
import com.appliedmoremachines.dissolution.DissolutionCharging;
import com.appliedmoremachines.factory.FactoryEnergy;
import com.appliedmoremachines.laser.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidLaserBaseBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer, FluidLaserProcessor.Host, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    private NonNullList<ItemStack> items = NonNullList.withSize(FluidLaserSlots.SIZE, ItemStack.EMPTY);
    private final FluidStack[] fluids = {FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY};
    private boolean autoExport;
    private int outputSides;
    private final FluidLaserProcessor processing = new FluidLaserProcessor();
    private final FluidLaserEnergyStorage energy = new FluidLaserEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setExposedOnSides(EnumSet.allOf(Direction.class))
            .setVisualRepresentation(ModBlocks.AE_FLUID_LASER_BASE.get()).addService(IAEPowerStorage.class, this);
    private final ContainerData settings = new ContainerData() {
        @Override public int get(int i) {
            if (i >= 2 && i < 14) {
                FluidStack fluid=fluids[(i-2)/4];int offset=(i-2)%4;
                int value=offset<2?BuiltInRegistries.FLUID.getId(fluid.getFluid()):fluid.getAmount();
                return value >>> (16*(offset%2)) & 65535;
            }
            return switch(i) {
                case 0 -> autoExport?1:0;case 1 -> outputSides;
                case 14 -> processing.ticks();case 15 -> processing.duration();
                case 16 -> (int)Math.floor(100*getAECurrentPower()/getAEMaxPower());default -> 0;
            };
        }
        @Override public void set(int i,int value) {
            if(i==0) autoExport=value!=0;else if(i==1)outputSides=value&63;else return;
            setChanged();
        }
        @Override public int getCount(){return FluidLaserBaseMenu.DATA_COUNT;}
    };
    public FluidLaserBaseBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.AE_FLUID_LASER_BASE.get(),pos,state); }
    @Override public int getContainerSize(){return FluidLaserSlots.SIZE;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
    @Override protected Component getDefaultName(){return Component.translatable("block.applied_more_machines.ae_fluid_laser_base");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new FluidLaserBaseMenu(id,inventory,this);}
    public ContainerData getSettings(){return settings;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return FluidLaserSlots.accepts(this,slot,stack);}
    @Override public int getMaxStackSize(){return 36;}
    @Override public int getMaxStackSize(ItemStack stack){return Math.min(36,stack.getMaxStackSize());}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public void clearContent(){super.clearContent();setChanged();}
    public int installUpgradeCards(ItemStack offered){
        if(level==null||level.isClientSide)return 0;
        for(int i=FluidLaserSlots.UPGRADE;i<FluidLaserSlots.SIZE;i++)if(canPlaceItem(i,offered)){
            ItemStack present=getItem(i);
            if(!present.isEmpty()&&!ItemStack.isSameItemSameComponents(present,offered))continue;
            int count=Math.min(offered.getCount(),Math.min(offered.getMaxStackSize(),FluidLaserSlots.limit(i,offered))-present.getCount());
            if(count>0){setItem(i,offered.copyWithCount(present.getCount()+count));return count;}
        }
        return 0;
    }
    private boolean outputEnabled(Direction direction){
        if(direction==null)return outputSides!=0;
        Direction front=getBlockState().getValue(FluidLaserBaseBlock.FACING);
        for(var side:InfusionOutputSide.values())if(side.worldDirection(front)==direction)return(outputSides&side.mask())!=0;
        return false;
    }
    // No item capability; prevent vanilla hopper fallback from accessing any installed equipment.
    @Override public int[] getSlotsForFace(Direction side){return new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public FluidStack tankContents(int tank){if(tank<0||tank>=3)throw new IndexOutOfBoundsException(tank);return fluids[tank].copy();}
    public IFluidHandler getFluidHandler(Direction side){return fluidHandler(side,-1,false);}
    public IFluidHandler getGuiTank(int tank){if(tank<0||tank>=3)throw new IndexOutOfBoundsException(tank);return fluidHandler(null,tank,true);}
    private IFluidHandler fluidHandler(Direction side,int only,boolean gui){
        return new IFluidHandler(){
            private int index(int tank){if(tank<0||tank>=getTanks())throw new IndexOutOfBoundsException(tank);return only<0?tank:only;}
            @Override public int getTanks(){return only<0?3:1;}
            @Override public FluidStack getFluidInTank(int tank){return tankContents(index(tank));}
            @Override public int getTankCapacity(int tank){index(tank);return FluidLaserProcessor.CAPACITY;}
            @Override public boolean isFluidValid(int tank,FluidStack stack){index(tank);return false;}
            @Override public int fill(FluidStack stack,FluidAction action){return 0;}
            private FluidStack take(int tank,int max,FluidAction action){
                if(max<=0||!gui&&!outputEnabled(side))return FluidStack.EMPTY;
                int count=Math.min(max,fluids[tank].getAmount());if(count<=0)return FluidStack.EMPTY;
                FluidStack result=fluids[tank].copyWithAmount(count);
                if(action.execute()){fluids[tank].shrink(count);setChanged();}return result;
            }
            @Override public FluidStack drain(FluidStack requested,FluidAction action){
                if(requested.isEmpty())return FluidStack.EMPTY;
                for(int i=0;i<getTanks();i++){int tank=index(i);if(FluidStack.isSameFluidSameComponents(fluids[tank],requested))return take(tank,requested.getAmount(),action);}
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int max,FluidAction action){
                for(int i=0;i<getTanks();i++){int tank=index(i);if(!fluids[tank].isEmpty())return take(tank,max,action);}
                return FluidStack.EMPTY;
            }
        };
    }
    private void pushAdjacent(){
        if(!autoExport)return;
        Direction front=getBlockState().getValue(FluidLaserBaseBlock.FACING);
        for(var side:InfusionOutputSide.values()){
            Direction direction=side.worldDirection(front);BlockPos pos=worldPosition.relative(direction);
            if(!outputEnabled(direction)||!level.hasChunkAt(pos))continue;
            var target=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,direction.getOpposite());
            if(target!=null)for(var fluid:fluids)if(!fluid.isEmpty()){
                int accepted=Math.clamp(target.fill(fluid.copy(),IFluidHandler.FluidAction.EXECUTE),0,fluid.getAmount());
                if(accepted>0){fluid.shrink(accepted);setChanged();}
            }
        }
    }
    public void serverTick(){
        if (!multiplicationUpgrade.tick()) return;
        if(level==null||level.isClientSide)return;
        if(mainNode.isActive()&&getAECurrentPower()<getAEMaxPower()) mainNode.ifPresent(grid ->
            DissolutionCharging.charge(energy,FactoryEnergy.source(grid,IActionSource.ofMachine(this))));
        if(level.getGameTime()%4==0)pushAdjacent();
        processing.tick(this,mainNode.isActive(),FluidLaserRecipes.all(level),level.registryAccess());
    }
    @Override public boolean powered(){return getAECurrentPower()>=FluidLaserProcessor.COST;}
    @Override public void commit(FluidStack[] outputs){
        extractAEPower(FluidLaserProcessor.COST,Actionable.MODULATE,PowerMultiplier.ONE);
        for(int i=0;i<3;i++)if(!outputs[i].isEmpty())fluids[i]=MultiplicationOutputs.fluid(this, fluids[i], outputs[i], FluidLaserProcessor.CAPACITY);
        setChanged();
    }
    @Override public void dirty(){setChanged();}
    @Override public IManagedGridNode getMainNode(){return mainNode;}
    @Override public void saveChanges(){setChanged();}
    public void setOwner(Player player){mainNode.setOwningPlayer(player);}
    public IEnergyStorage getForgeEnergyStorage(){return forgeEnergy;}
    @Override public void clearRemoved(){
        super.clearRemoved();GridHelper.onFirstTick(this,machine -> {
            if(!machine.isRemoved()&&machine.level!=null&&!machine.level.isClientSide&&!machine.mainNode.isReady())
                machine.mainNode.create(machine.level,machine.worldPosition);
        });
    }
    @Override public void setRemoved(){mainNode.destroy();super.setRemoved();}
    @Override public void onChunkUnloaded(){mainNode.destroy();super.onChunkUnloaded();}
    @Override public double injectAEPower(double amount,Actionable mode){return energy.injectAEPower(amount,mode);}
    @Override public double extractAEPower(double amount,Actionable mode,PowerMultiplier multiplier){return energy.extractAEPower(amount,mode,multiplier);}
    @Override public double getAECurrentPower(){return energy.getAECurrentPower();}
    @Override public double getAEMaxPower(){return energy.getAEMaxPower();}
    @Override public boolean isAEPublicPowerStorage(){return false;}
    @Override public AccessRestriction getPowerFlow(){return energy.getPowerFlow();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);
        multiplicationUpgrade.save(tag, lookup);ContainerHelper.saveAllItems(tag,items,lookup);
        for(int i=0;i<3;i++)tag.put("Fluid"+i,fluids[i].saveOptional(lookup));
        tag.putBoolean("AutoExport",autoExport);tag.putInt("OutputSides",outputSides);
        tag.put("LaserWork",processing.save(lookup));tag.putDouble("StoredAE",getAECurrentPower());mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);
        multiplicationUpgrade.load(tag, lookup);items=NonNullList.withSize(FluidLaserSlots.SIZE,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,lookup);
        for(int i=0;i<3;i++){
            fluids[i]=FluidStack.parseOptional(lookup,tag.getCompound("Fluid"+i));
            fluids[i].setAmount(Math.min(FluidLaserProcessor.CAPACITY,fluids[i].getAmount()));
        }
        autoExport=tag.getBoolean("AutoExport");outputSides=tag.getInt("OutputSides")&63;
        processing.load(tag.getCompound("LaserWork"),lookup);energy.load(tag.getDouble("StoredAE"));mainNode.loadFromNBT(tag);
    }
}
