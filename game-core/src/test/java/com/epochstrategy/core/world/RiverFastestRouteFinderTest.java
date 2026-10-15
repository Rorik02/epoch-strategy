package com.epochstrategy.core.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiverFastestRouteFinderTest {


    @Test
    void choosesLongerDownstreamPathWhenItCostsLess() {

        WorldMap map = flatMap(6, 4);
        RiverNetwork network = new RiverNetwork(6, 4);

        // Shorter route: 3 segments upstream.
        // Total cost: 3 * 2 = 6.
        addRiver(network, 2, 1, 1, 1, 0.35f, 2f);
        addRiver(network, 3, 1, 2, 1, 0.35f, 2f);
        addRiver(network, 4, 1, 3, 1, 0.35f, 2f);

        // Longer route: 5 segments downstream.
        // Total cost: 5 * 1 = 5.
        addRiver(network, 1, 1, 1, 2, 0.35f, 2f);
        addRiver(network, 1, 2, 2, 2, 0.35f, 2f);
        addRiver(network, 2, 2, 3, 2, 0.35f, 2f);
        addRiver(network, 3, 2, 4, 2, 0.35f, 2f);
        addRiver(network, 4, 2, 4, 1, 0.35f, 2f);

        var fastest =
                RiverFastestRouteFinder.findFastestRoute(
                        map,
                        network,
                        RiverVesselType.SMALL_BOAT,
                        1, 1,
                        4, 1
                ).orElseThrow();

        assertEquals(5, fastest.movementCost());

        assertEquals(5, fastest.route().getSegmentCount());

        assertEquals(5, fastest.route().downstreamSegments());

        assertEquals(0, fastest.route().upstreamSegments());

        assertEquals(
                List.of(
                        new RiverRouteFinder.Point(1, 1),
                        new RiverRouteFinder.Point(1, 2),
                        new RiverRouteFinder.Point(2, 2),
                        new RiverRouteFinder.Point(3, 2),
                        new RiverRouteFinder.Point(4, 2),
                        new RiverRouteFinder.Point(4, 1)
                ),
                fastest.route().points()
        );
    }



    @Test
    void returnsEmptyWhenRiverIsTooShallowAndNarrow() {
        WorldMap map = flatMap(3, 2);
        RiverNetwork network = new RiverNetwork(3, 2);
        addRiver(network, 0, 0, 1, 0, 0.05f, 0.10f);
        addRiver(network, 1, 0, 2, 0, 0.05f, 0.10f);

        assertTrue(RiverFastestRouteFinder.findFastestRoute(
                map, network, RiverVesselType.SMALL_BOAT,
                0, 0, 2, 0
        ).isEmpty());
    }

    @Test
    void countsUpstreamAndDownstreamCostsCorrectly() {
        WorldMap map = flatMap(3, 2);
        RiverNetwork network = new RiverNetwork(3, 2);
        addRiver(network, 0, 0, 1, 0, 0.35f, 2f);
        addRiver(network, 1, 0, 2, 0, 0.35f, 2f);

        var downstream = RiverFastestRouteFinder.findFastestRoute(
                map, network, RiverVesselType.SMALL_BOAT,
                0, 0, 2, 0
        ).orElseThrow();

        var upstream = RiverFastestRouteFinder.findFastestRoute(
                map, network, RiverVesselType.SMALL_BOAT,
                2, 0, 0, 0
        ).orElseThrow();

        assertEquals(2, downstream.movementCost());
        assertEquals(0, downstream.route().upstreamSegments());
        assertEquals(4, upstream.movementCost());
        assertEquals(2, upstream.route().upstreamSegments());
    }

    @Test
    void returnsEmptyForDisconnectedRivers() {
        WorldMap map = flatMap(5, 3);
        RiverNetwork network = new RiverNetwork(5, 3);
        addRiver(network, 0, 0, 1, 0, 0.35f, 2f);
        addRiver(network, 3, 2, 4, 2, 0.35f, 2f);

        assertTrue(RiverFastestRouteFinder.findFastestRoute(
                map, network, RiverVesselType.SMALL_BOAT,
                0, 0, 4, 2
        ).isEmpty());
    }

    private static WorldMap flatMap(int width, int height) {
        WorldMap map = new WorldMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.setTile(new Tile(x, y, TerrainType.PLAINS, 1));
            }
        }
        return map;
    }

    private static void addRiver(
            RiverNetwork network,
            int fromX,
            int fromY,
            int toX,
            int toY,
            float width,
            float depth
    ) {
        RiverNetwork.RiverSegment segment =
                new RiverNetwork.RiverSegment(fromX, fromY, toX, toY);
        network.addSegment(segment);
        network.setProperties(segment,
                new RiverProperties(
                        20, width, depth, RiverType.MAJOR_RIVER,
                        width >= 0.24f && depth >= 1.40f
                ));
    }
}
