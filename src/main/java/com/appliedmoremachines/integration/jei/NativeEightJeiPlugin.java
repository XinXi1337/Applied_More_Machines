package com.appliedmoremachines.integration.jei;
import com.appliedmoremachines.MachineAvailability;
import mezz.jei.api.*;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
@JeiPlugin
public final class NativeEightJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("applied_more_machines:native_eight"); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        if(MachineAvailability.enabled("crystal_assembler_x8")) Extended.catalysts(r);
        if(MachineAvailability.enabled("circuit_etcher_x8")) Etcher.catalysts(r);
        if(MachineAvailability.enabled("overload_processing_factory_x8")) Factory.catalysts(r);
    }
    private static net.minecraft.world.item.Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("applied_more_machines",id)); }
    private static final class Extended {
        static void catalysts(IRecipeCatalystRegistration r) {
            r.addRecipeCatalyst(item("crystal_assembler_x8"),com.glodblock.github.extendedae.xmod.jei.recipe.CrystalAssemblerCategory.RECIPE_TYPE);
            r.addRecipeCatalyst(item("circuit_cutter_x8"),com.glodblock.github.extendedae.xmod.jei.recipe.CircuitCutterCategory.RECIPE_TYPE);
        }
    }
    private static final class Etcher {
        static void catalysts(IRecipeCatalystRegistration r) { r.addRecipeCatalyst(item("circuit_etcher_x8"),io.github.lounode.ae2cs.integration.jei.CircuitEtcherRecipeCategory.RECIPE_TYPE); }
    }
    private static final class Factory {
        static void catalysts(IRecipeCatalystRegistration r) { r.addRecipeCatalyst(item("overload_processing_factory_x8"),com.moakiee.ae2lt.integration.jei.category.OverloadProcessingCategory.TYPE); }
    }
}
