
package com.epochstrategy.core.building;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CityBuildingManager {

    private final Map<Long, EnumSet<BuildingType>>
            buildingsByCity = new HashMap<>();

    public boolean canConstruct(
            long cityId,
            BuildingType buildingType,
            CityManager cityManager
    ) {

        Objects.requireNonNull(
                buildingType,
                "buildingType"
        );

        Objects.requireNonNull(
                cityManager,
                "cityManager"
        );

        City city = cityManager.findCity(cityId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "City not found: " + cityId
                        )
                );

        if (hasBuilding(cityId, buildingType)) {
            return false;
        }

        ResourceInventory inventory =
                city.getInventory();

        for (var cost :
                buildingType.getConstructionCosts().entrySet()) {

            if (inventory.getAmount(cost.getKey())
                    < cost.getValue()) {
                return false;
            }
        }

        return true;
    }

    public void construct(
            long cityId,
            BuildingType buildingType,
            CityManager cityManager
    ) {

        Objects.requireNonNull(
                buildingType,
                "buildingType"
        );

        Objects.requireNonNull(
                cityManager,
                "cityManager"
        );

        City city = cityManager.findCity(cityId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "City not found: " + cityId
                        )
                );

        if (!canConstruct(
                cityId,
                buildingType,
                cityManager
        )) {

            throw new IllegalStateException(
                    "Cannot construct "
                            + buildingType
                            + " in city " + cityId
            );
        }

        ResourceInventory inventory =
                city.getInventory();

        // All costs have been verified before spending.
        for (var cost :
                buildingType.getConstructionCosts().entrySet()) {

            if (cost.getValue() > 0) {

                inventory.remove(
                        cost.getKey(),
                        cost.getValue()
                );
            }
        }

        buildingsByCity
                .computeIfAbsent(
                        cityId,
                        id -> EnumSet.noneOf(
                                BuildingType.class
                        )
                )
                .add(buildingType);


        // Apply one-time warehouse upgrade.
        int storageBonus =
                buildingType.getStorageCapacityBonus();

        if (storageBonus > 0) {

            inventory.increaseCapacity(storageBonus);
        }

    }

    public boolean hasBuilding(
            long cityId,
            BuildingType buildingType
    ) {

        Objects.requireNonNull(
                buildingType,
                "buildingType"
        );

        EnumSet<BuildingType> buildings =
                buildingsByCity.get(cityId);

        return buildings != null
                && buildings.contains(buildingType);
    }

    public Set<BuildingType> getBuildings(
            long cityId
    ) {

        EnumSet<BuildingType> buildings =
                buildingsByCity.get(cityId);

        if (buildings == null) {
            return Set.of();
        }

        return Set.copyOf(buildings);
    }

    public int getBuildingCount(
            long cityId
    ) {
        return getBuildings(cityId).size();
    }

    public void removeCityBuildings(
            long cityId
    ) {
        buildingsByCity.remove(cityId);
    }
}
