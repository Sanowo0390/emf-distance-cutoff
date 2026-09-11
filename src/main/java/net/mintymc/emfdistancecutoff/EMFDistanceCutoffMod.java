package net.mintymc.emfdistancecutoff;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.utils.EMFEntity;

public final class EMFDistanceCutoffMod implements ClientModInitializer {
    public static final String MOD_ID = "emf_distance_cutoff";

    @Override
    public void onInitializeClient() {
        CutoffConfig.load();
        try {
            EMFAnimationApi.registerVanillaModelCondition(EMFDistanceCutoffMod::shouldUseVanillaModel);
        } catch (Exception e) {
            throw new RuntimeException("[emf_distance_cutoff] Failed to register the EMF performance conditions. Make sure Entity Model Features is installed and up to date.", e);
        }
    }

    private static Boolean shouldUseVanillaModel(EMFEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        String entityId = EntityIdResolver.resolve(entity);
        CutoffConfig config = CutoffConfig.get();
        CutoffConfig.EntityOverride override = entityId == null ? null : config.getOverride(entityId);
        if (override != null && !override.enabled) return false;
        double distance = override != null && override.distanceBlocks != null ? override.distanceBlocks : config.cutoffDistanceBlocks;
        if (distance <= 0.0) return false;
        return distanceSquared(entity, client) > distance * distance;
    }

    /**
     * Whether this entity should reuse its last complete EMF pose.
     *
     * <p>EMF 3.0.x's pause API skips its model animation run entirely.  The
     * vanilla setup still resets parts, so that can leave a model with mixed
     * poses.  The mixin restores a complete cached pose instead.</p>
     */
    public static boolean shouldFreezeAnimations(EMFEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        String entityId = EntityIdResolver.resolve(entity);
        CutoffConfig config = CutoffConfig.get();
        CutoffConfig.EntityOverride override = entityId == null ? null : config.getOverride(entityId);
        if (override != null && !override.enabled) return false;

        double modelDistance = override != null && override.distanceBlocks != null
                ? override.distanceBlocks : config.cutoffDistanceBlocks;
        double pauseDistance = override != null && override.animationPauseDistanceBlocks != null
                ? override.animationPauseDistanceBlocks : config.animationPauseDistanceBlocks;

        if (pauseDistance <= 0.0 || modelDistance <= 0.0) return false;
        if (pauseDistance >= modelDistance) return false;

        double distanceSquared = distanceSquared(entity, client);
        return distanceSquared >= pauseDistance * pauseDistance
                && distanceSquared <= modelDistance * modelDistance;
    }

    private static double distanceSquared(EMFEntity entity, MinecraftClient client) {
        double dx = entity.emf$getX() - client.player.getX();
        double dy = entity.emf$getY() - client.player.getY();
        double dz = entity.emf$getZ() - client.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
