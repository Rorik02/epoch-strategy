
package com.epochstrategy.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

public final class WaterwayFastestRouteFinder {

    private WaterwayFastestRouteFinder() {
    }

    public record WaterwayRoute(
            List<RiverRouteFinder.Point> points,
            int movementCost,
            int downstreamSegments,
            int upstreamSegments,
            int lakeSegments
    ) {

        public WaterwayRoute {

            points = List.copyOf(
                    Objects.requireNonNull(points)
            );

            if (points.isEmpty()) {
                throw new IllegalArgumentException(
                        "Route cannot be empty"
                );
            }

            if (movementCost < 0
                    || downstreamSegments < 0
                    || upstreamSegments < 0
                    || lakeSegments < 0) {
                throw new IllegalArgumentException(
                        "Route values cannot be negative"
                );
            }

            int totalSegments =
                    downstreamSegments
                            + upstreamSegments
                            + lakeSegments;

            if (totalSegments != points.size() - 1) {
                throw new IllegalArgumentException(
                        "Route segment counts do not match"
                );
            }
        }

        public int getSegmentCount() {
            return points.size() - 1;
        }
    }

    private record QueueEntry(
            RiverRouteFinder.Point point,
            int cost,
            int segments
    ) {
    }

    private record Previous(
            RiverRouteFinder.Point point,
            WaterwayGraph.Edge edge
    ) {
    }

    public static Optional<WaterwayRoute> findFastestRoute(
            WorldMap map,
            RiverNetwork network,
            RiverVesselType vesselType,
            int startX,
            int startY,
            int endX,
            int endY
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(vesselType, "vesselType");

        if (!map.isInsideMap(startX, startY)
                || !map.isInsideMap(endX, endY)) {
            return Optional.empty();
        }

        if (!isWaterwayPoint(map, network, startX, startY)
                || !isWaterwayPoint(map, network, endX, endY)) {
            return Optional.empty();
        }

        var start = new RiverRouteFinder.Point(
                startX, startY
        );

        var goal = new RiverRouteFinder.Point(
                endX, endY
        );

        if (start.equals(goal)) {

            return Optional.of(
                    new WaterwayRoute(
                            List.of(start),
                            0,
                            0,
                            0,
                            0
                    )
            );
        }

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                vesselType
        );

        return findInGraph(graph, start, goal);
    }

    private static boolean isWaterwayPoint(
            WorldMap map,
            RiverNetwork network,
            int x,
            int y
    ) {

        return map.getTile(x, y).getTerrain()
                == TerrainType.LAKE
                || network.hasRiverAt(x, y);
    }

    private static Optional<WaterwayRoute> findInGraph(
            WaterwayGraph graph,
            RiverRouteFinder.Point start,
            RiverRouteFinder.Point goal
    ) {

        Comparator<QueueEntry> comparator =
                Comparator.comparingInt(QueueEntry::cost)
                        .thenComparingInt(QueueEntry::segments)
                        .thenComparingInt(
                                entry -> entry.point().x()
                        )
                        .thenComparingInt(
                                entry -> entry.point().y()
                        );

        PriorityQueue<QueueEntry> queue =
                new PriorityQueue<>(comparator);

        Map<RiverRouteFinder.Point, Integer> costs =
                new HashMap<>();

        Map<RiverRouteFinder.Point, Integer> segments =
                new HashMap<>();

        Map<RiverRouteFinder.Point, Previous> previous =
                new HashMap<>();

        costs.put(start, 0);
        segments.put(start, 0);

        queue.add(new QueueEntry(start, 0, 0));

        while (!queue.isEmpty()) {

            QueueEntry current = queue.poll();

            int bestCost = costs.getOrDefault(
                    current.point(),
                    Integer.MAX_VALUE
            );

            int bestSegments = segments.getOrDefault(
                    current.point(),
                    Integer.MAX_VALUE
            );

            // Ignore outdated queue entries
            if (current.cost() != bestCost
                    || current.segments() != bestSegments) {
                continue;
            }

            if (current.point().equals(goal)) {
                break;
            }

            for (WaterwayGraph.Edge edge :
                    graph.getConnections(current.point())) {

                if (edge.movementCost() <= 0) {
                    throw new IllegalStateException(
                            "Waterway edge cost must be positive"
                    );
                }

                long candidateCost =
                        (long) current.cost()
                                + edge.movementCost();

                if (candidateCost > Integer.MAX_VALUE) {
                    continue;
                }

                int newCost = (int) candidateCost;
                int newSegments = current.segments() + 1;

                int oldCost = costs.getOrDefault(
                        edge.to(),
                        Integer.MAX_VALUE
                );

                int oldSegments = segments.getOrDefault(
                        edge.to(),
                        Integer.MAX_VALUE
                );

                boolean better =
                        newCost < oldCost
                                || (newCost == oldCost
                                && newSegments < oldSegments);

                if (!better) {
                    continue;
                }

                costs.put(edge.to(), newCost);
                segments.put(edge.to(), newSegments);

                previous.put(
                        edge.to(),
                        new Previous(
                                current.point(),
                                edge
                        )
                );

                queue.add(
                        new QueueEntry(
                                edge.to(),
                                newCost,
                                newSegments
                        )
                );
            }
        }

        if (!costs.containsKey(goal)) {
            return Optional.empty();
        }

        return Optional.of(
                reconstructRoute(
                        start,
                        goal,
                        costs.get(goal),
                        previous
                )
        );
    }

    private static WaterwayRoute reconstructRoute(
            RiverRouteFinder.Point start,
            RiverRouteFinder.Point goal,
            int movementCost,
            Map<RiverRouteFinder.Point, Previous> previous
    ) {

        List<RiverRouteFinder.Point> reversed =
                new ArrayList<>();

        int downstream = 0;
        int upstream = 0;
        int lake = 0;

        RiverRouteFinder.Point current = goal;

        reversed.add(current);

        while (!current.equals(start)) {

            Previous step = previous.get(current);

            if (step == null) {
                throw new IllegalStateException(
                        "Incomplete waterway route"
                );
            }

            switch (step.edge().type()) {

                case RIVER_DOWNSTREAM -> downstream++;

                case RIVER_UPSTREAM -> upstream++;

                case LAKE -> lake++;
            }

            current = step.point();

            reversed.add(current);
        }

        Collections.reverse(reversed);

        return new WaterwayRoute(
                reversed,
                movementCost,
                downstream,
                upstream,
                lake
        );
    }
}
