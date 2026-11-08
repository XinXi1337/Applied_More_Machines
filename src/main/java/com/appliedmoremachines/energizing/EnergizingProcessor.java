package com.appliedmoremachines.energizing;

import com.appliedmoremachines.production.*;
import appeng.api.config.*;
import appeng.api.networking.energy.IAEPowerStorage;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A single batch accumulates actual FE payment; ingredients stay until completion. */
public final class EnergizingProcessor {
 private ResourceLocation recipe;
 private long paid,cost;
 private ItemStack output=ItemStack.EMPTY;
 public int progress(){return cost>0?(int)Math.min(10000,paid/(double)cost*10000):0;}
 public long paid(){return paid;}
 public void reset(){recipe=null;paid=cost=0;output=ItemStack.EMPTY;}
 public void tick(Container c,IAEPowerStorage energy,List<EnergizingRecipes.Entry> recipes,boolean active,long rate) {
  EnergizingRecipes.Entry selected=null;int[] plan=null;
  if(recipe!=null)for(var e:recipes)if(e.id().equals(recipe)&&e.energy()==cost&&ItemStack.matches(output,e.output())){
   plan=EnergizingRecipes.plan(e,c);if(plan!=null)selected=e;break;
  }
  if(selected==null){
   if(recipe!=null){reset();c.setChanged();}
   for(var e:recipes){plan=EnergizingRecipes.plan(e,c);if(plan!=null){selected=e;break;}}
   if(selected==null)return;
   recipe=selected.id();cost=selected.energy();output=selected.output().copy();c.setChanged();
  }
  if(!active||rate<=0)return;
  ItemStack present=c.getItem(25);
  if(!MultiplicationUpgrade.active(c) && (output.getCount()>Math.min(c.getMaxStackSize(),output.getMaxStackSize()) || !present.isEmpty() &&
    (!ItemStack.isSameItemSameComponents(present,output)||present.getCount()+output.getCount()>Math.min(c.getMaxStackSize(),output.getMaxStackSize()))))return;
  long request=Math.min(rate,cost-paid);
  if(request>0){
   double ratio=PowerUnit.FE.convertTo(PowerUnit.AE,1);
   if(!Double.isFinite(ratio)||ratio<=0)return;
   long available=(long)Math.min(request,Math.floor(energy.extractAEPower(request*ratio,Actionable.SIMULATE,PowerMultiplier.ONE)/ratio));
   if(available<=0)return;
   double taken=energy.extractAEPower(available*ratio,Actionable.MODULATE,PowerMultiplier.ONE);
   paid+=Math.min(available,(long)Math.floor(taken/ratio+0.0000001));c.setChanged();
  }
  if(paid<cost)return;
  for(int i=0;i<9;i++)if(plan[i]>0)c.removeItem(16+i,plan[i]);
  MultiplicationOutputs.deliver(c,new int[]{25},List.of(MultiplicationUpgrade.product(c,output,1)));
  reset();c.setChanged();
 }
 public CompoundTag save(HolderLookup.Provider lookup){var t=new CompoundTag();if(recipe!=null){t.putString("Recipe",recipe.toString());t.putLong("PaidFE",paid);t.putLong("CostFE",cost);t.put("Output",output.save(lookup));}return t;}
 public void load(CompoundTag t,HolderLookup.Provider lookup){reset();recipe=ResourceLocation.tryParse(t.getString("Recipe"));cost=Math.max(0,t.getLong("CostFE"));paid=Math.clamp(t.getLong("PaidFE"),0,cost);output=ItemStack.parseOptional(lookup,t.getCompound("Output"));if(recipe==null||output.isEmpty())reset();}
}
