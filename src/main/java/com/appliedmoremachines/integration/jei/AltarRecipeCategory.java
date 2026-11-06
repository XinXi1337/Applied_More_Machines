package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.processing.SeedRecipe;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Nine input cells with the source recipe's material positions and quantities. */
public final class AltarRecipeCategory implements IRecipeCategory<SeedRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_infusion_ui.png");
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawableAnimated crystal;
    private final IDrawableAnimated progress;

    public AltarRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_INFUSION_PEDESTA.get());
        slot = gui.createDrawable(TEXTURE, 52, 21, 18, 18);
        crystal = gui.createAnimatedDrawable(gui.createDrawable(TEXTURE, 204, 5, 13, 23),
                80, IDrawableAnimated.StartDirection.LEFT, false);
        progress = gui.createAnimatedDrawable(gui.createDrawable(TEXTURE, 176, 34, 6, 18),
                80, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override public RecipeType<SeedRecipe> getRecipeType() { return AltarJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.applied_more_machines.ae_infusion_pedesta"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 84; }
    @Override public ResourceLocation getRegistryName(SeedRecipe recipe) { return recipe.id(); }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SeedRecipe recipe, IFocusGroup focuses) {
        List<SeedRecipe.DisplayIngredient> inputs = recipe.displayInputs();
        for (int i = 0; i < inputs.size(); i++) {
            int x = 17 + i % 3 * 18;
            int y = 22 + i / 3 * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setSlotName("material_" + i)
                    .addItemStacks(inputs.get(i).stacks());
        }
        var level = Minecraft.getInstance().level;
        if (level != null) builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 41)
                .addItemStack(recipe.source().getResultItem(level.registryAccess()).copy());
    }

    @Override
    public void draw(SeedRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, 176, 84, 256, 256);
        graphics.fill(1, 81, 175, 83, 0xffcbccd4);
        graphics.fill(0, 83, 176, 84, 0xff413f54);
        graphics.fill(14, 18, 72, 78, 0xffcbccd4);
        for (int i = 0; i < 9; i++) {
            slot.draw(graphics, 16 + i % 3 * 18, 21 + i / 3 * 18);
        }
        crystal.draw(graphics, 82, 39);
        progress.draw(graphics, 132, 40);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, SeedRecipe recipe, IRecipeSlotsView slots, double x, double y) {
        if (x >= 78 && x <= 140 && y >= 32 && y <= 66) {
            tooltip.add(Component.translatable("gui.applied_more_machines.recipe_cost"));
        }
    }
}
