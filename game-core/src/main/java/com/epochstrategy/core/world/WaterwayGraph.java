
package com.epochstrategy.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class WaterwayGraph {

    private static final int DOWNSTREAM_COST = 1;
    private static final int UPSTREAM_COST = 2;
    private static final int LAKE_COST = 1;

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    public enum MovementType {
        RIVER_DOWNSTREAM,
        RIVER_UPSTREAM,
        LAKE
    }

    public record Edge(
            RiverRouteFinder.Point from,
            RiverRouteFinder.Point to,
            int movementCost,
            MovementType type
    ) {
    }

    private final Map<
            RiverRouteFinder.Point,
            List<Edge>
            > connections;

    private WaterwayGraph(
            Map<RiverRouteFinder.Point, List<Edge>> connections
    ) {

        Map<RiverRouteFinder.Point, List<Edge>> copy =
                new HashMap<>();

        for (var entry : connections.entrySet()) {
            copy.put(
                    entry.getKey(),
                    List.copyOf(entry.getValue())
            );
        }

        this.connections =
                Collections.unmodifiableMap(copy);
    }

    public static WaterwayGraph build(
            WorldMap map,
            RiverNetwork network,
            RiverVesselType vesselType
    ) {

        Objects.requireNonNull(map);
        Objects.requireNonNull(network);
        Objects.requireNonNull(vesselType);

        Map<RiverRouteFinder.Point, List<Edge>> graph =
                new HashMap<>();

        // 1. Add navigable river connections
        for (RiverNetwork.RiverSegment segment
                : network.getSegments()) {

            if (!RiverNavigationService.canNavigate(
                    map,
                    network,
                    segment,
                    vesselType
            )) {
                continue;
            }

            var from = new RiverRouteFinder.Point(
                    segment.fromX(),
                    segment.fromY()
            );

            var to = new RiverRouteFinder.Point(
                    segment.toX(),
                    segment.toY()
            );

            // Traveling with the current
            addConnection(
                    graph,
                    from,
                    to,
                    DOWNSTREAM_COST,
                    MovementType.RIVER_DOWNSTREAM
            );

            // Traveling against the current
            addConnection(
                    graph,
                    to,
                    from,
                    UPSTREAM_COST,
                    MovementType.RIVER_UPSTREAM
            );
        }

        // 2. Connect adjacent lake tiles
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                Tile tile = map.getTile(x, y);

                if (tile.getTerrain() != TerrainType.LAKE) {
                    continue;
                }

                var from = new RiverRouteFinder.Point(x, y);

                for (int[] direction : DIRECTIONS) {

                    int nx = x + direction[0];
                    int ny = y + direction[1];

                    if (!map.isInsideMap(nx, ny)) {
                        continue;
                    }

                    Tile neighbor = map.getTile(nx, ny);

                    if (neighbor.getTerrain()
                            != TerrainType.LAKE) {
                        continue;
                    }

                    var to = new RiverRouteFinder.Point(
                            nx,
                            ny
                    );

                    addConnection(
                            graph,
                            from,
                            to,
                            LAKE_COST,
                            MovementType.LAKE
                    );
                }
            }
        }

        return new WaterwayGraph(graph);
    }

    private static void addConnection(
            Map<RiverRouteFinder.Point, List<Edge>> graph,
            RiverRouteFinder.Point from,
            RiverRouteFinder.Point to,
            int movementCost,
            MovementType type
    ) {

        Edge edge = new Edge(
                from,
                to,
                movementCost,
                type
        );

        graph.computeIfAbsent(
                from,
                unused -> new ArrayList<>()
        ).add(edge);
    }

    public List<Edge> getConnections(
            RiverRouteFinder.Point point
    ) {

        return connections.getOrDefault(
                point,
                List.of()
        );
    }

    public Optional<Edge> findConnection(
            RiverRouteFinder.Point from,
            RiverRouteFinder.Point to
    ) {

        return getConnections(from)
                .stream()
                .filter(edge -> edge.to().equals(to))
                .findFirst();
    }

    public boolean canTravel(
            RiverRouteFinder.Point from,
            RiverRouteFinder.Point to
    ) {

        return findConnection(from, to).isPresent();
    }

    public int getNavigableTileCount() {
        return connections.size();
    }

    public int getConnectionCount() {

        int total = 0;

        for (List<Edge> edges : connections.values()) {
            total += edges.size();
        }

        return total;
    }
}
