package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Interactive 3D view of the complete AE factory and its aggregated block costs. */
public final class FactoryJeiRecipeCategory implements IRecipeCategory<FactoryJeiRecipe> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_factory_jei_1.png");
    private static final ResourceLocation MATERIALS = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_factory_jei_2.png");
    private static final int WIDTH = 183;
    private static final int HEIGHT = 126;
    private static final int MATERIAL_X = 116;
    private static final int MATERIAL_Y = 10;
    private static final int MATERIAL_COLUMNS = 3;
    private static final int MATERIAL_ROWS = 6;
    private static final int PREVIEW_X = 5;
    private static final int PREVIEW_Y = 7;
    private static final int PREVIEW_WIDTH = 107;
    private static final int PREVIEW_HEIGHT = 119;
    private final IDrawable icon;

    public FactoryJeiRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_FACTORY_PATTERN.get());
    }

    @Override public RecipeType<FactoryJeiRecipe> getRecipeType() { return FactoryJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("gui.applied_more_machines.factory_jei.title"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(FactoryJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "ae_factory_structure");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FactoryJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.materials().size(); i++) {
            FactoryJeiRecipe.Material material = recipe.materials().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, 0, 0)
                    .setSlotName("material_" + i)
                    .addItemStack(new ItemStack(material.block(), material.count()));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, FactoryJeiRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> materialSlots = extras.getRecipeSlots().getSlots();
        var scroll = new FactoryMaterialScroll(materialSlots.size());
        extras.addSlottedWidget(new FactoryMaterialGridWidget(materialSlots, scroll), materialSlots);
        extras.addGuiEventListener(new FactoryMaterialScrollHandler(scroll));
        var rotation = new FactoryPreviewRotation();
        extras.addWidget(new FactoryPreviewWidget(recipe, rotation, PREVIEW_X, PREVIEW_Y,
                PREVIEW_WIDTH, PREVIEW_HEIGHT));
        extras.addInputHandler(new FactoryPreviewInputHandler(rotation, PREVIEW_X, PREVIEW_Y,
                PREVIEW_WIDTH, PREVIEW_HEIGHT));
    }

    @Override
    public void draw(FactoryJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        graphics.blit(BACKGROUND, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, 133);
        graphics.blit(MATERIALS, MATERIAL_X, MATERIAL_Y, 0, 0, 65, 112, 65, 112);
    }
}
