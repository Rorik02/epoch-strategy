
package com.epochstrategy.core.engine;

public class GameState {

    private int turnNumber;
    private final GameCalendar calendar;

    public GameState() {
        this.turnNumber = 1;
        this.calendar = new GameCalendar();
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public GameCalendar getCalendar() {
        return calendar;
    }

    void advanceTurn() {
        calendar.advance();
        turnNumber++;
    }
}
