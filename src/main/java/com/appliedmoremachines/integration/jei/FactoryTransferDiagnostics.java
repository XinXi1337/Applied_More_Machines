package com.appliedmoremachines.integration.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferListener;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Temporary tracing of the real GUI path; never performs or retries a transfer. */
final class FactoryTransferDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static IJeiRuntime runtime;
    private static boolean listening;
    private static WeakReference<PatternEncodingTermMenu> checkedMenu = new WeakReference<>(null);

    static void runtime(IJeiRuntime value) {
        runtime = value;
        checkedMenu.clear();
        if (value != null && !listening) {
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, FactoryTransferDiagnostics::pressed);
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, FactoryTransferDiagnostics::released);
            listening = true;
        }
        var source = FactoryJeiPlugin.class.getProtectionDomain().getCodeSource();
        if (value != null) LOGGER.info("[Factory JEI] diagnostics active; plugin source={}",
                source == null ? "unknown" : source.getLocation());
    }

    static IRecipeTransferListener listener() {
        return new IRecipeTransferListener() {
            @Override public void beforeRecipeTransfer(IRecipeTransferContext<?, ?> context) {
                if (FactoryJeiPlugin.PROCESSING_TYPE.equals(context.getRecipeType())) {
                    LOGGER.info("[Factory JEI] service entered; menu={}, recipe={}, shift={}",
                            context.getContainer().getClass().getName(),
                            context.getRecipe() instanceof FactoryProcessingJeiRecipe recipe ? recipe.id() : context.getRecipe(),
                            context.isMaxTransfer());
                }
            }
        };
    }

    static void handler(PatternEncodingTermMenu menu, FactoryProcessingJeiRecipe recipe, boolean transfer) {
        if (transfer || checkedMenu.get() != menu) {
            LOGGER.info("[Factory JEI] handler {}; menu={}, recipe={}, mode={}, inputs={}, outputs={}",
                    transfer ? "execute" : "preflight", menu.getClass().getName(), recipe.id(), menu.getMode(),
                    menu.getProcessingInputSlots().length, menu.getProcessingOutputSlots().length);
            checkedMenu = new WeakReference<>(menu);
        }
    }

    static void sent(FactoryProcessingJeiRecipe recipe) {
        LOGGER.info("[Factory JEI] native encoder returned; recipe={}", recipe.id());
    }

    private static void pressed(ScreenEvent.MouseButtonPressed.Pre event) {
        click(event.getScreen(), event.getButton(), event.getMouseX(), event.getMouseY(), "press");
    }

    private static void released(ScreenEvent.MouseButtonReleased.Pre event) {
        click(event.getScreen(), event.getButton(), event.getMouseX(), event.getMouseY(), "release");
    }

    private static void click(Object screen, int button, double x, double y, String phase) {
        var current = runtime;
        var player = Minecraft.getInstance().player;
        if (current == null || button != 0 || screen != current.getRecipesGui()
                || player == null || !(player.containerMenu instanceof PatternEncodingTermMenu menu)) return;
        var category = new FactoryProcessingJeiCategory(current.getJeiHelpers().getGuiHelper());
        var handler = current.getRecipeTransferManager().getRecipeTransferHandler(menu, category);
        LOGGER.info("[Factory JEI] GUI {}; x={}, y={}, menu={}, factory handler={}, parent={}", phase, x, y,
                menu.getClass().getName(), handler.map(value -> value.getClass().getName()).orElse("MISSING"),
                current.getRecipesGui().getParentScreen().map(value -> value.getClass().getName()).orElse("MISSING"));
    }
}
