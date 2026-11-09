package com.appliedmoremachines.nativeeight;
public final class EightFactoryInventory extends com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory {
    private final NativeEightHost owner;
    public EightFactoryInventory(Runnable changed, NativeEightHost owner) { super(changed); this.owner=owner; }
    public boolean multiplied() { return owner!=null && com.appliedmoremachines.production.MultiplicationUpgrade.active(owner); }
    @Override public int getInstalledParallelCapacity() {
        return (int)Math.min(Integer.MAX_VALUE, (long)super.getInstalledParallelCapacity()+7);
    }
    @Override public int getSlotLimit(int slot) { return Math.multiplyExact(super.getSlotLimit(slot),slot==SLOT_MATRIX ? 1 : 8); }
}
