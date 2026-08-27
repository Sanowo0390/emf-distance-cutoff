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
    private static final int ROW_WIDTH = 700;
    private final ConfigScreen parentScreen;
    private final CutoffConfig config;

    public EntityListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, ConfigScreen parentScreen) {
        super(minecraft, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        this.config = CutoffConfig.get();
    }

    @Override
    public int getRowWidth() {
        return Math.min(ROW_WIDTH, Math.max(200, this.width - 40));
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

        private Component getName() {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(reference -> reference.value()).orElse(null);
            return type != null ? type.getDescription() : Component.literal(id.toString());
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
            return getName().copy().append(Component.literal(" - ")).append(statusText());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX();
            int y = getContentY();
            int w = getContentWidth();
            int h = getContentHeight();

            int background = hovered ? 0xC0383838 : 0xAA1A1A1A;
            int border = hovered ? 0xFF8A8A8A : 0xFF555555;
            graphics.fill(x, y, x + w, y + h, background);
            graphics.fill(x, y, x + w, y + 1, border);
            graphics.fill(x, y + h - 1, x + w, y + h, border);

            graphics.text(Minecraft.getInstance().font, getName(), x + 10, y + 5, 0xFFFFFFFF, true);
            graphics.text(Minecraft.getInstance().font, statusText(), x + 10, y + 18, 0xFFB8B8B8, false);
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
