package com.example.core.geometry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common color palette shared between Desktop and Android.
 *
 * <p>Provides canonical color constants and reads theme overrides from {@code /palette.json}.
 * Both Desktop CSS / views and Android Compose / XML resources are synchronized to this palette.
 */
public class AppColorPalette {

    // ── Canonical Color Constants ──────────────────────────────────────────────
    public static final String BACKGROUND           = "#0b1120";
    public static final String SURFACE              = "#111827";
    public static final String SURFACE_HEADER       = "#0d1117";
    public static final String SURFACE_VARIANT      = "#1f2937";
    public static final String BORDER               = "#1f2937";
    public static final String BORDER_STRONG        = "#374151";

    public static final String TEXT_PRIMARY         = "#f9fafb";
    public static final String TEXT_SECONDARY       = "#d1d5db";
    public static final String TEXT_MUTED           = "#9ca3af";

    public static final String PRIMARY              = "#7c3aed";
    public static final String PRIMARY_DARK         = "#6d28d9";
    public static final String PRIMARY_LIGHT        = "#818cf8";

    public static final String SECONDARY            = "#ec4899";

    public static final String DANGER               = "#ef4444";
    public static final String DANGER_DARK          = "#dc2626";

    public static final String SUCCESS              = "#10b981";

    public static final String SPIN_GRADIENT_START  = "#f59e0b";
    public static final String SPIN_GRADIENT_END    = "#ef4444";

    public static final String SAVED_BADGE          = "#06b6d4";
    public static final String WHEEL_BADGE          = "#3b82f6";

    public static final String POINTER              = "#f43f5e";
    public static final String HUB_OUTER            = "#0b1120";
    public static final String HUB_INNER            = "#7c3aed";

    // ── Singleton Instance ─────────────────────────────────────────────────────
    private static final AppColorPalette INSTANCE = new AppColorPalette();

    private String background = BACKGROUND;
    private String surface = SURFACE;
    private String surfaceHeader = SURFACE_HEADER;
    private String surfaceVariant = SURFACE_VARIANT;
    private String border = BORDER;
    private String borderStrong = BORDER_STRONG;
    private String textPrimary = TEXT_PRIMARY;
    private String textSecondary = TEXT_SECONDARY;
    private String textMuted = TEXT_MUTED;
    private String primary = PRIMARY;
    private String primaryDark = PRIMARY_DARK;
    private String primaryLight = PRIMARY_LIGHT;
    private String secondary = SECONDARY;
    private String danger = DANGER;
    private String dangerDark = DANGER_DARK;
    private String success = SUCCESS;
    private String spinGradientStart = SPIN_GRADIENT_START;
    private String spinGradientEnd = SPIN_GRADIENT_END;
    private String savedBadge = SAVED_BADGE;
    private String wheelBadge = WHEEL_BADGE;
    private String pointer = POINTER;
    private String hubOuter = HUB_OUTER;
    private String hubInner = HUB_INNER;

    public AppColorPalette() {
        reload();
    }

    public static AppColorPalette getInstance() {
        return INSTANCE;
    }

    public final synchronized void reload() {
        try (InputStream is = getClass().getResourceAsStream("/palette.json")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                loadFromJson(sb.toString());
                return;
            }
        } catch (Exception ignored) {
        }
        applyDefaults();
    }

    public synchronized void loadFromJson(String json) {
        if (json == null || json.isBlank()) {
            applyDefaults();
            return;
        }

        this.background = extractColor(json, "background", BACKGROUND);
        this.surface = extractColor(json, "surface", SURFACE);
        this.surfaceHeader = extractColor(json, "surfaceHeader", SURFACE_HEADER);
        this.surfaceVariant = extractColor(json, "surfaceVariant", SURFACE_VARIANT);
        this.border = extractColor(json, "border", BORDER);
        this.borderStrong = extractColor(json, "borderStrong", BORDER_STRONG);
        this.textPrimary = extractColor(json, "textPrimary", TEXT_PRIMARY);
        this.textSecondary = extractColor(json, "textSecondary", TEXT_SECONDARY);
        this.textMuted = extractColor(json, "textMuted", TEXT_MUTED);
        this.primary = extractColor(json, "primary", PRIMARY);
        this.primaryDark = extractColor(json, "primaryDark", PRIMARY_DARK);
        this.primaryLight = extractColor(json, "primaryLight", PRIMARY_LIGHT);
        this.secondary = extractColor(json, "secondary", SECONDARY);
        this.danger = extractColor(json, "danger", DANGER);
        this.dangerDark = extractColor(json, "dangerDark", DANGER_DARK);
        this.success = extractColor(json, "success", SUCCESS);
        this.spinGradientStart = extractColor(json, "spinGradientStart", SPIN_GRADIENT_START);
        this.spinGradientEnd = extractColor(json, "spinGradientEnd", SPIN_GRADIENT_END);
        this.savedBadge = extractColor(json, "savedBadge", SAVED_BADGE);
        this.wheelBadge = extractColor(json, "wheelBadge", WHEEL_BADGE);
        this.pointer = extractColor(json, "pointerColor", POINTER);
        this.hubOuter = extractColor(json, "hubOuterColor", HUB_OUTER);
        this.hubInner = extractColor(json, "hubInnerColor", HUB_INNER);
    }

    private void applyDefaults() {
        this.background = BACKGROUND;
        this.surface = SURFACE;
        this.surfaceHeader = SURFACE_HEADER;
        this.surfaceVariant = SURFACE_VARIANT;
        this.border = BORDER;
        this.borderStrong = BORDER_STRONG;
        this.textPrimary = TEXT_PRIMARY;
        this.textSecondary = TEXT_SECONDARY;
        this.textMuted = TEXT_MUTED;
        this.primary = PRIMARY;
        this.primaryDark = PRIMARY_DARK;
        this.primaryLight = PRIMARY_LIGHT;
        this.secondary = SECONDARY;
        this.danger = DANGER;
        this.dangerDark = DANGER_DARK;
        this.success = SUCCESS;
        this.spinGradientStart = SPIN_GRADIENT_START;
        this.spinGradientEnd = SPIN_GRADIENT_END;
        this.savedBadge = SAVED_BADGE;
        this.wheelBadge = WHEEL_BADGE;
        this.pointer = POINTER;
        this.hubOuter = HUB_OUTER;
        this.hubInner = HUB_INNER;
    }

    private String extractColor(String json, String property, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + property + "\"\\s*:\\s*\"(#[0-9a-fA-F]{6})\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return defaultValue;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String getBackground() { return background; }
    public String getSurface() { return surface; }
    public String getSurfaceHeader() { return surfaceHeader; }
    public String getSurfaceVariant() { return surfaceVariant; }
    public String getBorder() { return border; }
    public String getBorderStrong() { return borderStrong; }
    public String getTextPrimary() { return textPrimary; }
    public String getTextSecondary() { return textSecondary; }
    public String getTextMuted() { return textMuted; }
    public String getPrimary() { return primary; }
    public String getPrimaryDark() { return primaryDark; }
    public String getPrimaryLight() { return primaryLight; }
    public String getSecondary() { return secondary; }
    public String getDanger() { return danger; }
    public String getDangerDark() { return dangerDark; }
    public String getSuccess() { return success; }
    public String getSpinGradientStart() { return spinGradientStart; }
    public String getSpinGradientEnd() { return spinGradientEnd; }
    public String getSavedBadge() { return savedBadge; }
    public String getWheelBadge() { return wheelBadge; }
    public String getPointer() { return pointer; }
    public String getHubOuter() { return hubOuter; }
    public String getHubInner() { return hubInner; }
}
