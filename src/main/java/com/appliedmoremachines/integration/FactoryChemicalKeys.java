package com.appliedmoremachines.integration;

import appeng.api.stacks.AEKey;
import java.lang.reflect.Method;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.neoforged.fml.ModList;

/** Applied Mekanistics is optional, including on installations without Mekanism. */
public final class FactoryChemicalKeys {
    private static Method factory;
    private static boolean checked;
    private FactoryChemicalKeys() {}

    public static boolean available() {
        if (!ModList.get().isLoaded("appmek")) return false;
        if (!checked) {
            checked = true;
            try {
                factory = Class.forName("me.ramidzkh.mekae2.ae2.MekanismKey").getMethod("of", ChemicalStack.class);
            } catch (ReflectiveOperationException ex) {
                com.appliedmoremachines.AppliedMoreMachines.LOGGER.warn("Applied Mekanistics chemical API unavailable", ex);
            }
        }
        return factory != null;
    }

    public static ChemicalStack stack(AEKey key, long amount) {
        return available() && key.getPrimaryKey() instanceof Chemical chemical
                ? new ChemicalStack(mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), amount) : ChemicalStack.EMPTY;
    }

    public static boolean isChemical(AEKey key) {
        return available() && key.getPrimaryKey() instanceof Chemical;
    }

    public static AEKey key(ChemicalStack stack) {
        if (stack.isEmpty() || !available()) return null;
        try {
            return (AEKey) factory.invoke(null, stack);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
