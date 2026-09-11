package net.mintymc.emfdistancecutoff.mixin;

import net.mintymc.emfdistancecutoff.EMFDistanceCutoffMod;
import net.mintymc.emfdistancecutoff.FrozenPoseCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.parts.EMFModelPart;
import traben.entity_model_features.models.parts.EMFModelPartWithState;
import traben.entity_model_features.utils.EMFEntity;

/**
 * EMF 3.0.x performs custom animation from its ModelPart render path.  Reuse
 * the last full part pose in place of running the animation expressions.
 */
@Mixin(value = EMFModelPartWithState.class, remap = false)
public abstract class EMFModelPartWithStateMixin {
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Ltraben/entity_model_features/models/parts/EMFModelPart$Animator;run()V"),
            remap = false
    )
    private void emfDistanceCutoff$restoreFrozenPose(EMFModelPart.Animator animator) {
        EMFEntity entity = EMFAnimationApi.getCurrentEntity();
        EMFModelPartWithState part = (EMFModelPartWithState) (Object) this;

        if (EMFDistanceCutoffMod.shouldFreezeAnimations(entity)
                && FrozenPoseCache.restore(part, entity)) {
            return;
        }

        // An entity seen for the first time at a distance has no cached pose.
        // Run EMF once so later frames can reuse a complete, valid pose.
        animator.run();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void emfDistanceCutoff$captureCompletePose(CallbackInfo ci) {
        FrozenPoseCache.capture((EMFModelPartWithState) (Object) this, EMFAnimationApi.getCurrentEntity());
    }
}
