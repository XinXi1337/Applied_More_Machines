package com.appliedmoremachines;

import com.appliedmoremachines.factory.FactoryMachineTiers;
import com.appliedmoremachines.factory.FactoryRecipes;
import com.appliedmoremachines.factory.FactoryStructures;
import com.appliedmoremachines.factory.FactoryTierUpgradePlan;
import com.appliedmoremachines.integration.FactoryMachineReplacement;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class FactoryTierUpgradeInteraction {
    private FactoryTierUpgradeInteraction() {}
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        var tier = FactoryMachineTiers.installer(BuiltInRegistries.ITEM.getKey(held.getItem()));
        if (tier == null || !(event.getLevel().getBlockEntity(event.getPos()) instanceof FactoryControllerBlockEntity controller)) return;
        var player = event.getEntity();
        if (player.isSpectator() || !player.getAbilities().mayBuild
                || !event.getLevel().mayInteract(player, event.getPos())
                || !player.mayUseItemAt(event.getPos(), event.getHitVec().getDirection(), held)) return;
        if (event.getLevel() instanceof ServerLevel level) {
            var result = upgrade(level, controller, player, held, tier);
            player.displayClientMessage(Component.translatable("gui.applied_more_machines.factory_upgrade."
                    + result.name().toLowerCase(java.util.Locale.ROOT), Component.translatable(tier.translationKey()), tier.cost()), true);
        }
        // Consume successful and refused attempts, preventing item fallback or the controller menu opening.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    private static FactoryTierUpgradePlan.Result upgrade(ServerLevel level, FactoryControllerBlockEntity controller,
            Player player, ItemStack held, FactoryMachineTiers.Tier tier) {
        FactoryStructures.refresh(controller);
        var engine = controller.processor();
        var plan = FactoryTierUpgradePlan.plan(FactoryStructures.view(level), controller.formation(), tier,
                held.getCount(), engine != null && engine.hasWork(), BuiltInRegistries.BLOCK::containsKey);
        if (plan.result() != FactoryTierUpgradePlan.Result.SUCCESS) return plan.result();
        if (!ModList.get().isLoaded("mekanism")) return FactoryTierUpgradePlan.Result.NO_TARGET;
        var replacements = new ArrayList<FactoryMachineReplacement>();
        for (var change : plan.changes()) {
            if (!level.mayInteract(player, change.pos()) || !player.mayUseItemAt(change.pos(), eventFace(controller), held)
                    || !FactoryMachineReplacement.accessible(level, change.pos(), player)) return FactoryTierUpgradePlan.Result.ACCESS;
            try {
                var replacement = FactoryMachineReplacement.prepare(level, change);
                if (replacement == null) return FactoryTierUpgradePlan.Result.FAILED;
                replacements.add(replacement);
            } catch (RuntimeException ex) {
                AppliedMoreMachines.LOGGER.warn("Cannot prepare factory machine upgrade at {}", change.pos(), ex);
                return FactoryTierUpgradePlan.Result.FAILED;
            }
        }
        if (!FactoryTierUpgradePlan.commit(replacements)) return FactoryTierUpgradePlan.Result.FAILED;
        if (!player.getAbilities().instabuild) held.shrink(tier.cost());
        replacements.forEach(FactoryMachineReplacement::finish);
        FactoryRecipes.invalidate();
        FactoryStructures.refresh(controller);
        return FactoryTierUpgradePlan.Result.SUCCESS;
    }

    private static net.minecraft.core.Direction eventFace(FactoryControllerBlockEntity controller) {
        return controller.getBlockState().getValue(FactoryComponentBlock.FACING);
    }
}
