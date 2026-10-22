
package com.epochstrategy.core.city;

import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.territory.TerritoryExpansionConfig;
import com.epochstrategy.core.unit.LandUnit;
import com.epochstrategy.core.unit.LandUnitType;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.core.economy.ResourceType;

import java.util.Objects;

public final class CityFoundingService {

    public record FoundingCheck(
            boolean allowed,
            String reason
    ) {}

    public FoundingCheck checkFounding(
            LandUnit settler,
            GameState gameState,
            WorldMap worldMap
    ) {

        Objects.requireNonNull(settler, "settler");
        Objects.requireNonNull(gameState, "gameState");
        Objects.requireNonNull(worldMap, "worldMap");

        if (settler.getType() != LandUnitType.SETTLER) {
            return blocked("Unit is not a settler");
        }

        if (gameState.getLandUnitManager()
                .findUnit(settler.getId())
                .filter(unit -> unit == settler)
                .isEmpty()) {

            return blocked("Settler is not registered");
        }

        if (gameState.getCivilizationManager()
                .findCivilization(settler.getCivilizationId())
                .isEmpty()) {

            return blocked("Unknown civilization");
        }

        int x = settler.getX();
        int y = settler.getY();

        if (!worldMap.isInsideMap(x, y)) {
            return blocked("Outside map");
        }

        if (!worldMap.getTile(x, y)
                .isPassableForLandUnits()) {

            return blocked("Invalid terrain");
        }

        if (gameState.getCityManager()
                .findCityAt(x, y)
                .isPresent()) {

            return blocked("City already exists here");
        }

        if (gameState.getTerritoryManager()
                .findClaim(x, y)
                .isPresent()) {

            return blocked("Tile is already controlled");
        }

        CityFoundingSettings settings =
                gameState.getCityFoundingSettings();

        for (City existingCity :
                gameState.getCityManager().getCities()) {

            if (!settings.isDistanceAllowed(
                    x,
                    y,
                    existingCity.getX(),
                    existingCity.getY()
            )) {

                return blocked(
                        "Too close to " + existingCity.getName()
                                + " (min. "
                                + settings.getMinimumCityDistance()
                                + " tiles)"
                );
            }
        }

        return new FoundingCheck(
                true,
                "Available"
        );
    }

    public City foundCity(
            long settlerId,
            GameState gameState,
            WorldMap worldMap
    ) {

        Objects.requireNonNull(gameState, "gameState");
        Objects.requireNonNull(worldMap, "worldMap");

        LandUnit settler =
                gameState.getLandUnitManager()
                        .findUnit(settlerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Unknown settler: " + settlerId
                                )
                        );

        FoundingCheck check =
                checkFounding(
                        settler,
                        gameState,
                        worldMap
                );

        if (!check.allowed()) {
            throw new IllegalStateException(
                    check.reason()
            );
        }

        long maxCityId =
                gameState.getCityManager()
                        .getCities()
                        .stream()
                        .mapToLong(City::getId)
                        .max()
                        .orElse(0L);

        long newCityId = Math.addExact(maxCityId, 1L);

        int x = settler.getX();
        int y = settler.getY();


        City newCity = new City(
                newCityId,
                "Settlement " + newCityId,
                x,
                y,
                CityFoundingBalance.STARTING_POPULATION,
                settler.getCivilizationId()
        );

// Initial supplies for the new settlement.

        newCity.getInventory().add(
                ResourceType.FOOD,
                CityFoundingBalance.STARTING_FOOD
        );

        newCity.getInventory().add(
                ResourceType.WOOD,
                CityFoundingBalance.STARTING_WOOD
        );

        newCity.getInventory().add(
                ResourceType.STONE,
                CityFoundingBalance.STARTING_STONE
        );


        gameState.getCityManager()
                .registerCity(newCity);

        // Initial city territory: up to 3x3.
        // Only free, non-ocean tiles can be claimed.

        int radius =
                TerritoryExpansionConfig.INITIAL_RADIUS;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {

                int tileX = x + dx;
                int tileY = y + dy;

                if (!worldMap.isInsideMap(
                        tileX, tileY
                )) {
                    continue;
                }

                if (worldMap.getTile(tileX, tileY)
                        .getTerrain() == TerrainType.OCEAN) {
                    continue;
                }

                if (gameState.getTerritoryManager()
                        .findClaim(tileX, tileY)
                        .isPresent()) {
                    continue;
                }

                gameState.getTerritoryManager()
                        .claimTile(
                                tileX,
                                tileY,
                                newCity.getCivilizationId(),
                                newCity.getId()
                        );
            }
        }

        // Settler is consumed when the city is founded.
        gameState.getLandUnitManager()
                .removeUnit(settlerId);

        return newCity;
    }

    private FoundingCheck blocked(String reason) {

        return new FoundingCheck(
                false,
                reason
        );
    }
}
