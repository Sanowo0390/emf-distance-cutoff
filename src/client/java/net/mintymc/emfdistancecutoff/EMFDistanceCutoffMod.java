package net.mintymc.emfdistancecutoff;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
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
            throw new RuntimeException(
                    "[emf_distance_cutoff] Failed to register the EMF performance conditions. " +
                    "Make sure Entity Model Features is installed and up to date.", e);
        }
    }

    private static Boolean shouldUseVanillaModel(EMFEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        String entityId = EntityIdResolver.resolve(entity);
        CutoffConfig config = CutoffConfig.get();
        CutoffConfig.EntityOverride override = entityId == null ? null : config.getOverride(entityId);

        if (override != null && !override.enabled) return false;

        double distance = override != null && override.distanceBlocks != null
                ? override.distanceBlocks
                : config.cutoffDistanceBlocks;

        if (distance <= 0.0) return false;

        return distanceSquared(entity, minecraft) > distance * distance;
    }

    private static Boolean shouldPauseAnimations(EMFEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        String entityId = EntityIdResolver.resolve(entity);
        CutoffConfig config = CutoffConfig.get();
        CutoffConfig.EntityOverride override = entityId == null ? null : config.getOverride(entityId);

        if (override != null && !override.enabled) return false;

        double modelDistance = override != null && override.distanceBlocks != null
                ? override.distanceBlocks
                : config.cutoffDistanceBlocks;
        double pauseDistance = override != null && override.animationPauseDistanceBlocks != null
                ? override.animationPauseDistanceBlocks
                : config.animationPauseDistanceBlocks;

        if (pauseDistance <= 0.0 || modelDistance <= 0.0) return false;
        // Never pause outside the range where this mod still allows the EMF model.
        if (pauseDistance >= modelDistance) return false;

        return distanceSquared(entity, minecraft) >= pauseDistance * pauseDistance;
    }

    private static double distanceSquared(EMFEntity entity, Minecraft minecraft) {
        double dx = entity.emf$getX() - minecraft.player.getX();
        double dy = entity.emf$getY() - minecraft.player.getY();
        double dz = entity.emf$getZ() - minecraft.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
