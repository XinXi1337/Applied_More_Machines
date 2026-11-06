package com.appliedmoremachines.factory;

import appeng.api.networking.crafting.ICraftingProvider;
import com.appliedmoremachines.integration.FactoryEcoBatch;
import com.appliedmoremachines.integration.FactoryOmniBatch;
import com.appliedmoremachines.integration.FactoryThunderBatch;
import com.appliedmoremachines.integration.FactoryTrinityBatch;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.ModList;

/** Expose only installed CPU APIs; each typed implementation is isolated in its own class. */
public final class FactoryBatchProviders {
    private FactoryBatchProviders() {}

    public static ICraftingProvider create(ProcessingCraftingProvider base) {
        List<ICraftingProvider> bridges = new ArrayList<>();
        if (ModList.get().isLoaded("molecularmanipulator")) bridges.add(new FactoryOmniBatch(base));
        if (ModList.get().isLoaded("neoecoae")) bridges.add(new FactoryEcoBatch(base));
        if (ModList.get().isLoaded("thunderbolt")) bridges.add(new FactoryThunderBatch(base));
        if (ModList.get().isLoaded("data_energistics")) bridges.add(new FactoryTrinityBatch(base));
        if (bridges.isEmpty()) return base;
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.add(ICraftingProvider.class);
        for (var bridge : bridges) for (var api : bridge.getClass().getInterfaces()) interfaces.add(api);
        return (ICraftingProvider) Proxy.newProxyInstance(FactoryBatchProviders.class.getClassLoader(),
                interfaces.toArray(Class<?>[]::new), (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        default -> "AE Universal Factory batch provider";
                    };
                    Object target = base;
                    if (!method.getDeclaringClass().isInstance(base)) {
                        target = bridges.stream().filter(method.getDeclaringClass()::isInstance).findFirst().orElseThrow();
                    }
                    try { return method.invoke(target, args); }
                    catch (InvocationTargetException ex) { throw ex.getCause(); }
                });
    }
}
