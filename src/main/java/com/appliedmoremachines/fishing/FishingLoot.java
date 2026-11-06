package com.appliedmoremachines.fishing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.appliedmoremachines.integration.ProductiveBeesFishing;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

/** Derived, filtered copies of the currently loaded fishing tables; original tables stay untouched. */
public final class FishingLoot {
    public static final int NORMAL = 0, FISH = 1, TREASURE = 2, SPECIAL = 3;
    public static final int BLOCK_UNSTACKABLE = 4;
    private record Selection(LootTable table, Set<ResourceLocation> items) {}
    private static final Map<Object, Selection[]> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ResourceLocation BEE_FISHING = ResourceLocation.fromNamespaceAndPath("productivebees", "bee_fishing");
    private FishingLoot() {}
    public static void invalidate() { CACHE.clear(); }
    public static boolean special(ItemStack stack) {
        return !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
    }
    public static ResourceKey<LootTable> source(int selection) {
        return switch (selection & 3) {
            case FISH -> BuiltInLootTables.FISHING_FISH;
            case TREASURE -> BuiltInLootTables.FISHING_TREASURE;
            default -> BuiltInLootTables.FISHING;
        };
    }

    public static List<ItemStack> rollBees(ServerLevel level, int selection) {
        int category = selection & 3;
        // Do not load optional-mod classes when its recipe type is absent.
        if ((category != NORMAL && category != SPECIAL) || !BuiltInRegistries.RECIPE_TYPE.containsKey(BEE_FISHING)) return List.of();
        return ProductiveBeesFishing.roll(level, selection);
    }

