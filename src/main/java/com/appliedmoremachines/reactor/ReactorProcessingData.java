package com.appliedmoremachines.reactor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned ledger survives unloading or breaking either controller. */
public final class ReactorProcessingData extends SavedData {
    private final Map<String, ReactorProcessor> factories = new LinkedHashMap<>();
    private final Set<UUID> canceled = new LinkedHashSet<>();

    public static ReactorProcessingData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ReactorProcessingData::new, ReactorProcessingData::load),
                "applied_more_machines_reactor_processing");
    }

    public ReactorProcessor engine(ServerLevel level, ReactorFormation formation) {
        String key = level.dimension().location() + "/" + formation.origin().asLong() + "/" + formation.quarterTurns();
        return factories.computeIfAbsent(key, ignored -> new ReactorProcessor(this));
    }

    public boolean canceled(UUID job) { return canceled.contains(job); }

    public void cancel(UUID job) {
        if (factories.values().stream().anyMatch(factory -> factory.containsJob(job)) && canceled.add(job)) {
            factories.values().forEach(ReactorProcessor::cancellationChanged);
            setDirty();
        }
    }

    public void pruneCancellations() {
        if (canceled.removeIf(job -> factories.values().stream().noneMatch(factory -> factory.containsJob(job)))) setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag engines = new ListTag();
        factories.forEach((key, engine) -> {
            // Empty runtime caches are cheap to recreate and need not grow the save.
            if (!engine.hasWork()) return;
            CompoundTag entry = engine.save();
            entry.putString("Reactor", key);
            engines.add(entry);
        });
        tag.put("Factories", engines);
        ListTag cancellations = new ListTag();
        for (UUID job : canceled) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Job", job);
            cancellations.add(entry);
        }
        tag.put("Canceled", cancellations);
        return tag;
    }

    public static ReactorProcessingData load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new ReactorProcessingData();
        for (Tag value : tag.getList("Factories", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            var engine = new ReactorProcessor(result);
            engine.load(entry);
            result.factories.put(entry.getString("Reactor"), engine);
        }
        for (Tag value : tag.getList("Canceled", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("Job")) result.canceled.add(entry.getUUID("Job"));
        }
        return result;
    }
}
