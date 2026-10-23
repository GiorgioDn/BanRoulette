package com.example.core.geometry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and provides the curated color palette shared between desktop and mobile.
 *
 * <p>Reads {@code /palette.json} from the classpath on initialization. Changing
 * values in {@code palette.json} automatically updates colors on all platforms.
 */
public class WheelPalette {

    private static final List<String> DEFAULT_SLICE_COLORS = List.of(
            "#6366f1",  // indigo
            "#ec4899",  // pink
            "#f59e0b",  // amber
            "#10b981",  // emerald
            "#3b82f6",  // blue
            "#ef4444",  // red
            "#8b5cf6",  // violet
            "#14b8a6",  // teal
            "#f97316",  // orange
            "#06b6d4",  // cyan
            "#a3e635",  // lime
            "#e879f9"   // fuchsia
    );

    private static final String DEFAULT_POINTER_COLOR   = "#f43f5e";
    private static final String DEFAULT_HUB_OUTER_COLOR = "#0f172a";
    private static final String DEFAULT_HUB_INNER_COLOR = "#6366f1";

    private static final WheelPalette INSTANCE = new WheelPalette();

    private List<String> sliceColors;
    private String pointerColor;
    private String hubOuterColor;
    private String hubInnerColor;

    public WheelPalette() {
        reload();
    }

    public static WheelPalette getInstance() {
        return INSTANCE;
    }

    /**
     * Re-reads `/palette.json` from the classpath.
     */
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

    /**
     * Parses palette configuration from raw JSON text.
     */
    public synchronized void loadFromJson(String json) {
        if (json == null || json.isBlank()) {
            applyDefaults();
            return;
        }

        List<String> colors = new ArrayList<>();
        Pattern arrayPattern = Pattern.compile("\"sliceColors\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Pattern hexPattern = Pattern.compile("\"(#[0-9a-fA-F]{6})\"");
            Matcher hexMatcher = hexPattern.matcher(arrayContent);
            while (hexMatcher.find()) {
                colors.add(hexMatcher.group(1).toLowerCase());
            }
        }

        this.sliceColors = colors.isEmpty() ? DEFAULT_SLICE_COLORS : Collections.unmodifiableList(colors);
        this.pointerColor = extractStringProperty(json, "pointerColor", DEFAULT_POINTER_COLOR);
        this.hubOuterColor = extractStringProperty(json, "hubOuterColor", DEFAULT_HUB_OUTER_COLOR);
        this.hubInnerColor = extractStringProperty(json, "hubInnerColor", DEFAULT_HUB_INNER_COLOR);
    }

    private void applyDefaults() {
        this.sliceColors = DEFAULT_SLICE_COLORS;
        this.pointerColor = DEFAULT_POINTER_COLOR;
        this.hubOuterColor = DEFAULT_HUB_OUTER_COLOR;
        this.hubInnerColor = DEFAULT_HUB_INNER_COLOR;
    }

    private String extractStringProperty(String json, String property, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + property + "\"\\s*:\\s*\"(#[0-9a-fA-F]{6})\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return defaultValue;
    }

    public List<String> getSliceColors() {
        return sliceColors;
    }

    /**
     * Returns the slice color hex for slice index {@code i}, cycling through
     * available palette colors.
     */
    public String getSliceColor(int index) {
        if (sliceColors.isEmpty()) return DEFAULT_SLICE_COLORS.get(0);
        int safeIndex = Math.floorMod(index, sliceColors.size());
        return sliceColors.get(safeIndex);
    }

    public String getPointerColor() {
        return pointerColor;
    }

    public String getHubOuterColor() {
        return hubOuterColor;
    }

    public String getHubInnerColor() {
        return hubInnerColor;
    }

    /**
     * Computes high-contrast text color (#ffffff or #111827) for a given background hex.
     * Uses WCAG 2.0 relative luminance.
     */
    public static String getContrastTextColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() < 7) {
            return "#ffffff";
        }
        try {
            int r = Integer.parseInt(hexColor.substring(1, 3), 16);
            int g = Integer.parseInt(hexColor.substring(3, 5), 16);
            int b = Integer.parseInt(hexColor.substring(5, 7), 16);

            double lum = 0.2126 * linearise(r / 255.0)
                       + 0.7152 * linearise(g / 255.0)
                       + 0.0722 * linearise(b / 255.0);

            return lum > 0.35 ? "#111827" : "#ffffff";
        } catch (Exception e) {
            return "#ffffff";
        }
    }

    private static double linearise(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
