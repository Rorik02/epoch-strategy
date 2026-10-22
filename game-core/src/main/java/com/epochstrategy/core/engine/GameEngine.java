
package com.epochstrategy.core.engine;

import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.core.world.RiverTransportService;
import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.core.city.CityPlacementService;
import com.epochstrategy.core.city.CityTurnService;
import com.epochstrategy.core.city.CityFoodService;
import com.epochstrategy.core.city.CityResourceProductionService;
import com.epochstrategy.core.territory.CityAutomaticExpansionService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class GameEngine {

    private final GameState gameState;
    private final WorldMap worldMap;
    private final RiverNetwork riverNetwork;

    // Compatibility with calendar-only simulations
    public GameEngine(GameState gameState) {
        this(gameState, null, null);
    }

    private final CityTurnService cityTurnService;

    private final CityAutomaticExpansionService automaticExpansionService =
            new CityAutomaticExpansionService();

    // Full simulation with world and river transport
    public GameEngine(
            GameState gameState,
            WorldMap worldMap,
            RiverNetwork riverNetwork
    ) {

        this.gameState =
                Objects.requireNonNull(gameState);

        this.cityTurnService = new CityTurnService(
                new CityPlacementService(6),
                this.gameState.getCityBuildingManager()
        );

        if ((worldMap == null) != (riverNetwork == null)) {
            throw new IllegalArgumentException(
                    "WorldMap and RiverNetwork must both be set"
            );
        }

        this.worldMap = worldMap;
        this.riverNetwork = riverNetwork;
    }




    public TurnReport endTurn() {

        int completedTurn =
                gameState.getTurnNumber();

        Map<Long, RiverTransportService.MovementResult>
                movements = Map.of();

        Map<Long, CityFoodService.FoodTurnResult>
                foodResults = Map.of();

        Map<Long, CityResourceProductionService.ResourceTurnResult>
                resourceResults = Map.of();

        if (worldMap != null) {

            // 1. Move all transport units.
            movements = gameState
                    .getRiverTransportManager()
                    .advanceTurn(
                            worldMap,
                            riverNetwork
                    );

            // 2. Update all city economies once.
            var economyResult =
                    cityTurnService.advanceEconomyTurn(
                            worldMap,
                            riverNetwork,
                            gameState.getCityManager()
                    );

            foodResults = economyResult.foodResults();
            resourceResults = economyResult.resourceResults();



        }


        gameState.getCityInfluenceManager()
                .advanceTurn(
                        gameState.getCityManager().getCities()
                );
        // 3. Save reports for the user interface.
        gameState.setLastCityFoodResults(foodResults);
        gameState.setLastCityResourceResults(resourceResults);




        // Automatic expansion requires a world map.
        // Calendar-only engine instances do not have one.
        if (worldMap != null) {

            var automaticExpansions =
                    automaticExpansionService.advanceTurn(
                            worldMap,
                            gameState.getCityManager(),
                            gameState.getTerritoryManager(),
                            gameState.getCityInfluenceManager(),
                            gameState.getCityExpansionManager()
                    );

            for (var expansion : automaticExpansions) {

                System.out.println(
                        "AUTO EXPANSION | City: "
                                + expansion.cityId()
                                + " | Tile: "
                                + expansion.x()
                                + ", "
                                + expansion.y()
                );
            }
        }


        gameState.getLandUnitManager()
                .restoreAllMovementPoints();


        // 4. Advance the game calendar.
        gameState.advanceTurn();

        return new TurnReport(
                completedTurn,
                gameState.getTurnNumber(),
                movements
        );
    }




    public GameState getGameState() {
        return gameState;
    }

    public record TurnReport(
            int completedTurn,
            int nextTurn,
            Map<Long, RiverTransportService.MovementResult> movements
    ) {

        public TurnReport {
            movements = Collections.unmodifiableMap(
                    new LinkedHashMap<>(
                            Objects.requireNonNull(movements)
                    )
            );
        }
    }
}
