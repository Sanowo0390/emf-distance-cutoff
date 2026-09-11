package net.mintymc.emfdistancecutoff;

import net.minecraft.client.model.geom.ModelPart;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.utils.EMFEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Stores the complete final pose for an EMF root on a per-entity basis. */
public final class FrozenPoseCache {
    private static final int MAX_CACHED_POSES = 2_048;

    private static final Map<PoseKey, Pose> POSES = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PoseKey, Pose> eldest) {
            return size() > MAX_CACHED_POSES;
        }
    };

    private FrozenPoseCache() {}

    /**
     * Restores an entity's last complete custom-model pose.
     *
     * @return {@code true} when a pose existed and was restored
     */
    public static boolean restore(EMFModelPartRoot root, EMFEntity entity) {
        PoseKey key = keyFor(root, entity);
        if (key == null) return false;

        Pose pose = POSES.get(key);
        if (pose == null) return false;

        pose.apply();
        return true;
    }

    /** Captures the pose after EMF has completed a normal animation update. */
    public static void capture(EMFModelPartRoot root, EMFEntity entity) {
        PoseKey key = keyFor(root, entity);
        if (key != null) POSES.put(key, Pose.capture(root));
    }

    private static PoseKey keyFor(EMFModelPartRoot root, EMFEntity entity) {
        if (root == null || entity == null || entity.etf$getUuid() == null) return null;
        return new PoseKey(root, entity.etf$getUuid(), root.currentModelVariant);
    }

    private record PoseKey(EMFModelPartRoot root, UUID entityUuid, int modelVariant) {}

    private record Pose(List<PartState> parts) {
        static Pose capture(EMFModelPartRoot root) {
            List<ModelPart> modelParts = new ArrayList<>(root.getAllParts());
            // ModelPart#getAllParts returns descendants, not this root.
            modelParts.add(root);

            List<PartState> states = new ArrayList<>(modelParts.size());
            for (ModelPart part : modelParts) {
                states.add(PartState.capture(part));
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
            float x, float y, float z,
            float xRot, float yRot, float zRot,
            float xScale, float yScale, float zScale,
            boolean visible, boolean skipDraw
    ) {
        static PartState capture(ModelPart part) {
            return new PartState(part,
                    part.x, part.y, part.z,
                    part.xRot, part.yRot, part.zRot,
                    part.xScale, part.yScale, part.zScale,
                    part.visible, part.skipDraw);
        }

        void apply() {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.skipDraw = skipDraw;
        }
    }
}
