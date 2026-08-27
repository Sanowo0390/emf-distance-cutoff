package net.mintymc.emfdistancecutoff;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ConfigScreen extends Screen {
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
        int top = 34;

        globalDistanceField = new EditBox(this.font, center - 100, top + 35, 200, 20,
                Component.translatable("emf_distance_cutoff.global_distance"));
        globalDistanceField.setMaxLength(12);
        globalDistanceField.setValue(format(config.cutoffDistanceBlocks));
        addRenderableWidget(globalDistanceField);

        searchField = new EditBox(this.font, center - 150, top + 90, 300, 20,
                Component.translatable("emf_distance_cutoff.search"));
        searchField.setMaxLength(64);
        searchField.setResponder(value -> {
            if (entityList != null) entityList.rebuild(value);
        });
        addRenderableWidget(searchField);

        int listTop = top + 124;
        int listBottom = this.height - 46;
        entityList = new EntityListWidget(Minecraft.getInstance(), this.width,
                Math.max(60, listBottom - listTop), listTop, 24, this);
        entityList.rebuild(searchField.getValue());
        addRenderableWidget(entityList);

        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.save"), button -> saveAndClose())
                .bounds(center - 100, this.height - 34, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("emf_distance_cutoff.cancel"), button -> onClose())
                .bounds(center + 5, this.height - 34, 95, 20).build());
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
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int center = this.width / 2;
        graphics.centeredText(this.font, this.title, center, 14, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.global_distance_label"), center, 62, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("emf_distance_cutoff.entities"), center, 119, 0xFFFFFFFF);
    }
}
