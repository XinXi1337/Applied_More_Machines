package com.appliedmoremachines.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Idle stack-upgrade slots increase each participating native recipe's batch size. */
public final class FactoryRoundPlanner {
    private FactoryRoundPlanner() {}

    public static int recipeParallel(int parallel, int kinds, int selectedKinds) {
        return Math.max(1, parallel) * Math.max(1, kinds - Math.max(1, selectedKinds));
    }

    /** Maximum total round size over all possible participating recipe-kind counts. */
    public static int maximumRoundSize(int parallel, int kinds) {
        return maximumRoundSize(parallel, kinds, 0);
    }

    /** Count bulk kinds once, even if several installed patterns match the same native recipe. */
    public static int maximumRoundSize(int parallel, int kinds, int bulkKinds) {
        int maximum = 0;
        for (int count = 1; count <= Math.max(1, kinds); count++) {
            int weight = count + Math.min(count, bulkKinds) * (FactoryRecipes.BULK_MULTIPLIER - 1);
            maximum = Math.max(maximum, weight * recipeParallel(parallel, kinds, count));
        }
        return maximum;
    }

    public static List<Integer> select(List<String> recipes, int parallel, int kinds) {
        return new ArrayList<>(select(recipes, Collections.nCopies(recipes.size(), 1), parallel, kinds).keySet());
    }

    /** Same FIFO selection over counted runs, without expanding one index per native operation. */
    public static Map<Integer, Integer> select(List<String> recipes, List<Integer> copies, int parallel, int kinds) {
        return select(recipes, copies, parallel, kinds, Map.of());
    }

    public static Map<Integer, Integer> select(List<String> recipes, List<Integer> copies, int parallel, int kinds,
            Map<String, Integer> multipliers) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String recipe : recipes) {
            if (recipe != null && counts.size() < Math.max(1, kinds)) counts.putIfAbsent(recipe, 0);
        }
        int allowance = recipeParallel(parallel, kinds, counts.size());
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < recipes.size(); index++) {
            String recipe = recipes.get(index);
            if (recipe == null || !counts.containsKey(recipe)) continue;
            int take = Math.min(copies.get(index), allowance * multipliers.getOrDefault(recipe, 1) - counts.get(recipe));
            if (take <= 0) continue;
            counts.put(recipe, counts.get(recipe) + take);
            result.put(index, take);
        }
        return result;
    }
}
