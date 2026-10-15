
package com.epochstrategy.core.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiverTransportManagerWaterwayTest {

    @Test
    void transportUnitCrossesLakeAcrossMultipleTurns() {

        WorldMap map = new WorldMap(5, 3);

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 3; y++) {
                map.setTile(
                        new Tile(
                                x, y,
                                TerrainType.FOREST,
                                1
                        )
                );
            }
        }

        map.setTile(
                new Tile(2, 0, TerrainType.LAKE, 1)
        );

        map.setTile(
                new Tile(2, 1, TerrainType.LAKE, 1)
        );

        RiverNetwork network = new RiverNetwork(5, 3);

        addRiver(network, 0, 0, 1, 0);
        addRiver(network, 1, 0, 2, 0);

        addRiver(network, 3, 1, 2, 1);
        addRiver(network, 4, 1, 3, 1);

        var route =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        0, 0,
                        4, 1
                ).orElseThrow();

        assertEquals(1, route.lakeSegments());

        RiverTransportUnit unit =
                new RiverTransportUnit(
                        1L,
                        RiverVesselType.SMALL_BOAT,
                        0,
                        0
                );

        unit.assignRoute(route);

        RiverTransportManager manager =
                new RiverTransportManager();

        manager.registerUnit(unit);

        // SMALL_BOAT has 5 movement points per turn.
        var firstTurn = manager.advanceTurn(
                map,
                network
        ).get(1L);

        assertEquals(4, firstTurn.traveledSegments());

        assertEquals(3, unit.getX());
        assertEquals(1, unit.getY());

        assertFalse(firstTurn.arrived());
        assertFalse(firstTurn.blocked());

        assertEquals(1, unit.getRemainingSegments());

        // Second turn completes the remaining upstream edge.
        var secondTurn = manager.advanceTurn(
                map,
                network
        ).get(1L);

        assertEquals(1, secondTurn.traveledSegments());

        assertEquals(4, unit.getX());
        assertEquals(1, unit.getY());

        assertTrue(secondTurn.arrived());
        assertFalse(secondTurn.blocked());

        assertFalse(unit.hasActiveRoute());
        assertEquals(0, unit.getRemainingSegments());
    }

    private static void addRiver(
            RiverNetwork network,
            int fromX,
            int fromY,
            int toX,
            int toY
    ) {

        var segment =
                new RiverNetwork.RiverSegment(
                        fromX,
                        fromY,
                        toX,
                        toY
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
}
