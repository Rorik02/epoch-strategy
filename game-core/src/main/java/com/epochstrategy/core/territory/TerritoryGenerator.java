
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.core.territory.TerritoryExpansionConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class TerritoryGenerator {

    private final int territoryRadius;
    private final boolean squareCoverage;


    public TerritoryGenerator(int territoryRadius) {
        this(territoryRadius, false);
    }

    public TerritoryGenerator(
            int territoryRadius,
            boolean squareCoverage
    ) {

        if (territoryRadius < 1) {
            throw new IllegalArgumentException(
                    "Territory radius must be positive"
            );
        }

        this.territoryRadius = territoryRadius;
        this.squareCoverage = squareCoverage;
    }


    public int generate(
            WorldMap map,
            CityManager cityManager,
            CivilizationManager civilizationManager,
            TerritoryManager territoryManager
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(cityManager, "cityManager");

        Objects.requireNonNull(
                civilizationManager,
                "civilizationManager"
        );

        Objects.requireNonNull(
                territoryManager,
                "territoryManager"
        );

        if (!territoryManager.isEmpty()) {
            throw new IllegalStateException(
                    "Territory manager must be empty before generation"
            );
        }

        // Only cities belonging to registered civilizations
        // can generate state territories.
        List<City> cities = cityManager.getCities()
                .stream()
                .filter(city -> !city.isNeutral())
                .sorted(
                        Comparator.comparingLong(City::getId)
                )
                .toList();

        // Validate all owners before changing territories.
        for (City city : cities) {

            if (!map.isInsideMap(city.getX(), city.getY())) {
                throw new IllegalStateException(
                        "City outside map: " + city.getId()
                );
            }

            if (civilizationManager
                    .findCivilization(city.getCivilizationId())
                    .isEmpty()) {

                throw new IllegalStateException(
                        "Unknown civilization for city: "
                                + city.getId()
                );
            }
        }

        long radiusSquared =
                (long) territoryRadius * territoryRadius;

        for (int x = 0; x < map.getWidth(); x++) {

            for (int y = 0; y < map.getHeight(); y++) {

                // Ocean territory will be handled separately
                // by the future naval control system.
                if (map.getTile(x, y).getTerrain()
                        == TerrainType.OCEAN) {
                    continue;
                }

                City closestCity = null;
                long bestDistanceSquared =
                        Long.MAX_VALUE;

                for (City city : cities) {

                    long dx = (long) x - city.getX();
                    long dy = (long) y - city.getY();

                    long distanceSquared =
                            dx * dx + dy * dy;

                    if (squareCoverage) {

                        if (Math.abs(dx) > territoryRadius
                                || Math.abs(dy) > territoryRadius) {
                            continue;
                        }

                    } else if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    // Sorted by city ID: ties are resolved
                    // in favor of the lower city ID.
                    if (distanceSquared < bestDistanceSquared) {

                        bestDistanceSquared = distanceSquared;
                        closestCity = city;
                    }
                }

                if (closestCity == null) {
                    continue;
                }

                territoryManager.claimTile(
                        x,
                        y,
                        closestCity.getCivilizationId(),
                        closestCity.getId()
                );
            }
        }

        return territoryManager.getClaimedTileCount();
    }

    public int getTerritoryRadius() {
        return territoryRadius;
    }
}
