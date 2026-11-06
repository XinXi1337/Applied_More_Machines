package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.client.FactoryProgressMask;
import com.appliedmoremachines.factory.FactoryEnergy;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Uses the user's unscaled 209x167 panel inside the 256x256 atlas. */
public final class ReactorProcessingJeiCategory implements IRecipeCategory<ReactorProcessingJeiRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_factory_jei.png");
    public static final int INPUT_X = 13, OUTPUT_X = 144, SLOT_Y = 57, SLOT_SPACING = 18;
    private final IDrawable icon;
    private final ITickTimer timer;
    private FactoryProgressMask progress;

    public ReactorProcessingJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_REACTORS_CONTROLLER.get());
        timer = gui.createTickTimer(160, 160, false);
    }

    @Override public RecipeType<ReactorProcessingJeiRecipe> getRecipeType() { return ReactorJeiPlugin.PROCESSING_TYPE; }
    @Override public Component getTitle() { return Component.translatable("gui.applied_more_machines.reactor_jei.processing"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 209; }
    @Override public int getHeight() { return 167; }
    @Override public ResourceLocation getRegistryName(ReactorProcessingJeiRecipe recipe) { return recipe.id(); }
    @Override public boolean isHandled(ReactorProcessingJeiRecipe recipe) { return recipe.fits(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, ReactorProcessingJeiRecipe recipe, IFocusGroup focuses) {
        // JEI clips tall recipes at large GUI scales. Keep + and the bookmark above the ingredient grids.
        builder.moveRecipeTransferButton(181, 34);
        slots(builder, recipe.inputs(), RecipeIngredientRole.INPUT, INPUT_X);
        slots(builder, recipe.outputs(), RecipeIngredientRole.OUTPUT, OUTPUT_X);
    }

    private static void slots(IRecipeLayoutBuilder builder, List<ReactorProcessingJeiRecipe.Cell> cells,
            RecipeIngredientRole role, int x) {
        for (int i = 0; i < cells.size(); i++) {
            var variants = cells.get(i).alternatives();
            var slot = builder.addSlot(role, x + i % 3 * SLOT_SPACING, SLOT_Y + i / 3 * SLOT_SPACING)
                    .setSlotName(role.name().toLowerCase(Locale.ROOT) + "_" + i).addIngredientsUnsafe(variants);
            if (variants.getFirst() instanceof FluidStack) {
                long capacity = variants.stream().mapToLong(value -> ((FluidStack) value).getAmount()).max().orElse(1);
                slot.setFluidRenderer(capacity, false, 16, 16);
            }
        }
    }

    @Override public void draw(ReactorProcessingJeiRecipe recipe, IRecipeSlotsView slots,
            GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight(), 256, 256);
        if (progress == null) progress = new FactoryProgressMask(TEXTURE, 80, 65, 48, 36);
        progress.draw(graphics, 0, 0, timer.getValue() / 160F);
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, ReactorProcessingJeiRecipe recipe,
            IRecipeSlotsView slots, double x, double y) {
        if (x >= 84 && x < 124 && y >= 69 && y < 97) {
            tooltip.add(Component.translatable("gui.applied_more_machines.factory_jei.operation"));
            tooltip.add(Component.translatable("gui.applied_more_machines.factory_jei.round_cost",
                    NumberFormat.getIntegerInstance(Locale.ROOT).format(FactoryEnergy.ROUND_COST)));
        }
    }
}
