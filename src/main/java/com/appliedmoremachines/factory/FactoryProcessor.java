package com.appliedmoremachines.factory;

import com.appliedmoremachines.production.*;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.appliedmoremachines.FactoryControllerBlockEntity;
import com.appliedmoremachines.FactoryPatternBlockEntity;
import com.appliedmoremachines.FactoryUpgradeCards;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Bounded, durable CPU input ownership and one shared round per multiblock. */
public final class FactoryProcessor {
    public static final int IDLE = 0, WORKING = 1, OFFLINE = 2, NO_POWER = 3,
            CHEMICAL_MISSING = 4, RECIPE_CHANGED = 5, OUTPUT_FULL = 6, UNFORMED = 7;
    private final FactoryProcessingData data;
    /** A run of identical native operations, also stored as one counted Work entry. */
    private static final class Work {
        final CompoundTag tag;
        final UUID job;
        final String recipe;
        int copies;
        Work(CompoundTag tag, int copies) {
            this.tag = tag;
            this.job = tag.getUUID("Job");
            this.recipe = tag.getString("Recipe");
            this.copies = copies;
        }
    }
    private final List<Work> work = new ArrayList<>();
    private final Map<String, Integer> recipeCounts = new LinkedHashMap<>();
    private final Map<UUID, Integer> jobCounts = new LinkedHashMap<>();
    private int queued, active;
    private Map<AEItemKey, IPatternDetails> available = new LinkedHashMap<>();
    private Set<ResourceLocation> machines = Set.of();
    private List<AEItemKey> installed = List.of();
    private FactoryControllerBlockEntity advertisedHost;
    private final Set<FactoryControllerBlockEntity> pendingProviderUpdates = new LinkedHashSet<>();
    private long refreshed = Long.MIN_VALUE, generation = -1, lastTick = Long.MIN_VALUE;
    private long lastAccepted = Long.MIN_VALUE;
    private Map<AEItemKey, FactoryRecipes.Match> matches = new LinkedHashMap<>();
    private int bulkRecipeKinds;
    private int elapsed, duration, roundParallel, roundKinds, state, illegal, chemicalPaused;
    private double credit;
    private boolean validationRequired = true;
    private List<GenericStack> displayCache;

    public FactoryProcessor(FactoryProcessingData data) { this.data = data; }
    public boolean hasWork() { return !work.isEmpty(); }
    public boolean containsJob(UUID job) { return jobCounts.containsKey(job); }
    public void cancellationChanged() { displayCache = null; }
    public int state() { return state; }
    public int illegal() { return illegal; }
    public int chemicalPaused() { return chemicalPaused; }
    public int queued() { return queued; }
    public int activeCount() { return active; }
    public int progress() { return duration > 0 ? Math.clamp(elapsed * 1000 / duration, 0, 1000) : 0; }

    public List<IPatternDetails> patterns(FactoryControllerBlockEntity owner) {
        refresh(owner);
        return host(owner) == owner && owner.isFormed() ? List.copyOf(available.values()) : List.of();
    }

    public boolean busy(FactoryControllerBlockEntity owner) {
        return !owner.isFormed() || host(owner) != owner || queued >= capacity(owner);
    }

    public boolean push(FactoryControllerBlockEntity owner, IPatternDetails pattern, KeyCounter[] delivered) {
        var link = FactoryJobContext.current();
        if (link == null || link.isCanceled()) return false;
        try {
            return pushBatch(owner, pattern, FactoryAmounts.of(delivered), 1, link.getCraftingID());
        } catch (RuntimeException ex) { return false; }
    }

    /** Logical CPU copies may wrap a legal encoded pattern, but never change its native ratio. */
    private record BatchRecipe(IPatternDetails pattern, FactoryRecipes.Match match, long units) {}

