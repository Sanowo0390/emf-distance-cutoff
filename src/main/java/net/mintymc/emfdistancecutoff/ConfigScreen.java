package net.mintymc.emfdistancecutoff;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

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
    private TextFieldWidget globalDistanceField;
    private TextFieldWidget animationPauseDistanceField;
    private TextFieldWidget searchField;
    private EntityListWidget entityList;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("emf_distance_cutoff.title"));
        this.parent = parent;
        this.config = CutoffConfig.get();
    }

    @Override
    protected void init() {
        super.init();

        int center = this.width / 2;
        int horizontalMargin = Math.min(60, Math.max(12, this.width / 8));
        int fieldWidth = Math.min(760, Math.max(180, this.width - horizontalMargin * 2));
        int left = center - fieldWidth / 2;

        // Use real GUI widgets for these labels.  In 1.21.11's extracted GUI
        // renderer, text emitted directly from Screen#render can be submitted
        // behind later widget layers on some modded render pipelines.
        Text modelLabel = Text.translatable("emf_distance_cutoff.global_distance_label");
        Text pauseLabel = Text.translatable("emf_distance_cutoff.animation_pause_distance_label");
        addDrawableChild(new TextWidget(left + 2, GLOBAL_LABEL_Y, this.textRenderer.getWidth(modelLabel), 9,
                modelLabel, this.textRenderer));
        addDrawableChild(new TextWidget(left + 2, ANIMATION_LABEL_Y, this.textRenderer.getWidth(pauseLabel), 9,
                pauseLabel, this.textRenderer));

        globalDistanceField = createNumberField(left, GLOBAL_FIELD_Y,
                Text.translatable("emf_distance_cutoff.global_distance"),
                format(config.cutoffDistanceBlocks),
                Text.translatable("emf_distance_cutoff.distance_placeholder"));
        addDrawableChild(globalDistanceField);

        animationPauseDistanceField = createNumberField(left, ANIMATION_FIELD_Y,
                Text.translatable("emf_distance_cutoff.animation_pause_distance"),
                format(config.animationPauseDistanceBlocks),
                Text.translatable("emf_distance_cutoff.animation_pause_placeholder"));
        addDrawableChild(animationPauseDistanceField);

        searchField = new TextFieldWidget(this.textRenderer, left, SEARCH_Y, fieldWidth, 20,
                Text.translatable("emf_distance_cutoff.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("emf_distance_cutoff.search_placeholder"));
        searchField.setChangedListener(value -> { if (entityList != null) entityList.rebuild(value); });
        addDrawableChild(searchField);

        int listTop = ENTITY_LABEL_Y + 14;
        int listBottom = this.height - 48;
        int listWidth = fieldWidth;
        entityList = new EntityListWidget(this.client, listWidth, Math.max(80, listBottom - listTop), listTop, 28, this);
        entityList.setX(left);
        entityList.rebuild(searchField.getText());
        addDrawableChild(entityList);

        int buttonY = this.height - 40;
        int buttonMargin = Math.max(8, Math.min(40, this.width / 12));
        int availableButtonWidth = this.width - buttonMargin * 2;
        int resetWidth = 230;
        int actionWidth = 130;
        int gap = 10;
        int preferredButtonWidth = resetWidth + actionWidth * 2 + gap * 2;
        if (availableButtonWidth < preferredButtonWidth) {
            gap = 4;
            resetWidth = Math.max(120, availableButtonWidth * 2 / 5);
            actionWidth = Math.max(1, (availableButtonWidth - resetWidth - gap * 2) / 2);
        }
        int totalWidth = resetWidth + actionWidth * 2 + gap * 2;
        int buttonLeft = center - totalWidth / 2;

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.reset_all"), button -> {
            config.resetAll();
            globalDistanceField.setText(format(config.cutoffDistanceBlocks));
            animationPauseDistanceField.setText(format(config.animationPauseDistanceBlocks));
            if (entityList != null) entityList.rebuild(searchField.getText());
            CutoffConfig.save();
        }).dimensions(buttonLeft, buttonY, resetWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.save"), button -> saveAndClose())
                .dimensions(buttonLeft + resetWidth + gap, buttonY, actionWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.cancel"), button -> close())
                .dimensions(buttonLeft + resetWidth + gap + actionWidth + gap, buttonY, actionWidth, 20).build());
    }

    private TextFieldWidget createNumberField(int x, int y, Text message, String value, Text placeholder) {
        int horizontalMargin = Math.min(60, Math.max(12, this.width / 8));
        int fieldWidth = Math.min(760, Math.max(180, this.width - horizontalMargin * 2));
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, fieldWidth, 20, message);
        field.setMaxLength(12);
        field.setText(value);
        field.setTextPredicate(input -> input.matches("[0-9]*([.][0-9]*)?"));
        field.setPlaceholder(placeholder);
        return field;
    }

    private void saveAndClose() {
        Double modelDistance = parsePositiveOrZero(globalDistanceField.getText());
        Double pauseDistance = parsePositiveOrZero(animationPauseDistanceField.getText());
        if (modelDistance != null && pauseDistance != null) {
            config.cutoffDistanceBlocks = modelDistance;
            config.animationPauseDistanceBlocks = pauseDistance;
            CutoffConfig.save();
            close();
        }
    }

    private static Double parsePositiveOrZero(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) { return null; }
    }

    static String format(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void close() { if (this.client != null) this.client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}
