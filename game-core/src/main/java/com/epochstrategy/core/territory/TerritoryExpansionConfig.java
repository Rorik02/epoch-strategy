
package com.epochstrategy.core.territory;

public final class TerritoryExpansionConfig {

    private TerritoryExpansionConfig() {
    }

    // Territory limits
    public static final int INITIAL_RADIUS = 1;
    public static final int MAX_RADIUS = 3;

    // Influence is stored in hundredths.
    // 100 units = 1.0 Influence.
    public static final int INFLUENCE_SCALE = 100;

    // Population thresholds
    public static final int POPULATION_STEP = 500;

    // Influence production per turn
    public static final int BASE_INFLUENCE = 50;
    public static final int INFLUENCE_PER_THRESHOLD = 50;

    // Expansion costs
    public static final int FIRST_EXPANSION_COST = 20;
    public static final int EXPANSION_COST_INCREASE = 10;

    public static long calculateIncome(
            int population
    ) {

        if (population <= 0) {
            return 0;
        }

        long reachedThresholds =
                population / POPULATION_STEP;

        return BASE_INFLUENCE
                + reachedThresholds
                * INFLUENCE_PER_THRESHOLD;
    }

    public static long calculateExpansionCost(
            int previousExpansions
    ) {

        if (previousExpansions < 0) {
            throw new IllegalArgumentException(
                    "Expansion count cannot be negative"
            );
        }

        return (
                FIRST_EXPANSION_COST
                        + (long) previousExpansions
                        * EXPANSION_COST_INCREASE
        ) * INFLUENCE_SCALE;
    }

    public static boolean isWithinCityLimits(
            int cityX,
            int cityY,
            int tileX,
            int tileY
    ) {

        return Math.abs((long) tileX - cityX) <= MAX_RADIUS
                && Math.abs((long) tileY - cityY) <= MAX_RADIUS;
    }
}
