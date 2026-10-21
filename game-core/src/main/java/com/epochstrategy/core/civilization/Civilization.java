
package com.epochstrategy.core.civilization;

public final class Civilization {

    private final long id;
    private final String name;

    // RGB color in 0xRRGGBB format.
    private final int colorRgb;

    public Civilization(
            long id,
            String name,
            int colorRgb
    ) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Civilization ID must be positive"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Civilization name cannot be empty"
            );
        }

        if (colorRgb < 0 || colorRgb > 0xFFFFFF) {
            throw new IllegalArgumentException(
                    "Invalid civilization RGB color"
            );
        }

        this.id = id;
        this.name = name.trim();
        this.colorRgb = colorRgb;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColorRgb() {
        return colorRgb;
    }

    public String getHexColor() {
        return String.format(
                java.util.Locale.ROOT,
                "#%06X",
                colorRgb
        );
    }
}
