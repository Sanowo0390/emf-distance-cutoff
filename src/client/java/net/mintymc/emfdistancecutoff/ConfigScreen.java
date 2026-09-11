package net.mintymc.emfdistancecutoff;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ConfigScreen extends Screen {
    private static final int TITLE_Y = 8;
    private static final int GLOBAL_LABEL_Y = 27;
    private static final int GLOBAL_FIELD_Y = 39;
    private static final int ANIMATION_LABEL_Y = 64;
    private static final int ANIMATION_FIELD_Y = 76;
    private static final int SEARCH_Y = 103;
    private static final int ENTITY_LABEL_Y = 128;

    private final Screen parent;
    private final CutoffConfig config;
    private EditBox globalDistanceField;
    private EditBox animationPauseDistanceField;
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
        int fieldWidth = Math.min(760, Math.max(300, this.width - 120));
        int left = center - fieldWidth / 2;

        globalDistanceField = createNumberField(left, GLOBAL_FIELD_Y,
                Component.translatable("emf_distance_cutoff.global_distance"),
                format(config.cutoffDistanceBlocks),
                Component.translatable("emf_distance_cutoff.distance_placeholder"));
        addRenderableWidget(globalDistanceField);

        animationPauseDistanceField = createNumberField(left, ANIMATION_FIELD_Y,
                Component.translatable("emf_distance_cutoff.animation_pause_distance"),
                format(config.animationPauseDistanceBlocks),
                Component.translatable("emf_distance_cutoff.animation_pause_placeholder"));
        addRenderableWidget(animationPauseDistanceField);

        searchField = new EditBox(this.font, left, SEARCH_Y, fieldWidth, 20,
                Component.translatable("emf_distance_cutoff.search"));
        searchField.setMaxLength(64);
        searchField.setHint(Component.translatable("emf_distance_cutoff.search_placeholder"));
        searchField.setResponder(value -> {
            if (entityList != null) entityList.rebuild(value);
        });
        addRenderableWidget(searchField);

        int listTop = ENTITY_LABEL_Y + 16;
        int listBottom = this.height - 66;
        int listWidth = Math.min(1000, Math.max(300, this.width - 80));
        entityList = new EntityListWidget(Minecraft.getInstance(), listWidth,
                Math.max(80, listBottom - listTop), listTop, 34, this);
        entityList.rebuild(searchField.getValue());
        addRenderableWidget(entityList);

        int buttonY = this.height - 40;
        int resetWidth = 230;
        int actionWidth = 130;
        int gap = 10;
        int totalWidth = resetWidth + actionWidth * 2 + gap * 2;
        int buttonLeft = center - totalWidth / 2;

        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.reset_all"), button -> resetAll())
                .bounds(buttonLeft, buttonY, resetWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.save"), button -> saveAndClose())
                .bounds(buttonLeft + resetWidth + gap, buttonY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.cancel"), button -> onClose())
                .bounds(buttonLeft + resetWidth + gap + actionWidth + gap, buttonY, actionWidth, 20).build());
    }

    private EditBox createNumberField(int x, int y, Component message, String value, Component hint) {
        EditBox field = new EditBox(this.font, x, y,
                Math.min(760, Math.max(300, this.width - 120)), 20, message);
        field.setMaxLength(12);
        field.setValue(value);
        field.setHint(hint);
        return field;
    }

    private void resetAll() {
        config.resetAll();
        globalDistanceField.setValue(format(config.cutoffDistanceBlocks));
        animationPauseDistanceField.setValue(format(config.animationPauseDistanceBlocks));
        if (entityList != null) entityList.rebuild(searchField.getValue());
        CutoffConfig.save();
    }

    private void saveAndClose() {
        Double modelDistance = parsePositiveOrZero(globalDistanceField.getValue());
        Double pauseDistance = parsePositiveOrZero(animationPauseDistanceField.getValue());
        if (modelDistance != null && pauseDistance != null) {
            config.cutoffDistanceBlocks = modelDistance;
            config.animationPauseDistanceBlocks = pauseDistance;
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
                Component.translatable("emf_distance_cutoff.animation_pause_distance_label"), this.width / 2, ANIMATION_LABEL_Y, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.entities"), this.width / 2, ENTITY_LABEL_Y, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