    private BatchRecipe batchRecipe(FactoryControllerBlockEntity owner, IPatternDetails details) {
        refresh(owner);
        if (details == null || busy(owner)) return null;
        IPatternDetails canonical = available.get(details.getDefinition());
        if (canonical == null) return null;
        var match = cachedMatch(owner, canonical, null);
        if (match == null) return null;
        var outputs = FactoryAmounts.of(details.getOutputs());
        if (!outputs.keySet().equals(match.outputs().keySet())) return null;
        long units = 0;
        for (var entry : match.outputs().entrySet()) {
            long amount = outputs.get(entry.getKey()), base = entry.getValue();
            if (amount % base != 0 || amount / base < 1) return null;
            if (units != 0 && units != amount / base) return null;
            units = amount / base;
        }
        return units > 0 ? new BatchRecipe(canonical, match, units) : null;
    }

    public long batchCapacity(FactoryControllerBlockEntity owner, IPatternDetails pattern) {
        try {
            var recipe = batchRecipe(owner, pattern);
            return recipe == null ? 0 : recipeCapacity(owner, recipe) / recipe.units();
        } catch (RuntimeException ex) { return 0; }
    }

    private int recipeCapacity(FactoryControllerBlockEntity owner, BatchRecipe recipe) {
        int recipeQueued = recipeCounts.getOrDefault(recipe.match().recipe(), 0);
        // Buffer two maximum accelerated rounds of this kind, bounded by total capacity.
        return Math.max(0, Math.min(capacity(owner) - queued,
                maximumRecipeQueue(owner, recipe.match().parallelMultiplier()) - recipeQueued));
    }

    public int freeCapacity(FactoryControllerBlockEntity owner) {
        // The legacy ECO query has no pattern argument. Pattern-aware APIs advertise bulk capacity.
        return busy(owner) ? 0 : Math.min(maximumRecipeQueue(owner, 1), capacity(owner) - queued);
    }

    /** Total inputs are already extracted by the CPU. Accept all copies atomically or none. */
    public boolean pushBatch(FactoryControllerBlockEntity owner, IPatternDetails pattern,
            Map<AEKey, Long> inputs, long copies, UUID job) {
        if (job == null || data.canceled(job) || copies < 1) return false;
        BatchRecipe recipe;
        long count;
        try {
            recipe = batchRecipe(owner, pattern);
            if (recipe == null) return false;
            count = Math.multiplyExact(copies, recipe.units());
            if (count > recipeCapacity(owner, recipe) || !validStructure(owner)
                    || !inputs.equals(FactoryAmounts.multiply(recipe.match().inputs(), count))) return false;
        } catch (RuntimeException ex) { return false; }
        var registries = owner.getLevel().registryAccess();
        CompoundTag task = new CompoundTag();
        task.putUUID("Job", job);
        task.put("Pattern", recipe.pattern().getDefinition().toTagGeneric(registries));
        task.put("Inputs", FactoryAmounts.save(recipe.match().inputs(), registries));
        task.put("Outputs", FactoryAmounts.save(recipe.match().outputs(), registries));
        task.put("Remainders", FactoryAmounts.save(recipe.match().remainders(), registries));
        task.put("Display", GenericStack.writeTag(registries, recipe.pattern().getPrimaryOutput()));
        task.putString("Recipe", recipe.match().recipe());
        append(work, task, (int) count);
        queued += (int) count;
        recipeCounts.merge(recipe.match().recipe(), (int) count, Integer::sum);
        jobCounts.merge(job, (int) count, Integer::sum);
        displayCache = null;
        lastAccepted = owner.getLevel().getGameTime();
        data.setDirty();
        // Ownership transfers only on true. The AE CPU already removed these inputs.
        return true;
    }

