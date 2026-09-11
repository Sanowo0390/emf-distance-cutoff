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
 * EMF 3.2.x evaluates all of a root model's custom animation from
 * {@code EMFModelPartRoot#animate}.  Reapply the entity's last complete pose
 * instead of skipping that update and leaving vanilla-reset parts behind.
 */
@Mixin(value = EMFModelPartRoot.class, remap = false)
public abstract class EMFModelPartRootMixin {
    @Inject(method = "animate", at = @At("HEAD"), cancellable = true, remap = false)
    private void emfDistanceCutoff$restoreFrozenPose(CallbackInfo ci) {
        EMFEntity entity = EMFAnimationApi.getCurrentEntity();
        EMFModelPartRoot root = (EMFModelPartRoot) (Object) this;

        if (EMFDistanceCutoffMod.shouldFreezeAnimations(entity)
                && FrozenPoseCache.restore(root, entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "animate", at = @At("RETURN"), remap = false)
    private void emfDistanceCutoff$captureCompletePose(CallbackInfo ci) {
        FrozenPoseCache.capture((EMFModelPartRoot) (Object) this, EMFAnimationApi.getCurrentEntity());
    }
}
