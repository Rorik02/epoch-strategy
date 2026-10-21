
package com.epochstrategy.core.city;

import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import java.util.Objects;

public final class CityResourceProductionService {

    // City tile and four orthogonal neighbors.
    private static final int[][] OFFSETS = {
            {0, 0},
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private CityResourceProductionService() {
    }

    public record ResourceTurnResult(
            long cityId,
            int woodProduced,
            int stoneProduced,
            int ironProduced,
            int storageUsed,
            int storageCapacity
    ) {

        public int totalProduced() {
            return woodProduced
                    + stoneProduced
                    + ironProduced;
        }
    }

    public static ResourceTurnResult advanceTurn(
            City city,
            WorldMap map
    ) {
        return advanceTurn(
                city,
                map,
                0,
                0,
                0
        );
    }



    public static ResourceTurnResult advanceTurn(
            City city,
            WorldMap map,
            int woodBonus,
            int stoneBonus,
            int ironBonus
    ) {

        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(map, "map");

        if (woodBonus < 0 || stoneBonus < 0 || ironBonus < 0) {
            throw new IllegalArgumentException(
                    "Production bonuses cannot be negative"
            );
        }

        int cityX = city.getX();
        int cityY = city.getY();

        if (!map.isInsideMap(cityX, cityY)) {
            throw new IllegalArgumentException(
                    "City is outside the world map"
            );
        }

        int plannedWood = 0;
        int plannedStone = 0;
        int plannedIron = 0;

        for (int[] offset : OFFSETS) {

            int x = cityX + offset[0];
            int y = cityY + offset[1];

            if (!map.isInsideMap(x, y)) {
                continue;
            }

            Tile tile = map.getTile(x, y);

            TerrainType terrain = tile.getTerrain();

            switch (terrain) {

                case FOREST -> plannedWood += 2;

                case HILLS -> plannedStone += 2;

                case MOUNTAINS -> {
                    plannedStone += 1;
                    plannedIron += 1;
                }

                default -> {
                }
            }
        }

        plannedWood = Math.addExact(plannedWood, woodBonus);
        plannedStone = Math.addExact(plannedStone, stoneBonus);
        plannedIron = Math.addExact(plannedIron, ironBonus);

        ResourceInventory inventory = city.getInventory();

        int woodProduced = storeResource(
                inventory, ResourceType.WOOD, plannedWood
        );

        int stoneProduced = storeResource(
                inventory, ResourceType.STONE, plannedStone
        );

        int ironProduced = storeResource(
                inventory, ResourceType.IRON, plannedIron
        );

        return new ResourceTurnResult(
                city.getId(),
                woodProduced,
                stoneProduced,
                ironProduced,
                inventory.getTotalUnits(),
                inventory.getCapacity()
        );
    }


    private static int storeResource(
            ResourceInventory inventory,
            ResourceType resourceType,
            int plannedAmount
    ) {

        if (plannedAmount <= 0) {
            return 0;
        }

        int amount = Math.min(
                plannedAmount,
                inventory.getFreeCapacity()
        );

        if (amount > 0) {
            inventory.add(
                    resourceType,
                    amount
            );
        }

        return amount;
    }
}
