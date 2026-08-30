package net.mintymc.emfdistancecutoff;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ConfigScreen extends Screen {
    private static final int TITLE_Y = 14;
    private static final int GLOBAL_LABEL_Y = 40;
    private static final int GLOBAL_FIELD_Y = 56;
    private static final int ENTITY_LABEL_Y = 83;
    private static final int SEARCH_Y = 100;

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
        int fieldWidth = Math.min(320, Math.max(180, this.width - 80));
        int left = center - fieldWidth / 2;

        globalDistanceField = new TextFieldWidget(this.textRenderer, left, GLOBAL_FIELD_Y, fieldWidth, 20,
                Text.translatable("emf_distance_cutoff.global_distance"));
        globalDistanceField.setMaxLength(12);
        globalDistanceField.setText(format(config.cutoffDistanceBlocks));
        globalDistanceField.setTextPredicate(value -> value.matches("[0-9]*([.][0-9]*)?"));
        globalDistanceField.setPlaceholder(Text.translatable("emf_distance_cutoff.distance_placeholder"));
        addDrawableChild(globalDistanceField);

        searchField = new TextFieldWidget(this.textRenderer, left, SEARCH_Y, fieldWidth, 20,
                Text.translatable("emf_distance_cutoff.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("emf_distance_cutoff.search_placeholder"));
        searchField.setChangedListener(value -> { if (entityList != null) entityList.rebuild(value); });
        addDrawableChild(searchField);

        int listTop = SEARCH_Y + 28;
        int listBottom = this.height - 64;
        int listWidth = Math.min(760, Math.max(220, this.width - 80));
        entityList = new EntityListWidget(this.client, listWidth, Math.max(80, listBottom - listTop), listTop, 28, this);
        entityList.rebuild(searchField.getText());
        addDrawableChild(entityList);

        int buttonY = this.height - 50;
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.reset_all"), button -> {
            config.resetAll();
            globalDistanceField.setText(format(config.cutoffDistanceBlocks));
            if (entityList != null) entityList.rebuild(searchField.getText());
            CutoffConfig.save();
        }).dimensions(center - 155, buttonY, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.save"), button -> saveAndClose())
                .dimensions(center + 5, buttonY, 75, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("emf_distance_cutoff.cancel"), button -> close())
                .dimensions(center + 85, buttonY, 75, 20).build());
    }

    private void saveAndClose() {
        Double value = parsePositiveOrZero(globalDistanceField.getText());
        if (value != null) {
            config.cutoffDistanceBlocks = value;
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
        int center = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, TITLE_Y, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.global_distance_label"), center, GLOBAL_LABEL_Y, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("emf_distance_cutoff.entities"), center, ENTITY_LABEL_Y, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
