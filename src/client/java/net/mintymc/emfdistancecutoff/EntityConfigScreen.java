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
    private final Screen parent;
    private final Identifier entityId;
    private final CutoffConfig config;
    private EditBox distanceField;
    private boolean enabled;
    private boolean useGlobal;

    public EntityConfigScreen(Screen parent, Identifier entityId) {
        super(titleFor(entityId));
        this.parent = parent;
        this.entityId = entityId;
        this.config = CutoffConfig.get();
    }

    private static Component titleFor(Identifier entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId).map(reference -> reference.value()).orElse(null);
        return type != null ? type.getDescription() : Component.literal(entityId.toString());
    }

    @Override
    protected void init() {
        super.init();

        CutoffConfig.EntityOverride override = config.getOverride(entityId.toString());
        enabled = override == null || override.enabled;
        useGlobal = override == null || override.distanceBlocks == null;

        int center = this.width / 2;
        int fieldWidth = Math.min(320, this.width - 80);
        int top = Math.max(54, this.height / 2 - 100);
        int left = center - fieldWidth / 2;

        addRenderableWidget(Button.builder(enabledText(), button -> {
            enabled = !enabled;
            button.setMessage(enabledText());
        }).bounds(left, top + 34, fieldWidth, 20).build());

        distanceField = new EditBox(this.font, left, top + 94, fieldWidth, 20,
                Component.translatable("emf_distance_cutoff.custom_distance"));
        distanceField.setMaxLength(12);
        distanceField.setValue(useGlobal
                ? ConfigScreen.format(config.cutoffDistanceBlocks)
                : ConfigScreen.format(override.distanceBlocks));
        distanceField.setHint(Component.translatable("emf_distance_cutoff.distance_placeholder"));
        distanceField.setEditable(!useGlobal);
        addRenderableWidget(distanceField);

        addRenderableWidget(Button.builder(globalButtonText(), button -> {
            useGlobal = true;
            distanceField.setValue(ConfigScreen.format(config.cutoffDistanceBlocks));
            distanceField.setEditable(false);
        }).bounds(left, top + 123, fieldWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.reset"), button -> {
            config.resetOverride(entityId.toString());
            CutoffConfig.save();
            onClose();
        }).bounds(left, top + 152, fieldWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.save"), button -> save())
                .bounds(left, top + 181, fieldWidth / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.cancel"), button -> onClose())
                .bounds(center + 4, top + 181, fieldWidth / 2 - 4, 20).build());
    }

    private Component enabledText() {
        return Component.translatable("emf_distance_cutoff.enabled_label")
                .append(Component.literal(": "))
                .append(Component.translatable(enabled
                        ? "emf_distance_cutoff.on"
                        : "emf_distance_cutoff.off"));
    }

    private Component globalButtonText() {
        return Component.translatable("emf_distance_cutoff.use_global_with_value",
                ConfigScreen.format(config.cutoffDistanceBlocks));
    }

    private void save() {
        CutoffConfig.EntityOverride override = config.getOrCreateOverride(entityId.toString());
        override.enabled = enabled;

        if (useGlobal) {
            override.distanceBlocks = null;
        } else {
            Double parsed = parsePositiveOrZero(distanceField.getValue());
            if (parsed == null) return;
            override.distanceBlocks = parsed;
        }

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
        int center = this.width / 2;
        int fieldWidth = Math.min(320, this.width - 80);
        int top = Math.max(54, this.height / 2 - 100);

        graphics.centeredText(this.font, this.title, center, 24, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal(entityId.toString()), center, 43, 0xFFAAAAAA);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.enabled_label"), center, top + 18, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.custom_distance_label"), center, top + 78, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
