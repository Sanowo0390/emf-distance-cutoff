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
        // Custom mode is intentionally the default when opening the screen so the field is immediately editable.
        useGlobal = override != null && override.distanceBlocks == null;
        useGlobalAnimationPause = override == null || override.animationPauseDistanceBlocks == null;

        int center = this.width / 2;
        int contentWidth = Math.min(520, Math.max(300, this.width - 80));
        int left = center - contentWidth / 2;
        int top = Math.max(30, (this.height - 330) / 2);

        addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
            enabled = !enabled;
            button.setMessage(enabledText());
        }).dimensions(left, top + 54, contentWidth, 20).build());

        distanceField = createNumberField(left, top + 112,
                Text.translatable("emf_distance_cutoff.custom_distance"),
                override != null && override.distanceBlocks != null
                        ? ConfigScreen.format(override.distanceBlocks)
                        : ConfigScreen.format(config.cutoffDistanceBlocks),
                Text.translatable("emf_distance_cutoff.distance_placeholder"));
        // Keep this immediately editable when entering the screen.
        distanceField.active = true;
        addDrawableChild(distanceField);

        addDrawableChild(ButtonWidget.builder(modeButtonText(), button -> {
            useGlobal = !useGlobal;
            if (useGlobal) distanceField.setText(ConfigScreen.format(config.cutoffDistanceBlocks));
            distanceField.active = !useGlobal;
            distanceField.setFocused(!useGlobal);
            button.setMessage(modeButtonText());
        }).dimensions(left, top + 141, contentWidth, 20).build());

        animationPauseDistanceField = createNumberField(left, top + 195,
                Text.translatable("emf_distance_cutoff.animation_pause_distance"),
                override != null && override.animationPauseDistanceBlocks != null
                        ? ConfigScreen.format(override.animationPauseDistanceBlocks)
                        : ConfigScreen.format(config.animationPauseDistanceBlocks),
                Text.translatable("emf_distance_cutoff.animation_pause_placeholder"));
        animationPauseDistanceField.active = true;
        addDrawableChild(animationPauseDistanceField);

        addDrawableChild(ButtonWidget.builder(animationModeButtonText(), button -> {
            useGlobalAnimationPause = !useGlobalAnimationPause;
            if (useGlobalAnimationPause) {
                animationPauseDistanceField.setText(ConfigScreen.format(config.animationPauseDistanceBlocks));
            }
            animationPauseDistanceField.active = !useGlobalAnimationPause;
            animationPauseDistanceField.setFocused(!useGlobalAnimationPause);
            button.setMessage(animationModeButtonText());
        }).dimensions(left, top + 224, contentWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.reset"), button -> {
            config.resetOverride(entityId.toString());
            CutoffConfig.save();
            if (this.client != null) this.client.setScreen(parent);
        }).dimensions(left, top + 253, contentWidth, 20).build());

        int half = (contentWidth - 8) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.save"), button -> save())
                .dimensions(left, top + 282, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.cancel"), button -> close())
                .dimensions(left + half + 8, top + 282, half, 20).build());
    }

    private TextFieldWidget createNumberField(int x, int y, Text message, String value, Text placeholder) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, Math.min(520, Math.max(300, this.width - 80)), 20, message);
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
        return Text.translatable(useGlobal
                ? "emf_distance_cutoff.use_custom_with_value"
                : "emf_distance_cutoff.use_global_with_value",
                ConfigScreen.format(useGlobal ? config.cutoffDistanceBlocks : parseFieldOrGlobal()));
    }

    private Text animationModeButtonText() {
        return Text.translatable(useGlobalAnimationPause
                ? "emf_distance_cutoff.use_custom_animation_pause_with_value"
                : "emf_distance_cutoff.use_global_animation_pause_with_value",
                ConfigScreen.format(useGlobalAnimationPause ? config.animationPauseDistanceBlocks : parseAnimationPauseFieldOrGlobal()));
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
        int top = Math.max(30, (this.height - 330) / 2);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, top + 5, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(entityId.toString()), center, top + 24, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.enabled_label"), center, top + 42, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.custom_distance_label"), center, top + 100, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.animation_pause_distance_label"), center, top + 183, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
