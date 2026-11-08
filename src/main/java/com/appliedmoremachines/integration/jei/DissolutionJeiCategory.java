package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.*;
import com.appliedmoremachines.dissolution.*;
import java.util.*;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native-sized inventory-free user panel. Quantities/components come from the processing bridge. */
public final class DissolutionJeiCategory implements IRecipeCategory<DissolutionRecipes.Entry> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_dissolution_chamber_jei.png");
    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_dissolution_chamber_gui.png");
    private final IDrawable icon;
    private final ITickTimer timer;
    public DissolutionJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_DISSOLUTION_CHAMBER_FRONT.get());
        timer = gui.createTickTimer(160, 160, false);
    }
    @Override public RecipeType<DissolutionRecipes.Entry> getRecipeType() { return DissolutionJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.applied_more_machines.ae_dissolution_chamber_front"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 100; }
    @Override public ResourceLocation getRegistryName(DissolutionRecipes.Entry recipe) { return recipe.id(); }
    @Override public boolean isHandled(DissolutionRecipes.Entry recipe) { return recipe.displayInputs().size() <= 8; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, DissolutionRecipes.Entry recipe, IFocusGroup focuses) {
        // Keep JEI's native side column at the lower right, clear of tanks and labels.
        builder.moveRecipeTransferButton(180, 85);
        var inputs = recipe.displayInputs();
        if (inputs.size() > 8) return;
        for (int i = 0; i < inputs.size(); i++) {
            int[] pos = DissolutionChamberMenu.INPUT_POSITIONS[i];
            var input = inputs.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, pos[0], pos[1]).setSlotName("input_" + i)
                    .addItemStacks(Arrays.stream(input.ingredient().getItems()).filter(input.ingredient()::test)
                            .map(stack -> stack.copyWithCount(input.count())).toList());
        }
        if (recipe.fluid() != null) fluid(builder, RecipeIngredientRole.INPUT, "input_fluid", 50, 39, 14,
                Arrays.stream(recipe.fluid().getFluids()).map(FluidStack::copy).toList());
        if (!recipe.output().isEmpty()) {
            int remaining = recipe.output().getCount(), limit = Math.min(64, recipe.output().getMaxStackSize());
            for (int i = 0; remaining > 0 && i < 3; i++) {
                int count = Math.min(remaining, limit); remaining -= count;
                builder.addSlot(RecipeIngredientRole.OUTPUT, 127, 21 + 18 * i).setSlotName("output_" + i).addItemStack(recipe.output().copyWithCount(count));
            }
        }
        if (!recipe.outputFluid().isEmpty()) fluid(builder, RecipeIngredientRole.OUTPUT, "output_fluid", 150, 22, 50, List.of(recipe.outputFluid().copy()));
    }
    private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, String name,
                              int x, int y, int height, List<FluidStack> choices) {
        builder.addSlot(role, x, y).setSlotName(name).addIngredients(NeoForgeTypes.FLUID_STACK, choices)
                .setFluidRenderer(DissolutionProcessor.CAPACITY, false, 14, height).setOverlay(new IDrawable() {
                    @Override public int getWidth() { return 14; }
                    @Override public int getHeight() { return height; }
                    @Override public void draw(GuiGraphics graphics, int dx, int dy) {
                        graphics.blit(GUI, dx, dy, height == 14 ? 184 : 208, 36, 14, height, 256, 256);
                    }
                }, 0, 0);
    }
    @Override public void draw(DissolutionRecipes.Entry recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mx, double my) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, 176, 100, 256, 256);
        int width = 24 * timer.getValue() / 160;
        if (width > 0) graphics.blit(GUI, 94, 36, 184, 8, width, 18, 256, 256);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("gui.applied_more_machines.latex.jei.time", 8), 8, 83, 0xff413f54, false);
        graphics.drawString(font, Component.translatable("gui.applied_more_machines.latex.jei.energy", "4,000"), 87, 83, 0xff413f54, false);
    }
}
