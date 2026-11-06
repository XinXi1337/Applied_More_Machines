package com.appliedmoremachines.reactor;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import appeng.crafting.pattern.AEProcessingPattern;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.factory.FactoryAmounts;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

/** One shared native catalog for pattern admission, execution and JEI. No power production. */
public final class ReactorRecipes {
    private static final String API = "it.zerono.mods.extremereactors.api.";
    private static final String RECIPES = "it.zerono.mods.extremereactors.gamecontent.multiblock.";
    private static final Set<String> FAMILIES = Set.of(
            RECIPES + "reprocessor.recipe.ReprocessorRecipe",
            RECIPES + "fluidizer.recipe.FluidizerSolidRecipe",
            RECIPES + "fluidizer.recipe.FluidizerSolidMixingRecipe",
            RECIPES + "fluidizer.recipe.FluidizerFluidMixingRecipe");
    private static long generation;
    private static final Map<Level, Snapshot> CACHE = new WeakHashMap<>();
    private record Snapshot(long generation, Object manager, List<Entry> entries) {}
    private ReactorRecipes() {}
    public static void invalidate() { generation++; CACHE.clear(); }
    public static long generation() { return generation; }
    public record Ingredient(List<GenericStack> alternatives, Predicate<AEKey> test, ToLongFunction<AEKey> amount) {
        public Ingredient { alternatives = List.copyOf(alternatives); }
    }
    public record Entry(ResourceLocation id, List<Ingredient> inputs, List<GenericStack> outputs) {
        public Entry { inputs = List.copyOf(inputs); outputs = List.copyOf(outputs); }
        public boolean matches(Map<AEKey, Long> actual) { return allocate(inputs, 0, new LinkedHashMap<>(actual)); }
    }
    public record Match(String recipe, Map<AEKey, Long> inputs, Map<AEKey, Long> outputs) {
        public int parallelMultiplier() { return 1; }
    }

    public static Match match(Level level, Set<ResourceLocation> ignored, IPatternDetails details, Map<AEKey,Long> delivered) {
        if (!(details instanceof AEProcessingPattern pattern)) return null;
        try {
            var inputs = FactoryAmounts.of(pattern.getSparseInputs());
            var outputs = FactoryAmounts.of(pattern.getOutputs());
            if (inputs.isEmpty() || outputs.isEmpty() || delivered != null && !inputs.equals(delivered)) return null;
            for (Entry entry : entries(level)) {
                if (FactoryAmounts.of(entry.outputs()).equals(outputs) && entry.matches(inputs))
                    return new Match(entry.id().toString(), inputs, outputs);
            }
        } catch (ArithmeticException | IllegalArgumentException ex) { return null; }
        return null;
    }

    /** Allocate each native slot once; overlapping tags never reuse the same delivered amount. */
    private static boolean allocate(List<Ingredient> inputs, int index, Map<AEKey,Long> remaining) {
        if (index == inputs.size()) return remaining.values().stream().allMatch(n -> n == 0);
        Ingredient ingredient = inputs.get(index);
        for (AEKey key : List.copyOf(remaining.keySet())) {
            if (!ingredient.test().test(key)) continue;
            long required = ingredient.amount().applyAsLong(key), available = remaining.get(key);
            if (required <= 0 || required > available) continue;
            remaining.put(key, available - required);
            boolean match = allocate(inputs, index + 1, remaining);
            remaining.put(key, available);
            if (match) return true;
        }
        return false;
    }

    public static List<Entry> entries(Level level) {
        if (level == null || !ModList.get().isLoaded("bigreactors")) return List.of();
        Object manager = level.getRecipeManager();
        Snapshot cached = CACHE.get(level);
        if (cached != null && cached.generation() == generation && cached.manager() == manager) return cached.entries();
        List<Entry> result = new ArrayList<>();
        for (var holder : level.getRecipeManager().getRecipes()) {
            Object recipe = holder.value();
            boolean supported = false;
            for (Class<?> type = recipe.getClass(); type != null; type = type.getSuperclass())
                if (FAMILIES.contains(type.getName())) supported = true;
            if (!supported) continue;
            try {
                List<Ingredient> ingredients = new ArrayList<>();
                if (has(recipe, "getIngredient")) ingredients.add(nativeIngredient(call(recipe, "getIngredient")));
                else {
                    ingredients.add(nativeIngredient(call(recipe, "getIngredient1")));
                    ingredients.add(nativeIngredient(call(recipe, "getIngredient2")));
                }
                Object nativeResult = call(recipe, "getResult");
                Object stack = call(nativeResult, "getResult");
                GenericStack output = generic(stack, ((Number)call(nativeResult, "getAmount")).longValue());
                if (output != null && ingredients.stream().noneMatch(i -> i.alternatives().isEmpty()))
                    result.add(new Entry(holder.id(), ingredients, List.of(output)));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                AppliedMoreMachines.LOGGER.warn("Skipping unsupported native AE reactor recipe {}", holder.id(), ex);
            }
        }
        try { addReactions(result); }
        catch (ReflectiveOperationException | RuntimeException ex) {
            AppliedMoreMachines.LOGGER.warn("Cannot read Extreme Reactors fuel mappings", ex);
        }
        List<Entry> snapshot = List.copyOf(result);
        CACHE.put(level, new Snapshot(generation, manager, snapshot));
        return snapshot;
    }

