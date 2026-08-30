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
    private boolean enabled;
    private boolean useGlobal;

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
        useGlobal = override == null || override.distanceBlocks == null;

        int center = this.width / 2;
        int contentWidth = Math.min(320, Math.max(200, this.width - 80));
        int left = center - contentWidth / 2;
        int top = Math.max(48, (this.height - 250) / 2);

        addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
            enabled = !enabled;
            button.setMessage(enabledText());
        }).dimensions(left, top + 54, contentWidth, 20).build());

        distanceField = new TextFieldWidget(this.textRenderer, left, top + 112, contentWidth, 20,
                Text.translatable("emf_distance_cutoff.custom_distance"));
        distanceField.setMaxLength(12);
        distanceField.setTextPredicate(value -> value.matches("[0-9]*([.][0-9]*)?"));
        distanceField.setPlaceholder(Text.translatable("emf_distance_cutoff.distance_placeholder"));
        distanceField.setText(useGlobal ? ConfigScreen.format(config.cutoffDistanceBlocks) : ConfigScreen.format(override.distanceBlocks));
        distanceField.active = !useGlobal;
        addDrawableChild(distanceField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.use_custom_with_value", ConfigScreen.format(config.cutoffDistanceBlocks)), button -> {
            useGlobal = false;
            distanceField.active = true;
            distanceField.setFocused(true);
        }).dimensions(left, top + 141, contentWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.use_global_with_value", ConfigScreen.format(config.cutoffDistanceBlocks)), button -> {
            useGlobal = true;
            distanceField.setText(ConfigScreen.format(config.cutoffDistanceBlocks));
            distanceField.active = false;
        }).dimensions(left, top + 170, contentWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.reset"), button -> {
            config.resetOverride(entityId.toString());
            CutoffConfig.save();
            if (this.client != null) this.client.setScreen(parent);
        }).dimensions(left, top + 199, contentWidth, 20).build());

        int half = (contentWidth - 8) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.save"), button -> save()).dimensions(left, top + 228, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.cancel"), button -> close()).dimensions(left + half + 8, top + 228, half, 20).build());
    }

    private Text enabledText() {
        return Text.translatable("emf_distance_cutoff.enabled_label")
                .append(Text.literal(": "))
                .append(Text.translatable(enabled ? "emf_distance_cutoff.on" : "emf_distance_cutoff.off"));
    }

    private void save() {
        CutoffConfig.EntityOverride override = config.getOrCreateOverride(entityId.toString());
        override.enabled = enabled;
        if (useGlobal) override.distanceBlocks = null;
        else {
            Double parsed = parsePositiveOrZero(distanceField.getText());
            if (parsed == null) return;
            override.distanceBlocks = parsed;
        }
        CutoffConfig.save();
        close();
    }

    private static Double parsePositiveOrZero(String value) {
        try { double parsed = Double.parseDouble(value); return Double.isFinite(parsed) && parsed >= 0 ? parsed : null; }
        catch (NumberFormatException e) { return null; }
    }

    @Override
    public void close() { if (this.client != null) this.client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int center = this.width / 2;
        int top = Math.max(48, (this.height - 250) / 2);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, top + 5, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(entityId.toString()), center, top + 24, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.enabled_label"), center, top + 42, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.custom_distance_label"), center, top + 100, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
