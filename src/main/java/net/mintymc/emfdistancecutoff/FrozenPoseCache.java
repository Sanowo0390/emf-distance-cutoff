package net.mintymc.emfdistancecutoff;

import net.minecraft.client.model.ModelPart;
import traben.entity_model_features.models.parts.EMFModelPartWithState;
import traben.entity_model_features.utils.EMFEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Stores final EMF part poses per entity, model part, and model variant. */
public final class FrozenPoseCache {
    private static final int MAX_CACHED_POSES = 2_048;

    private static final Map<PoseKey, Pose> POSES = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PoseKey, Pose> eldest) {
            return size() > MAX_CACHED_POSES;
        }
    };

    private FrozenPoseCache() {}

    /** @return {@code true} if a complete prior pose was restored. */
    public static boolean restore(EMFModelPartWithState part, EMFEntity entity) {
        PoseKey key = keyFor(part, entity);
        if (key == null) return false;

        Pose pose = POSES.get(key);
        if (pose == null) return false;

        pose.apply();
        return true;
    }

    /** Captures a part after EMF has applied its normal animation update. */
    public static void capture(EMFModelPartWithState part, EMFEntity entity) {
        PoseKey key = keyFor(part, entity);
        if (key != null) POSES.put(key, Pose.capture(part));
    }

    private static PoseKey keyFor(EMFModelPartWithState part, EMFEntity entity) {
        if (part == null || entity == null || entity.etf$getUuid() == null) return null;
        return new PoseKey(part, entity.etf$getUuid(), part.currentModelVariant);
    }

    private record PoseKey(EMFModelPartWithState part, UUID entityUuid, int modelVariant) {}

    private record Pose(List<PartState> parts) {
        static Pose capture(EMFModelPartWithState part) {
            List<ModelPart> modelParts = new ArrayList<>(part.traverse());
            // ModelPart#traverse returns descendants, not this part itself.
            modelParts.add(part);

            List<PartState> states = new ArrayList<>(modelParts.size());
            for (ModelPart modelPart : modelParts) {
                states.add(PartState.capture(modelPart));
            }
            return new Pose(states);
        }

        void apply() {
            for (PartState part : parts) {
                part.apply();
            }
        }
    }

    private record PartState(
            ModelPart part,
            float originX, float originY, float originZ,
            float pitch, float yaw, float roll,
            float xScale, float yScale, float zScale,
            boolean visible, boolean hidden
    ) {
        static PartState capture(ModelPart part) {
            return new PartState(part,
                    part.originX, part.originY, part.originZ,
                    part.pitch, part.yaw, part.roll,
                    part.xScale, part.yScale, part.zScale,
                    part.visible, part.hidden);
        }

        void apply() {
            part.originX = originX;
            part.originY = originY;
            part.originZ = originZ;
            part.pitch = pitch;
            part.yaw = yaw;
            part.roll = roll;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.hidden = hidden;
        }
    }
}
