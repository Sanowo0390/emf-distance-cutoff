package net.mintymc.emfdistancecutoff;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public final class EntityConfigScreen extends Screen {
    private static final int CONTENT_WIDTH = 360;
    private static final int CONTENT_HEIGHT = 350;

    private final Screen parent;
    private final Identifier entityId;
    private final CutoffConfig config;
    private EditBox distanceField;
    private EditBox animationPauseDistanceField;
    private boolean enabled;
    private boolean useGlobal;
    private boolean useGlobalAnimationPause;

    public EntityConfigScreen(Screen parent, Identifier entityId) {
        super(titleFor(entityId));
        this.parent = parent;
        this.entityId = entityId;
        this.config = CutoffConfig.get();
    }

    private static Component titleFor(Identifier entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId)
                .map(reference -> reference.value()).orElse(null);
        return type != null ? type.getDescription() : Component.literal(entityId.toString());
    }

    private int contentLeft() {
        return (this.width - Math.min(CONTENT_WIDTH, this.width - 40)) / 2;
    }

    private int contentWidth() {
        return Math.min(CONTENT_WIDTH, Math.max(160, this.width - 40));
    }

    private int contentTop() {
        return Math.max(12, (this.height - CONTENT_HEIGHT) / 2);
    }

    @Override
    protected void init() {
        super.init();

        CutoffConfig.EntityOverride override = config.getOverride(entityId.toString());
        enabled = override == null || override.enabled;
        // Custom mode is intentionally the default when opening the screen so the field is immediately editable.
        // A new entry opens in directly-editable custom mode, while a saved
        // entry that explicitly uses the global value restores that state.
        useGlobal = override != null && override.distanceBlocks == null;
        useGlobalAnimationPause = override != null && override.animationPauseDistanceBlocks == null;

        int left = contentLeft();
        int width = contentWidth();
        int top = contentTop();

        addRenderableWidget(Button.builder(enabledText(), button -> {
            enabled = !enabled;
            button.setMessage(enabledText());
        }).bounds(left, top + 58, width, 20).build());

        distanceField = createNumberField(left, top + 116,
                Component.translatable("emf_distance_cutoff.custom_distance"),
                override != null && override.distanceBlocks != null
                        ? ConfigScreen.format(override.distanceBlocks)
                        : ConfigScreen.format(config.cutoffDistanceBlocks),
                Component.translatable("emf_distance_cutoff.distance_placeholder"));
        // Keep this immediately editable when entering the screen.
        distanceField.setEditable(true);
        addRenderableWidget(distanceField);

        addRenderableWidget(Button.builder(modeButtonText(), button -> {
            useGlobal = !useGlobal;
            if (useGlobal) distanceField.setValue(ConfigScreen.format(config.cutoffDistanceBlocks));
            distanceField.setEditable(!useGlobal);
            distanceField.setFocused(!useGlobal);
            button.setMessage(modeButtonText());
        }).bounds(left, top + 145, width, 20).build());

        animationPauseDistanceField = createNumberField(left, top + 202,
                Component.translatable("emf_distance_cutoff.animation_pause_distance"),
                override != null && override.animationPauseDistanceBlocks != null
                        ? ConfigScreen.format(override.animationPauseDistanceBlocks)
                        : ConfigScreen.format(config.animationPauseDistanceBlocks),
                Component.translatable("emf_distance_cutoff.animation_pause_placeholder"));
        animationPauseDistanceField.setEditable(true);
        addRenderableWidget(animationPauseDistanceField);

        addRenderableWidget(Button.builder(animationModeButtonText(), button -> {
            useGlobalAnimationPause = !useGlobalAnimationPause;
            if (useGlobalAnimationPause) {
                animationPauseDistanceField.setValue(ConfigScreen.format(config.animationPauseDistanceBlocks));
            }
            animationPauseDistanceField.setEditable(!useGlobalAnimationPause);
            animationPauseDistanceField.setFocused(!useGlobalAnimationPause);
            button.setMessage(animationModeButtonText());
        }).bounds(left, top + 231, width, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.reset"), button -> {
            config.resetOverride(entityId.toString());
            CutoffConfig.save();
            onClose();
        }).bounds(left, top + 260, width, 20).build());

        int half = (width - 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.save"), button -> save())
                .bounds(left, top + 289, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.cancel"), button -> onClose())
                .bounds(left + half + 8, top + 289, half, 20).build());
    }

    private EditBox createNumberField(int x, int y, Component message, String value, Component hint) {
        EditBox field = new EditBox(this.font, x, y, contentWidth(), 20, message);
        field.setMaxLength(12);
        field.setValue(value);
        field.setHint(hint);
        return field;
    }

    private Component enabledText() {
        return Component.translatable("emf_distance_cutoff.enabled_label")
                .append(Component.literal(": "))
                .append(Component.translatable(enabled ? "emf_distance_cutoff.on" : "emf_distance_cutoff.off"));
    }

    private Component modeButtonText() {
        return Component.translatable(useGlobal
                ? "emf_distance_cutoff.use_custom_with_value"
                : "emf_distance_cutoff.use_global_with_value",
                ConfigScreen.format(useGlobal ? config.cutoffDistanceBlocks : parseFieldOrGlobal()));
    }

    private Component animationModeButtonText() {
        return Component.translatable(useGlobalAnimationPause
                ? "emf_distance_cutoff.use_custom_animation_pause_with_value"
                : "emf_distance_cutoff.use_global_animation_pause_with_value",
                ConfigScreen.format(useGlobalAnimationPause ? config.animationPauseDistanceBlocks : parseAnimationPauseFieldOrGlobal()));
    }

    private double parseFieldOrGlobal() {
        try {
            double value = Double.parseDouble(distanceField.getValue());
            return Double.isFinite(value) ? value : config.cutoffDistanceBlocks;
        } catch (NumberFormatException e) {
            return config.cutoffDistanceBlocks;
        }
    }

    private double parseAnimationPauseFieldOrGlobal() {
        try {
            double value = Double.parseDouble(animationPauseDistanceField.getValue());
            return Double.isFinite(value) ? value : config.animationPauseDistanceBlocks;
        } catch (NumberFormatException e) {
            return config.animationPauseDistanceBlocks;
        }
    }

    private void save() {
        CutoffConfig.EntityOverride override = config.getOrCreateOverride(entityId.toString());
        override.enabled = enabled;

        Double parsedDistance = parsePositiveOrZero(distanceField.getValue());
        Double parsedAnimationPause = parsePositiveOrZero(animationPauseDistanceField.getValue());
        if (parsedDistance == null || parsedAnimationPause == null) return;

        override.distanceBlocks = useGlobal ? null : parsedDistance;
        override.animationPauseDistanceBlocks = useGlobalAnimationPause ? null : parsedAnimationPause;
        CutoffConfig.save();
        onClose();
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
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int center = this.width / 2;
        int top = contentTop();
        graphics.centeredText(this.font, this.title, center, top + 6, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal(entityId.toString()), center, top + 25, 0xFFAAAAAA);
        graphics.centeredText(this.font, Component.translatable("emf_distance_cutoff.enabled_label"), center, top + 47, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("emf_distance_cutoff.custom_distance_label"), center, top + 103, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("emf_distance_cutoff.animation_pause_distance_label"), center, top + 189, 0xFFFFFFFF);
    }
}