    public void tick(FactoryControllerBlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        FactoryControllerBlockEntity host = host(owner);
        if (host != null && host != owner) return;
        if (lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        refresh(owner);
        publishProviderUpdates();
        IGrid grid = host == null ? null : host.getMainNode().getGrid();
        IActionSource source = IActionSource.ofMachine(host == null ? owner : host);
        var registries = level.registryAccess();
        boolean canceledActive = work.stream().anyMatch(task -> task.tag.getBoolean("Active") && canceled(task));
        if (canceledActive) {
            boolean survivingActive = work.stream().anyMatch(task -> task.tag.getBoolean("Active") && !canceled(task));
            resetProgress();
            clearActive();
            if (!survivingActive) credit = 0;
            validationRequired = true;
            displayCache = null;
            data.setDirty();
        }
        if (grid != null) flush(grid, source, registries);
        if (!hasWork()) {
            state = IDLE;
            elapsed = 0; credit = 0;
            return;
        }
        if (!owner.isFormed() || !validStructure(owner)) { pause(UNFORMED); return; }
        if (grid == null) { pause(OFFLINE); return; }
        if (work.stream().anyMatch(task -> task.tag.getBoolean("Complete"))) { pause(OUTPUT_FULL); return; }

        int nextDuration = FactoryUpgradeCards.durationTicks(owner);
        int parallel = FactoryUpgradeCards.parallel(owner);
        int kinds = kindSlots(owner);
        if (duration != nextDuration || roundParallel != parallel || roundKinds != kinds) {
            resetProgress();
            clearActive();
            duration = nextDuration; roundParallel = parallel; roundKinds = kinds;
            validationRequired = true;
            displayCache = null;
            data.setDirty();
        }

        // Recheck retained inputs and recipes before starting/resuming a round or after reloads.
        if (activeCount() == 0 || validationRequired) {
            List<String> candidates = new ArrayList<>();
            List<Integer> copies = new ArrayList<>();
            Map<String, Integer> multipliers = new LinkedHashMap<>();
            Map<ValidationKey, Validated> validated = new LinkedHashMap<>();
            boolean missing = false, invalid = false, invalidActive = false;
            boolean haveActive = activeCount() > 0;
            for (Work batch : work) {
                CompoundTag task = batch.tag;
                copies.add(batch.copies);
                if (canceled(batch) || task.getBoolean("Complete")) {
                    candidates.add(null); continue;
                }
                var key = new ValidationKey(task.getCompound("Pattern"), task.getList("Inputs", Tag.TAG_COMPOUND),
                        task.getList("Outputs", Tag.TAG_COMPOUND));
                var result = validated.computeIfAbsent(key, ignored -> validate(owner, task, registries));
                boolean rejected = result.recipe() == null;
                if (!rejected) task.put("Remainders", FactoryAmounts.save(result.remainders(), registries));
                invalidActive |= task.getBoolean("Active") && rejected;
                missing |= result.missing();
                invalid |= rejected && !result.missing();
                candidates.add(result.recipe());
                if (!rejected) multipliers.put(result.recipe(), result.multiplier());
            }
            if (haveActive && invalidActive) {
                resetProgress();
                clearActive();
                haveActive = false;
                displayCache = null;
                data.setDirty();
            }
            if (!haveActive) {
                var selected = FactoryRoundPlanner.select(candidates, copies, parallel, kinds, multipliers);
                if (selected.isEmpty()) { pause(missing ? CHEMICAL_MISSING : invalid ? RECIPE_CHANGED : OUTPUT_FULL); return; }
                // Native AE2's rolling operation budget can separate deliveries by four
                // ticks. Wait past that window before deciding a partial group is a tail.
                Map<String, Integer> counts = new LinkedHashMap<>();
                selected.forEach((index, count) -> counts.merge(candidates.get(index), count, Integer::sum));
                int allowance = FactoryRoundPlanner.recipeParallel(parallel, kinds, counts.size());
                if (lastAccepted != Long.MIN_VALUE && level.getGameTime() - lastAccepted < 5
                        && counts.entrySet().stream().noneMatch(entry -> entry.getValue()
                                >= allowance * multipliers.getOrDefault(entry.getKey(), 1))) {
                    state = IDLE; return;
                }
                List<Work> next = new ArrayList<>();
                for (int index = 0; index < work.size(); index++) {
                    Work batch = work.get(index);
                    int count = selected.getOrDefault(index, 0);
                    if (count > 0) {
                        CompoundTag task = batch.tag.copy();
                        task.putBoolean("Active", true);
                        append(next, task, count);
                        active += count;
                    }
                    if (count < batch.copies) append(next, batch.tag, batch.copies - count);
                }
                work.clear(); work.addAll(next);
                displayCache = null;
                data.setDirty();
            }
            validationRequired = false;
        }
        double paid = FactoryEnergy.pay(FactoryEnergy.source(grid, source), credit);
        if (paid != credit) { credit = paid; data.setDirty(); }
        if (credit + 0.000001 < FactoryEnergy.ROUND_COST) { pause(NO_POWER); return; }
        state = WORKING;
        elapsed++;
        data.setDirty();
        if (elapsed < duration) return;
        for (Work batch : work) {
            CompoundTag task = batch.tag;
            if (!task.getBoolean("Active")) continue;
            task.putBoolean("Active", false);
            if (MultiplicationUpgrade.active(owner)) {
                var outputs = FactoryAmounts.load(task.getList("Outputs", Tag.TAG_COMPOUND), registries);
                if (outputs == null) throw new IllegalStateException("Validated output became unavailable");
                java.util.Map<appeng.api.stacks.AEKey, Long> boosted = new java.util.LinkedHashMap<>();
                var remainders = FactoryAmounts.load(task.getList("Remainders", Tag.TAG_COMPOUND), registries);
                outputs.forEach((key, amount) -> {
                    long returned = remainders == null ? 0 : Math.clamp(remainders.getOrDefault(key, 0L), 0, amount);
                    boosted.put(key, Math.addExact(returned, MultiplicationUpgrade.boost(amount - returned)));
                });
                task.put("Outputs", FactoryAmounts.save(boosted, registries));
            }
            task.putBoolean("Complete", true);
            task.put("Inputs", new ListTag());
        }
        active = 0;
        elapsed = 0; credit = 0;
        displayCache = null;
        flush(grid, source, registries);
        if (!hasWork()) state = IDLE;
    }

    private record Transfer(Work work, String field, Map<AEKey, Long> amounts) {}
    private record Allocation(AEKey key, long amount, int full, long partial) {}

    private void flush(IGrid grid, IActionSource source, HolderLookup.Provider registries) {
        List<Transfer> transfers = new ArrayList<>();
        Map<AEKey, Long> totals = new LinkedHashMap<>();
        for (Work batch : work) {
            boolean complete = batch.tag.getBoolean("Complete");
            if (!complete && !canceled(batch)) continue;
            String field = complete ? "Outputs" : "Inputs";
            var amounts = FactoryAmounts.load(batch.tag.getList(field, Tag.TAG_COMPOUND), registries);
            if (amounts == null) continue;
            transfers.add(new Transfer(batch, field, amounts));
            amounts.forEach((key, amount) -> totals.merge(key, saturatedProduct(amount, batch.copies),
                    FactoryProcessor::saturatedSum));
        }
        // AE notifies crafting CPUs on each storage insertion. Thousands of 1mB outputs
        // must become one insertion per key, then only the accepted amount leaves our ledger.
        var inventory = grid.getStorageService().getInventory();
        totals.replaceAll((key, amount) -> Math.clamp(inventory.insert(key, amount, Actionable.MODULATE, source), 0, amount));
        Map<Work, List<Work>> replacements = new IdentityHashMap<>();
        for (Transfer transfer : transfers) {
            if (transfer.amounts().isEmpty() || transfer.amounts().keySet().stream().anyMatch(key -> totals.get(key) > 0)) {
                replacements.put(transfer.work(), retain(transfer, totals, registries));
            }
        }
        if (!replacements.isEmpty()) {
            // Compact once instead of shifting an ArrayList for every completed operation.
            // Storage callbacks may have appended work; preserve those new deliveries too.
            List<Work> next = new ArrayList<>();
            for (Work batch : work) {
                for (Work retained : replacements.getOrDefault(batch, List.of(batch))) {
                    append(next, retained.tag, retained.copies);
                }
            }
            work.clear(); work.addAll(next);
            reindex(); displayCache = null; data.setDirty();
        }
        data.pruneCancellations();
    }

    /** Split only at a partial insertion boundary, preserving each operation's remaining keys. */
    private static List<Work> retain(Transfer transfer, Map<AEKey, Long> accepted, HolderLookup.Provider registries) {
        Work batch = transfer.work();
        List<Allocation> allocations = new ArrayList<>();
        Set<Integer> boundaries = new TreeSet<>();
        boundaries.add(0); boundaries.add(batch.copies);
        transfer.amounts().forEach((key, amount) -> {
            long take = Math.min(accepted.get(key), saturatedProduct(amount, batch.copies));
            accepted.put(key, accepted.get(key) - take);
            int full = (int) (take / amount);
            long partial = take % amount;
            allocations.add(new Allocation(key, amount, full, partial));
            boundaries.add(full);
            if (partial > 0) boundaries.add(full + 1);
        });
        List<Work> result = new ArrayList<>();
        int start = 0;
        for (int end : boundaries) {
            if (end == start) continue;
            Map<AEKey, Long> remaining = new LinkedHashMap<>();
            for (Allocation allocation : allocations) {
                long amount = start < allocation.full() ? 0 : allocation.amount()
                        - (start == allocation.full() ? allocation.partial() : 0);
                if (amount > 0) remaining.put(allocation.key(), amount);
            }
            if (!remaining.isEmpty()) {
                CompoundTag task = batch.tag.copy();
                task.put(transfer.field(), FactoryAmounts.save(remaining, registries));
                append(result, task, end - start);
            }
            start = end;
        }
        return result;
    }

    private static long saturatedProduct(long amount, int copies) {
        return amount > Long.MAX_VALUE / copies ? Long.MAX_VALUE : amount * copies;
    }
    private static long saturatedSum(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static void append(List<Work> target, CompoundTag tag, int copies) {
        if (copies <= 0) return;
        if (!target.isEmpty() && target.getLast().tag.equals(tag)) target.getLast().copies += copies;
        else target.add(new Work(tag, copies));
    }

    private void reindex() {
        queued = 0; active = 0;
        recipeCounts.clear(); jobCounts.clear();
        for (Work batch : work) {
            queued += batch.copies;
            if (batch.tag.getBoolean("Active")) active += batch.copies;
            recipeCounts.merge(batch.recipe, batch.copies, Integer::sum);
            jobCounts.merge(batch.job, batch.copies, Integer::sum);
        }
    }

    private void clearActive() {
        work.forEach(task -> task.tag.putBoolean("Active", false));
        active = 0;
    }

    private record ValidationKey(CompoundTag pattern, ListTag inputs, ListTag outputs) {}
    private record Validated(String recipe, boolean missing, int multiplier, Map<AEKey, Long> remainders) {}
    private Validated validate(FactoryControllerBlockEntity owner, CompoundTag task, HolderLookup.Provider registries) {
        var inputs = FactoryAmounts.load(task.getList("Inputs", Tag.TAG_COMPOUND), registries);
        if (inputs == null || chemicalUnavailable(task)) return new Validated(null, true, 1, Map.of());
        var key = AEKey.fromTagGeneric(registries, task.getCompound("Pattern"));
        IPatternDetails details = key instanceof AEItemKey item ? decode(item, owner) : null;
        var match = details == null ? null : cachedMatch(owner, details, inputs);
        return new Validated(match == null ? null : match.recipe(), false, match == null ? 1 : match.parallelMultiplier(), match == null ? Map.of() : match.remainders());
    }

    private boolean canceled(Work task) { return data.canceled(task.job); }
    private void pause(int reason) {
        if (state != reason || elapsed != 0) validationRequired = true;
        state = reason;
        resetProgress();
    }
    private void resetProgress() { if (elapsed != 0) { elapsed = 0; data.setDirty(); } }
    private int capacity(FactoryControllerBlockEntity owner) {
        return 2 * FactoryRoundPlanner.maximumRoundSize(FactoryUpgradeCards.parallel(owner), kindSlots(owner), bulkRecipeKinds);
    }

    private int maximumRecipeQueue(FactoryControllerBlockEntity owner, int multiplier) {
        return 2 * FactoryRoundPlanner.recipeParallel(FactoryUpgradeCards.parallel(owner), kindSlots(owner), 1) * multiplier;
    }

    private int kindSlots(FactoryControllerBlockEntity owner) {
        return 1 + FactoryUpgradeCards.count(owner, FactoryUpgradeCards.Kind.MEKANISM_STACK);
    }

    private static IPatternDetails decode(AEItemKey key, FactoryControllerBlockEntity owner) {
        try { return PatternDetailsHelper.decodePattern(key, owner.getLevel()); }
        catch (RuntimeException ex) { return null; }
    }

    private boolean chemicalUnavailable(CompoundTag task) {
        return !FactoryRecipes.chemicalsAvailable() && task.toString().contains("appmek:");
    }

    public void refresh(FactoryControllerBlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        FactoryControllerBlockEntity nextHost = host(owner);
        boolean formed = owner.isFormed();
        if (refreshed != Long.MIN_VALUE && now - refreshed < 20 && generation == FactoryRecipes.generation()
                && advertisedHost == nextHost && (formed || available.isEmpty())) return;
        refreshed = now;
        long nextGeneration = FactoryRecipes.generation();
        List<AEItemKey> nextInstalled = new ArrayList<>();
        Set<ResourceLocation> nextMachines = new LinkedHashSet<>();
        if (formed) {
            for (var cell : FactoryStructure.definition().cells()) {
                var pos = owner.formation().position(cell.offset());
                if (!level.hasChunkAt(pos)) continue;
                nextMachines.add(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
                if (level.getBlockEntity(pos) instanceof FactoryPatternBlockEntity assembly) {
                    for (int slot = 0; slot < assembly.getContainerSize(); slot++) {
                        if (!assembly.getItem(slot).isEmpty()) nextInstalled.add(AEItemKey.of(assembly.getItem(slot)));
                    }
                }
            }
        }
        if (!installed.equals(nextInstalled) || !machines.equals(nextMachines) || generation != nextGeneration) {
            installed = List.copyOf(nextInstalled); machines = Set.copyOf(nextMachines);
            matches.clear();
            validationRequired = true;
            generation = nextGeneration;
            Map<AEItemKey, IPatternDetails> nextAvailable = new LinkedHashMap<>();
            illegal = 0; chemicalPaused = 0;
            for (AEItemKey key : installed) {
                IPatternDetails details = decode(key, owner);
                if (!FactoryRecipes.chemicalsAvailable()
                        && key.toTagGeneric(level.registryAccess()).toString().contains("appmek:")) {
                    chemicalPaused++; continue;
                }
                if (details != null && cachedMatch(owner, details, null) != null) {
                    nextAvailable.put(key, details);
                } else illegal++;
            }
            available = nextAvailable;
            bulkRecipeKinds = (int) nextAvailable.keySet().stream().map(matches::get)
                    .filter(match -> match.parallelMultiplier() == FactoryRecipes.BULK_MULTIPLIER)
                    .map(FactoryRecipes.Match::recipe).distinct().count();
            queueProviderUpdates(owner);
        }
        if (advertisedHost != nextHost) {
            if (advertisedHost != null) pendingProviderUpdates.add(advertisedHost);
            advertisedHost = nextHost;
            queueProviderUpdates(owner);
        }
    }

    private void queueProviderUpdates(FactoryControllerBlockEntity owner) {
        pendingProviderUpdates.addAll(FactoryUpgradeCards.controllers(owner));
    }

    private void publishProviderUpdates() {
        // AE refreshes providers synchronously and calls patterns() again. Publish only
        // from our server tick, after refresh committed its host and recipe snapshot.
        var updates = List.copyOf(pendingProviderUpdates);
        pendingProviderUpdates.clear();
        // Changes discovered by a callback stay queued until the next tick.
        for (var controller : updates) ICraftingProvider.requestUpdate(controller.getMainNode());
    }

    public static boolean validStructure(FactoryControllerBlockEntity owner) {
        return owner.formation() != null && FactoryStructure.definition().matches(
                FactoryStructures.view(owner.getLevel()), owner.formation());
    }

    /** Prefer the same deterministic controller even when its sibling is on a separate grid. */
    public static FactoryControllerBlockEntity host(FactoryControllerBlockEntity owner) {
        if (owner.getLevel() == null) return null;
        FactoryFormation identity = owner.processingFormation();
        if (identity == null) return null;
        for (var offset : FactoryStructure.definition().controllerOffsets()) {
            var pos = identity.position(offset);
            if (!owner.getLevel().hasChunkAt(pos)) continue;
            if (owner.getLevel().getBlockEntity(pos) instanceof FactoryControllerBlockEntity controller
                    && controller.getMainNode().isActive() && controller.getMainNode().isOnline()) {
                IGrid grid = controller.getMainNode().getGrid();
                if (grid != null) for (var node : grid.getNodes()) {
                    if (!(node.getOwner() instanceof FactoryControllerBlockEntity)) return controller;
                }
            }
        }
        return null;
    }

    public List<GenericStack> display(HolderLookup.Provider registries) {
        if (displayCache != null) return displayCache;
        Map<AEKey, Long> outputs = new LinkedHashMap<>();
        boolean active = activeCount() > 0;
        for (Work batch : work) {
            CompoundTag task = batch.tag;
            if (canceled(batch) || active && !task.getBoolean("Active")) continue;
            GenericStack stack;
            try { stack = GenericStack.readTag(registries, task.getCompound("Display")); }
            catch (RuntimeException ex) { continue; }
            if (stack == null) continue;
            if (outputs.size() >= 9 && !outputs.containsKey(stack.what())) continue;
            outputs.merge(stack.what(), Math.multiplyExact(stack.amount(), batch.copies), Math::addExact);
        }
        displayCache = outputs.entrySet().stream().map(entry -> new GenericStack(entry.getKey(), entry.getValue())).toList();
        return displayCache;
    }

    private FactoryRecipes.Match cachedMatch(FactoryControllerBlockEntity owner, IPatternDetails details,
            Map<AEKey, Long> inputs) {
        if (!(details instanceof appeng.crafting.pattern.AEProcessingPattern)) return null;
        AEItemKey definition = details.getDefinition();
        if (!matches.containsKey(definition)) matches.put(definition,
                FactoryRecipes.match(owner.getLevel(), machines, details, null));
        var result = matches.get(definition);
        return result != null && (inputs == null || result.inputs().equals(inputs)) ? result : null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag tasks = new ListTag();
        for (Work batch : work) {
            CompoundTag task = batch.tag.copy();
            if (batch.copies > 1) task.putInt("Copies", batch.copies);
            tasks.add(task);
        }
        tag.put("Work", tasks);
        tag.putDouble("PaidAE", credit);
        tag.putInt("Duration", duration);
        tag.putInt("Parallel", roundParallel);
        tag.putInt("Kinds", roundKinds);
        // Progress is deliberately restarted after load. Payment and material ownership persist.
        return tag;
    }

    public void load(CompoundTag tag) {
        work.clear();
        displayCache = null;
        validationRequired = true;
        for (Tag value : tag.getList("Work", Tag.TAG_COMPOUND)) {
            CompoundTag task = ((CompoundTag) value).copy();
            int copies = task.contains("Copies", Tag.TAG_INT) ? task.getInt("Copies") : 1;
            task.remove("Copies");
            task.putBoolean("Active", false);
            append(work, task, copies);
        }
        reindex();
        double saved = tag.getDouble("PaidAE");
        credit = Double.isFinite(saved) ? Math.clamp(saved, 0, FactoryEnergy.ROUND_COST) : 0;
        duration = Math.max(1, tag.getInt("Duration"));
        roundParallel = Math.max(1, tag.getInt("Parallel"));
        roundKinds = Math.max(1, tag.getInt("Kinds"));
        elapsed = 0;
    }
}
