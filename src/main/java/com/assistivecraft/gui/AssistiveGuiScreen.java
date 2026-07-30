package com.assistivecraft.gui;

import com.assistivecraft.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabbed settings screen. Each category tab shows its own list of features,
 * and every feature gets one dedicated ON/OFF button — no shared master
 * toggles. Some features additionally expose a slider for a numeric value.
 */
public class AssistiveGuiScreen extends Screen {

    private enum Category { COMBAT, OUTLINES, AUTOMATION, UTILITY }

    private Category activeCategory = Category.COMBAT;
    private final List<ButtonWidget> categoryButtons = new ArrayList<>();
    private final List<Object> featureWidgets = new ArrayList<>(); // buttons + sliders for current category

    public AssistiveGuiScreen() {
        super(Text.literal("AssistiveCraft"));
    }

    @Override
    protected void init() {
        super.init();
        categoryButtons.clear();

        int tabWidth = 110;
        int startX = this.width / 2 - (tabWidth * 4 + 12) / 2;
        int y = 24;

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            ButtonWidget tab = ButtonWidget.builder(Text.literal(labelFor(cat)), b -> switchCategory(cat))
                    .dimensions(startX + i * (tabWidth + 4), y, tabWidth, 20)
                    .build();
            categoryButtons.add(tab);
            this.addDrawableChild(tab);
        }

        buildFeatureWidgets();
    }

    private String labelFor(Category cat) {
        return switch (cat) {
            case COMBAT -> "Combat";
            case OUTLINES -> "Outlines / ESP";
            case AUTOMATION -> "Automation";
            case UTILITY -> "Utility";
        };
    }

    private void switchCategory(Category cat) {
        this.activeCategory = cat;
        // Remove old feature widgets, rebuild for the newly selected category.
        for (Object w : featureWidgets) {
            if (w instanceof ButtonWidget bw) this.remove(bw);
            if (w instanceof SliderWidget sw) this.remove(sw);
        }
        featureWidgets.clear();
        buildFeatureWidgets();
    }

    private void buildFeatureWidgets() {
        ModuleManager mm = ModuleManager.INSTANCE;
        int listY = 60;
        int rowHeight = 24;
        int width = 240;
        int x = this.width / 2 - width / 2;

        List<FeatureRow> rows = new ArrayList<>();
        switch (activeCategory) {
            case COMBAT -> {
                rows.add(toggleRow("Aim Assist", () -> mm.aimAssistEnabled, v -> mm.aimAssistEnabled = v));
                rows.add(sliderRow("Aim Assist FOV", mm.aimAssistFovDegrees, 1, 60,
                        v -> mm.aimAssistFovDegrees = v));
                rows.add(sliderRow("Aim Assist Speed", mm.aimAssistSpeed, 0.01f, 1.0f,
                        v -> mm.aimAssistSpeed = v));
                rows.add(toggleRow("Auto Attack", () -> mm.autoAttackEnabled, v -> mm.autoAttackEnabled = v));
                rows.add(toggleRow("Auto Totem", () -> mm.autoTotemEnabled, v -> mm.autoTotemEnabled = v));
            }
            case OUTLINES -> {
                rows.add(toggleRow("Mob Outline", () -> mm.mobOutlineEnabled, v -> mm.mobOutlineEnabled = v));
                rows.add(toggleRow("Diamond Outline", () -> mm.diamondOutlineEnabled,
                        v -> mm.diamondOutlineEnabled = v));
                rows.add(sliderRow("Diamond Outline Radius", mm.diamondOutlineRadius, 4, 32,
                        v -> mm.diamondOutlineRadius = Math.round(v)));
            }
            case AUTOMATION -> {
                rows.add(toggleRow("Auto Eat", () -> mm.autoEatEnabled, v -> mm.autoEatEnabled = v));
                rows.add(sliderRow("Auto Eat Hunger Threshold", mm.autoEatHungerThreshold, 1, 19,
                        v -> mm.autoEatHungerThreshold = v));
                rows.add(toggleRow("Fall Mitigation", () -> mm.fallMitigationEnabled,
                        v -> mm.fallMitigationEnabled = v));
            }
            case UTILITY -> {
                rows.add(toggleRow("Projectile Path", () -> mm.projectilePathEnabled,
                        v -> mm.projectilePathEnabled = v));
                rows.add(toggleRow("High Contrast Mode", () -> mm.highContrastMode,
                        v -> mm.highContrastMode = v));
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            FeatureRow row = rows.get(i);
            int rowY = listY + i * rowHeight;

            if (row.isToggle) {
                ButtonWidget btn = ButtonWidget.builder(
                                Text.literal(row.label + ": " + (row.toggleGetter.get() ? "ON" : "OFF")),
                                b -> {
                                    boolean newVal = !row.toggleGetter.get();
                                    row.toggleSetter.accept(newVal);
                                    b.setMessage(Text.literal(row.label + ": " + (newVal ? "ON" : "OFF")));
                                })
                        .dimensions(x, rowY, width, 20)
                        .build();
                featureWidgets.add(btn);
                this.addDrawableChild(btn);
            } else {
                FeatureSlider slider = new FeatureSlider(x, rowY, width, 20, row);
                featureWidgets.add(slider);
                this.addDrawableChild(slider);
            }
        }
    }

    private FeatureRow toggleRow(String label, java.util.function.Supplier<Boolean> getter,
                                  java.util.function.Consumer<Boolean> setter) {
        FeatureRow row = new FeatureRow();
        row.label = label;
        row.isToggle = true;
        row.toggleGetter = getter;
        row.toggleSetter = setter;
        return row;
    }

    private FeatureRow sliderRow(String label, float current, float min, float max,
                                  java.util.function.Consumer<Float> setter) {
        FeatureRow row = new FeatureRow();
        row.label = label;
        row.isToggle = false;
        row.sliderMin = min;
        row.sliderMax = max;
        row.sliderCurrent = current;
        row.sliderSetter = setter;
        return row;
    }

    @Override
    public boolean shouldPause() {
        return false; // stay live so players can preview changes without freezing the world
    }

    private static class FeatureRow {
        String label;
        boolean isToggle;
        java.util.function.Supplier<Boolean> toggleGetter;
        java.util.function.Consumer<Boolean> toggleSetter;
        float sliderMin, sliderMax, sliderCurrent;
        java.util.function.Consumer<Float> sliderSetter;
    }

    private static class FeatureSlider extends SliderWidget {
        private final FeatureRow row;

        FeatureSlider(int x, int y, int width, int height, FeatureRow row) {
            super(x, y, width, height,
                    Text.literal(row.label + ": " + String.format("%.2f", row.sliderCurrent)),
                    normalize(row.sliderCurrent, row.sliderMin, row.sliderMax));
            this.row = row;
        }

        private static double normalize(float value, float min, float max) {
            return (value - min) / (max - min);
        }

        @Override
        protected void updateMessage() {
            float value = row.sliderMin + (float) this.value * (row.sliderMax - row.sliderMin);
            this.setMessage(Text.literal(row.label + ": " + String.format("%.2f", value)));
        }

        @Override
        protected void applyValue() {
            float value = row.sliderMin + (float) this.value * (row.sliderMax - row.sliderMin);
            row.sliderSetter.accept(value);
        }
    }
}
