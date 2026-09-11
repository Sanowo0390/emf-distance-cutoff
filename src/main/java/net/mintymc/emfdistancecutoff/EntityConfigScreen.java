package net.mintymc.emfdistancecutoff;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class EntityConfigScreen extends Screen {
    private final Screen parent;
    private final Identifier entityId;
    private final CutoffConfig config;
    private TextFieldWidget distanceField;
    private TextFieldWidget animationPauseDistanceField;
    private boolean enabled;
    private boolean useGlobal;
    private boolean useGlobalAnimationPause;

    public EntityConfigScreen(Screen parent, Identifier entityId) {
        super(titleFor(entityId));
        this.parent = parent;
        this.entityId = entityId;
        this.config = CutoffConfig.get();
    }

    private static Text titleFor(Identifier entityId) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
        return type != null ? type.getName() : Text.literal(entityId.toString());
    }

    @Override
    protected void init() {
        super.init();

        CutoffConfig.EntityOverride override = config.getOverride(entityId.toString());
        enabled = override == null || override.enabled;
        // Custom mode is intentionally the default when opening the screen so the fields are immediately editable.
        // New entries use the global values.  Users explicitly opt in to each
        // individual value with the toggle, matching Sodium's toggle behavior.
        useGlobal = override == null || override.distanceBlocks == null;
        useGlobalAnimationPause = override == null || override.animationPauseDistanceBlocks == null;

        int center = this.width / 2;
        boolean compact = this.height < 340;
        int contentWidth = Math.min(520, Math.max(180, this.width - 24));
        int left = center - contentWidth / 2;
        int top = compact ? 6 : Math.max(30, (this.height - 330) / 2);
        int enabledY = top + (compact ? 40 : 54);
        int modelLabelY = top + (compact ? 66 : 100);
        int modelFieldY = top + (compact ? 78 : 112);
        int modelModeY = top + (compact ? 102 : 141);
        int pauseLabelY = top + (compact ? 128 : 183);
        int pauseFieldY = top + (compact ? 140 : 195);
        int pauseModeY = top + (compact ? 164 : 224);
        int resetY = top + (compact ? 188 : 253);
        int actionsY = top + (compact ? 212 : 282);

        addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
            enabled = !enabled;
            button.setMessage(enabledText());
        }).dimensions(left, enabledY, contentWidth, 20).build());

        distanceField = createNumberField(left, modelFieldY,
                Text.translatable("emf_distance_cutoff.custom_distance"),
                override != null && override.distanceBlocks != null
                        ? ConfigScreen.format(override.distanceBlocks)
                        : ConfigScreen.format(config.cutoffDistanceBlocks),
                Text.translatable("emf_distance_cutoff.distance_placeholder"));
        updateDistanceFieldState();
        addDrawableChild(distanceField);

        addDrawableChild(ButtonWidget.builder(modeButtonText(), button -> {
            useGlobal = !useGlobal;
            if (useGlobal) distanceField.setText(ConfigScreen.format(config.cutoffDistanceBlocks));
            updateDistanceFieldState();
            distanceField.setFocused(!useGlobal);
            button.setMessage(modeButtonText());
        }).dimensions(left, modelModeY, contentWidth, 20).build());

        animationPauseDistanceField = createNumberField(left, pauseFieldY,
                Text.translatable("emf_distance_cutoff.animation_pause_distance"),
                override != null && override.animationPauseDistanceBlocks != null
                        ? ConfigScreen.format(override.animationPauseDistanceBlocks)
                        : ConfigScreen.format(config.animationPauseDistanceBlocks),
                Text.translatable("emf_distance_cutoff.animation_pause_placeholder"));
        updateAnimationPauseFieldState();
        addDrawableChild(animationPauseDistanceField);

        addDrawableChild(ButtonWidget.builder(animationModeButtonText(), button -> {
            useGlobalAnimationPause = !useGlobalAnimationPause;
            if (useGlobalAnimationPause) {
                animationPauseDistanceField.setText(ConfigScreen.format(config.animationPauseDistanceBlocks));
            }
            updateAnimationPauseFieldState();
            animationPauseDistanceField.setFocused(!useGlobalAnimationPause);
            button.setMessage(animationModeButtonText());
        }).dimensions(left, pauseModeY, contentWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.reset"), button -> {
            config.resetOverride(entityId.toString());
            CutoffConfig.save();
            if (this.client != null) this.client.setScreen(parent);
        }).dimensions(left, resetY, contentWidth, 20).build());

        int half = (contentWidth - 8) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.save"), button -> save())
                .dimensions(left, actionsY, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.cancel"), button -> close())
                .dimensions(left + half + 8, actionsY, half, 20).build());
    }

    private TextFieldWidget createNumberField(int x, int y, Text message, String value, Text placeholder) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, Math.min(520, Math.max(180, this.width - 24)), 20, message);
        field.setMaxLength(12);
        field.setTextPredicate(input -> input.matches("[0-9]*([.][0-9]*)?"));
        field.setPlaceholder(placeholder);
        field.setText(value);
        return field;
    }

    private Text enabledText() {
        return Text.translatable("emf_distance_cutoff.enabled_label")
                .append(Text.literal(": "))
                .append(Text.translatable(enabled ? "emf_distance_cutoff.on" : "emf_distance_cutoff.off"));
    }

    private Text modeButtonText() {
        return Text.translatable("emf_distance_cutoff.individual_model_toggle",
                Text.translatable(useGlobal ? "emf_distance_cutoff.off" : "emf_distance_cutoff.on"));
    }

    private Text animationModeButtonText() {
        return Text.translatable("emf_distance_cutoff.individual_pause_toggle",
                Text.translatable(useGlobalAnimationPause ? "emf_distance_cutoff.off" : "emf_distance_cutoff.on"));
    }

    private void updateDistanceFieldState() {
        distanceField.active = !useGlobal;
        distanceField.setEditable(!useGlobal);
    }

    private void updateAnimationPauseFieldState() {
        animationPauseDistanceField.active = !useGlobalAnimationPause;
        animationPauseDistanceField.setEditable(!useGlobalAnimationPause);
    }

    private double parseFieldOrGlobal() {
        try {
            double value = Double.parseDouble(distanceField.getText());
            return Double.isFinite(value) ? value : config.cutoffDistanceBlocks;
        } catch (NumberFormatException e) {
            return config.cutoffDistanceBlocks;
        }
    }

    private double parseAnimationPauseFieldOrGlobal() {
        try {
            double value = Double.parseDouble(animationPauseDistanceField.getText());
            return Double.isFinite(value) ? value : config.animationPauseDistanceBlocks;
        } catch (NumberFormatException e) {
            return config.animationPauseDistanceBlocks;
        }
    }

    private void save() {
        CutoffConfig.EntityOverride override = config.getOrCreateOverride(entityId.toString());
        override.enabled = enabled;

        Double parsedDistance = parsePositiveOrZero(distanceField.getText());
        Double parsedAnimationPause = parsePositiveOrZero(animationPauseDistanceField.getText());
        if (parsedDistance == null || parsedAnimationPause == null) return;

        override.distanceBlocks = useGlobal ? null : parsedDistance;
        override.animationPauseDistanceBlocks = useGlobalAnimationPause ? null : parsedAnimationPause;
        CutoffConfig.save();
        close();
    }

    private static Double parsePositiveOrZero(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int center = this.width / 2;
        boolean compact = this.height < 340;
        int top = compact ? 6 : Math.max(30, (this.height - 330) / 2);
        int enabledLabelY = top + (compact ? 28 : 42);
        int modelLabelY = top + (compact ? 66 : 100);
        int pauseLabelY = top + (compact ? 128 : 183);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, top + 5, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(entityId.toString()), center, top + 24, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.enabled_label"), center, enabledLabelY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.custom_distance_label"), center, modelLabelY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.animation_pause_distance_label"), center, pauseLabelY, 0xFFFFFF);
    }
}
