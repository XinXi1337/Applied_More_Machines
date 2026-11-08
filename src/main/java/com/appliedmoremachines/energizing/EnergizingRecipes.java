package com.appliedmoremachines.energizing;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import owmii.powah.block.energizing.EnergizingRecipe;
import owmii.powah.block.energizing.EnergizingRodBlock;

/** Powah's current world recipes and configured rod values; no fixed tier table. */
public final class EnergizingRecipes {
 public record Entry(ResourceLocation id,List<Ingredient> ingredients,ItemStack output,long energy) {}
 public record Rod(long capacity,long rate) {}
 public static boolean rod(ItemStack s) { return s.getItem() instanceof BlockItem b && b.getBlock() instanceof EnergizingRodBlock; }
 public static Rod rodStats(ItemStack s) {
  if(!rod(s))return new Rod(0,0);
  var b=(EnergizingRodBlock)((BlockItem)s.getItem()).getBlock();
  return new Rod(Math.max(0,b.getConfig().getCapacity(b.getVariant())),Math.max(0,b.getConfig().getTransfer(b.getVariant())));
 }
 public static long sum(long a,long b) { return a>Long.MAX_VALUE-b ? Long.MAX_VALUE:a+b; }
 public static Rod totals(Container c) {
  long capacity=0,rate=0;
  for(int i=0;i<16;i++){var r=rodStats(c.getItem(i));capacity=sum(capacity,r.capacity());rate=sum(rate,r.rate());}
  return new Rod(capacity,rate);
 }
 public static List<ItemStack> rods() {return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(EnergizingRecipes::rod).toList();}
 public static List<Entry> all(Level level) {
  if(level==null)return List.of();
  return level.getRecipeManager().getRecipes().stream().filter(h->h.value() instanceof EnergizingRecipe)
   .map(h->{var r=(EnergizingRecipe)h.value();return new Entry(h.id(),List.copyOf(r.getIngredients()),r.getResultItem(level.registryAccess()).copy(),Math.max(0,r.getScaledEnergy()));})
   .filter(EnergizingRecipes::valid).sorted(Comparator.comparing(e->e.id().toString())).toList();
 }
 public static boolean valid(Entry e){return !e.output().isEmpty() && e.ingredients().size()>0 && e.ingredients().size()<=9 && e.ingredients().stream().allMatch(i->!i.isEmpty() && i.getItems().length>0);}
 public static boolean material(Level level,ItemStack s){return !s.isEmpty() && !rod(s) && all(level).stream().anyMatch(e->e.ingredients().stream().anyMatch(i->i.test(s)));}
 /** Match ingredient units to item units with augmenting paths, including overlapping tags. */
 public static int[] plan(Entry entry, Container inventory) {
  int count = entry.ingredients().size();
  List<Integer> units = new ArrayList<>();
  for (int slot = 0; slot < 9; slot++) {
   int available = Math.min(count, inventory.getItem(16 + slot).getCount());
   for (int n = 0; n < available; n++) units.add(slot);
  }
  if (units.size() < count) return null;
  int[] owners = new int[units.size()];
  Arrays.fill(owners, -1);
  for (int ingredient = 0; ingredient < count; ingredient++) {
   if (!assign(entry, inventory, units, owners, ingredient, new boolean[units.size()])) return null;
  }
  int[] used = new int[9];
  for (int unit = 0; unit < owners.length; unit++) if (owners[unit] >= 0) used[units.get(unit)]++;
  return used;
 }
 private static boolean assign(Entry entry, Container inventory, List<Integer> units,
         int[] owners, int ingredient, boolean[] visited) {
  for (int unit = 0; unit < units.size(); unit++) {
   if (visited[unit] || !entry.ingredients().get(ingredient).test(inventory.getItem(16 + units.get(unit)))) continue;
   visited[unit] = true;
   if (owners[unit] < 0 || assign(entry, inventory, units, owners, owners[unit], visited)) {
    owners[unit] = ingredient;
    return true;
   }
  }
  return false;
 }
 private EnergizingRecipes(){}
}
