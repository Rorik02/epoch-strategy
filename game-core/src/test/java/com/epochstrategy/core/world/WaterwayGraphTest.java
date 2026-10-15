
package com.epochstrategy.core.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaterwayGraphTest {

    @Test
    void connectsTwoRiversThroughLake() {

        WorldMap map = createMap(5, 3);

        // Two connected lake tiles
        map.setTile(
                new Tile(2, 0, TerrainType.LAKE, 1)
        );

        map.setTile(
                new Tile(2, 1, TerrainType.LAKE, 1)
        );

        RiverNetwork network = new RiverNetwork(5, 3);

        // First river flows into the lake
        addRiver(network, 0, 0, 1, 0);
        addRiver(network, 1, 0, 2, 0);

        // Second river flows into the lake
        addRiver(network, 3, 1, 2, 1);
        addRiver(network, 4, 1, 3, 1);

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                RiverVesselType.SMALL_BOAT
        );

        var riverA = new RiverRouteFinder.Point(1, 0);
        var lakeA = new RiverRouteFinder.Point(2, 0);
        var lakeB = new RiverRouteFinder.Point(2, 1);
        var riverB = new RiverRouteFinder.Point(3, 1);

        // River A -> lake
        assertTrue(graph.canTravel(riverA, lakeA));

        // Lake -> adjacent lake tile
        assertTrue(graph.canTravel(lakeA, lakeB));

        // Lake -> River B (upstream)
        assertTrue(graph.canTravel(lakeB, riverB));

        assertEquals(
                1,
                graph.findConnection(lakeA, lakeB)
                        .orElseThrow()
                        .movementCost()
        );

        assertEquals(
                2,
                graph.findConnection(lakeB, riverB)
                        .orElseThrow()
                        .movementCost()
        );
    }

    @Test
    void doesNotConnectAdjacentIndependentRivers() {

        WorldMap map = createMap(2, 2);

        RiverNetwork network = new RiverNetwork(2, 2);

        addRiver(network, 0, 0, 0, 1);
        addRiver(network, 1, 0, 1, 1);

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                RiverVesselType.SMALL_BOAT
        );

        var left = new RiverRouteFinder.Point(0, 0);
        var right = new RiverRouteFinder.Point(1, 0);

        assertFalse(graph.canTravel(left, right));

        assertTrue(
                graph.canTravel(
                        left,
                        new RiverRouteFinder.Point(0, 1)
                )
        );
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

        var segment = new RiverNetwork.RiverSegment(
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
