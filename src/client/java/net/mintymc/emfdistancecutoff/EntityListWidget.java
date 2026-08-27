package net.mintymc.emfdistancecutoff;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EntityListWidget extends ObjectSelectionList<EntityListWidget.EntityEntry> {
    private final ConfigScreen parentScreen;
    private final CutoffConfig config;

    public EntityListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, ConfigScreen parentScreen) {
        super(minecraft, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        this.config = CutoffConfig.get();
    }

    public void rebuild(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Identifier> ids = new ArrayList<>();

        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(reference -> reference.value()).orElse(null);
            String translatedName = type != null ? type.getDescription().getString() : "";
            String searchable = id + " " + translatedName;
            if (normalized.isEmpty() || searchable.toLowerCase(Locale.ROOT).contains(normalized)) {
                ids.add(id);
            }
        }

        ids.sort(Comparator.comparing(id -> {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(reference -> reference.value()).orElse(null);
            return (type != null ? type.getDescription().getString() : id.toString()).toLowerCase(Locale.ROOT);
        }));

        List<EntityEntry> entries = new ArrayList<>(ids.size());
        for (Identifier id : ids) entries.add(new EntityEntry(id));
        replaceEntries(entries);
        setScrollAmount(0);
    }

    /** Public because the enclosing widget exposes this type as its generic parameter. */
    public final class EntityEntry extends ObjectSelectionList.Entry<EntityEntry> {
        private final Identifier id;

        private EntityEntry(Identifier id) {
            this.id = id;
        }

        private Component getLabel() {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(reference -> reference.value()).orElse(null);
            String name = type != null ? type.getDescription().getString() : id.toString();
            return Component.literal(name).append(Component.literal("  ")).append(statusText());
        }

        private Component statusText() {
            CutoffConfig.EntityOverride override = config.getOverride(id.toString());
            if (override == null) {
                return Component.translatable("emf_distance_cutoff.status_inherited", ConfigScreen.format(config.cutoffDistanceBlocks));
            }
            if (!override.enabled) {
                return Component.translatable("emf_distance_cutoff.status_disabled");
            }
            if (override.distanceBlocks == null) {
                return Component.translatable("emf_distance_cutoff.status_global", ConfigScreen.format(config.cutoffDistanceBlocks));
            }
            return Component.translatable("emf_distance_cutoff.status_custom", ConfigScreen.format(override.distanceBlocks));
        }

        @Override
        public Component getNarration() {
            return getLabel();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX() + 4;
            int y = getContentY() + 3;
            graphics.text(Minecraft.getInstance().font, getLabel(), x, y, 0xFFFFFFFF, true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                setSelected(this);
                if (parentScreen != null) {
                    Minecraft.getInstance().gui.setScreen(new EntityConfigScreen(parentScreen, id));
                }
                return true;
            }
            return false;
        }
    }
}
