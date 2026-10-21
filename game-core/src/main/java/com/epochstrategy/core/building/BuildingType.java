
package com.epochstrategy.core.building;

import com.epochstrategy.core.economy.ResourceType;

import java.util.Map;

public enum BuildingType {

    FARM(
            8, 4,
            ResourceType.FOOD, 4,
            0, 0
    ),

    SAWMILL(
            10, 5,
            ResourceType.WOOD, 3,
            0, 0
    ),

    QUARRY(
            12, 6,
            ResourceType.STONE, 3,
            0, 0
    ),

    IRON_MINE(
            14, 10,
            ResourceType.IRON, 2,
            0, 0
    ),

    WAREHOUSE(
            20, 12,
            null, 0,
            100, 0
    ),

    HOUSING(
            12, 8,
            null, 0,
            0, 500
    );

    private final int woodCost;
    private final int stoneCost;

    private final ResourceType producedResource;
    private final int productionBonus;

    private final int storageCapacityBonus;
    private final int populationCapacityBonus;

    BuildingType(
            int woodCost,
            int stoneCost,
            ResourceType producedResource,
            int productionBonus,
            int storageCapacityBonus,
            int populationCapacityBonus
    ) {

        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.producedResource = producedResource;
        this.productionBonus = productionBonus;
        this.storageCapacityBonus = storageCapacityBonus;
        this.populationCapacityBonus = populationCapacityBonus;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getStoneCost() {
        return stoneCost;
    }

    // Returns null for non-production buildings.
    public ResourceType getProducedResource() {
        return producedResource;
    }

    public int getProductionBonus() {
        return productionBonus;
    }

    public int getStorageCapacityBonus() {
        return storageCapacityBonus;
    }

    public int getPopulationCapacityBonus() {
        return populationCapacityBonus;
    }

    public Map<ResourceType, Integer> getConstructionCosts() {

        return Map.of(
                ResourceType.WOOD, woodCost,
                ResourceType.STONE, stoneCost
        );
    }
}
