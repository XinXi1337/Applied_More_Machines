package com.appliedmoremachines.integration.jei;
import com.appliedmoremachines.reactor.ReactorRecipes;
import java.util.List;
import net.minecraft.world.level.Level;
/** JEI uses exactly the catalog admitted by the server processor. */
public final class ReactorProcessingJeiRecipes {
    private ReactorProcessingJeiRecipes() {}
    public static List<ReactorProcessingJeiRecipe> get(Level level) {
        return ReactorRecipes.entries(level).stream().map(entry -> new ReactorProcessingJeiRecipe(entry.id(),
                entry.inputs().stream().map(input -> new ReactorProcessingJeiRecipe.Cell(input.alternatives().stream()
                        .map(stack -> ReactorRecipes.stack(stack.what(),stack.amount())).toList())).toList(),
                entry.outputs().stream().map(stack -> new ReactorProcessingJeiRecipe.Cell(
                        List.of(ReactorRecipes.stack(stack.what(),stack.amount())))).toList()))
                .filter(ReactorProcessingJeiRecipe::fits).toList();
    }
}
