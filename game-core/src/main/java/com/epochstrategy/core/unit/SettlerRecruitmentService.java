
package com.epochstrategy.core.unit;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.economy.ResourceInventory;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.world.WorldMap;

import java.util.Objects;

public final class SettlerRecruitmentService {

    private static final int[][] SPAWN_DIRECTIONS = {
            {1, 0},
            {0, 1},
            {-1, 0},
            {0, -1},
            {1, 1},
            {-1, 1},
            {1, -1},
            {-1, -1}
    };

    public record RecruitmentCheck(
            boolean allowed,
            String reason,
            int spawnX,
            int spawnY
    ) {
    }

    public RecruitmentCheck checkRecruitment(
            long cityId,
            GameState gameState,
            WorldMap worldMap
    ) {

        Objects.requireNonNull(gameState, "gameState");
        Objects.requireNonNull(worldMap, "worldMap");

        City city = gameState.getCityManager()
                .findCity(cityId)
                .orElse(null);

        if (city == null) {
            return blocked("City does not exist");
        }

        Long civilizationId = city.getCivilizationId();

        if (civilizationId == null) {
            return blocked("Neutral city cannot train settlers");
        }

        if (gameState.getCivilizationManager()
                .findCivilization(civilizationId)
                .isEmpty()) {

            return blocked("Unknown civilization");
        }

        int requiredPopulation =
                SettlerRecruitmentConfig.POPULATION_COST
                        + SettlerRecruitmentConfig
                        .MIN_POPULATION_AFTER_TRAINING;

        if (city.getPopulation() < requiredPopulation) {
            return blocked(
                    "Requires at least "
                            + requiredPopulation
                            + " population"
            );
        }

        ResourceInventory inventory =
                city.getInventory();

        if (inventory.getAmount(ResourceType.WOOD)
                < SettlerRecruitmentConfig.WOOD_COST) {

            return blocked("Not enough wood");
        }

        if (inventory.getAmount(ResourceType.FOOD)
                < SettlerRecruitmentConfig.FOOD_COST) {

            return blocked("Not enough food");
        }

        for (int[] direction : SPAWN_DIRECTIONS) {

            int x = city.getX() + direction[0];
            int y = city.getY() + direction[1];

            if (!worldMap.isInsideMap(x, y)) {
                continue;
            }

            if (!worldMap.getTile(x, y)
                    .isPassableForLandUnits()) {
                continue;
            }

            // Do not spawn on another city.
            if (gameState.getCityManager()
                    .findCityAt(x, y)
                    .isPresent()) {
                continue;
            }

            // Do not spawn on an occupied tile.
            if (gameState.getLandUnitManager()
                    .findUnitAt(x, y)
                    .isPresent()) {
                continue;
            }

            // Spawn only on territory controlled
            // by the recruiting city.
            boolean ownedByCity =
                    gameState.getTerritoryManager()
                            .findClaim(x, y)
                            .filter(claim ->
                                    claim.cityId() == cityId
                                            && claim.civilizationId()
                                            == civilizationId
                            )
                            .isPresent();

            if (!ownedByCity) {
                continue;
            }

            return new RecruitmentCheck(
                    true,
                    "Available",
                    x,
                    y
            );
        }

        return blocked("No free adjacent territory");
    }

    public LandUnit recruitSettler(
            long cityId,
            GameState gameState,
            WorldMap worldMap
    ) {

        RecruitmentCheck check =
                checkRecruitment(
                        cityId,
                        gameState,
                        worldMap
                );

        if (!check.allowed()) {
            throw new IllegalStateException(
                    check.reason()
            );
        }

        City city = gameState.getCityManager()
                .findCity(cityId)
                .orElseThrow();

        ResourceInventory inventory =
                city.getInventory();

        int previousPopulation =
                city.getPopulation();

        // Create the unit before spending resources.
        // An invalid spawn cannot charge the city.
        LandUnit settler =
                gameState.getLandUnitManager()
                        .createSettler(
                                city.getCivilizationId(),
                                check.spawnX(),
                                check.spawnY(),
                                worldMap,
                                gameState.getCivilizationManager()
                        );

        boolean woodRemoved = false;
        boolean foodRemoved = false;

        try {

            inventory.remove(
                    ResourceType.WOOD,
                    SettlerRecruitmentConfig.WOOD_COST
            );

            woodRemoved = true;

            inventory.remove(
                    ResourceType.FOOD,
                    SettlerRecruitmentConfig.FOOD_COST
            );

            foodRemoved = true;

            city.setPopulation(
                    previousPopulation
                            - SettlerRecruitmentConfig
                            .POPULATION_COST
            );

        } catch (RuntimeException exception) {

            // Roll back changes if payment fails.
            gameState.getLandUnitManager()
                    .removeUnit(settler.getId());

            if (foodRemoved) {
                inventory.add(
                        ResourceType.FOOD,
                        SettlerRecruitmentConfig.FOOD_COST
                );
            }

            if (woodRemoved) {
                inventory.add(
                        ResourceType.WOOD,
                        SettlerRecruitmentConfig.WOOD_COST
                );
            }

            city.setPopulation(previousPopulation);

            throw exception;
        }

        return settler;
    }

    private RecruitmentCheck blocked(String reason) {

        return new RecruitmentCheck(
                false,
                reason,
                -1,
                -1
        );
    }
}
