
package com.epochstrategy.core.city;

import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;

import java.util.Objects;

public final class CityFoodService {

    private static final int PEOPLE_PER_FOOD = 100;
    private static final int STARVATION_PERCENT = 5;

    private CityFoodService() {
    }

    public record FoodTurnResult(
            long cityId,
            int populationBefore,
            int populationAfter,
            int populationChange,
            int producedFood,
            int requiredFood,
            int consumedFood,
            int missingFood,
            int foodInStorage,
            boolean starving
    ) {
    }

    // Existing method: no building bonus.
    public static FoodTurnResult advanceTurn(
            City city,
            CityPlacementService.WaterAccess waterAccess
    ) {

        return advanceTurn(city, waterAccess, 0);
    }

    // New method: includes food production bonuses.
    public static FoodTurnResult advanceTurn(
            City city,
            CityPlacementService.WaterAccess waterAccess,
            int productionBonus,
            int populationCapacityBonus
    ) {

        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(
                waterAccess,
                "waterAccess"
        );

        if (productionBonus < 0) {
            throw new IllegalArgumentException(
                    "Production bonus cannot be negative"
            );
        }

        if (populationCapacityBonus < 0) {
            throw new IllegalArgumentException(
                    "Population capacity bonus cannot be negative"
            );
        }

        ResourceInventory inventory =
                city.getInventory();

        int populationBefore =
                city.getPopulation();

        int baseProduction = switch (waterAccess) {
            case FRESHWATER -> 8;
            case COASTAL -> 6;
            case NONE -> 3;
        };

        // Base production + building bonuses.
        long plannedProduction =
                (long) baseProduction + productionBonus;

        int producedFood = (int) Math.min(
                plannedProduction,
                inventory.getFreeCapacity()
        );

        if (producedFood > 0) {
            inventory.add(
                    ResourceType.FOOD,
                    producedFood
            );
        }

        // Calculate food consumption.
        int requiredFood = (int) (
                ((long) populationBefore
                        + PEOPLE_PER_FOOD - 1)
                        / PEOPLE_PER_FOOD
        );

        int availableFood =
                inventory.getAmount(ResourceType.FOOD);

        int consumedFood = Math.min(
                availableFood,
                requiredFood
        );

        if (consumedFood > 0) {
            inventory.remove(
                    ResourceType.FOOD,
                    consumedFood
            );
        }

        int missingFood =
                requiredFood - consumedFood;

        // Update population.
        if (missingFood == 0) {

            CityPopulationService.advanceTurn(
                    city,
                    waterAccess,
                    populationCapacityBonus
            );

        } else {

            long numerator =
                    (long) populationBefore
                            * STARVATION_PERCENT
                            * missingFood;

            long denominator =
                    (long) requiredFood * 100;

            long populationLoss = Math.max(
                    1L,
                    (numerator + denominator - 1)
                            / denominator
            );

            int newPopulation = (int) Math.max(
                    1L,
                    (long) populationBefore
                            - populationLoss
            );

            city.setPopulation(newPopulation);
        }

        int populationAfter =
                city.getPopulation();

        return new FoodTurnResult(
                city.getId(),
                populationBefore,
                populationAfter,
                populationAfter - populationBefore,
                producedFood,
                requiredFood,
                consumedFood,
                missingFood,
                inventory.getAmount(ResourceType.FOOD),
                missingFood > 0
        );
    }


    public static FoodTurnResult advanceTurn(
            City city,
            CityPlacementService.WaterAccess waterAccess,
            int productionBonus
    ) {

        return advanceTurn(
                city,
                waterAccess,
                productionBonus,
                0
        );
    }

}
