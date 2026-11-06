package com.appliedmoremachines.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import java.util.function.Supplier;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** CPU pushPattern has no job argument; mixins scope its existing link to that call. */
public final class FactoryJobContext {
    private static final ThreadLocal<ICraftingLink> CURRENT = new ThreadLocal<>();
    private FactoryJobContext() {}
    public static ICraftingLink current() { return CURRENT.get(); }

    public static void run(ICraftingLink link, Runnable action) {
        call(link, () -> { action.run(); return null; });
    }

    public static <T> T call(ICraftingLink link, Supplier<T> action) {
        ICraftingLink previous = CURRENT.get();
        if (link == null) CURRENT.remove(); else CURRENT.set(link);
        try { return action.get(); }
        finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    public static boolean push(ICraftingLink link, ICraftingProvider provider, IPatternDetails details, KeyCounter[] inputs) {
        return call(link, () -> provider.pushPattern(details, inputs));
    }

    public static void cancel(ICraftingLink link) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (link != null && server != null) {
            FactoryProcessingData.get(server).cancel(link.getCraftingID());
            if (net.neoforged.fml.ModList.get().isLoaded("bigreactors"))
                com.appliedmoremachines.reactor.ReactorProcessingData.get(server).cancel(link.getCraftingID());
        }
    }
}
