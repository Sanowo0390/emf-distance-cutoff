package net.mintymc.emfdistancecutoff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CutoffConfig {
    public static final double DEFAULT_DISTANCE_BLOCKS = 24.0;
    public static final double DEFAULT_ANIMATION_PAUSE_DISTANCE_BLOCKS = 12.0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("emf_distance_cutoff.json");
    public double cutoffDistanceBlocks = DEFAULT_DISTANCE_BLOCKS;
    public double animationPauseDistanceBlocks = DEFAULT_ANIMATION_PAUSE_DISTANCE_BLOCKS;
    public Map<String, EntityOverride> entities = new LinkedHashMap<>();
    private static CutoffConfig instance;

    public static CutoffConfig get() { if (instance == null) load(); return instance; }
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                instance = GSON.fromJson(reader, CutoffConfig.class);
                if (instance == null) instance = new CutoffConfig();
                if (instance.entities == null) instance.entities = new LinkedHashMap<>();
                return;
            } catch (Exception ignored) { }
        }
        instance = new CutoffConfig();
        save();
    }
    public static void save() {
        if (instance == null) instance = new CutoffConfig();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) { GSON.toJson(instance, writer); }
        } catch (IOException ignored) { }
    }
    public EntityOverride getOverride(String entityId) { return entities.get(entityId); }
    /**
     * EMF normally supplies registry ids such as minecraft:creeper, but some
     * compatibility render paths omit minecraft:.  GUI settings always use
     * canonical registry ids, so accept that equivalent spelling too.
     */
    public EntityOverride getOverrideForEmfType(String entityId) {
        if (entityId == null || entityId.isBlank()) return null;
        EntityOverride direct = entities.get(entityId);
        if (direct != null) return direct;

        // EMF's 3.2.x EMFEntity#getTypeString is EntityType#toString.
        // EntityType#toString returns its translation key (for example
        // entity.minecraft.creeper), while the GUI stores minecraft:creeper.
        if (entityId.startsWith("entity.")) {
            int namespaceEnd = entityId.indexOf('.', "entity.".length());
            if (namespaceEnd > "entity.".length()) {
                String registryId = entityId.substring("entity.".length(), namespaceEnd)
                        + ":" + entityId.substring(namespaceEnd + 1);
                EntityOverride translated = entities.get(registryId);
                if (translated != null) return translated;
            }
        }

        if (entityId.indexOf(':') >= 0) return null;
        return entities.get("minecraft:" + entityId);
    }
    public EntityOverride getOrCreateOverride(String entityId) { return entities.computeIfAbsent(entityId, id -> new EntityOverride()); }
    public void resetOverride(String entityId) { entities.remove(entityId); }
    public void resetAll() {
        cutoffDistanceBlocks = DEFAULT_DISTANCE_BLOCKS;
        animationPauseDistanceBlocks = DEFAULT_ANIMATION_PAUSE_DISTANCE_BLOCKS;
        entities.clear();
    }
    public static final class EntityOverride {
        public boolean enabled = true;
        public Double distanceBlocks = null;
        public Double animationPauseDistanceBlocks = null;
    }
}
