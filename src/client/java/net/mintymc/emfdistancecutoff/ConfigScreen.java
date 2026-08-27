package net.mintymc.emfdistancecutoff;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ConfigScreen extends Screen {
    private static final int TITLE_Y = 12;
    private static final int GLOBAL_LABEL_Y = 38;
    private static final int GLOBAL_FIELD_Y = 54;
    private static final int ENTITY_LABEL_Y = 82;
    private static final int SEARCH_Y = 98;

    private final Screen parent;
    private final CutoffConfig config;
    private EditBox globalDistanceField;
    private EditBox searchField;
    private EntityListWidget entityList;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("emf_distance_cutoff.title"));
        this.parent = parent;
        this.config = CutoffConfig.get();
    }

    @Override
    protected void init() {
        super.init();

        int center = this.width / 2;
        int fieldWidth = Math.min(320, Math.max(180, this.width - 80));

        globalDistanceField = new EditBox(this.font, center - fieldWidth / 2, GLOBAL_FIELD_Y, fieldWidth, 20,
                Component.translatable("emf_distance_cutoff.global_distance"));
        globalDistanceField.setMaxLength(12);
        globalDistanceField.setValue(format(config.cutoffDistanceBlocks));
        globalDistanceField.setHint(Component.translatable("emf_distance_cutoff.distance_placeholder"));
        addRenderableWidget(globalDistanceField);

        searchField = new EditBox(this.font, center - fieldWidth / 2, SEARCH_Y, fieldWidth, 20,
                Component.translatable("emf_distance_cutoff.search"));
        searchField.setMaxLength(64);
        searchField.setHint(Component.translatable("emf_distance_cutoff.search_placeholder"));
        searchField.setResponder(value -> {
            if (entityList != null) entityList.rebuild(value);
        });
        addRenderableWidget(searchField);

        int listTop = SEARCH_Y + 28;
        int listBottom = this.height - 38;
        int listWidth = Math.min(760, Math.max(220, this.width - 80));
        entityList = new EntityListWidget(Minecraft.getInstance(), listWidth,
                Math.max(80, listBottom - listTop), listTop, 34, this);
        entityList.rebuild(searchField.getValue());
        addRenderableWidget(entityList);

        int buttonY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.save"), button -> saveAndClose())
                .bounds(center - 100, buttonY, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.cancel"), button -> onClose())
                .bounds(center + 5, buttonY, 95, 20).build());
    }

    private void saveAndClose() {
        Double value = parsePositiveOrZero(globalDistanceField.getValue());
        if (value != null) {
            config.cutoffDistanceBlocks = value;
            CutoffConfig.save();
            onClose();
        }
    }

    private static Double parsePositiveOrZero(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String format(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.centeredText(this.font, this.title, this.width / 2, TITLE_Y, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.global_distance_label"), this.width / 2, GLOBAL_LABEL_Y, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.entities"), this.width / 2, ENTITY_LABEL_Y, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
