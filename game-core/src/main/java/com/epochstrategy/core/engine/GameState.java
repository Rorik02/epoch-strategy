
package com.epochstrategy.core.engine;

import com.epochstrategy.core.world.RiverTransportManager;

public class GameState {

    private int turnNumber;
    private final GameCalendar calendar;

    private final RiverTransportManager riverTransportManager;

    public GameState() {
        this.turnNumber = 1;
        this.calendar = new GameCalendar();
        this.riverTransportManager =
                new RiverTransportManager();
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public GameCalendar getCalendar() {
        return calendar;
    }

    public RiverTransportManager getRiverTransportManager() {
        return riverTransportManager;
    }

    void advanceTurn() {
        calendar.advance();
        turnNumber++;
    }
}
