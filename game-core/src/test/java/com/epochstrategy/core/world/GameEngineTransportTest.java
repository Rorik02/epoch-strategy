
package com.epochstrategy.core.world;

import com.epochstrategy.core.engine.GameEngine;
import com.epochstrategy.core.engine.GameState;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTransportTest {

    @Test
    void shouldMoveAllRegisteredUnitsDuringOneTurn() {

        WorldMap map = new WorldMap(1, 8);

        RiverNetwork network = new RiverNetwork(1, 8);

        // Create flat terrain
        for (int y = 0; y < 8; y++) {

            map.setTile(
                    new Tile(
                            0,
                            y,
                            TerrainType.PLAINS,
                            1
                    )
            );
        }

        // Create a straight navigable river
        for (int y = 0; y < 7; y++) {

            RiverNetwork.RiverSegment segment =
                    new RiverNetwork.RiverSegment(
                            0, y,
                            0, y + 1
                    );

            network.addSegment(segment);

            network.setProperties(
                    segment,
                    new RiverProperties(
                            20,
                            0.35f,
                            2.0f,
                            RiverType.MAJOR_RIVER,
                            true
                    )
            );
        }

        List<RiverRouteFinder.Point> points =
                new ArrayList<>();

        for (int y = 0; y < 8; y++) {
            points.add(
                    new RiverRouteFinder.Point(0, y)
            );
        }

        RiverRouteFinder.RiverRoute route =
                new RiverRouteFinder.RiverRoute(
                        points,
                        7,
                        0
                );

        RiverTransportUnit smallBoat =
                new RiverTransportUnit(
                        1L,
                        RiverVesselType.SMALL_BOAT,
                        0,
                        0
                );

        RiverTransportUnit riverBoat =
                new RiverTransportUnit(
                        2L,
                        RiverVesselType.RIVER_BOAT,
                        0,
                        0
                );

        smallBoat.assignRoute(route);
        riverBoat.assignRoute(route);

        GameState state = new GameState();

        state.getRiverTransportManager()
                .registerUnit(smallBoat);

        state.getRiverTransportManager()
                .registerUnit(riverBoat);

        GameEngine engine = new GameEngine(
                state,
                map,
                network
        );

        // First turn
        GameEngine.TurnReport first = engine.endTurn();

        assertEquals(1, first.completedTurn());
        assertEquals(2, first.nextTurn());

        assertEquals(5, smallBoat.getY());
        assertEquals(4, riverBoat.getY());

        assertEquals(2, first.movements().size());

        // Second turn
        GameEngine.TurnReport second = engine.endTurn();

        assertEquals(7, smallBoat.getY());
        assertEquals(7, riverBoat.getY());

        assertTrue(
                second.movements().get(1L).arrived()
        );

        assertTrue(
                second.movements().get(2L).arrived()
        );

        assertEquals(3, state.getTurnNumber());
    }
}
