package net.mintymc.emfdistancecutoff.mixin;

import net.mintymc.emfdistancecutoff.EMFDistanceCutoffMod;
import net.mintymc.emfdistancecutoff.FrozenPoseCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.utils.EMFEntity;

/**
 * Replaces EMF 3.2.x's all-or-nothing pause with a cached whole-model pose.
 */
@Mixin(value = EMFModelPartRoot.class, remap = false)
public abstract class EMFModelPartRootMixin {
    @Inject(method = "animate", at = @At("HEAD"), cancellable = true, remap = false)
    private void emfDistanceCutoff$restoreFrozenPose(CallbackInfo ci) {
        EMFEntity entity = EMFAnimationApi.getCurrentEntity();
        if (!EMFDistanceCutoffMod.shouldFreezeAnimations(entity)) return;

        if (FrozenPoseCache.restore((EMFModelPartRoot) (Object) this, entity)) {
            ci.cancel();
        }
        // A newly seen distant entity has no valid cache yet.  Let EMF animate
        // it once so the following frames can safely reuse a complete pose.
    }

    @Inject(method = "animate", at = @At("RETURN"), remap = false)
    private void emfDistanceCutoff$captureCompletePose(CallbackInfo ci) {
        FrozenPoseCache.capture((EMFModelPartRoot) (Object) this, EMFAnimationApi.getCurrentEntity());
    }
}
