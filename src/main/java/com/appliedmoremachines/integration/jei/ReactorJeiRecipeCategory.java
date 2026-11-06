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
public final class ReactorJeiRecipeCategory implements IRecipeCategory<ReactorJeiRecipe> {
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

    public ReactorJeiRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_REACTORS_PATTERN.get());
    }

    @Override public RecipeType<ReactorJeiRecipe> getRecipeType() { return ReactorJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("gui.applied_more_machines.reactor_jei.title"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(ReactorJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "ae_reactors_structure");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReactorJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.materials().size(); i++) {
            ReactorJeiRecipe.Material material = recipe.materials().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, 0, 0)
                    .setSlotName("material_" + i)
                    .addItemStack(new ItemStack(material.block(), material.count()));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, ReactorJeiRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> materialSlots = extras.getRecipeSlots().getSlots();
        var scroll = new ReactorMaterialScroll(materialSlots.size());
        extras.addSlottedWidget(new ReactorMaterialGridWidget(materialSlots, scroll), materialSlots);
        extras.addGuiEventListener(new ReactorMaterialScrollHandler(scroll));
        var rotation = new ReactorPreviewRotation();
        extras.addWidget(new ReactorPreviewWidget(recipe, rotation, PREVIEW_X, PREVIEW_Y,
                PREVIEW_WIDTH, PREVIEW_HEIGHT));
        extras.addInputHandler(new ReactorPreviewInputHandler(rotation, PREVIEW_X, PREVIEW_Y,
                PREVIEW_WIDTH, PREVIEW_HEIGHT));
    }

    @Override
    public void draw(ReactorJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        graphics.blit(BACKGROUND, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, 133);
        graphics.blit(MATERIALS, MATERIAL_X, MATERIAL_Y, 0, 0, 65, 112, 65, 112);
    }
}
