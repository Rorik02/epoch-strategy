
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.WorldMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CityAutomaticExpansionService {

    public record AutomaticExpansion(
            long cityId,
            int x,
            int y
    ) {
    }

    private final TerritoryExpansionService expansionService =
            new TerritoryExpansionService();

    public List<AutomaticExpansion> advanceTurn(
            WorldMap map,
            CityManager cityManager,
            TerritoryManager territoryManager,
            CityInfluenceManager influenceManager,
            CityExpansionManager expansionManager
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(cityManager, "cityManager");
        Objects.requireNonNull(
                territoryManager, "territoryManager"
        );
        Objects.requireNonNull(
                influenceManager, "influenceManager"
        );
        Objects.requireNonNull(
                expansionManager, "expansionManager"
        );

        List<AutomaticExpansion> results =
                new ArrayList<>();

        // Deterministic processing order.
        List<City> cities = new ArrayList<>(
                cityManager.getCities()
        );

        cities.sort(
                Comparator.comparingLong(City::getId)
        );

        for (City city : cities) {

            long cityId = city.getId();

            if (city.getCivilizationId() == null) {
                continue;
            }

            if (!expansionManager.isAutomatic(cityId)) {
                continue;
            }

            if (!influenceManager.hasEnoughInfluence(
                    cityId
            )) {
                continue;
            }

            List<TerritoryExpansionService.ExpansionTile>
                    eligibleTiles =
                    expansionService.getEligibleTiles(
                            city,
                            map,
                            cityManager,
                            territoryManager
                    );

            if (eligibleTiles.isEmpty()) {
                continue;
            }

            var bestTile = selectBestTile(
                    city,
                    map,
                    eligibleTiles
            );

            if (bestTile == null) {
                continue;
            }

            // Uses the same validation and Influence
            // spending as manual expansion.
            expansionService.expand(
                    city,
                    bestTile.x(),
                    bestTile.y(),
                    map,
                    cityManager,
                    territoryManager,
                    influenceManager
            );

            results.add(
                    new AutomaticExpansion(
                            cityId,
                            bestTile.x(),
                            bestTile.y()
                    )
            );
        }

        return List.copyOf(results);
    }

    private TerritoryExpansionService.ExpansionTile
    selectBestTile(
            City city,
            WorldMap map,
            List<TerritoryExpansionService.ExpansionTile>
                    eligibleTiles
    ) {

        TerritoryExpansionService.ExpansionTile best =
                null;

        int bestScore = Integer.MIN_VALUE;

        for (var tile : eligibleTiles) {

            int score = evaluateTile(
                    city,
                    map,
                    tile
            );

            if (score > bestScore) {

                bestScore = score;
                best = tile;
            }
        }

        return best;
    }

    private int evaluateTile(
            City city,
            WorldMap map,
            TerritoryExpansionService.ExpansionTile tile
    ) {

        TerrainType terrain =
                map.getTile(
                        tile.x(),
                        tile.y()
                ).getTerrain();

        int terrainScore = switch (terrain) {

            case FOREST -> 120;
            case PLAINS -> 100;
            case HILLS -> 90;
            case LAKE -> 60;
            case MOUNTAINS -> 40;
            case OCEAN -> 0;
        };

        int distance =
                Math.abs(tile.x() - city.getX())
                        + Math.abs(tile.y() - city.getY());

        return terrainScore - distance * 5;
    }
}
