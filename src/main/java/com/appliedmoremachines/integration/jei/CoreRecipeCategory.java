package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.fusion.CoreRecipe;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Same sixteen material cells and central slots as the machine, plus a separate result preview. */
public final class CoreRecipeCategory implements IRecipeCategory<CoreJeiRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_crafting_core_gui.png");
    private static final String[] INJECTORS = { "basic", "wyvern", "awakened", "chaotic" };
    private static final int RESULT_X = 80;
    private static final int RESULT_Y = 112;
    private final IDrawable icon;

    public CoreRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_CRAFTING_CORE.get());
    }

    @Override public RecipeType<CoreJeiRecipe> getRecipeType() { return CoreJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.applied_more_machines.ae_crafting_core"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 136; }
    @Override public ResourceLocation getRegistryName(CoreJeiRecipe recipe) { return recipe.source().id(); }

    @Override public boolean isHandled(CoreJeiRecipe recipe) {
        // Recipes that cannot be represented in the machine's sixteen cells must not draw outside the card.
        return recipe.source().displayParts().size() <= 16;
    }

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems()).filter(stack -> CoreRecipe.accepts(ingredient, count, stack))
                .map(stack -> stack.copyWithCount(count)).toList();
    }

    private static ItemStack injector(int tier) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                "draconicevolution", INJECTORS[tier] + "_crafting_injector")));
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, CoreJeiRecipe display, IFocusGroup focuses) {
        CoreRecipe recipe = display.source();
        var materials = recipe.displayParts();
        for (int i = 0; i < materials.size(); i++) {
            CoreRecipe.Part part = materials.get(i);
            int group = i / 8, cell = i % 8;
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 18 + group * 106 + cell % 2 * 18,
                    26 + cell / 2 * 18).setSlotName("material_" + i)
                    .addItemStacks(stacks(part.ingredient(), part.count()));
            if (!part.consume()) slot.addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("gui.applied_more_machines.core_recipe_retained")));
        }
        List<ItemStack> equipment = new ArrayList<>();
        for (int tier = recipe.tier(); tier < INJECTORS.length; tier++) equipment.add(injector(tier));
        builder.addSlot(RecipeIngredientRole.CATALYST, 80, 26).setSlotName("injectors")
                .addItemStacks(equipment).addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("gui.applied_more_machines.core_recipe_tier", injector(recipe.tier()).getHoverName()));
                    tooltip.add(Component.translatable("gui.applied_more_machines.core_injector_help"));
                    tooltip.add(Component.translatable("gui.applied_more_machines.core_recipe_layout"));
                });
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 80).setSlotName("catalyst")
                .addItemStacks(stacks(recipe.catalyst(), recipe.catalystCount()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("gui.applied_more_machines.core_center_help")));
        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, RESULT_Y).setSlotName("result")
                .addItemStack(display.result().copy());
    }

    @Override public void draw(CoreJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        // Extend the original side rails and close them with the original bottom edge.
        // A flat footer would erase the frame; the player inventory must stay outside this crop.
        graphics.blit(TEXTURE, 0, 0, 176, 106, 0.0F, 0.0F, 149, 90, 256, 256);
        graphics.blit(TEXTURE, 0, 106, 176, 25, 0.0F, 90.0F, 149, 1, 256, 256);
        graphics.blit(TEXTURE, 0, 131, 176, 5, 0.0F, 174.0F, 149, 4, 256, 256);
        // Reuse the top equipment slot's pale AE frame, at the same 18px size as the inputs.
        graphics.blit(TEXTURE, RESULT_X - 1, RESULT_Y - 1, 18, 18, 67.0F, 21.0F, 15, 15, 256, 256);
        var font = Minecraft.getInstance().font;
        var label = Component.translatable("gui.applied_more_machines.core_recipe_result");
        graphics.drawString(font, label, RESULT_X - 8 - font.width(label), RESULT_Y + 4, 0xff413f54, false);
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, CoreJeiRecipe display,
            IRecipeSlotsView slots, double x, double y) {
        if (x >= 76 && x < 100 && y >= 49 && y < 73) {
            tooltip.add(Component.translatable("gui.applied_more_machines.core_recipe_cost",
                    NumberFormat.getIntegerInstance(Locale.ROOT).format(display.source().energyFE())));
            tooltip.add(Component.translatable("gui.applied_more_machines.core_recipe_layout"));
        }
    }
}
