package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.*;
import com.appliedmoremachines.brewing.*;
import com.appliedmoremachines.client.BrewingVisuals;
import java.util.List;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Display three real bottle slots; the encoding bridge aggregates their amount to three. */
public final class BrewingJeiCategory implements IRecipeCategory<BrewingRecipes.Entry> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_brewing_jei.png");
    private final IDrawable icon;
    private final ITickTimer timer;
    public BrewingJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_BREWING.get());
        timer = gui.createTickTimer(400, 400, false);
    }
    @Override public RecipeType<BrewingRecipes.Entry> getRecipeType() { return BrewingJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.applied_more_machines.ae_brewing"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 100; }
    @Override public ResourceLocation getRegistryName(BrewingRecipes.Entry recipe) { return recipe.id(); }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, BrewingRecipes.Entry recipe, IFocusGroup focuses) {
        builder.moveRecipeTransferButton(180, 85);
        for (int i = 0; i < 3; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 62 + 18 * i, 18).setSlotName("bottle_" + i).addItemStack(recipe.input().copyWithCount(1));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 71 + 18 * i, 74).setSlotName("output_" + i).addItemStack(recipe.output().copyWithCount(1));
        }
        if (!recipe.ingredient().isEmpty()) builder.addSlot(RecipeIngredientRole.INPUT, 116, 18).setSlotName("ingredient").addItemStack(recipe.ingredient().copyWithCount(1));
    }
    @Override public void draw(BrewingRecipes.Entry recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mx, double my) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, 176, 100, 256, 256);
        BrewingVisuals.water(graphics, 16, 18);
        BrewingVisuals.progress(graphics, 86, 43, recipe.free() ? 1 : timer.getValue() / 400.0);
    }
    @SuppressWarnings("removal")
    @Override public List<Component> getTooltipStrings(BrewingRecipes.Entry recipe, IRecipeSlotsView slots, double x, double y) {
        if (x >= 86 && x < 110 && y >= 43 && y < 67) return List.of(Component.translatable(
                "gui.applied_more_machines.brewing.jei.cost", recipe.free() ? "0" : "16,000", recipe.free() ? 1 : 400));
        if (x >= 16 && x < 32 && y >= 18 && y < 90) return List.of(Component.translatable("block.minecraft.water"),
                Component.literal("8000 / 8000 mB"), Component.translatable("gui.applied_more_machines.brewing.sealed_water"));
        return List.of();
    }
}
