package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.latex.LatexProcessor;
import com.appliedmoremachines.latex.LatexRecipes;
import java.text.NumberFormat;
import java.util.Locale;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** The user's 176x140 inventory-free panel, drawn at its native pixel size. */
public final class LatexJeiCategory implements IRecipeCategory<LatexJeiRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_latex_extraction_processor_jei.png");
    private static final IDrawable GRADUATIONS = new IDrawable() {
        @Override public int getWidth() { return 14; }
        @Override public int getHeight() { return 56; }
        @Override public void draw(GuiGraphics graphics, int x, int y) {
            for (int i = 0; i < 11; i++) {
                int width = i % 2 == 0 ? 2 : 4;
                graphics.fill(x + 14 - width, y + 3 + i * 5, x + 14, y + 4 + i * 5, 0xfff2f2f2);
            }
        }
    };
    private final IDrawable icon;
    private final ITickTimer timer;

    public LatexJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_LATEX_EXTRACTION_PROCESSOR.get());
        timer = gui.createTickTimer(80, 80, false);
    }

    @Override public RecipeType<LatexJeiRecipe> getRecipeType() { return LatexJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.applied_more_machines.ae_latex_extraction_processor"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 140; }
    @Override public ResourceLocation getRegistryName(LatexJeiRecipe recipe) { return recipe.id(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, LatexJeiRecipe recipe, IFocusGroup focuses) {
        builder.moveRecipeTransferButton(151, 26);
        var latex = BuiltInRegistries.FLUID.get(LatexRecipes.LATEX);
        fluid(builder, recipe.extraction() ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT,
                "latex", 17, latex, recipe.latex());
        if (recipe.extraction()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 16, 113).setSlotName("log").addItemStacks(recipe.logs());
        } else {
            fluid(builder, RecipeIngredientRole.INPUT, "water", 45, Fluids.WATER, recipe.water());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 57).setSlotName("rubber").addItemStack(recipe.result());
        }
    }

    private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, String name,
            int x, Fluid fluid, int amount) {
        builder.addSlot(role, x, 37).setSlotName(name).addFluidStack(fluid, amount)
                .setFluidRenderer(Math.max(LatexProcessor.CAPACITY, amount), false, 14, 56)
                .setOverlay(GRADUATIONS, 0, 0);
    }

    @Override public void draw(LatexJeiRecipe recipe, IRecipeSlotsView slots,
            GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight(), 256, 256);
        float progress = timer.getValue() / 80F;
        if (recipe.extraction()) {
            int width = (int) (48 * progress);
            if (width > 0) graphics.blit(TEXTURE, 76, 47, 184, 8, width, 36, 256, 256);
        } else {
            int height = (int) (16 * progress);
            if (height > 0) graphics.blit(TEXTURE, 162, 73 - height, 184, 68 - height, 4, height, 256, 256);
        }
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text(recipe.extraction() ? "extraction" : "rubber"), 8, 12, 0xff413f54, false);
        graphics.drawString(font, text("time", recipe.ticks() / 20), 76, 90, 0xff413f54, false);
        graphics.drawString(font, text("energy", NumberFormat.getIntegerInstance(Locale.ROOT).format(LatexProcessor.COST)),
                76, 103, 0xff413f54, false);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.applied_more_machines.latex.jei." + key, args);
    }
}
