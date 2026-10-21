
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.WorldMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TerritoryExpansionService {

    public record ExpansionTile(int x, int y) {
    }

    private static final int[][] DIRECTIONS = {
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
    };

    public List<ExpansionTile> getEligibleTiles(
            City city,
            WorldMap map,
            CityManager cityManager,
            TerritoryManager territoryManager
    ) {

        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(cityManager, "cityManager");
        Objects.requireNonNull(
                territoryManager,
                "territoryManager"
        );

        if (city.getCivilizationId() == null) {
            return List.of();
        }

        // The city must control its own center tile.
        var centerClaim = territoryManager.findClaim(
                city.getX(),
                city.getY()
        );

        if (centerClaim.isEmpty()
                || centerClaim.get().cityId() != city.getId()
                || centerClaim.get().civilizationId()
                != city.getCivilizationId()) {

            return List.of();
        }

        List<ExpansionTile> eligible =
                new ArrayList<>();

        int radius =
                TerritoryExpansionConfig.MAX_RADIUS;

        int minX = Math.max(0, city.getX() - radius);
        int maxX = Math.min(
                map.getWidth() - 1,
                city.getX() + radius
        );

        int minY = Math.max(0, city.getY() - radius);
        int maxY = Math.min(
                map.getHeight() - 1,
                city.getY() + radius
        );

        for (int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {

                if (!isEligible(
                        city,
                        x,
                        y,
                        map,
                        cityManager,
                        territoryManager
                )) {
                    continue;
                }

                eligible.add(
                        new ExpansionTile(x, y)
                );
            }
        }

        return List.copyOf(eligible);
    }

    private boolean isEligible(
            City city,
            int x,
            int y,
            WorldMap map,
            CityManager cityManager,
            TerritoryManager territoryManager
    ) {

        if (!map.isInsideMap(x, y)) {
            return false;
        }

        if (!TerritoryExpansionConfig.isWithinCityLimits(
                city.getX(),
                city.getY(),
                x,
                y
        )) {
            return false;
        }

        // Already claimed by any city or civilization.
        if (territoryManager.findClaim(x, y).isPresent()) {
            return false;
        }

        // Naval territory will have its own system.
        if (map.getTile(x, y).getTerrain()
                == TerrainType.OCEAN) {
            return false;
        }

        // Do not expand onto another city's center,
        // including neutral cities.
        if (cityManager.findCityAt(x, y).isPresent()) {
            return false;
        }

        // At least one orthogonally adjacent tile
        // must belong to THIS city.
        for (int[] direction : DIRECTIONS) {

            int neighborX = x + direction[0];
            int neighborY = y + direction[1];

            var neighborClaim =
                    territoryManager.findClaim(
                            neighborX,
                            neighborY
                    );

            if (neighborClaim.isPresent()
                    && neighborClaim.get().cityId()
                    == city.getId()) {

                return true;
            }
        }

        return false;
    }

    public void expand(
            City city,
            int tileX,
            int tileY,
            WorldMap map,
            CityManager cityManager,
            TerritoryManager territoryManager,
            CityInfluenceManager influenceManager
    ) {

        Objects.requireNonNull(
                influenceManager,
                "influenceManager"
        );

        // First validate the requested target.
        if (!getEligibleTiles(
                city,
                map,
                cityManager,
                territoryManager
        ).contains(new ExpansionTile(tileX, tileY))) {

            throw new IllegalArgumentException(
                    "Tile is not eligible for city expansion"
            );
        }

        if (!influenceManager.hasEnoughInfluence(
                city.getId()
        )) {

            throw new IllegalStateException(
                    "Not enough Influence"
            );
        }

        // Claim only after all checks have passed.
        territoryManager.claimTile(
                tileX,
                tileY,
                city.getCivilizationId(),
                city.getId()
        );

        // Executed in the single-threaded game turn/UI
        // flow, immediately after the territory claim.
        if (!influenceManager.trySpendForExpansion(
                city.getId()
        )) {

            throw new IllegalStateException(
                    "Influence spending failed"
            );
        }
    }
}
