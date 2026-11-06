package com.appliedmoremachines;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.appliedmoremachines.reactor.ReactorFormation;
import com.appliedmoremachines.reactor.ReactorStructure;
import com.appliedmoremachines.reactor.ReactorStructures;
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
public final class ReactorOperations {
    private static final int[] PLAYER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35, 40
    };

    private ReactorOperations() {}

    public static void place(ReactorControllerBlockEntity controller, Player player) {
        if (!(controller.getLevel() instanceof ServerLevel level) || !level.mayInteract(player, controller.getBlockPos())
                || controller.isFormed()) {
            controller.setActionStatus(controller.isFormed()
                    ? ReactorControllerBlockEntity.STATUS_ALREADY_FORMED
                    : ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
            return;
        }

        controller.flushStructureReturns(player);
        if (!controller.structureReturns().isEmpty()) {
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_RETURN_FULL,0); return;
        }
        Direction facing = controller.getBlockState().getValue(ReactorComponentBlock.FACING);
        ReactorFormation formation = ReactorStructures.prospective(
                ReactorStructures.view(level), facing, controller.getBlockPos());
        List<MissingCell> missing = new ArrayList<>();
        Map<ResourceLocation, Integer> required = new LinkedHashMap<>();
        int conflicts = 0;

        for (ReactorStructure.Cell cell : ReactorStructure.definition().cells()) {
            BlockPos pos = formation.position(cell.offset());
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem())) {
                conflicts++;
                continue;
            }
            ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if (cell.block().equals(actual)) {
                continue;
            }
            if (!level.getBlockState(pos).isAir()) {
                conflicts++;
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.get(cell.block());
            if (block == Blocks.AIR || block.asItem() == net.minecraft.world.item.Items.AIR) {
                controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
                return;
            }
            BlockState state = ReactorStructure.definition().state(cell, formation.quarterTurns());
            missing.add(new MissingCell(pos, state));
            required.merge(cell.block(), 1, Integer::sum);
        }

        if (conflicts > 0) {
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_CONFLICT, conflicts);
            return;
        }
        if (missing.isEmpty()) {
            ReactorStructures.refresh(controller);
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACED, 0);
            return;
        }

        if (player.getAbilities().instabuild) {
            if (!placeMissing(level, missing)) {
                controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
                return;
            }
            refreshReactor(level, controller, formation);
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACED, 0);
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
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_MATERIALS, safeInt(shortageTotal));
            return;
        }

        Map<AEItemKey, Long> extractedFromAe = new LinkedHashMap<>();
        if (grid != null) {
            for (var entry : aeExtract.entrySet()) {
                long extracted = grid.getStorageService().getInventory().extract(
                        entry.getKey(), entry.getValue(), Actionable.MODULATE, source);
                if (extracted > 0) extractedFromAe.put(entry.getKey(), extracted);
                if (extracted != entry.getValue()) {
                    controller.retainStructureReturns(extractedFromAe);
                    controller.flushStructureReturns(player);
                    controller.setActionStatus(ReactorControllerBlockEntity.STATUS_MATERIALS, safeInt(missingTotal));
                    return;
                }
                extractedFromAe.put(entry.getKey(), extracted);
            }
        }
        for (var entry : playerExtract.entrySet()) {
            int count = safeInt(entry.getValue());
            if (takeFromPlayer(player, entry.getKey().getReadOnlyStack(), count) != count) {
                restorePlayer(player, playerBefore);
                controller.retainStructureReturns(extractedFromAe);
                controller.flushStructureReturns(player);
                controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
                return;
            }
        }

        if (!placeMissing(level, missing)) {
            restorePlayer(player, playerBefore);
            controller.retainStructureReturns(extractedFromAe);
            controller.flushStructureReturns(player);
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0);
            return;
        }

        refreshReactor(level, controller, formation);
        controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACED, 0);
    }

    private static void refreshReactor(ServerLevel level, ReactorControllerBlockEntity controller,
            ReactorFormation formation) {
        for (ReactorControllerBlockEntity member : ReactorUpgradeCards.controllers(controller)) {
            ReactorStructures.refresh(member);
        }
        if (level.getBlockEntity(formation.position(ReactorStructure.definition().controllerOffsets().get(1)))
                instanceof ReactorControllerBlockEntity second) {
            ReactorStructures.refresh(second);
        }
    }

    public static void dismantle(ReactorControllerBlockEntity controller, Player player) {
        controller.flushStructureReturns(player);
        if (!(controller.getLevel() instanceof ServerLevel level) || !controller.isFormed()) {
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_NOT_FORMED, 0);
            return;
        }
        ReactorFormation formation = controller.formation();
        if (formation == null) return;

        if (!ReactorStructure.definition().matches(ReactorStructures.view(level), formation)) {
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_NOT_FORMED, 0); return;
        }
        List<ItemStack> recovered = new ArrayList<>();
        Map<BlockPos, List<ItemStack>> recoveredByBlock = new LinkedHashMap<>();
        List<BlockPos> removable = new ArrayList<>();
        for (ReactorStructure.Cell cell : ReactorStructure.definition().cells()) {
            BlockPos pos = formation.position(cell.offset());
            if (cell.block().equals(ReactorStructure.CONTROLLER)) continue;
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem())) {
                controller.setActionStatus(ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, 0); return;
            }
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof appeng.blockentity.storage.SkyStoneTankBlockEntity tank && !tank.getTank().getFluid().isEmpty()) {
                controller.setActionStatus(ReactorControllerBlockEntity.STATUS_TANK_NOT_EMPTY,0); return;
            }
            List<ItemStack> cellRecovered = new ArrayList<>();
            if (blockEntity instanceof ReactorPatternBlockEntity pattern) {
                for (int slot = 0; slot < pattern.getContainerSize(); slot++) {
                    ItemStack stack = pattern.getItem(slot).copy();
                    if (!stack.isEmpty()) cellRecovered.add(stack);
                }
            }
            List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, player.getMainHandItem());
            if (drops.isEmpty()) {
                Item item = state.getBlock().asItem();
                if (item != net.minecraft.world.item.Items.AIR) drops = List.of(new ItemStack(item));
            }
            cellRecovered.addAll(drops);
            recovered.addAll(cellRecovered);
            recoveredByBlock.put(pos, cellRecovered);
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
            controller.setActionStatus(ReactorControllerBlockEntity.STATUS_RETURN_FULL, recovered.size());
            return;
        }

        // Only removed blocks become owned returns. A refused removal keeps its inventory.
        boolean complete = removeBlocks(level, removable, recoveredByBlock, controller);
        controller.flushStructureReturns(player);
        for (ReactorControllerBlockEntity member : ReactorUpgradeCards.controllers(controller)) {
            ReactorStructures.refresh(member);
        }
        if (level.getBlockEntity(formation.position(ReactorStructure.definition().controllerOffsets().get(1)))
                instanceof ReactorControllerBlockEntity second) {
            ReactorStructures.refresh(second);
        }
        controller.setActionStatus(complete ? ReactorControllerBlockEntity.STATUS_DISMANTLED
                : ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED, complete ? removable.size() : 0);
    }

    private static boolean removeBlocks(ServerLevel level, List<BlockPos> positions,
            Map<BlockPos, List<ItemStack>> recoveredByBlock, ReactorControllerBlockEntity controller) {
        for (BlockPos pos : positions) {
            ReactorPatternBlockEntity assembly = level.getBlockEntity(pos) instanceof ReactorPatternBlockEntity found
                    ? found : null;
            List<ItemStack> contents = new ArrayList<>();
            if (assembly != null) {
                for (int slot = 0; slot < assembly.getContainerSize(); slot++) contents.add(assembly.getItem(slot).copy());
                assembly.clearContent();
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (!level.getBlockState(pos).isAir()) {
                if (assembly != null) {
                    for (int slot = 0; slot < contents.size(); slot++) assembly.setItem(slot, contents.get(slot));
                }
                return false;
            }
            controller.retainStructureReturns(countByKey(recoveredByBlock.get(pos)));
        }
        return true;
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

    private static boolean placeMissing(ServerLevel level, List<MissingCell> missing) {
        List<BlockPos> placed = new ArrayList<>();
        for (MissingCell cell : missing) {
            level.setBlock(cell.pos(), cell.state(), Block.UPDATE_ALL);
            placed.add(cell.pos());
            if (!level.getBlockState(cell.pos()).is(cell.state().getBlock())) {
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
