package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.FluidLaserBaseMenu;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.laser.FluidLaserProcessor;
import com.appliedmoremachines.laser.FluidLaserRecipes;
import com.appliedmoremachines.laser.FluidLaserSlots;
import com.appliedmoremachines.latex.LatexUpgrades;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** One loaded recipe per lens lane; equipment is retained and amounts reflect one drill without upgrades. */
public final class FluidLaserJeiCategory implements IRecipeCategory<FluidLaserRecipes.Entry> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_fluid_laser_base_jei.png");
    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_fluid_laser_base_gui.png");
    private static final int BASE_TICKS = new LatexUpgrades.Stats(0, 0, 0, 0, 0).ticks(true);
    private static final IDrawable GRADUATIONS = new IDrawable() {
        @Override public int getWidth() { return 14; }
        @Override public int getHeight() { return 44; }
        @Override public void draw(GuiGraphics graphics, int x, int y) {
            graphics.blit(GUI, x, y, 184, 48, 14, 44, 256, 256);
        }
    };
    private final IDrawable icon;
    private final ITickTimer timer;

    public FluidLaserJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModBlocks.AE_FLUID_LASER_BASE.get());
        timer = gui.createTickTimer(BASE_TICKS, BASE_TICKS, false);
    }
    @Override public RecipeType<FluidLaserRecipes.Entry> getRecipeType() { return FluidLaserJeiPlugin.TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.applied_more_machines.ae_fluid_laser_base"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 140; }
    @Override public ResourceLocation getRegistryName(FluidLaserRecipes.Entry recipe) {
        // A data pack may let one native recipe accept more than one of the three lenses.
        return ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(), recipe.id().getPath() + "/lane_" + recipe.lane());
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, FluidLaserRecipes.Entry recipe, IFocusGroup focuses) {
        builder.moveRecipeTransferButton(180, 125);
        int lane = recipe.lane();
        int first = lane == 0 ? 0 : lane == 1 ? 7 : 10;
        int end = lane == 0 ? 7 : lane == 1 ? 10 : 19;
        for (int slot = first; slot < end; slot++) equipment(builder, slot);
        equipment(builder, FluidLaserSlots.LENS + lane);
        equipment(builder, FluidLaserSlots.DRILL + lane);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 117 + 18 * lane, 82).setSlotName("fluid_" + lane)
                .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.fluid().copyWithAmount(FluidLaserProcessor.amount(1, 1))))
                .setFluidRenderer(FluidLaserProcessor.CAPACITY, false, 14, 44).setOverlay(GRADUATIONS, 0, 0);
    }
    private static void equipment(IRecipeLayoutBuilder builder, int slot) {
        int[] position = FluidLaserBaseMenu.EQUIPMENT_POSITIONS[slot];
        builder.addSlot(RecipeIngredientRole.CATALYST, position[0], position[1]).setSlotName("equipment_" + slot)
                .addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(FluidLaserSlots.expected(slot))))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("gui.applied_more_machines.fluid_laser.jei.retained"));
                    if (slot >= FluidLaserSlots.DRILL)
                        tooltip.add(Component.translatable("gui.applied_more_machines.fluid_laser.jei.drills"));
                });
    }
    @Override public void draw(FluidLaserRecipes.Entry recipe, IRecipeSlotsView slots,
            GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight(), 256, 256);
        int width = 40 * timer.getValue() / BASE_TICKS;
        if (width > 0) graphics.blit(GUI, 68, 94, 184, 8, width, 30, 256, 256);
    }
    @Override public void getTooltip(ITooltipBuilder tooltip, FluidLaserRecipes.Entry recipe,
            IRecipeSlotsView slots, double x, double y) {
        if (x >= 68 && x < 108 && y >= 94 && y < 124) {
            tooltip.add(Component.translatable("gui.applied_more_machines.fluid_laser.jei.preview"));
            tooltip.add(Component.translatable("gui.applied_more_machines.latex.jei.time", BASE_TICKS / 20));
            tooltip.add(Component.translatable("gui.applied_more_machines.latex.jei.energy",
                    NumberFormat.getIntegerInstance(Locale.ROOT).format(FluidLaserProcessor.COST)));
        }
    }
}
