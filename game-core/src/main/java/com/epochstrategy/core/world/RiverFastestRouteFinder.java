package com.epochstrategy.core.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Finds the navigable river route with the lowest movement-point cost.
 * Going downstream costs 1 point; going upstream costs 2 points.
 * RiverSegment direction is assumed to point downstream.
 */
public final class RiverFastestRouteFinder {

    private static final int DOWNSTREAM_COST = 1;
    private static final int UPSTREAM_COST = 2;

    private RiverFastestRouteFinder() {
    }

    public record FastestRoute(
            RiverRouteFinder.RiverRoute route,
            int movementCost
    ) {
        public FastestRoute {
            Objects.requireNonNull(route);
            if (movementCost < 0) {
                throw new IllegalArgumentException("Movement cost cannot be negative");
            }
        }
    }

    private record Edge(
            RiverRouteFinder.Point destination,
            int cost,
            boolean downstream
    ) {
    }

    private record QueueEntry(
            RiverRouteFinder.Point point,
            int totalCost,
            int segmentCount
    ) {
    }

    private record Previous(
            RiverRouteFinder.Point point,
            boolean downstream
    ) {
    }

    public static Optional<FastestRoute> findFastestRoute(
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

        if (!network.hasRiverAt(startX, startY)
                || !network.hasRiverAt(endX, endY)) {
            return Optional.empty();
        }

        RiverRouteFinder.Point start =
                new RiverRouteFinder.Point(startX, startY);
        RiverRouteFinder.Point goal =
                new RiverRouteFinder.Point(endX, endY);

        if (start.equals(goal)) {
            RiverRouteFinder.RiverRoute emptyRoute =
                    new RiverRouteFinder.RiverRoute(List.of(start), 0, 0);
            return Optional.of(new FastestRoute(emptyRoute, 0));
        }

        Map<RiverRouteFinder.Point, List<Edge>> graph = new HashMap<>();

        for (RiverNetwork.RiverSegment segment : network.getSegments()) {
            if (!RiverNavigationService.canNavigate(
                    map, network, segment, vesselType)) {
                continue;
            }

            RiverRouteFinder.Point from =
                    new RiverRouteFinder.Point(segment.fromX(), segment.fromY());
            RiverRouteFinder.Point to =
                    new RiverRouteFinder.Point(segment.toX(), segment.toY());

            // In the segment's direction: downstream.
            graph.computeIfAbsent(from, unused -> new ArrayList<>())
                    .add(new Edge(to, DOWNSTREAM_COST, true));

            // In reverse: upstream.
            graph.computeIfAbsent(to, unused -> new ArrayList<>())
                    .add(new Edge(from, UPSTREAM_COST, false));
        }

        Comparator<QueueEntry> byBestCost = Comparator
                .comparingInt(QueueEntry::totalCost)
                .thenComparingInt(QueueEntry::segmentCount)
                .thenComparingInt(entry -> entry.point().x())
                .thenComparingInt(entry -> entry.point().y());

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(byBestCost);
        Map<RiverRouteFinder.Point, Integer> distances = new HashMap<>();
        Map<RiverRouteFinder.Point, Integer> segmentCounts = new HashMap<>();
        Map<RiverRouteFinder.Point, Previous> previous = new HashMap<>();

        distances.put(start, 0);
        segmentCounts.put(start, 0);
        queue.add(new QueueEntry(start, 0, 0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();

            if (current.totalCost() != distances.getOrDefault(
                    current.point(), Integer.MAX_VALUE)
                    || current.segmentCount() != segmentCounts.getOrDefault(
                    current.point(), Integer.MAX_VALUE)) {
                continue;
            }

            if (current.point().equals(goal)) {
                break;
            }

            for (Edge edge : graph.getOrDefault(current.point(), List.of())) {
                int newCost = current.totalCost() + edge.cost();
                int newSegments = current.segmentCount() + 1;
                int oldCost = distances.getOrDefault(
                        edge.destination(), Integer.MAX_VALUE);
                int oldSegments = segmentCounts.getOrDefault(
                        edge.destination(), Integer.MAX_VALUE);

                if (newCost < oldCost
                        || (newCost == oldCost && newSegments < oldSegments)) {
                    distances.put(edge.destination(), newCost);
                    segmentCounts.put(edge.destination(), newSegments);
                    previous.put(edge.destination(),
                            new Previous(current.point(), edge.downstream()));
                    queue.add(new QueueEntry(
                            edge.destination(), newCost, newSegments));
                }
            }
        }

        if (!distances.containsKey(goal)) {
            return Optional.empty();
        }

        List<RiverRouteFinder.Point> reversePoints = new ArrayList<>();
        int downstreamSegments = 0;
        int upstreamSegments = 0;
        RiverRouteFinder.Point current = goal;
        reversePoints.add(current);

        while (!current.equals(start)) {
            Previous predecessor = previous.get(current);
            if (predecessor == null) {
                throw new IllegalStateException("Incomplete river path");
            }

            if (predecessor.downstream()) {
                downstreamSegments++;
            } else {
                upstreamSegments++;
            }

            current = predecessor.point();
            reversePoints.add(current);
        }

        java.util.Collections.reverse(reversePoints);

        RiverRouteFinder.RiverRoute route =
                new RiverRouteFinder.RiverRoute(
                        List.copyOf(reversePoints),
                        downstreamSegments,
                        upstreamSegments
                );

        return Optional.of(new FastestRoute(route, distances.get(goal)));
    }
}
