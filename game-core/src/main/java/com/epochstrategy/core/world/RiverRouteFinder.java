
package com.epochstrategy.core.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class RiverRouteFinder {

    private RiverRouteFinder() {
    }

    public static Optional<RiverRoute> findRoute(
            WorldMap map,
            RiverNetwork network,
            RiverVesselType vesselType,
            int startX,
            int startY,
            int targetX,
            int targetY
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(vesselType, "vesselType");

        if (!map.isInsideMap(startX, startY)
                || !map.isInsideMap(targetX, targetY)) {
            return Optional.empty();
        }

        if (!network.hasRiverAt(startX, startY)
                || !network.hasRiverAt(targetX, targetY)) {
            return Optional.empty();
        }

        Map<Point, List<Connection>> graph =
                new HashMap<>();

        // Build connections available to the vessel
        for (RiverNetwork.RiverSegment segment
                : network.getSegments()) {

            if (!RiverNavigationService.canNavigate(
                    map, network, segment, vesselType)) {
                continue;
            }

            Point from = new Point(
                    segment.fromX(),
                    segment.fromY()
            );

            Point to = new Point(
                    segment.toX(),
                    segment.toY()
            );

            // Travel downstream
            graph.computeIfAbsent(
                    from, key -> new ArrayList<>()
            ).add(new Connection(to, true));

            // Travel upstream
            graph.computeIfAbsent(
                    to, key -> new ArrayList<>()
            ).add(new Connection(from, false));
        }

        Point start = new Point(startX, startY);
        Point target = new Point(targetX, targetY);

        if (!graph.containsKey(start)
                || !graph.containsKey(target)) {
            return Optional.empty();
        }

        ArrayDeque<Point> queue = new ArrayDeque<>();
        Set<Point> visited = new HashSet<>();

        Map<Point, Previous> previous =
                new HashMap<>();

        queue.add(start);
        visited.add(start);

        // Breadth-first search
        while (!queue.isEmpty()
                && !visited.contains(target)) {

            Point current = queue.removeFirst();

            for (Connection connection :
                    graph.getOrDefault(current, List.of())) {

                Point next = connection.destination();

                if (visited.add(next)) {

                    previous.put(
                            next,
                            new Previous(
                                    current,
                                    connection.downstream()
                            )
                    );

                    queue.addLast(next);
                }
            }
        }

        if (!visited.contains(target)) {
            return Optional.empty();
        }

        // Reconstruct the route
        List<Point> routePoints = new ArrayList<>();

        int downstreamSegments = 0;
        int upstreamSegments = 0;

        Point current = target;

        while (!current.equals(start)) {

            routePoints.add(current);

            Previous step = previous.get(current);

            if (step.downstream()) {
                downstreamSegments++;
            } else {
                upstreamSegments++;
            }

            current = step.point();
        }

        routePoints.add(start);
        Collections.reverse(routePoints);

        return Optional.of(
                new RiverRoute(
                        routePoints,
                        downstreamSegments,
                        upstreamSegments
                )
        );
    }

    public record Point(int x, int y) {
    }

    public record RiverRoute(
            List<Point> points,
            int downstreamSegments,
            int upstreamSegments
    ) {

        public RiverRoute {
            points = List.copyOf(points);
        }

        public int getSegmentCount() {
            return points.size() - 1;
        }
    }

    private record Connection(
            Point destination,
            boolean downstream
    ) {
    }

    private record Previous(
            Point point,
            boolean downstream
    ) {
    }
}
