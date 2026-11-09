package com.appliedmoremachines.nativeeight;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.AppEngSlot;
import com.appliedmoremachines.processing.AltarTransfers;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class EightMenuActions {
    private EightMenuActions() {}
    public static boolean expanded(Slot slot) { return slot instanceof AppEngSlot ae && EightInventories.expanded(ae.getInventory()); }
    public static boolean click(AEBaseMenu menu,int index,int button,ClickType type,Player player) {
        if(index<0||index>=menu.slots.size()) return false;
        Slot slot=menu.slots.get(index);
        if(!expanded(slot)) return false;
        if(!slot.isActive()) return true;
        ItemStack existing=slot.getItem(),carried=menu.getCarried();
        if(type==ClickType.SWAP && existing.getCount()>existing.getMaxStackSize()) {
            if((button<0||button>=9)&&button!=Inventory.SLOT_OFFHAND) return true;
            if(menu.isPlayerInventorySlotLocked(Inventory.SLOT_OFFHAND)||menu.isPlayerInventorySlotLocked(button)||!slot.mayPickup(player)) return true;
            if(player.getInventory().getItem(button).isEmpty()) {
                var taken=slot.remove(existing.getMaxStackSize());player.getInventory().setItem(button,taken);slot.onTake(player,taken);
            }
            return true;
        }
        if(type!=ClickType.PICKUP || (button!=0&&button!=1)) return false;
        if(carried.isEmpty()) {
            if(!existing.isEmpty()&&slot.mayPickup(player)) {
                var taken=slot.remove(Math.min(existing.getMaxStackSize(),button==0?existing.getCount():(existing.getCount()+1)/2));
                menu.setCarried(taken);slot.onTake(player,taken);
            }
        } else if(slot.mayPlace(carried)&&(existing.isEmpty()||ItemStack.isSameItemSameComponents(existing,carried))) {
            int n=Math.max(0,Math.min(button==1?1:carried.getCount(),slot.getMaxStackSize(carried)-existing.getCount()));
            if(n>0) { slot.setByPlayer(carried.copyWithCount(existing.getCount()+n));carried.shrink(n); }
        } else if(slot.mayPickup(player)) {
            if(ItemStack.isSameItemSameComponents(existing,carried)) {
                int n=Math.min(existing.getCount(),carried.getMaxStackSize()-carried.getCount());
                if(n>0) {var taken=slot.remove(n);carried.grow(taken.getCount());slot.onTake(player,taken);}
            } else if(slot.mayPlace(carried)&&existing.getCount()<=existing.getMaxStackSize()&&carried.getCount()<=slot.getMaxStackSize(carried)) {
                slot.setByPlayer(carried);menu.setCarried(existing);slot.onTake(player,existing);
            }
        }
        slot.setChanged();return true;
    }
    public static ItemStack quickMove(AEBaseMenu menu,Player player,int index) {
        if(!(menu.getTarget() instanceof NativeEightHost)||index<0||index>=menu.slots.size()) return null;
        var slot=menu.slots.get(index);
        if(!expanded(slot)) return null;
        if(!slot.isActive()||!slot.mayPickup(player)||!slot.hasItem()) return ItemStack.EMPTY;
        var before=slot.getItem().copy();var left=before.copy();
        if(!AltarTransfers.move(left,menu.slots.stream().filter(s->s.container==player.getInventory()).toList().reversed())) return ItemStack.EMPTY;
        slot.setByPlayer(left);slot.onTake(player,before.copyWithCount(before.getCount()-left.getCount()));return before;
    }
}
