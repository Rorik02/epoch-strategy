
package com.epochstrategy.core.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterwayMovementServiceTest {

    @Test
    void movesThroughRiverLakeAndRiverAcrossTwoTurns() {

        WorldMap map = createMap(5, 3);

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

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                RiverVesselType.SMALL_BOAT
        );

        // First turn: 3 movement points
        var first = WaterwayMovementService.advanceTurn(
                graph,
                route,
                1,
                3
        );

        assertEquals(3, first.traveledSegments());
        assertEquals(3, first.usedMovementPoints());

        assertEquals(2, first.currentX());
        assertEquals(1, first.currentY());

        assertEquals(4, first.nextPointIndex());

        assertFalse(first.arrived());
        assertFalse(first.blocked());

        // Second turn: 4 movement points
        var second = WaterwayMovementService.advanceTurn(
                graph,
                route,
                first.nextPointIndex(),
                4
        );

        assertEquals(2, second.traveledSegments());
        assertEquals(4, second.usedMovementPoints());

        assertEquals(4, second.currentX());
        assertEquals(1, second.currentY());

        assertTrue(second.arrived());
        assertFalse(second.blocked());
        assertEquals(0, second.remainingSegments());
    }

    @Test
    void blocksMovementWithoutWaterwayConnection() {

        WorldMap map = createMap(2, 1);

        RiverNetwork network = new RiverNetwork(2, 1);

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                RiverVesselType.SMALL_BOAT
        );

        var route =
                new WaterwayFastestRouteFinder.WaterwayRoute(
                        List.of(
                                new RiverRouteFinder.Point(0, 0),
                                new RiverRouteFinder.Point(1, 0)
                        ),
                        1,
                        1,
                        0,
                        0
                );

        var movement =
                WaterwayMovementService.advanceTurn(
                        graph,
                        route,
                        1,
                        5
                );

        assertTrue(movement.blocked());
        assertFalse(movement.arrived());

        assertEquals(0, movement.traveledSegments());
        assertEquals(0, movement.currentX());
        assertEquals(0, movement.currentY());
    }

    @Test
    void waitsWhenMovementBudgetIsInsufficient() {

        WorldMap map = createMap(2, 1);

        RiverNetwork network = new RiverNetwork(2, 1);

        addRiver(network, 0, 0, 1, 0);

        var route =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        1, 0,
                        0, 0
                ).orElseThrow();

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                RiverVesselType.SMALL_BOAT
        );

        // Upstream movement costs 2 points
        var movement =
                WaterwayMovementService.advanceTurn(
                        graph,
                        route,
                        1,
                        1
                );

        assertEquals(0, movement.traveledSegments());
        assertEquals(0, movement.usedMovementPoints());

        assertEquals(1, movement.currentX());
        assertEquals(0, movement.currentY());

        assertFalse(movement.arrived());
        assertFalse(movement.blocked());
    }

    private static WorldMap createMap(
            int width,
            int height
    ) {

        WorldMap map = new WorldMap(width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                map.setTile(
                        new Tile(
                                x,
                                y,
                                TerrainType.FOREST,
                                1
                        )
                );
            }
        }

        return map;
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
