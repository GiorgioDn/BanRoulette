package com.example.core.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppColorPaletteTest {

    @Test
    @DisplayName("Loads common palette correctly from classpath")
    void loadsCommonPaletteFromClasspath() {
        AppColorPalette palette = new AppColorPalette();

        assertEquals("#0b1120", palette.getBackground());
        assertEquals("#111827", palette.getSurface());
        assertEquals("#1f2937", palette.getSurfaceVariant());
        assertEquals("#7c3aed", palette.getPrimary());
        assertEquals("#ef4444", palette.getDanger());
        assertEquals("#10b981", palette.getSuccess());
        assertEquals("#f59e0b", palette.getSpinGradientStart());
        assertEquals("#ef4444", palette.getSpinGradientEnd());
        assertEquals("#06b6d4", palette.getSavedBadge());
        assertEquals("#3b82f6", palette.getWheelBadge());
        assertEquals("#f43f5e", palette.getPointer());
    }

    @Test
    @DisplayName("Overrides work when custom JSON is supplied")
    void overridesWork() {
        AppColorPalette palette = new AppColorPalette();
        String json = """
                {
                  "background": "#000000",
                  "primary": "#ff00ff"
                }
                """;
        palette.loadFromJson(json);
        assertEquals("#000000", palette.getBackground());
        assertEquals("#ff00ff", palette.getPrimary());
        // Defaults preserved
        assertEquals("#111827", palette.getSurface());

        palette.reload();
        assertEquals("#0b1120", palette.getBackground());
        assertEquals("#7c3aed", palette.getPrimary());
    }
}
