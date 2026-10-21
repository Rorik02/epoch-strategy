
package com.epochstrategy.core.city;

import com.epochstrategy.core.world.*;

import java.util.Objects;

public final class CityPlacementService {

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    public enum PlacementStatus {
        VALID,
        OUTSIDE_MAP,
        INVALID_TERRAIN,
        ON_RIVER,
        TILE_OCCUPIED,
        TOO_CLOSE_TO_CITY
    }

    public enum WaterAccess {
        FRESHWATER,
        COASTAL,
        NONE
    }

    public record PlacementEvaluation(
            PlacementStatus status,
            WaterAccess waterAccess,
            int locationScore
    ) {
        public boolean canPlace() {
            return status == PlacementStatus.VALID;
        }
    }

    private final int minimumCityDistance;

    public CityPlacementService(
            int minimumCityDistance
    ) {
        if (minimumCityDistance < 1) {
            throw new IllegalArgumentException(
                    "Minimum city distance must be positive"
            );
        }

        this.minimumCityDistance =
                minimumCityDistance;
    }

    public PlacementEvaluation evaluate(
            WorldMap map,
            RiverNetwork riverNetwork,
            CityManager cityManager,
            int x,
            int y
    ) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(riverNetwork, "riverNetwork");
        Objects.requireNonNull(cityManager, "cityManager");

        if (!map.isInsideMap(x, y)) {
            return rejected(
                    PlacementStatus.OUTSIDE_MAP
            );
        }

        Tile tile = map.getTile(x, y);
        TerrainType terrain = tile.getTerrain();

        // Initially settlements can be built on
        // plains and forest tiles.
        if (terrain != TerrainType.PLAINS
                && terrain != TerrainType.FOREST) {

            return rejected(
                    PlacementStatus.INVALID_TERRAIN
            );
        }

        // Keep the river itself available for navigation.
        if (riverNetwork.hasRiverAt(x, y)) {
            return rejected(
                    PlacementStatus.ON_RIVER
            );
        }

        if (cityManager.isTileOccupied(x, y)) {
            return rejected(
                    PlacementStatus.TILE_OCCUPIED
            );
        }

        long minimumDistanceSquared =
                (long) minimumCityDistance
                        * minimumCityDistance;

        for (City city : cityManager.getCities()) {

            long dx = (long) x - city.getX();
            long dy = (long) y - city.getY();

            long distanceSquared =
                    dx * dx + dy * dy;

            if (distanceSquared < minimumDistanceSquared) {
                return rejected(
                        PlacementStatus.TOO_CLOSE_TO_CITY
                );
            }
        }

        WaterAccess waterAccess =
                calculateWaterAccess(
                        map,
                        riverNetwork,
                        x,
                        y
                );

        int score = switch (terrain) {
            case PLAINS -> 10;
            case FOREST -> 5;
            default -> 0;
        };

        score += switch (waterAccess) {
            case FRESHWATER -> 30;
            case COASTAL -> 15;
            case NONE -> 0;
        };

        return new PlacementEvaluation(
                PlacementStatus.VALID,
                waterAccess,
                score
        );
    }

    public void placeCity(
            WorldMap map,
            RiverNetwork riverNetwork,
            CityManager cityManager,
            City city
    ) {
        Objects.requireNonNull(city, "city");

        PlacementEvaluation evaluation =
                evaluate(
                        map,
                        riverNetwork,
                        cityManager,
                        city.getX(),
                        city.getY()
                );

        if (!evaluation.canPlace()) {
            throw new IllegalArgumentException(
                    "Cannot place city: "
                            + evaluation.status()
            );
        }

        cityManager.registerCity(city);
    }


    public WaterAccess getWaterAccess(
            WorldMap map,
            RiverNetwork riverNetwork,
            int x,
            int y
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(riverNetwork, "riverNetwork");

        if (!map.isInsideMap(x, y)) {
            throw new IllegalArgumentException(
                    "Coordinates are outside the map"
            );
        }

        return calculateWaterAccess(
                map,
                riverNetwork,
                x,
                y
        );
    }



    private WaterAccess calculateWaterAccess(
            WorldMap map,
            RiverNetwork riverNetwork,
            int x,
            int y
    ) {
        boolean coastal = false;

        for (int[] direction : DIRECTIONS) {

            int nx = x + direction[0];
            int ny = y + direction[1];

            if (!map.isInsideMap(nx, ny)) {
                continue;
            }

            TerrainType neighborTerrain =
                    map.getTile(nx, ny).getTerrain();

            if (neighborTerrain == TerrainType.LAKE
                    || riverNetwork.hasRiverAt(nx, ny)) {

                return WaterAccess.FRESHWATER;
            }

            if (neighborTerrain == TerrainType.OCEAN) {
                coastal = true;
            }
        }

        return coastal
                ? WaterAccess.COASTAL
                : WaterAccess.NONE;
    }

    private static PlacementEvaluation rejected(
            PlacementStatus status
    ) {
        return new PlacementEvaluation(
                status,
                WaterAccess.NONE,
                0
        );
    }

    public int getMinimumCityDistance() {
        return minimumCityDistance;
    }
}
