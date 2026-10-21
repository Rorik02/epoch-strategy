
package com.epochstrategy.core.city;

import com.epochstrategy.core.building.BuildingType;
import com.epochstrategy.core.building.CityBuildingManager;
import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.core.world.WorldMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CityTurnService {

    private final CityPlacementService placementService;
    private final CityBuildingManager buildingManager;

    // Constructor for simulations without buildings.
    public CityTurnService(
            CityPlacementService placementService
    ) {
        this(
                placementService,
                new CityBuildingManager()
        );
    }

    // Constructor with building production bonuses.
    public CityTurnService(
            CityPlacementService placementService,
            CityBuildingManager buildingManager
    ) {

        this.placementService = Objects.requireNonNull(
                placementService,
                "placementService"
        );

        this.buildingManager = Objects.requireNonNull(
                buildingManager,
                "buildingManager"
        );
    }

    public record CityEconomyTurnResult(
            Map<Long, CityFoodService.FoodTurnResult> foodResults,
            Map<Long, CityResourceProductionService.ResourceTurnResult>
            resourceResults
    ) {

        public CityEconomyTurnResult {

            foodResults = Collections.unmodifiableMap(
                    new LinkedHashMap<>(
                            Objects.requireNonNull(foodResults)
                    )
            );

            resourceResults = Collections.unmodifiableMap(
                    new LinkedHashMap<>(
                            Objects.requireNonNull(resourceResults)
                    )
            );
        }
    }

    public Map<Long, CityFoodService.FoodTurnResult> advanceTurn(
            WorldMap map,
            RiverNetwork riverNetwork,
            CityManager cityManager
    ) {

        return advanceEconomyTurn(
                map,
                riverNetwork,
                cityManager
        ).foodResults();
    }

    public CityEconomyTurnResult advanceEconomyTurn(
            WorldMap map,
            RiverNetwork riverNetwork,
            CityManager cityManager
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(riverNetwork, "riverNetwork");
        Objects.requireNonNull(cityManager, "cityManager");

        Map<Long, CityFoodService.FoodTurnResult> foodResults =
                new LinkedHashMap<>();

        Map<Long, CityResourceProductionService.ResourceTurnResult>
                resourceResults = new LinkedHashMap<>();

        for (City city : cityManager.getCities()) {

            long cityId = city.getId();

            var waterAccess =
                    placementService.getWaterAccess(
                            map,
                            riverNetwork,
                            city.getX(),
                            city.getY()
                    );

            // Determine production bonuses.
            int foodBonus = getBonus(
                    cityId,
                    BuildingType.FARM
            );

            int housingBonus =
                    buildingManager.hasBuilding(
                            cityId,
                            BuildingType.HOUSING
                    )
                            ? BuildingType.HOUSING
                            .getPopulationCapacityBonus()
                            : 0;

            int woodBonus = getBonus(
                    cityId,
                    BuildingType.SAWMILL
            );

            int stoneBonus = getBonus(
                    cityId,
                    BuildingType.QUARRY
            );

            int ironBonus = getBonus(
                    cityId,
                    BuildingType.IRON_MINE
            );

            // Food and population are processed first.
            var foodResult =
                    CityFoodService.advanceTurn(
                            city,
                            waterAccess,
                            foodBonus,
                            housingBonus
                    );

            foodResults.put(
                    cityId,
                    foodResult
            );

            // Then produce remaining resources.
            var resourceResult =
                    CityResourceProductionService.advanceTurn(
                            city,
                            map,
                            woodBonus,
                            stoneBonus,
                            ironBonus
                    );

            resourceResults.put(
                    cityId,
                    resourceResult
            );
        }

        return new CityEconomyTurnResult(
                foodResults,
                resourceResults
        );
    }

    private int getBonus(
            long cityId,
            BuildingType buildingType
    ) {

        if (!buildingManager.hasBuilding(
                cityId,
                buildingType
        )) {
            return 0;
        }

        return buildingType.getProductionBonus();
    }
}