    public static List<ItemStack> roll(ServerLevel level, BlockPos pos, int mode) {
        int category = mode & 3;
        var loaded = level.getServer().reloadableRegistries();
        Function<ResourceLocation, LootTable> resolve = id -> loaded.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        LootTable table;
        Set<ResourceLocation> allowed = Set.of();
        if (mode == NORMAL) table = loaded.getLootTable(BuiltInLootTables.FISHING);
        else {
            Selection[] modes = CACHE.computeIfAbsent(loaded, key -> new Selection[8]);
            Selection selection = modes[mode];
            if (selection == null) {
                ResourceLocation source = source(mode).location();
                JsonObject json = LootTable.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), resolve.apply(source)).getOrThrow().getAsJsonObject();
                JsonObject filtered = filter(json, mode, id -> LootTable.DIRECT_CODEC.encodeStart(
                        level.registryAccess().createSerializationContext(JsonOps.INSTANCE), resolve.apply(id)).getOrThrow().getAsJsonObject(), new HashSet<>());
                table = LootTable.DIRECT_CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), filtered).getOrThrow();
                // Global loot modifiers see the same outer fishing ID as a real cast.
                table.setLootTableId(BuiltInLootTables.FISHING.location());
                Set<ResourceLocation> items = new HashSet<>();
                collectItems(filtered, items);
                if (items.contains(BuiltInRegistries.ITEM.getKey(Items.BOOK))) items.add(BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_BOOK));
                selection = new Selection(table, Set.copyOf(items));
                modes[mode] = selection;
            }
            table = selection.table();
            allowed = selection.items();
        }
        FishingHook hook = new FishingHook(EntityType.FISHING_BOBBER, level);
        hook.setPos(Vec3.atCenterOf(pos));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, hook).withLuck(0).create(LootContextParamSets.FISHING);
        // Roll event-based bee catches once per draw, outside the filtered-table
        // retries, so special mode cannot multiply their original probability.
        List<ItemStack> bees = rollBees(level, mode);
        // Mod functions can replace a filtered item and global modifiers can append
        // other categories. Retain only matching final stacks; retries never cost AE.
        for (int attempt = 0; attempt < (mode == NORMAL ? 1 : 8); attempt++) {
            List<ItemStack> result = filterResult(table.getRandomItems(params), mode, allowed);
            // A modifier may depend on an original vanilla catch before adding its
            // own special item. Give it that normal context when no direct candidate exists.
            if (result.isEmpty() && category == SPECIAL) {
                result = filterResult(loaded.getLootTable(BuiltInLootTables.FISHING).getRandomItems(params), mode, allowed);
            }
            if (!result.isEmpty() || !bees.isEmpty()) {
                List<ItemStack> combined = new ArrayList<>(result);
                combined.addAll(bees);
                return combined;
            }
        }
        return List.of();
    }

    public static List<ItemStack> filterResult(List<ItemStack> stacks, int mode, Set<ResourceLocation> allowed) {
        int category = mode & 3;
        return stacks.stream().filter(s -> !s.isEmpty())
                .filter(s -> (mode & BLOCK_UNSTACKABLE) == 0 || s.isStackable())
                .filter(s -> category == NORMAL || category == SPECIAL && special(s)
                        || category != SPECIAL && allowed.contains(BuiltInRegistries.ITEM.getKey(s.getItem()))).toList();
    }

    private static boolean mayChangeStackability(JsonObject entry) {
        if (!entry.has("functions")) return false;
        for (JsonElement value : entry.getAsJsonArray("functions")) {
            String type = value.getAsJsonObject().get("function").getAsString();
            if (!type.startsWith("minecraft:") || type.equals("minecraft:set_item") || type.equals("minecraft:set_components")) return true;
        }
        return false;
    }

    private static boolean candidate(JsonObject entry, int mode, boolean transformed) {
        ResourceLocation id = ResourceLocation.parse(entry.get("name").getAsString());
        if ((mode & 3) == SPECIAL && id.getNamespace().equals("minecraft")) return false;
        if ((mode & BLOCK_UNSTACKABLE) == 0) return true;
        if (transformed || mayChangeStackability(entry)) return true;
        ItemStack item = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
        // Functions that can replace the item or its components need final-stack
        // validation instead of rejecting a potentially stackable replacement.
        if (entry.has("functions")) {
            if (item.is(Items.BOOK)) for (JsonElement value : entry.getAsJsonArray("functions")) {
                JsonObject function = value.getAsJsonObject();
                String type = function.get("function").getAsString();
                if ((type.equals("minecraft:enchant_randomly") || type.equals("minecraft:enchant_with_levels"))
                        && !function.has("conditions")) return false;
            }
        }
        return item.isStackable();
    }

    private static void collectItems(JsonElement value, Set<ResourceLocation> result) {
        if (value.isJsonArray()) { value.getAsJsonArray().forEach(child -> collectItems(child, result)); return; }
        if (!value.isJsonObject()) return;
        JsonObject object = value.getAsJsonObject();
        if (object.has("type") && object.get("type").getAsString().equals("minecraft:item"))
            result.add(ResourceLocation.parse(object.get("name").getAsString()));
        if (object.has("function") && object.get("function").getAsString().equals("minecraft:set_item"))
            result.add(ResourceLocation.parse(object.get("item").getAsString()));
        object.entrySet().forEach(entry -> collectItems(entry.getValue(), result));
    }

    /** Keep weights, conditions, functions, counts and nested data-pack tables. */
    public static JsonObject filter(JsonObject source, int mode, Function<ResourceLocation, JsonObject> resolve, Set<ResourceLocation> visiting) {
        return filter(source, mode, resolve, visiting, false);
    }

    private static JsonObject filter(JsonObject source, int mode, Function<ResourceLocation, JsonObject> resolve, Set<ResourceLocation> visiting, boolean transformed) {
        JsonObject result = source.deepCopy();
        JsonArray pools = new JsonArray();
        for (JsonElement value : source.has("pools") ? source.getAsJsonArray("pools") : new JsonArray()) {
            JsonObject pool = value.getAsJsonObject().deepCopy();
            JsonArray entries = entries(pool.getAsJsonArray("entries"), mode, resolve, visiting,
                    transformed || mayChangeStackability(source) || mayChangeStackability(pool));
            if (!entries.isEmpty()) { pool.add("entries", entries); pools.add(pool); }
        }
        result.add("pools", pools);
        return result;
    }

    private static JsonArray entries(JsonArray source, int mode, Function<ResourceLocation, JsonObject> resolve, Set<ResourceLocation> visiting, boolean transformed) {
        JsonArray result = new JsonArray();
        for (JsonElement value : source) {
            JsonObject entry = value.getAsJsonObject().deepCopy();
            String type = entry.get("type").getAsString();
            if (type.equals("minecraft:empty")) continue;
            if (type.equals("minecraft:item")) {
                if (!candidate(entry, mode, transformed)) continue;
            } else if (type.equals("minecraft:loot_table")) {
                JsonElement target = entry.get("value");
                ResourceLocation id = target.isJsonPrimitive() ? ResourceLocation.parse(target.getAsString()) : null;
                if (id != null && !visiting.add(id)) continue;
                JsonObject nested = filter(id == null ? target.getAsJsonObject() : resolve.apply(id), mode, resolve, visiting,
                        transformed || mayChangeStackability(entry));
                if (id != null) visiting.remove(id);
                if (nested.getAsJsonArray("pools").isEmpty()) continue;
                entry.add("value", nested);
            } else if (type.equals("minecraft:tag")) {
                ResourceLocation id = ResourceLocation.parse(entry.get("name").getAsString());
                JsonArray children = new JsonArray();
                BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id)).ifPresent(items -> items.forEach(item -> {
                    JsonObject child = entry.deepCopy(); child.addProperty("type", "minecraft:item");
                    child.addProperty("name", BuiltInRegistries.ITEM.getKey(item.value()).toString()); child.remove("expand");
                    if (candidate(child, mode, transformed)) children.add(child);
                }));
                if (children.isEmpty()) continue;
                if (entry.has("expand") && entry.get("expand").getAsBoolean()) {
                    // The tag's conditions run once before its weighted members
                    // expand, including when this is a child of alternatives.
                    children.forEach(child -> child.getAsJsonObject().remove("conditions"));
                    JsonObject group = new JsonObject(); group.addProperty("type", "minecraft:group");
                    if (entry.has("conditions")) group.add("conditions", entry.get("conditions"));
                    group.add("children", children); result.add(group); continue;
                }
                // A non-expanded tag generates every member on one selected entry.
                JsonObject nested = new JsonObject();
                JsonArray pools = new JsonArray();
                for (JsonElement childValue : children) {
                    JsonObject child = childValue.getAsJsonObject();
                    child.remove("functions"); child.remove("conditions"); child.remove("weight"); child.remove("quality");
                    JsonObject pool = new JsonObject(); pool.addProperty("rolls", 1);
                    JsonArray members = new JsonArray(); members.add(child); pool.add("entries", members); pools.add(pool);
                }
                nested.add("pools", pools);
                entry.addProperty("type", "minecraft:loot_table"); entry.remove("name"); entry.remove("expand");
                entry.add("value", nested);
            } else if (entry.has("children")) {
                JsonArray children = entries(entry.getAsJsonArray("children"), mode, resolve, visiting, transformed);
                if (children.isEmpty()) continue;
                entry.add("children", children);
            }
            result.add(entry);
        }
        return result;
    }
}
