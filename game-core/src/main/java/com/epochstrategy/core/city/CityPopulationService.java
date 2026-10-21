
package com.epochstrategy.core.city;

import java.util.Objects;

public final class CityPopulationService {

    private static final int CAPACITY_PER_LEVEL = 1000;

    private CityPopulationService() {
    }

    public record PopulationTurnResult(
            long cityId,
            int previousPopulation,
            int currentPopulation,
            int growth,
            int capacity,
            CityPlacementService.WaterAccess waterAccess
    ) {
    }

    // Compatibility with existing code.
    public static PopulationTurnResult advanceTurn(
            City city,
            CityPlacementService.WaterAccess waterAccess
    ) {

        return advanceTurn(
                city,
                waterAccess,
                0
        );
    }

    // Includes additional housing capacity.
    public static PopulationTurnResult advanceTurn(
            City city,
            CityPlacementService.WaterAccess waterAccess,
            int populationCapacityBonus
    ) {

        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(waterAccess, "waterAccess");

        int previousPopulation =
                city.getPopulation();

        int capacity = getPopulationCapacity(
                city,
                populationCapacityBonus
        );

        int growthPercent = switch (waterAccess) {
            case FRESHWATER -> 3;
            case COASTAL -> 2;
            case NONE -> 1;
        };

        int actualGrowth = 0;

        if (previousPopulation < capacity) {

            long calculatedGrowth = Math.max(
                    1L,
                    (long) previousPopulation
                            * growthPercent / 100
            );

            actualGrowth = (int) Math.min(
                    calculatedGrowth,
                    (long) capacity - previousPopulation
            );

            city.setPopulation(
                    previousPopulation + actualGrowth
            );
        }

        return new PopulationTurnResult(
                city.getId(),
                previousPopulation,
                city.getPopulation(),
                actualGrowth,
                capacity,
                waterAccess
        );
    }

    // Existing API: base capacity only.
    public static int getPopulationCapacity(
            City city
    ) {

        return getPopulationCapacity(city, 0);
    }

    // New API: base capacity + housing.
    public static int getPopulationCapacity(
            City city,
            int populationCapacityBonus
    ) {

        Objects.requireNonNull(city, "city");

        if (populationCapacityBonus < 0) {
            throw new IllegalArgumentException(
                    "Population capacity bonus cannot be negative"
            );
        }

        long capacity =
                (long) city.getLevel()
                        * CAPACITY_PER_LEVEL
                        + populationCapacityBonus;

        return (int) Math.min(
                capacity,
                Integer.MAX_VALUE
        );
    }
}
