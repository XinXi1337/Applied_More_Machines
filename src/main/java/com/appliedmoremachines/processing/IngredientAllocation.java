package com.appliedmoremachines.processing;

import java.util.Arrays;

/** Assign each original recipe unit to a real input slot without overusing any stack. */
public final class IngredientAllocation {
    private IngredientAllocation() {}

    public static int[] find(int[] available, boolean[][] accepts) {
        int[] assigned = new int[accepts.length];
        Arrays.fill(assigned, -1);
        int[] used = new int[available.length];
        for (int unit = 0; unit < accepts.length; unit++) {
            if (!assign(unit, available, used, accepts, assigned, new boolean[available.length])) {
                return null;
            }
        }
        return assigned;
    }

    private static boolean assign(int unit, int[] available, int[] used, boolean[][] accepts,
            int[] assigned, boolean[] visited) {
        for (int slot = 0; slot < available.length; slot++) {
            if (!accepts[unit][slot] || visited[slot]) continue;
            visited[slot] = true;
            if (used[slot] < available[slot]) {
                used[slot]++;
                assigned[unit] = slot;
                return true;
            }
            for (int other = 0; other < assigned.length; other++) {
                if (assigned[other] == slot && assign(other, available, used, accepts, assigned, visited)) {
                    assigned[unit] = slot;
                    return true;
                }
            }
        }
        return false;
    }
}
