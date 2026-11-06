package com.appliedmoremachines.factory;

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
public final class FactoryProcessingData extends SavedData {
    private final Map<String, FactoryProcessor> factories = new LinkedHashMap<>();
    private final Set<UUID> canceled = new LinkedHashSet<>();

    public static FactoryProcessingData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FactoryProcessingData::new, FactoryProcessingData::load),
                "applied_more_machines_factory_processing");
    }

    public FactoryProcessor engine(ServerLevel level, FactoryFormation formation) {
        String key = level.dimension().location() + "/" + formation.origin().asLong() + "/" + formation.quarterTurns();
        return factories.computeIfAbsent(key, ignored -> new FactoryProcessor(this));
    }

    public boolean canceled(UUID job) { return canceled.contains(job); }

    public void cancel(UUID job) {
        if (factories.values().stream().anyMatch(factory -> factory.containsJob(job)) && canceled.add(job)) {
            factories.values().forEach(FactoryProcessor::cancellationChanged);
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
            entry.putString("Factory", key);
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

    public static FactoryProcessingData load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new FactoryProcessingData();
        for (Tag value : tag.getList("Factories", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            var engine = new FactoryProcessor(result);
            engine.load(entry);
            result.factories.put(entry.getString("Factory"), engine);
        }
        for (Tag value : tag.getList("Canceled", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("Job")) result.canceled.add(entry.getUUID("Job"));
        }
        return result;
    }
}
