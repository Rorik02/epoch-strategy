
package com.epochstrategy.core.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterwayFastestRouteFinderTest {

    @Test
    void findsRouteFromRiverThroughLakeToAnotherRiver() {

        WorldMap map = createMap(5, 3);

        map.setTile(
                new Tile(2, 0, TerrainType.LAKE, 1)
        );

        map.setTile(
                new Tile(2, 1, TerrainType.LAKE, 1)
        );

        RiverNetwork network = new RiverNetwork(5, 3);

        // River A flows into the lake
        addRiver(network, 0, 0, 1, 0);
        addRiver(network, 1, 0, 2, 0);

        // River B also flows into the lake
        addRiver(network, 3, 1, 2, 1);
        addRiver(network, 4, 1, 3, 1);

        var result =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        0, 0,
                        4, 1
                ).orElseThrow();

        assertEquals(7, result.movementCost());
        assertEquals(5, result.getSegmentCount());

        assertEquals(2, result.downstreamSegments());
        assertEquals(2, result.upstreamSegments());
        assertEquals(1, result.lakeSegments());

        assertEquals(
                List.of(
                        new RiverRouteFinder.Point(0, 0),
                        new RiverRouteFinder.Point(1, 0),
                        new RiverRouteFinder.Point(2, 0),
                        new RiverRouteFinder.Point(2, 1),
                        new RiverRouteFinder.Point(3, 1),
                        new RiverRouteFinder.Point(4, 1)
                ),
                result.points()
        );
    }

    @Test
    void handlesRiverDirectionCorrectly() {

        WorldMap map = createMap(3, 1);

        RiverNetwork network = new RiverNetwork(3, 1);

        addRiver(network, 0, 0, 1, 0);
        addRiver(network, 1, 0, 2, 0);

        var downstream =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        0, 0,
                        2, 0
                ).orElseThrow();

        var upstream =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        2, 0,
                        0, 0
                ).orElseThrow();

        assertEquals(2, downstream.movementCost());
        assertEquals(4, upstream.movementCost());

        assertEquals(2, downstream.downstreamSegments());
        assertEquals(2, upstream.upstreamSegments());

        assertEquals(0, downstream.lakeSegments());
        assertEquals(0, upstream.lakeSegments());
    }

    @Test
    void doesNotCrossLandBetweenSeparateLakes() {

        WorldMap map = createMap(3, 1);

        map.setTile(
                new Tile(0, 0, TerrainType.LAKE, 1)
        );

        map.setTile(
                new Tile(2, 0, TerrainType.LAKE, 1)
        );

        RiverNetwork network = new RiverNetwork(3, 1);

        var route =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        0, 0,
                        2, 0
                );

        assertTrue(route.isEmpty());
    }

    @Test
    void doesNotConnectAdjacentIndependentRivers() {

        WorldMap map = createMap(2, 2);

        RiverNetwork network = new RiverNetwork(2, 2);

        addRiver(network, 0, 0, 0, 1);
        addRiver(network, 1, 0, 1, 1);

        var route =
                WaterwayFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        0, 0,
                        1, 0
                );

        assertTrue(route.isEmpty());
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
