
package com.epochstrategy.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RiverNetwork {

    private final List<RiverSegment> segments =
            new ArrayList<>();

    private final boolean[][] riverTiles;

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
}
