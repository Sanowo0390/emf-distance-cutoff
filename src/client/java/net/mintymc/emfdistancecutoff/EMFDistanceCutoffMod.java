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
        CutoffConfig.EntityOverride override = config.getOverrideForEmfType(entityId);

        if (override != null && !override.enabled) return false;

        double distance = override != null && override.distanceBlocks != null
                ? override.distanceBlocks
                : config.cutoffDistanceBlocks;

        if (distance <= 0.0) return false;

        return distanceSquared(entity, minecraft) > distance * distance;
    }

    /**
     * Whether this entity should use its previously calculated EMF pose.
     *
     * <p>EMF 3.2.x's {@code registerPauseCondition} skips the complete model
     * animation step.  That leaves freshly reset vanilla parts mixed with stale
     * custom parts, which is why models can appear split apart.  The mixin uses
     * this condition to restore one complete cached pose instead.</p>
     */
    public static boolean shouldFreezeAnimations(EMFEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        String entityId = EntityIdResolver.resolve(entity);
        CutoffConfig config = CutoffConfig.get();
        CutoffConfig.EntityOverride override = config.getOverrideForEmfType(entityId);

        if (override != null && !override.enabled) return false;

        double modelDistance = override != null && override.distanceBlocks != null
                ? override.distanceBlocks
                : config.cutoffDistanceBlocks;
        double pauseDistance = override != null && override.animationPauseDistanceBlocks != null
                ? override.animationPauseDistanceBlocks
                : config.animationPauseDistanceBlocks;

        if (pauseDistance <= 0.0 || modelDistance <= 0.0) return false;
        if (pauseDistance >= modelDistance) return false;

        double distanceSquared = distanceSquared(entity, minecraft);
        // Preserve the intended three tiers: animated EMF, frozen EMF, then vanilla.
        return distanceSquared >= pauseDistance * pauseDistance
                && distanceSquared <= modelDistance * modelDistance;
    }

    private static double distanceSquared(EMFEntity entity, Minecraft minecraft) {
        double dx = entity.emf$getX() - minecraft.player.getX();
        double dy = entity.emf$getY() - minecraft.player.getY();
        double dz = entity.emf$getZ() - minecraft.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
