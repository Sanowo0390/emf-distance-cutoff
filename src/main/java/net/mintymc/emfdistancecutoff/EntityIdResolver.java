package net.mintymc.emfdistancecutoff;

import traben.entity_model_features.utils.EMFEntity;

public final class EntityIdResolver {
    private EntityIdResolver() {}
    public static String resolve(EMFEntity emfEntity) {
        if (emfEntity == null) return null;
        String typeString = emfEntity.emf$getTypeString();
        return typeString == null || typeString.isBlank() ? null : typeString;
    }
}
