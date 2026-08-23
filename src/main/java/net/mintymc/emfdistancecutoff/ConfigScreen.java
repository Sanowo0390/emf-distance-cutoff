package net.mintymc.emfdistancecutoff;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ConfigScreen extends Screen {
    private final Screen parent;
    private final CutoffConfig config;
    private TextFieldWidget globalDistanceField;
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
        int top = 42;
        globalDistanceField = new TextFieldWidget(this.textRenderer, center - 100, top + 34, 200, 20, Text.translatable("emf_distance_cutoff.global_distance"));
        globalDistanceField.setMaxLength(12);
        globalDistanceField.setText(format(config.cutoffDistanceBlocks));
        globalDistanceField.setTextPredicate(value -> value.matches("[0-9]*([.][0-9]*)?"));
        globalDistanceField.setPlaceholder(Text.translatable("emf_distance_cutoff.distance_placeholder"));
        addDrawableChild(globalDistanceField);

        searchField = new TextFieldWidget(this.textRenderer, center - 150, top + 91, 300, 20, Text.translatable("emf_distance_cutoff.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("emf_distance_cutoff.search_placeholder"));
        searchField.setChangedListener(value -> { if (entityList != null) entityList.rebuild(value); });
        addDrawableChild(searchField);

        int listTop = top + 125;
        int listBottom = this.height - 46;
        entityList = new EntityListWidget(this.client, this.width, Math.max(60, listBottom - listTop), listTop, 28, this);
        entityList.rebuild(searchField.getText());
        addDrawableChild(entityList);

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.save"), button -> saveAndClose()).dimensions(center - 100, this.height - 34, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.cancel"), button -> close()).dimensions(center + 5, this.height - 34, 95, 20).build());
    }

    private void saveAndClose() {
        Double value = parsePositiveOrZero(globalDistanceField.getText());
        if (value != null) { config.cutoffDistanceBlocks = value; CutoffConfig.save(); close(); }
    }

    private static Double parsePositiveOrZero(String value) {
        try { double parsed = Double.parseDouble(value); return Double.isFinite(parsed) && parsed >= 0 ? parsed : null; }
        catch (NumberFormatException e) { return null; }
    }

    static String format(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override public void close() { if (this.client != null) this.client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int center = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.global_distance_label"), center, 62, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.entities"), center, 133, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
