package com.appliedmoremachines.production;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.*;
import com.appliedmoremachines.factory.FactoryAmounts;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** One dedicated card and already-owned, undelivered production. No optional API types. */
public final class MultiplicationUpgrade extends SimpleContainer {
    private static final ResourceLocation CARD = ResourceLocation.parse("data_energistics:multiplication_card");
    private final BlockEntity owner;
    private final Map<AEKey, Long> pending = new LinkedHashMap<>();
    private ListTag unresolved = new ListTag();

    public MultiplicationUpgrade(BlockEntity owner) { super(1); this.owner = owner; }
    public static boolean enabled() { return ModList.get() != null && ModList.get().isLoaded("data_energistics"); }
    public static boolean isCard(ItemStack stack) {
        return enabled() && !stack.isEmpty() && CARD.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
    public boolean installed() { return isCard(getItem(0)); }
    private List<? extends MultiplicationHost> partners() {
        if (owner instanceof FactoryControllerBlockEntity factory) return FactoryUpgradeCards.controllers(factory);
        if (owner instanceof ReactorControllerBlockEntity reactor) return ReactorUpgradeCards.controllers(reactor);
        return List.of((MultiplicationHost) owner);
    }
    public static boolean active(Object host) {
        if (!(host instanceof MultiplicationHost machine) || !enabled()) return false;
        return machine.multiplicationUpgrade().partners().stream().anyMatch(p -> p.multiplicationUpgrade().installed());
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && isCard(stack) && (owner == null || partners().stream()
                .allMatch(p -> p == owner || p.multiplicationUpgrade().isEmpty()));
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int getMaxStackSize(ItemStack stack) { return 1; }
    @Override public void setChanged() {
        super.setChanged();
        if (owner != null) {
            owner.setChanged();
            if (owner instanceof com.appliedmoremachines.nativeeight.NativeEightHost host) host.multiplicationChanged();
            if (owner instanceof IGridConnectedBlockEntity gridHost) gridHost.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
    }

    /** Round a native operation before multiplying by actual parallel copies. */
    public static long boost(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative production");
        return Math.addExact(amount, amount / 2 + amount % 2);
    }
    public static long amount(Object host, long unit, long copies) {
        return Math.multiplyExact(active(host) ? boost(unit) : unit, copies);
    }
    public static ItemStack product(Object host, ItemStack unit, int copies) {
        return unit.isEmpty() ? ItemStack.EMPTY : unit.copyWithCount(Math.toIntExact(amount(host, unit.getCount(), copies)));
    }
    public void retain(Map<? extends AEKey, Long> amounts) {
        amounts.forEach((key, amount) -> { if (amount > 0) pending.merge(key, amount, Math::addExact); });
        if (!amounts.isEmpty()) setChanged();
    }
    public void retain(AEKey key, long amount) { if (amount > 0) retain(Map.of(key, amount)); }
    public boolean hasPending() { return !pending.isEmpty() || !unresolved.isEmpty(); }

    /** Actual acceptance, including partial writes, is the only way to reduce owned outputs. */
    public boolean tick() {
        if (owner == null || owner.getLevel() == null || owner.getLevel().isClientSide) return !hasPending();
        if (!unresolved.isEmpty()) {
            var decoded = FactoryAmounts.load(unresolved, owner.getLevel().registryAccess());
            if (decoded != null) { retain(decoded); unresolved = new ListTag(); setChanged(); }
        }
        if (pending.isEmpty()) return !hasPending();
        if (!(owner instanceof IGridConnectedBlockEntity host) || !host.getMainNode().isActive()) return false;
        var grid = host.getMainNode().getGrid();
        if (grid == null) return false;
        var inventory = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(host);
        for (var iterator = pending.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            long received = Math.clamp(inventory.insert(entry.getKey(), entry.getValue(), Actionable.MODULATE, source), 0, entry.getValue());
            if (received == 0) continue;
            if (received == entry.getValue()) iterator.remove(); else entry.setValue(entry.getValue() - received);
            setChanged();
        }
        return !hasPending();
    }
    public void save(CompoundTag parent, HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        tag.put("Card", getItem(0).saveOptional(lookup));
        ListTag list = FactoryAmounts.save(pending, lookup);
        for (Tag entry : unresolved) list.add(entry.copy());
        tag.put("Pending", list);
        parent.put("MultiplicationUpgrade", tag);
    }
    public void load(CompoundTag parent, HolderLookup.Provider lookup) {
        var tag = parent.getCompound("MultiplicationUpgrade");
        setItem(0, ItemStack.parseOptional(lookup, tag.getCompound("Card")));
        pending.clear(); unresolved = tag.getList("Pending", Tag.TAG_COMPOUND).copy();
    }
    public List<ItemStack> takeDrops() {
        List<ItemStack> result = new ArrayList<>();
        if (owner != null && owner.getLevel() != null && !unresolved.isEmpty()) {
            var decoded = FactoryAmounts.load(unresolved, owner.getLevel().registryAccess());
            if (decoded != null) { retain(decoded); unresolved = new ListTag(); }
        }
        if (!getItem(0).isEmpty()) result.add(removeItemNoUpdate(0));
        pending.forEach((key, amount) -> {
            if (key instanceof AEItemKey item) {
                long left = amount;
                while (left > 0) {
                    int count = (int) Math.min(left, item.getReadOnlyStack().getMaxStackSize());
                    result.add(item.toStack(count)); left -= count;
                }
            }
        });
        pending.clear(); unresolved = new ListTag(); setChanged();
        return result;
    }
}
