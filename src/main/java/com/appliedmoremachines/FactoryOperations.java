package com.appliedmoremachines;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.appliedmoremachines.factory.FactoryFormation;
import com.appliedmoremachines.factory.FactoryStructure;
import com.appliedmoremachines.factory.FactoryStructures;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative structure placement and dismantling without block effects. */
public final class FactoryOperations {
    private static final int[] PLAYER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35, 40
    };

    private FactoryOperations() {}

    public static void place(FactoryControllerBlockEntity controller, Player player) {
        if (!(controller.getLevel() instanceof ServerLevel level) || !level.mayInteract(player, controller.getBlockPos())
                || controller.isFormed()) {
            controller.setActionStatus(controller.isFormed()
                    ? FactoryControllerBlockEntity.STATUS_ALREADY_FORMED
                    : FactoryControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
            return;
        }

        Direction facing = controller.getBlockState().getValue(FactoryComponentBlock.FACING);
        FactoryFormation formation = FactoryStructures.prospective(
                FactoryStructures.view(level), facing, controller.getBlockPos());
        List<MissingCell> missing = new ArrayList<>();
        Map<ResourceLocation, Integer> required = new LinkedHashMap<>();
        int conflicts = 0;

        for (FactoryStructure.Cell cell : FactoryStructure.definition().cells()) {
            BlockPos pos = formation.position(cell.offset());
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) {
                conflicts++;
                continue;
            }
            ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if (FactoryStructure.definition().accepts(cell, actual)) {
                continue;
            }
            if (!level.getBlockState(pos).isAir()) {
                conflicts++;
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.get(cell.block());
            if (block == Blocks.AIR || block.asItem() == net.minecraft.world.item.Items.AIR) {
                controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
                return;
            }
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            missing.add(new MissingCell(pos, state));
            required.merge(cell.block(), 1, Integer::sum);
        }

        if (conflicts > 0) {
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_CONFLICT, conflicts);
            return;
        }
        if (missing.isEmpty()) {
            FactoryStructures.refresh(controller);
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACED, 0);
            return;
        }

        if (player.getAbilities().instabuild) {
            if (!placeMissing(level, missing)) {
                controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
                return;
            }
            refreshFactory(level, controller, formation);
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACED, 0);
            return;
        }

        IGrid grid = controller.aeGrid();
        IActionSource source = IActionSource.ofPlayer(player);
        Map<AEItemKey, Long> aeExtract = new LinkedHashMap<>();
        Map<AEItemKey, Long> playerExtract = new LinkedHashMap<>();
        List<ItemStack> playerBefore = playerSlots(player);
        long missingTotal = 0;
        long shortageTotal = 0;

        for (var entry : required.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            ItemStack sample = new ItemStack(block);
            long amount = entry.getValue();
            missingTotal += amount;
            long fromAe = grid == null ? 0
                    : grid.getStorageService().getInventory().extract(
                            AEItemKey.of(sample), amount, Actionable.SIMULATE, source);
            fromAe = Math.min(amount, Math.max(0, fromAe));
            if (fromAe > 0) aeExtract.put(AEItemKey.of(sample), fromAe);
            long fromPlayer = amount - fromAe;
            long availableInPlayer = countInPlayer(player, sample);
            if (fromPlayer > availableInPlayer) shortageTotal += fromPlayer - availableInPlayer;
            if (fromPlayer > 0) playerExtract.put(AEItemKey.of(sample), fromPlayer);
        }
        if (shortageTotal > 0) {
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_MATERIALS, safeInt(shortageTotal));
            return;
        }

        Map<AEItemKey, Long> extractedFromAe = new LinkedHashMap<>();
        if (grid != null) {
            for (var entry : aeExtract.entrySet()) {
                long extracted = grid.getStorageService().getInventory().extract(
                        entry.getKey(), entry.getValue(), Actionable.MODULATE, source);
                if (extracted != entry.getValue()) {
                    restoreToAe(grid, extractedFromAe, source);
                    controller.setActionStatus(FactoryControllerBlockEntity.STATUS_MATERIALS, safeInt(missingTotal));
                    return;
                }
                extractedFromAe.put(entry.getKey(), extracted);
            }
        }
        for (var entry : playerExtract.entrySet()) {
            int count = safeInt(entry.getValue());
            if (takeFromPlayer(player, entry.getKey().getReadOnlyStack(), count) != count) {
                restoreToAe(grid, extractedFromAe, source);
                restorePlayer(player, playerBefore);
                controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
                return;
            }
        }

        if (!placeMissing(level, missing)) {
            restoreToAe(grid, extractedFromAe, source);
            restorePlayer(player, playerBefore);
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
            return;
        }

        refreshFactory(level, controller, formation);
        controller.setActionStatus(FactoryControllerBlockEntity.STATUS_PLACED, 0);
    }

    private static void refreshFactory(ServerLevel level, FactoryControllerBlockEntity controller,
            FactoryFormation formation) {
        for (FactoryControllerBlockEntity member : FactoryUpgradeCards.controllers(controller)) {
            FactoryStructures.refresh(member);
        }
        if (level.getBlockEntity(formation.position(FactoryStructure.definition().controllerOffsets().get(1)))
                instanceof FactoryControllerBlockEntity second) {
            FactoryStructures.refresh(second);
        }
    }

    public static void dismantle(FactoryControllerBlockEntity controller, Player player) {
        if (!(controller.getLevel() instanceof ServerLevel level) || !controller.isFormed()) {
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_NOT_FORMED, 0);
            return;
        }
        FactoryFormation formation = controller.formation();
        if (formation == null) return;

        List<ItemStack> recovered = new ArrayList<>();
        List<BlockPos> removable = new ArrayList<>();
        for (FactoryStructure.Cell cell : FactoryStructure.definition().cells()) {
            BlockPos pos = formation.position(cell.offset());
            if (cell.block().equals(FactoryStructure.CONTROLLER)) continue;
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FactoryPatternBlockEntity pattern) {
                for (int slot = 0; slot < pattern.getContainerSize(); slot++) {
                    ItemStack stack = pattern.getItem(slot).copy();
                    if (!stack.isEmpty()) recovered.add(stack);
                }
            }
            List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, player.getMainHandItem());
            if (drops.isEmpty()) {
                Item item = state.getBlock().asItem();
                if (item != net.minecraft.world.item.Items.AIR) drops = List.of(new ItemStack(item));
            }
            recovered.addAll(drops);
            removable.add(pos);
        }

        IGrid grid = controller.aeGrid();
        IActionSource source = IActionSource.ofPlayer(player);
        Map<AEItemKey, Long> aeRemaining = new LinkedHashMap<>(countByKey(recovered));
        if (grid != null) {
            for (var entry : new LinkedHashMap<>(aeRemaining).entrySet()) {
                long inserted = grid.getStorageService().getInventory().insert(
                        entry.getKey(), entry.getValue(), Actionable.SIMULATE, source);
                inserted = Math.min(entry.getValue(), Math.max(0, inserted));
                if (inserted >= entry.getValue()) aeRemaining.remove(entry.getKey());
                else aeRemaining.put(entry.getKey(), entry.getValue() - inserted);
            }
        }
        if (!canFitInPlayer(player, aeRemaining)) {
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_RETURN_FULL, recovered.size());
            return;
        }

        Map<AEItemKey, Long> insertedIntoAe = new LinkedHashMap<>();
        if (grid != null) {
            for (var entry : countByKey(recovered).entrySet()) {
                long inserted = grid.getStorageService().getInventory().insert(
                        entry.getKey(), entry.getValue(), Actionable.MODULATE, source);
                if (inserted > 0) insertedIntoAe.put(entry.getKey(), inserted);
            }
        }
        Map<AEItemKey, Long> remainingAfterAe = countByKey(recovered);
        for (var entry : insertedIntoAe.entrySet()) {
            long left = remainingAfterAe.getOrDefault(entry.getKey(), 0L) - entry.getValue();
            if (left <= 0) remainingAfterAe.remove(entry.getKey());
            else remainingAfterAe.put(entry.getKey(), left);
        }
        if (!insertRemainingIntoPlayer(player, remainingAfterAe)) {
            restoreToAe(grid, insertedIntoAe, source);
            controller.setActionStatus(FactoryControllerBlockEntity.STATUS_RETURN_FULL, recovered.size());
            return;
        }

        for (BlockPos pos : removable) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (FactoryControllerBlockEntity member : FactoryUpgradeCards.controllers(controller)) {
            FactoryStructures.refresh(member);
        }
        if (level.getBlockEntity(formation.position(FactoryStructure.definition().controllerOffsets().get(1)))
                instanceof FactoryControllerBlockEntity second) {
            FactoryStructures.refresh(second);
        }
        controller.setActionStatus(FactoryControllerBlockEntity.STATUS_DISMANTLED, removable.size());
    }

    private static long countInPlayer(Player player, ItemStack sample) {
        long total = 0;
        Inventory inventory = player.getInventory();
        for (int slot : PLAYER_SLOTS) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, sample)) total += stack.getCount();
        }
        return total;
    }

    private static int takeFromPlayer(Player player, ItemStack sample, int amount) {
        Inventory inventory = player.getInventory();
        int remaining = amount;
        for (int slot : PLAYER_SLOTS) {
            ItemStack stack = inventory.getItem(slot);
            if (remaining <= 0) break;
            if (!ItemStack.isSameItemSameComponents(stack, sample)) continue;
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
            remaining -= taken;
        }
        inventory.setChanged();
        return amount - remaining;
    }

    private static boolean canFitInPlayer(Player player, Map<AEItemKey, Long> items) {
        List<ItemStack> slots = playerSlots(player);
        for (var entry : items.entrySet()) {
            long remaining = entry.getValue();
            if (remaining <= 0) continue;
            if (!insertInto(slots, entry.getKey().getReadOnlyStack(), remaining)) return false;
        }
        return true;
    }

    private static boolean insertRemainingIntoPlayer(Player player, Map<AEItemKey, Long> items) {
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = playerSlots(player);
        for (var entry : items.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (!insertInto(slots, entry.getKey().getReadOnlyStack(), entry.getValue())) return false;
        }
        for (int index = 0; index < slots.size(); index++) inventory.setItem(PLAYER_SLOTS[index], slots.get(index));
        inventory.setChanged();
        return true;
    }

    private static List<ItemStack> playerSlots(Player player) {
        List<ItemStack> slots = new ArrayList<>(PLAYER_SLOTS.length);
        for (int slot : PLAYER_SLOTS) slots.add(player.getInventory().getItem(slot).copy());
        return slots;
    }

    private static void restorePlayer(Player player, List<ItemStack> snapshot) {
        for (int index = 0; index < snapshot.size(); index++) {
            player.getInventory().setItem(PLAYER_SLOTS[index], snapshot.get(index).copy());
        }
        player.getInventory().setChanged();
    }

    private static boolean insertInto(List<ItemStack> slots, ItemStack sample, long amount) {
        long remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int index = 0; index < slots.size() && remaining > 0; index++) {
                ItemStack existing = slots.get(index);
                if (pass == 0 && (existing.isEmpty()
                        || !ItemStack.isSameItemSameComponents(existing, sample))) continue;
                if (pass == 1 && !existing.isEmpty()) continue;
                int limit = Math.min(sample.getMaxStackSize(), 64);
                int current = existing.isEmpty() ? 0 : existing.getCount();
                int inserted = (int) Math.min(remaining, Math.max(0, limit - current));
                if (inserted <= 0) continue;
                slots.set(index, sample.copyWithCount(current + inserted));
                remaining -= inserted;
            }
        }
        return remaining == 0;
    }

    private static Map<AEItemKey, Long> countByKey(List<ItemStack> stacks) {
        Map<AEItemKey, Long> counts = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            counts.merge(AEItemKey.of(stack), (long) stack.getCount(), Long::sum);
        }
        return counts;
    }

    private static void restoreToAe(IGrid grid, Map<AEItemKey, Long> items, IActionSource source) {
        if (grid == null) return;
        for (var entry : items.entrySet()) {
            grid.getStorageService().getInventory().insert(entry.getKey(), entry.getValue(), Actionable.MODULATE, source);
        }
    }

    private static boolean placeMissing(ServerLevel level, List<MissingCell> missing) {
        List<BlockPos> placed = new ArrayList<>();
        for (MissingCell cell : missing) {
            level.setBlock(cell.pos(), cell.state(), Block.UPDATE_ALL);
            placed.add(cell.pos());
            if (!level.getBlockState(cell.pos()).equals(cell.state())) {
                rollbackPlacement(level, placed);
                return false;
            }
        }
        return true;
    }

    private static void rollbackPlacement(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static int safeInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    private record MissingCell(BlockPos pos, BlockState state) {}
}
