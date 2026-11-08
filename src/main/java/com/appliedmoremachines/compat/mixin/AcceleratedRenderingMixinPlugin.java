package com.appliedmoremachines.compat.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.service.MixinService;

/** Repairs captured-field names used by Accelerated Rendering's text mixins in dev mappings. */
public final class AcceleratedRenderingMixinPlugin implements IMixinConfigPlugin {
    private static final String TEXT_CONFIG = "acceleratedrendering.feature.text.mixins.json";
    private static boolean installed;

    @Override
    public void onLoad(String mixinPackage) {
        if (installed) {
            return;
        }
        try (InputStream config = MixinService.getService().getResourceAsStream(TEXT_CONFIG)) {
            if (config == null) {
                return;
            }
        } catch (IOException e) {
            MixinService.getService().getLogger("applied_more_machines/compat")
                    .warn("Could not inspect Accelerated Rendering's text config", e);
            return;
        }

        // Config plugins load before mixin preprocessing, including @Shadow resolution.
        var remapper = new CapturedTextRemapper();
        var current = MixinEnvironment.getCurrentEnvironment();
        current.getRemappers().add(remapper);
        var defaults = MixinEnvironment.getDefaultEnvironment();
        if (defaults != current) {
            defaults.getRemappers().add(remapper);
        }
        installed = true;
    }

    private static final class CapturedTextRemapper implements IRemapper {
        private static final String PLAIN = "net/minecraft/network/chat/FormattedText$2";
        private static final String STYLED = "net/minecraft/network/chat/FormattedText$3";
        private static final String STRING_DESC = "Ljava/lang/String;";
        private static final String STYLE_DESC = "Lnet/minecraft/network/chat/Style;";
        private final Map<String, Map<String, String>> fields = new ConcurrentHashMap<>();

        @Override
        public String mapFieldName(String owner, String name, String desc) {
            boolean text = "val$text".equals(name) && STRING_DESC.equals(desc);
            boolean style = "val$style".equals(name) && STYLE_DESC.equals(desc) && STYLED.equals(owner);
            if ((!PLAIN.equals(owner) && !STYLED.equals(owner)) || (!text && !style)) {
                return name;
            }
            return fields.computeIfAbsent(owner, this::readCapturedFields).getOrDefault(name, name);
        }

        private Map<String, String> readCapturedFields(String owner) {
            // Read the resource without loading the game class or re-entering its transformers.
            // ModLauncher's bytecode provider does not support runTransformers=false.
            try (InputStream bytes = MixinService.getService().getResourceAsStream(owner + ".class")) {
                if (bytes == null) {
                    MixinService.getService().getLogger("applied_more_machines/compat")
                            .warn("Could not read captured text fields in {}", owner);
                    return Map.of();
                }
                ClassNode target = new ClassNode();
                new ClassReader(bytes).accept(target,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                var names = new HashMap<String, String>();
                matchField(target, "val$text", STRING_DESC, names);
                if (STYLED.equals(owner)) {
                    matchField(target, "val$style", STYLE_DESC, names);
                }
                return Map.copyOf(names);
            } catch (IOException e) {
                MixinService.getService().getLogger("applied_more_machines/compat")
                        .warn("Could not resolve captured text fields in {}", owner, e);
                return Map.of();
            }
        }

        private static void matchField(ClassNode target, String name, String desc, Map<String, String> names) {
            // Production mappings (or a future upstream fix) already have the requested name.
            if (target.fields.stream().anyMatch(f -> name.equals(f.name) && desc.equals(f.desc))) {
                return;
            }
            FieldNode match = null;
            for (FieldNode field : target.fields) {
                if (!desc.equals(field.desc) || !field.name.startsWith("val$")
                        || (field.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC))
                        != (Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC)) {
                    continue;
                }
                if (match != null) {
                    return; // Ambiguous bytecode must retain Mixin's normal validation failure.
                }
                match = field;
            }
            if (match != null) {
                names.put(name, match.name);
                MixinService.getService().getLogger("applied_more_machines/compat")
                        .info("Accelerated Rendering text compatibility: {}.{} -> {}", target.name, name, match.name);
            }
        }

        @Override public String mapMethodName(String owner, String name, String desc) { return name; }
        @Override public String map(String typeName) { return typeName; }
        @Override public String unmap(String typeName) { return typeName; }
        @Override public String mapDesc(String desc) { return desc; }
        @Override public String unmapDesc(String desc) { return desc; }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
