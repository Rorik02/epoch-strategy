
package com.epochstrategy.core;

import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.core.world.DemoMapFactory;


import com.epochstrategy.core.engine.GameEngine;
import com.epochstrategy.core.engine.GameState;

public class Main {

    public static void main(String[] args) {

        GameState state = new GameState();
        GameEngine engine = new GameEngine(state);

        for (int i = 0; i < 3; i++) {

            int year = state.getCalendar().getYear();

            String formattedYear = year < 0
                    ? Math.abs(year) + " BC"
                    : year + " AD";

            System.out.println(
                    "Turn: " + state.getTurnNumber()
                            + " | Year: " + formattedYear
                            + " | Era: " + state.getCalendar()
                            .getCurrentEra().name()
            );

            engine.endTurn();
        }

        WorldMap map = DemoMapFactory.createDemoMap();

        System.out.println(
                "Map size: "
                        + map.getWidth()
                        + " x "
                        + map.getHeight()
        );

        System.out.println(
                "Tile (7, 5): "
                        + map.getTile(7, 5).getTerrain()
        );

        System.out.println(
                "Land passable: "
                        + map.getTile(7, 5)
                        .isPassableForLandUnits()
        );

    }
}
