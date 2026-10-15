
package com.epochstrategy.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RiverNetwork {

    private final List<RiverSegment> segments =
            new ArrayList<>();

    private final boolean[][] riverTiles;

    private final Map<RiverSegment, RiverProperties> properties =
            new HashMap<>();

    public RiverNetwork(int width, int height) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Invalid river network dimensions"
            );
        }

        riverTiles = new boolean[width][height];
    }

    void addSegment(RiverSegment segment) {

        segments.add(segment);

        riverTiles[segment.fromX()][segment.fromY()] = true;
        riverTiles[segment.toX()][segment.toY()] = true;
    }

    public List<RiverSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public boolean hasRiverAt(int x, int y) {

        if (x < 0 || y < 0
                || x >= riverTiles.length
                || y >= riverTiles[0].length) {
            return false;
        }

        return riverTiles[x][y];
    }

    public record RiverSegment(
            int fromX,
            int fromY,
            int toX,
            int toY
    ) {
    }


    void setProperties(
            RiverSegment segment,
            RiverProperties riverProperties
    ) {

        if (!segments.contains(segment)) {
            throw new IllegalArgumentException(
                    "River segment does not exist"
            );
        }

        properties.put(segment, riverProperties);
    }

    public RiverProperties getProperties(
            RiverSegment segment
    ) {

        RiverProperties result = properties.get(segment);

        if (result == null) {
            throw new IllegalStateException(
                    "River properties have not been calculated"
            );
        }

        return result;
    }

    public Optional<RiverProperties> findPropertiesAt(
            int x,
            int y
    ) {

        if (!hasRiverAt(x, y)) {
            return Optional.empty();
        }

        RiverSegment incomingSegment = null;

        for (RiverSegment segment : segments) {

            // Prefer the segment flowing out of this tile
            if (segment.fromX() == x
                    && segment.fromY() == y) {

                return Optional.of(
                        getProperties(segment)
                );
            }

            // Remember incoming segments, e.g. at a river mouth
            if (segment.toX() == x
                    && segment.toY() == y) {

                if (incomingSegment == null
                        || getProperties(segment).flow()
                        > getProperties(incomingSegment).flow()) {

                    incomingSegment = segment;
                }
            }
        }

        if (incomingSegment != null) {
            return Optional.of(
                    getProperties(incomingSegment)
            );
        }

        return Optional.empty();
    }


}
