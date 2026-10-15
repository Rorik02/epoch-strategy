
package com.epochstrategy.core.world;

import java.util.Objects;

public final class WaterAccessCalculator {

    private static final int FULL_FRESHWATER_VALUE = 100;
    private static final int DISTANT_FRESHWATER_VALUE = 50;
    private static final int COASTAL_VALUE = 50;

    private static final int FRESHWATER_RANGE = 2;

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private WaterAccessCalculator() {
    }

    // Compatibility with the previous API
    public static WaterAccess calculate(
            WorldMap map,
            RiverNetwork riverNetwork,
            int x,
            int y
    ) {
        return calculateDetails(
                map, riverNetwork, x, y
        ).type();
    }

    public static WaterAccessDetails calculateDetails(
            WorldMap map,
            RiverNetwork riverNetwork,
            int x,
            int y
    ) {

        Objects.requireNonNull(map);
        Objects.requireNonNull(riverNetwork);

        if (!map.isInsideMap(x, y)) {
            throw new IllegalArgumentException(
                    "Tile is outside the map"
            );
        }

        TerrainType selectedTerrain =
                map.getTile(x, y).getTerrain();

        if (selectedTerrain == TerrainType.OCEAN
                || selectedTerrain == TerrainType.LAKE) {

            throw new IllegalArgumentException(
                    "Water access requires a land tile"
            );
        }

        int nearestFreshwaterDistance =
                Integer.MAX_VALUE;

        // Search for rivers and lakes up to 2 tiles away
        for (int dx = -FRESHWATER_RANGE;
             dx <= FRESHWATER_RANGE;
             dx++) {

            for (int dy = -FRESHWATER_RANGE;
                 dy <= FRESHWATER_RANGE;
                 dy++) {

                int distance =
                        Math.abs(dx) + Math.abs(dy);

                if (distance > FRESHWATER_RANGE) {
                    continue;
                }

                int nx = x + dx;
                int ny = y + dy;

                if (!map.isInsideMap(nx, ny)) {
                    continue;
                }

                TerrainType terrain =
                        map.getTile(nx, ny).getTerrain();

                boolean freshwaterSource =
                        terrain == TerrainType.LAKE
                                || (terrain != TerrainType.OCEAN
                                && riverNetwork.hasRiverAt(nx, ny));

                if (freshwaterSource) {

                    nearestFreshwaterDistance = Math.min(
                            nearestFreshwaterDistance,
                            distance
                    );
                }
            }
        }

        // Check direct ocean access
        boolean coastal = false;

        for (int[] direction : DIRECTIONS) {

            int nx = x + direction[0];
            int ny = y + direction[1];

            if (!map.isInsideMap(nx, ny)) {
                continue;
            }

            if (map.getTile(nx, ny).getTerrain()
                    == TerrainType.OCEAN) {

                coastal = true;
                break;
            }
        }

        int freshwaterValue = 0;

        if (nearestFreshwaterDistance <= 1) {

            freshwaterValue = FULL_FRESHWATER_VALUE;

        } else if (nearestFreshwaterDistance == 2) {

            freshwaterValue = DISTANT_FRESHWATER_VALUE;
        }

        int coastalValue =
                coastal ? COASTAL_VALUE : 0;

        int totalValue = Math.max(
                freshwaterValue,
                coastalValue
        );

        boolean freshwaterNearby =
                freshwaterValue > 0;

        WaterAccess type;

        if (freshwaterNearby && coastal) {
            type = WaterAccess.BOTH;

        } else if (freshwaterNearby) {
            type = WaterAccess.FRESHWATER;

        } else if (coastal) {
            type = WaterAccess.COASTAL;

        } else {
            type = WaterAccess.NONE;
        }

        int freshwaterDistance =
                nearestFreshwaterDistance == Integer.MAX_VALUE
                        ? -1
                        : nearestFreshwaterDistance;

        return new WaterAccessDetails(
                type,
                freshwaterValue,
                coastalValue,
                totalValue,
                freshwaterDistance
        );
    }
}
