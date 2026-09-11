package net.mintymc.emfdistancecutoff;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EntityListWidget extends ElementListWidget<EntityListWidget.EntityEntry> {
    private final ConfigScreen parentScreen;
    private final CutoffConfig config;

    public EntityListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, ConfigScreen parentScreen) {
        super(client, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        this.config = CutoffConfig.get();
    }

    public void rebuild(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Identifier> ids = new ArrayList<>();
        for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            String translatedName = type != null ? type.getName().getString() : "";
            if (normalized.isEmpty() || (id.toString() + " " + translatedName).toLowerCase(Locale.ROOT).contains(normalized)) {
                ids.add(id);
            }
        }
        ids.sort(Comparator.comparing(id -> {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            return (type != null ? type.getName().getString() : id.toString()).toLowerCase(Locale.ROOT);
        }));
        List<EntityEntry> entries = new ArrayList<>(ids.size());
        for (Identifier id : ids) {
            entries.add(new EntityEntry(id));
        }
        replaceEntries(entries);
        setScrollY(0);
    }

    @Override
    public int getRowWidth() {
        // EntryListWidget defaults to a narrow 220-pixel row even when the
        // scroll area is wider. Use the available width for readable labels.
        return Math.max(100, getWidth() - 12);
    }

    /** Entry type must be accessible because it is exposed by ElementListWidget's generic type. */
    public final class EntityEntry extends ElementListWidget.Entry<EntityEntry> {
        private final Identifier id;
        private final ButtonWidget button;

        private EntityEntry(Identifier id) {
            this.id = id;
            this.button = ButtonWidget.builder(getLabel(id), b -> {
                if (parentScreen != null && client != null) {
                    client.setScreen(new EntityConfigScreen(parentScreen, id));
                }
            }).dimensions(0, 0, 280, 20).build();
        }

        private Text getLabel(Identifier id) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            String name = type != null ? type.getName().getString() : id.toString();
            return Text.literal(name).append(Text.literal("  "))
                    .append(statusText(config.getOverride(id.toString())));
        }

        private Text statusText(CutoffConfig.EntityOverride override) {
            if (override != null && !override.enabled) {
                return Text.translatable("emf_distance_cutoff.status_disabled");
            }
            Double customDistance = override == null ? null : override.distanceBlocks;
            Double customPauseDistance = override == null ? null : override.animationPauseDistanceBlocks;
            return Text.translatable("emf_distance_cutoff.status_summary",
                    distanceStatus("emf_distance_cutoff.status_model", customDistance, config.cutoffDistanceBlocks),
                    distanceStatus("emf_distance_cutoff.status_pause", customPauseDistance, config.animationPauseDistanceBlocks));
        }

        private Text distanceStatus(String labelKey, Double customValue, double globalValue) {
            return Text.translatable(labelKey,
                    Text.translatable(customValue == null
                                    ? "emf_distance_cutoff.status_value_global"
                                    : "emf_distance_cutoff.status_value_custom",
                            ConfigScreen.format(customValue == null ? globalValue : customValue)));
        }

        @Override
        public List<? extends Element> children() {
            return List.of(button);
        }

        @Override
        public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return List.of(button);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX() + 2;
            int y = getContentY();
            button.setPosition(x, y);
            button.setWidth(Math.max(100, getContentWidth() - 4));
            button.setMessage(getLabel(id));
            button.render(context, mouseX, mouseY, delta);
        }
    }
}
