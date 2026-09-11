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
            EMFAnimationApi.registerPauseCondition(EMFDistanceCutoffMod::shouldPauseAnimations);
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

    private static Boolean shouldPauseAnimations(EMFEntity entity) {
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
        // Never pause outside the range where this mod still allows the EMF model.
        if (pauseDistance >= modelDistance) return false;
        return distanceSquared(entity, client) >= pauseDistance * pauseDistance;
    }

    private static double distanceSquared(EMFEntity entity, MinecraftClient client) {
        double dx = entity.emf$getX() - client.player.getX();
        double dy = entity.emf$getY() - client.player.getY();
        double dz = entity.emf$getZ() - client.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
