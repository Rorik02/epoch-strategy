package com.epochstrategy.core.engine;

import java.util.Objects;

public class GameEngine {

    private final GameState gameState;

    public GameEngine(GameState gameState) {
        this.gameState = Objects.requireNonNull(gameState);
    }

    public void endTurn() {
        gameState.advanceTurn();
    }

    public GameState getGameState() {
        return gameState;
    }
}