    private static Ingredient nativeIngredient(Object nativeInput) throws ReflectiveOperationException {
        List<GenericStack> variants = new ArrayList<>();
        for (Object value : (List<?>)call(nativeInput, "getMatchingElements")) {
            var variant = generic(value, ((Number)call(nativeInput, "getAmount", value)).longValue());
            if (variant != null && !variants.contains(variant)) variants.add(variant);
        }
        return new Ingredient(variants, key -> {
            Object stack = stack(key, 1);
            if (stack == null) return false;
            try { return (Boolean)call(nativeInput, "testIgnoreAmount", stack); }
            catch (ReflectiveOperationException | RuntimeException ex) { return false; }
        }, key -> {
            Object stack = stack(key, 1);
            if (stack == null) return 0;
            try { return ((Number)call(nativeInput, "getAmount", stack)).longValue(); }
            catch (ReflectiveOperationException | RuntimeException ex) { return 0; }
        });
    }

    /** Read current fuel and waste mappings, using exact integral ratios without truncation. */
    private static void addReactions(List<Entry> result) throws ReflectiveOperationException {
        Class<?> mappings = Class.forName(API + "reactor.ReactantMappingsRegistry");
        Class<?> reactions = Class.forName(API + "reactor.ReactionsRegistry");
        List<Object> fuels = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) fuels.add(stack);
        }
        for (var fluid : BuiltInRegistries.FLUID) {
            FluidStack stack = new FluidStack(fluid, 1);
            if (!stack.isEmpty() && fluid.isSource(fluid.defaultFluidState())) fuels.add(stack);
        }
        Set<String> seen = new HashSet<>();
        for (Object fuel : fuels) {
            boolean solid = fuel instanceof ItemStack;
            Optional<?> input = (Optional<?>)call(mappings, solid ? "getFromSolid" : "getFromFluid", fuel);
            if (input.isEmpty()) continue;
            Object from = input.get(), reactant = mapping(from, "getProduct");
            Optional<?> reactionValue = (Optional<?>)call(reactions, "get", reactant);
            if (reactionValue.isEmpty()) continue;
            Object reaction = reactionValue.get(), waste = mapping(reaction, "getProduct");
            Optional<?> output = (Optional<?>)call(mappings, solid ? "getToSolid" : "getToFluid", waste);
            if (output.isEmpty()) continue;
            for (Object to : (List<?>)output.get()) {
                long[] ratio = exactRatio(number(from,"getSourceAmount"), number(from,"getProductAmount"),
                        number(reaction,"getSourceAmount"), number(reaction,"getProductAmount"),
                        number(to,"getSourceAmount"), number(to,"getProductAmount"));
                if (ratio == null || ratio[0] > Integer.MAX_VALUE || ratio[1] > Integer.MAX_VALUE) continue;
                Object product = call(mappings, solid ? "getSolidStackFrom" : "getFluidStackFrom", to,
                        ((Number)mapping(to,"getSourceAmount")).intValue());
                GenericStack in = generic(fuel, ratio[0]), out = generic(product, ratio[1]);
                if (in == null || out == null) continue;
                String signature = in + "->" + out;
                if (!seen.add(signature)) continue;
                ResourceLocation fuelId = solid ? BuiltInRegistries.ITEM.getKey(((ItemStack)fuel).getItem())
                        : BuiltInRegistries.FLUID.getKey(((FluidStack)fuel).getFluid());
                var id = ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,
                        "reactor_fuel/" + fuelId.getNamespace() + "/" + fuelId.getPath() + "/" + seen.size());
                Ingredient ingredient = new Ingredient(List.of(in), key -> key.equals(in.what()), key -> in.amount());
                result.add(new Entry(id, List.of(ingredient), List.of(out)));
            }
        }
    }

    public static long[] exactRatio(long source, long reactant, long consumed, long produced, long waste, long output) {
        if (source <= 0 || reactant <= 0 || consumed <= 0 || produced <= 0 || waste <= 0 || output <= 0) return null;
        BigInteger numerator = BigInteger.valueOf(reactant).multiply(BigInteger.valueOf(produced)).multiply(BigInteger.valueOf(output));
        BigInteger denominator = BigInteger.valueOf(source).multiply(BigInteger.valueOf(consumed)).multiply(BigInteger.valueOf(waste));
        BigInteger gcd = numerator.gcd(denominator);
        try { return new long[]{denominator.divide(gcd).longValueExact(), numerator.divide(gcd).longValueExact()}; }
        catch (ArithmeticException ex) { return null; }
    }

    public static GenericStack generic(Object value, long amount) {
        AEKey key = value instanceof ItemStack item && !item.isEmpty() ? AEItemKey.of(item)
                : value instanceof FluidStack fluid && !fluid.isEmpty() ? AEFluidKey.of(fluid) : null;
        return key == null || amount <= 0 ? null : new GenericStack(key, amount);
    }
    public static Object stack(AEKey key, long amount) {
        if (amount <= 0 || amount > Integer.MAX_VALUE) return null;
        return key instanceof AEItemKey item ? item.toStack((int)amount)
                : key instanceof AEFluidKey fluid ? fluid.toStack((int)amount) : null;
    }
    private static boolean has(Object target, String name) {
        return Arrays.stream(target.getClass().getMethods()).anyMatch(m -> m.getName().equals(name) && m.getParameterCount() == 0);
    }
    private static Object mapping(Object target, String name) throws ReflectiveOperationException {
        return Class.forName(API + "IMapping").getMethod(name).invoke(target);
    }
    private static long number(Object target,String name) throws ReflectiveOperationException { return ((Number)mapping(target,name)).longValue(); }
    private static Class<?> boxed(Class<?> type) { return type == int.class ? Integer.class : type == long.class ? Long.class : type; }
    private static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
        Class<?> type = target instanceof Class<?> c ? c : target.getClass();
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length
                    || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) continue;
            boolean matches = true;
            for (int i = 0; i < args.length; i++) if (!boxed(method.getParameterTypes()[i]).isInstance(args[i])) matches = false;
            if (matches) return method.invoke(target instanceof Class<?> ? null : target, args);
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }
}
