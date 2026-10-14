
package com.epochstrategy.core.engine;

import com.epochstrategy.core.technology.Era;

public class GameCalendar {

    private int year;
    private Era currentEra;

    public GameCalendar() {
        this.year = -3000;
        this.currentEra = Era.ANCIENT;
    }

    public void advance() {
        int nextYear = year + currentEra.getYearsPerTurn();

        // W kalendarzu historycznym nie ma roku zerowego
        if (year < 0 && nextYear >= 0) {
            nextYear++;
        }

        year = nextYear;
    }

    public int getYear() {
        return year;
    }

    public Era getCurrentEra() {
        return currentEra;
    }

    public String getFormattedYear() {
        if (year < 0) {
            return Math.abs(year) + " p.n.e.";
        }

        return year + " n.e.";
    }
}
