package com.appliedmoremachines.integration.jei;
import com.appliedmoremachines.*;
import com.appliedmoremachines.energizing.*;
import com.appliedmoremachines.client.EnergizingVisuals;
import java.util.*;
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
public final class EnergizingJeiCategory implements IRecipeCategory<EnergizingRecipes.Entry> {
 private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,"textures/gui/ae_energizing_orb_jei.png");
 private final IDrawable icon;private final ITickTimer timer;
 public EnergizingJeiCategory(IGuiHelper gui){icon=gui.createDrawableItemLike(ModBlocks.AE_ENERGIZING_ORB.get());timer=gui.createTickTimer(100,100,false);}
 public RecipeType<EnergizingRecipes.Entry> getRecipeType(){return EnergizingJeiPlugin.TYPE;}
 public Component getTitle(){return Component.translatable("block.applied_more_machines.ae_energizing_orb");}
 public IDrawable getIcon(){return icon;}
 public int getWidth(){return 176;}public int getHeight(){return 100;}
 public ResourceLocation getRegistryName(EnergizingRecipes.Entry e){return e.id();}
 public void setRecipe(IRecipeLayoutBuilder b,EnergizingRecipes.Entry e,IFocusGroup focus){
  b.moveRecipeTransferButton(180,85);
  for(int[] p:AEEnergizingMenu.positions(true))b.addSlot(RecipeIngredientRole.CATALYST,p[0],p[1]).addItemStacks(EnergizingRecipes.rods());
  int[][] p=AEEnergizingMenu.positions(false);
  for(int i=0;i<e.ingredients().size();i++)b.addSlot(RecipeIngredientRole.INPUT,p[i][0],p[i][1]).addIngredients(e.ingredients().get(i));
  b.addSlot(RecipeIngredientRole.OUTPUT,148,42).addItemStack(e.output());
 }
 public void draw(EnergizingRecipes.Entry e,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
  g.blit(TEXTURE,0,0,0,0,176,100,176,100);EnergizingVisuals.progress(g,108,40,timer.getValue()/100.0);
 }
 @SuppressWarnings("removal") public List<Component> getTooltipStrings(EnergizingRecipes.Entry e,IRecipeSlotsView slots,double x,double y){
  return x>=108&&x<136&&y>=40&&y<60?List.of(Component.translatable("gui.applied_more_machines.energizing.cost",e.energy()),Component.translatable("gui.applied_more_machines.energizing.time")):List.of();
 }
}
