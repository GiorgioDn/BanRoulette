package com.example.core.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WheelPaletteTest {

    @Test
    @DisplayName("Loads palette.json correctly from classpath")
    void loadsPaletteFromClasspath() {
        WheelPalette palette = new WheelPalette();
        List<String> colors = palette.getSliceColors();

        assertNotNull(colors);
        assertEquals(12, colors.size());
        assertEquals("#6366f1", colors.get(0));
        assertEquals("#ec4899", colors.get(1));
        assertEquals("#f43f5e", palette.getPointerColor());
    }

    @Test
    @DisplayName("getSliceColor wraps around slice indices")
    void sliceColorCycling() {
        WheelPalette palette = new WheelPalette();
        assertEquals(palette.getSliceColor(0), palette.getSliceColor(12));
        assertEquals(palette.getSliceColor(1), palette.getSliceColor(13));
    }

    @Test
    @DisplayName("Changing a color in the palette changes the output")
    void changingPaletteColorChangesOutput() {
        WheelPalette palette = new WheelPalette();
        String customJson = """
                {
                  "sliceColors": ["#112233", "#445566"],
                  "pointerColor": "#aabbcc",
                  "hubOuterColor": "#001122",
                  "hubInnerColor": "#334455"
                }
                """;

        palette.loadFromJson(customJson);
        assertEquals(2, palette.getSliceColors().size());
        assertEquals("#112233", palette.getSliceColor(0));
        assertEquals("#445566", palette.getSliceColor(1));
        assertEquals("#aabbcc", palette.getPointerColor());
        assertEquals("#001122", palette.getHubOuterColor());
        assertEquals("#334455", palette.getHubInnerColor());

        // Restore classpath palette
        palette.reload();
        assertEquals(12, palette.getSliceColors().size());
    }

    @Test
    @DisplayName("WCAG contrast calculates dark text on light backgrounds and white text on dark backgrounds")
    void contrastCalculation() {
        assertEquals("#111827", WheelPalette.getContrastTextColor("#ffffff")); // dark text on white
        assertEquals("#111827", WheelPalette.getContrastTextColor("#f59e0b")); // dark text on light amber
        assertEquals("#ffffff", WheelPalette.getContrastTextColor("#0f172a")); // white text on slate-900
        assertEquals("#ffffff", WheelPalette.getContrastTextColor("#000000")); // white text on black
    }
}
