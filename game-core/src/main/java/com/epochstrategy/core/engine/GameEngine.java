
package com.epochstrategy.core.engine;

import com.epochstrategy.core.world.RiverNetwork;
import com.epochstrategy.core.world.RiverTransportService;
import com.epochstrategy.core.world.WorldMap;

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

    // Full simulation with world and river transport
    public GameEngine(
            GameState gameState,
            WorldMap worldMap,
            RiverNetwork riverNetwork
    ) {

        this.gameState =
                Objects.requireNonNull(gameState);

        if ((worldMap == null) != (riverNetwork == null)) {
            throw new IllegalArgumentException(
                    "WorldMap and RiverNetwork must both be set"
            );
        }

        this.worldMap = worldMap;
        this.riverNetwork = riverNetwork;
    }

    public TurnReport endTurn() {

        int completedTurn = gameState.getTurnNumber();

        Map<Long, RiverTransportService.MovementResult>
                movements = Map.of();

        if (worldMap != null) {

            movements = gameState
                    .getRiverTransportManager()
                    .advanceTurn(worldMap, riverNetwork);
        }

        // Advance calendar after simulation updates
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
