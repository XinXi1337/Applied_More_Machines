package com.appliedmoremachines.integration;

import com.appliedmoremachines.fusion.CoreRecipe;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionInventory;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.StackIngredient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Only reached behind CoreRecipes' installed-mod guard. Never fabricate JSON recipes. */
public final class DraconicCoreRecipes {
    private DraconicCoreRecipes() {}
    public static List<CoreRecipe> read(Collection<RecipeHolder<?>> loaded) {
        List<CoreRecipe> result = new ArrayList<>();
        for (RecipeHolder<?> holder : loaded) {
            if (!(holder.value() instanceof IFusionRecipe source)) continue;
            var parts = source.fusionIngredients().stream().map(part ->
                    new CoreRecipe.Part(part.get(), count(part.get()), part.consume())).toList();
            CoreRecipe recipe = new CoreRecipe(holder.id(), source.getCatalyst(), count(source.getCatalyst()),
                    source.getRecipeTier().index, source.getEnergyCost(), parts, (level, registries, center, materials, tier) -> {
                        var input = new Input(center, materials, TechLevel.byIndex(tier));
                        return source.matches(input, level) && source.canStartCraft(input, level, null)
                                ? source.assemble(input, registries) : ItemStack.EMPTY;
                    });
            if (recipe.available()) result.add(recipe);
        }
        return List.copyOf(result);
    }
    private static int count(Ingredient ingredient) {
        return ingredient.getCustomIngredient() instanceof StackIngredient counted ? counted.getCount() : 1;
    }
    private static final class Input implements IFusionInventory {
        private ItemStack center;
        private ItemStack output = ItemStack.EMPTY;
        private final List<IFusionInjector> injectors;
        private final TechLevel tier;
        Input(ItemStack center, List<ItemStack> materials, TechLevel tier) {
            this.center = center; this.tier = tier;
            injectors = materials.stream().map(stack -> (IFusionInjector) new Injector(stack.copy(), tier)).toList();
        }
        @Override public ItemStack getCatalystStack() { return center; }
        @Override public ItemStack getOutputStack() { return output; }
        @Override public void setCatalystStack(ItemStack stack) { center = stack; }
        @Override public void setOutputStack(ItemStack stack) { output = stack; }
        @Override public List<IFusionInjector> getInjectors() { return injectors; }
        @Override public TechLevel getMinimumTier() { return tier; }
        @Override public int size() { return injectors.size() + 1; }
        @Override public ItemStack getItem(int slot) { return slot == 0 ? center : injectors.get(slot - 1).getInjectorStack(); }
    }
    private static final class Injector implements IFusionInjector {
        private ItemStack stack;
        private final TechLevel tier;
        Injector(ItemStack stack, TechLevel tier) { this.stack = stack; this.tier = tier; }
        @Override public TechLevel getInjectorTier() { return tier; }
        @Override public ItemStack getInjectorStack() { return stack; }
        @Override public void setInjectorStack(ItemStack stack) { this.stack = stack; }
        @Override public long getInjectorEnergy() { return 0; }
        @Override public void setInjectorEnergy(long energy) {}
        @Override public void setEnergyRequirement(long energy, long rate) {}
        @Override public long getEnergyRequirement() { return 0; }
        @Override public boolean validate() { return true; }
    }
}
